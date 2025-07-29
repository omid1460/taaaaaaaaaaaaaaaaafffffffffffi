package com.voiceclone.api

import com.voiceclone.core.VoiceCloneEngine
import com.voiceclone.core.CloneRequest
import com.voiceclone.core.VoiceProfile
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import mu.KotlinLogging
import java.io.File
import java.util.*

private val logger = KotlinLogging.logger {}

@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class RecordingRequest(
    val profileName: String,
    val durationMinutes: Int,
    val language: String = "fa"
)

@Serializable
data class RecordingStatus(
    val isRecording: Boolean,
    val profileId: String? = null,
    val elapsedSeconds: Int = 0,
    val volumeLevel: Float = 0.0f
)

@Serializable
data class SynthesisRequest(
    val text: String,
    val profileId: String,
    val language: String = "auto",
    val speed: Float = 1.0f,
    val pitch: Float = 1.0f,
    val emotion: String = "neutral"
)

@Serializable
data class SynthesisResponse(
    val audioUrl: String,
    val duration: Double,
    val language: String,
    val characterCount: Int
)

class VoiceCloneServer(private val engine: VoiceCloneEngine) {
    private val server = embeddedServer(Netty, port = 8080) {
        install(ContentNegotiation) {
            json()
        }
        
        install(CORS) {
            allowMethod(HttpMethod.Options)
            allowMethod(HttpMethod.Put)
            allowMethod(HttpMethod.Delete)
            allowMethod(HttpMethod.Patch)
            allowHeader(HttpHeaders.Authorization)
            allowHeader(HttpHeaders.ContentType)
            allowCredentials = true
            anyHost()
        }
        
        routing {
            // Health check endpoint
            get("/health") {
                call.respond(ApiResponse(success = true, data = "Voice Clone API is running"))
            }
            
            // Get API information
            get("/api/info") {
                val info = mapOf(
                    "version" to "1.0.0",
                    "name" to "Voice Clone API",
                    "supportedLanguages" to engine.getSupportedLanguages(),
                    "maxTextLength" to 50000,
                    "maxRecordingDuration" to 20
                )
                call.respond(ApiResponse(success = true, data = info))
            }
            
            // Voice profile management
            route("/api/profiles") {
                // Get all voice profiles
                get {
                    try {
                        val profiles = engine.getVoiceProfiles()
                        call.respond(ApiResponse(success = true, data = profiles))
                    } catch (e: Exception) {
                        logger.error("Failed to get voice profiles", e)
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Nothing>(success = false, error = e.message)
                        )
                    }
                }
                
                // Create new voice profile by recording
                post("/record") {
                    try {
                        val request = call.receive<RecordingRequest>()
                        
                        if (request.durationMinutes < 1 || request.durationMinutes > 20) {
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ApiResponse<Nothing>(success = false, error = "Duration must be between 1 and 20 minutes")
                            )
                            return@post
                        }
                        
                        val profile = engine.recordVoiceSample(
                            profileName = request.profileName,
                            durationMinutes = request.durationMinutes,
                            language = request.language
                        )
                        
                        call.respond(ApiResponse(success = true, data = profile))
                        
                    } catch (e: Exception) {
                        logger.error("Failed to record voice sample", e)
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Nothing>(success = false, error = e.message)
                        )
                    }
                }
                
                // Upload audio file for voice profile
                post("/upload") {
                    try {
                        val multipart = call.receiveMultipart()
                        var profileName = ""
                        var language = "fa"
                        var audioFile: File? = null
                        
                        multipart.forEachPart { part ->
                            when (part) {
                                is PartData.FormItem -> {
                                    when (part.name) {
                                        "profileName" -> profileName = part.value
                                        "language" -> language = part.value
                                    }
                                }
                                is PartData.FileItem -> {
                                    if (part.name == "audio") {
                                        val fileName = "upload_${UUID.randomUUID()}.wav"
                                        audioFile = File("temp", fileName)
                                        audioFile!!.parentFile.mkdirs()
                                        part.streamProvider().use { input ->
                                            audioFile!!.outputStream().use { output ->
                                                input.copyTo(output)
                                            }
                                        }
                                    }
                                }
                                else -> {}
                            }
                            part.dispose()
                        }
                        
                        if (audioFile == null || profileName.isEmpty()) {
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ApiResponse<Nothing>(success = false, error = "Missing audio file or profile name")
                            )
                            return@post
                        }
                        
                        // Create profile from uploaded audio
                        val profileId = UUID.randomUUID().toString()
                        val modelPath = engine.voiceModel.trainVoiceModel(audioFile!!, profileId, language)
                        
                        val profile = VoiceProfile(
                            id = profileId,
                            name = profileName,
                            language = language,
                            modelPath = modelPath,
                            createdAt = System.currentTimeMillis(),
                            duration = 0 // Unknown duration for uploaded files
                        )
                        
                        // Clean up temporary file
                        audioFile!!.delete()
                        
                        call.respond(ApiResponse(success = true, data = profile))
                        
                    } catch (e: Exception) {
                        logger.error("Failed to upload voice sample", e)
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Nothing>(success = false, error = e.message)
                        )
                    }
                }
                
                // Delete voice profile
                delete("/{profileId}") {
                    try {
                        val profileId = call.parameters["profileId"]
                        if (profileId == null) {
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ApiResponse<Nothing>(success = false, error = "Profile ID is required")
                            )
                            return@delete
                        }
                        
                        val deleted = engine.deleteVoiceProfile(profileId)
                        if (deleted) {
                            call.respond(ApiResponse(success = true, data = "Profile deleted successfully"))
                        } else {
                            call.respond(
                                HttpStatusCode.NotFound,
                                ApiResponse<Nothing>(success = false, error = "Profile not found")
                            )
                        }
                        
                    } catch (e: Exception) {
                        logger.error("Failed to delete voice profile", e)
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Nothing>(success = false, error = e.message)
                        )
                    }
                }
            }
            
            // Speech synthesis
            route("/api/synthesize") {
                // Generate speech from text
                post {
                    try {
                        val request = call.receive<SynthesisRequest>()
                        
                        if (request.text.length > 50000) {
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ApiResponse<Nothing>(success = false, error = "Text exceeds maximum length of 50,000 characters")
                            )
                            return@post
                        }
                        
                        val cloneRequest = CloneRequest(
                            text = request.text,
                            profileId = request.profileId,
                            language = request.language,
                            speed = request.speed,
                            pitch = request.pitch,
                            emotion = request.emotion
                        )
                        
                        val audioFile = engine.cloneVoice(cloneRequest)
                        
                        // Move file to public directory
                        val publicFile = File("public/audio", "${UUID.randomUUID()}.wav")
                        publicFile.parentFile.mkdirs()
                        audioFile.copyTo(publicFile, overwrite = true)
                        audioFile.delete()
                        
                        val response = SynthesisResponse(
                            audioUrl = "/audio/${publicFile.name}",
                            duration = 0.0, // Calculate actual duration
                            language = request.language,
                            characterCount = request.text.length
                        )
                        
                        call.respond(ApiResponse(success = true, data = response))
                        
                    } catch (e: Exception) {
                        logger.error("Failed to synthesize speech", e)
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Nothing>(success = false, error = e.message)
                        )
                    }
                }
                
                // Stream synthesis for long texts
                post("/stream") {
                    try {
                        val request = call.receive<SynthesisRequest>()
                        
                        call.respondTextWriter {
                            // Implement streaming synthesis for very long texts
                            write("data: {\"status\": \"started\"}\n\n")
                            flush()
                            
                            // Process text in chunks
                            val chunks = request.text.chunked(1000)
                            for ((index, chunk) in chunks.withIndex()) {
                                val chunkRequest = CloneRequest(
                                    text = chunk,
                                    profileId = request.profileId,
                                    language = request.language,
                                    speed = request.speed,
                                    pitch = request.pitch,
                                    emotion = request.emotion
                                )
                                
                                val audioFile = engine.cloneVoice(chunkRequest)
                                val publicFile = File("public/audio", "${UUID.randomUUID()}.wav")
                                publicFile.parentFile.mkdirs()
                                audioFile.copyTo(publicFile, overwrite = true)
                                audioFile.delete()
                                
                                write("data: {\"chunk\": ${index + 1}, \"total\": ${chunks.size}, \"audioUrl\": \"/audio/${publicFile.name}\"}\n\n")
                                flush()
                            }
                            
                            write("data: {\"status\": \"completed\"}\n\n")
                            flush()
                        }
                        
                    } catch (e: Exception) {
                        logger.error("Failed to stream synthesis", e)
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Nothing>(success = false, error = e.message)
                        )
                    }
                }
            }
            
            // Language detection
            post("/api/detect-language") {
                try {
                    val requestBody = call.receive<Map<String, String>>()
                    val text = requestBody["text"] ?: ""
                    
                    if (text.isEmpty()) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ApiResponse<Nothing>(success = false, error = "Text is required")
                        )
                        return@post
                    }
                    
                    val detectedLanguage = engine.multiLanguageTTS.detectLanguage(text)
                    val languageInfo = engine.multiLanguageTTS.getLanguageInfo(detectedLanguage)
                    
                    val result = mapOf(
                        "language" to detectedLanguage,
                        "languageInfo" to languageInfo
                    )
                    
                    call.respond(ApiResponse(success = true, data = result))
                    
                } catch (e: Exception) {
                    logger.error("Failed to detect language", e)
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<Nothing>(success = false, error = e.message)
                    )
                }
            }
            
            // Serve audio files
            static("/audio") {
                files("public/audio")
            }
            
            // Serve web interface
            static("/") {
                files("public/web")
                default("index.html")
            }
        }
    }
    
    fun start() {
        logger.info("Starting Voice Clone Server on port 8080")
        
        // Create necessary directories
        File("public/audio").mkdirs()
        File("public/web").mkdirs()
        File("temp").mkdirs()
        
        server.start(wait = true)
    }
    
    fun stop() {
        logger.info("Stopping Voice Clone Server")
        server.stop(1000, 2000)
    }
}