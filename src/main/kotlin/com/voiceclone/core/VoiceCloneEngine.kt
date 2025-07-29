package com.voiceclone.core

import com.voiceclone.audio.AudioRecorder
import com.voiceclone.audio.AudioProcessor
import com.voiceclone.ml.VoiceModel
import com.voiceclone.tts.MultiLanguageTTS
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import mu.KotlinLogging
import java.io.File
import java.util.*

private val logger = KotlinLogging.logger {}

@Serializable
data class VoiceProfile(
    val id: String,
    val name: String,
    val language: String,
    val modelPath: String,
    val createdAt: Long,
    val duration: Int, // in minutes
    val sampleRate: Int = 22050
)

@Serializable
data class CloneRequest(
    val text: String,
    val profileId: String,
    val language: String = "auto",
    val speed: Float = 1.0f,
    val pitch: Float = 1.0f,
    val emotion: String = "neutral"
)

class VoiceCloneEngine {
    private val audioRecorder = AudioRecorder()
    private val audioProcessor = AudioProcessor()
    private val voiceModel = VoiceModel()
    private val multiLanguageTTS = MultiLanguageTTS()
    
    private val voiceProfiles = mutableMapOf<String, VoiceProfile>()
    private val profilesDir = File("voice_profiles")
    
    init {
        // Create directories if they don't exist
        profilesDir.mkdirs()
        loadExistingProfiles()
        
        logger.info("Voice Clone Engine initialized")
    }
    
    /**
     * Record voice sample for cloning (10-20 minutes recommended)
     */
    suspend fun recordVoiceSample(
        profileName: String,
        durationMinutes: Int,
        language: String = "fa" // Default to Persian
    ): VoiceProfile = withContext(Dispatchers.IO) {
        logger.info("Starting voice recording for profile: $profileName, duration: ${durationMinutes}min")
        
        val profileId = UUID.randomUUID().toString()
        val audioFile = File(profilesDir, "$profileId.wav")
        
        try {
            // Record audio
            val recordedFile = audioRecorder.recordAudio(audioFile, durationMinutes)
            
            // Process and enhance audio quality
            val processedFile = audioProcessor.enhanceAudio(recordedFile)
            
            // Extract voice features and train model
            val modelPath = voiceModel.trainVoiceModel(processedFile, profileId, language)
            
            val profile = VoiceProfile(
                id = profileId,
                name = profileName,
                language = language,
                modelPath = modelPath,
                createdAt = System.currentTimeMillis(),
                duration = durationMinutes
            )
            
            voiceProfiles[profileId] = profile
            saveProfile(profile)
            
            logger.info("Voice profile created successfully: $profileId")
            profile
            
        } catch (e: Exception) {
            logger.error("Failed to create voice profile", e)
            throw VoiceCloneException("Failed to record voice sample: ${e.message}")
        }
    }
    
    /**
     * Clone voice and generate speech from text (up to 50,000 characters)
     */
    suspend fun cloneVoice(request: CloneRequest): File = withContext(Dispatchers.IO) {
        logger.info("Cloning voice for profile: ${request.profileId}, text length: ${request.text.length}")
        
        if (request.text.length > 50000) {
            throw VoiceCloneException("Text exceeds maximum length of 50,000 characters")
        }
        
        val profile = voiceProfiles[request.profileId]
            ?: throw VoiceCloneException("Voice profile not found: ${request.profileId}")
        
        try {
            // Detect language if auto
            val detectedLanguage = if (request.language == "auto") {
                detectLanguage(request.text)
            } else {
                request.language
            }
            
            // Split text into manageable chunks for better processing
            val textChunks = splitTextIntoChunks(request.text)
            val audioChunks = mutableListOf<File>()
            
            for ((index, chunk) in textChunks.withIndex()) {
                logger.debug("Processing chunk ${index + 1}/${textChunks.size}")
                
                // Generate speech using the cloned voice model
                val chunkAudio = voiceModel.generateSpeech(
                    text = chunk,
                    modelPath = profile.modelPath,
                    language = detectedLanguage,
                    speed = request.speed,
                    pitch = request.pitch,
                    emotion = request.emotion
                )
                
                audioChunks.add(chunkAudio)
            }
            
            // Combine all chunks into final audio file
            val outputFile = File("output", "${UUID.randomUUID()}.wav")
            outputFile.parentFile.mkdirs()
            
            val finalAudio = audioProcessor.combineAudioFiles(audioChunks, outputFile)
            
            // Clean up temporary chunk files
            audioChunks.forEach { it.delete() }
            
            logger.info("Voice cloning completed successfully")
            finalAudio
            
        } catch (e: Exception) {
            logger.error("Failed to clone voice", e)
            throw VoiceCloneException("Failed to clone voice: ${e.message}")
        }
    }
    
    /**
     * Get all available voice profiles
     */
    fun getVoiceProfiles(): List<VoiceProfile> {
        return voiceProfiles.values.toList()
    }
    
    /**
     * Delete a voice profile
     */
    suspend fun deleteVoiceProfile(profileId: String): Boolean = withContext(Dispatchers.IO) {
        val profile = voiceProfiles[profileId] ?: return@withContext false
        
        try {
            // Delete model files
            File(profile.modelPath).delete()
            File(profilesDir, "$profileId.wav").delete()
            File(profilesDir, "$profileId.json").delete()
            
            voiceProfiles.remove(profileId)
            
            logger.info("Voice profile deleted: $profileId")
            true
        } catch (e: Exception) {
            logger.error("Failed to delete voice profile", e)
            false
        }
    }
    
    /**
     * Get supported languages
     */
    fun getSupportedLanguages(): List<String> {
        return multiLanguageTTS.getSupportedLanguages()
    }
    
    private fun loadExistingProfiles() {
        profilesDir.listFiles { _, name -> name.endsWith(".json") }?.forEach { file ->
            try {
                // Load profile from JSON file
                // Implementation depends on JSON serialization library
                logger.debug("Loaded voice profile: ${file.name}")
            } catch (e: Exception) {
                logger.warn("Failed to load profile: ${file.name}", e)
            }
        }
    }
    
    private fun saveProfile(profile: VoiceProfile) {
        val profileFile = File(profilesDir, "${profile.id}.json")
        try {
            // Save profile to JSON file
            // Implementation depends on JSON serialization library
            logger.debug("Saved voice profile: ${profile.id}")
        } catch (e: Exception) {
            logger.error("Failed to save profile", e)
        }
    }
    
    private fun detectLanguage(text: String): String {
        return multiLanguageTTS.detectLanguage(text)
    }
    
    private fun splitTextIntoChunks(text: String, maxChunkSize: Int = 1000): List<String> {
        val chunks = mutableListOf<String>()
        var currentIndex = 0
        
        while (currentIndex < text.length) {
            val endIndex = minOf(currentIndex + maxChunkSize, text.length)
            var chunkEnd = endIndex
            
            // Try to break at sentence boundaries
            if (endIndex < text.length) {
                val lastSentenceEnd = text.lastIndexOfAny(charArrayOf('.', '!', '?', '۔', '؟'), endIndex - 1)
                if (lastSentenceEnd > currentIndex) {
                    chunkEnd = lastSentenceEnd + 1
                }
            }
            
            chunks.add(text.substring(currentIndex, chunkEnd).trim())
            currentIndex = chunkEnd
        }
        
        return chunks
    }
}

class VoiceCloneException(message: String) : Exception(message)