package com.voiceclone.audio

import kotlinx.coroutines.*
import mu.KotlinLogging
import java.io.File
import java.io.IOException
import javax.sound.sampled.*

private val logger = KotlinLogging.logger {}

class AudioRecorder {
    private var isRecording = false
    private var targetDataLine: TargetDataLine? = null
    
    companion object {
        const val SAMPLE_RATE = 22050f
        const val BITS_PER_SAMPLE = 16
        const val CHANNELS = 1 // Mono
        const val SIGNED = true
        const val BIG_ENDIAN = false
    }
    
    /**
     * Record audio for specified duration
     */
    suspend fun recordAudio(outputFile: File, durationMinutes: Int): File = withContext(Dispatchers.IO) {
        logger.info("Starting audio recording for $durationMinutes minutes to ${outputFile.name}")
        
        val audioFormat = AudioFormat(
            SAMPLE_RATE,
            BITS_PER_SAMPLE,
            CHANNELS,
            SIGNED,
            BIG_ENDIAN
        )
        
        val dataLineInfo = DataLine.Info(TargetDataLine::class.java, audioFormat)
        
        if (!AudioSystem.isLineSupported(dataLineInfo)) {
            throw AudioRecordingException("Audio format not supported by system")
        }
        
        try {
            targetDataLine = AudioSystem.getLine(dataLineInfo) as TargetDataLine
            targetDataLine?.open(audioFormat)
            targetDataLine?.start()
            
            isRecording = true
            
            val audioInputStream = AudioInputStream(targetDataLine)
            
            // Record in a separate coroutine with timeout
            val recordingJob = async {
                AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, outputFile)
            }
            
            // Stop recording after specified duration
            delay((durationMinutes * 60 * 1000).toLong())
            stopRecording()
            
            recordingJob.await()
            
            logger.info("Audio recording completed successfully")
            outputFile
            
        } catch (e: Exception) {
            logger.error("Failed to record audio", e)
            stopRecording()
            throw AudioRecordingException("Failed to record audio: ${e.message}")
        }
    }
    
    /**
     * Record audio with real-time monitoring
     */
    suspend fun recordAudioWithMonitoring(
        outputFile: File,
        durationMinutes: Int,
        onVolumeUpdate: (Float) -> Unit = {},
        onTimeUpdate: (Int) -> Unit = {}
    ): File = withContext(Dispatchers.IO) {
        logger.info("Starting monitored audio recording")
        
        val audioFormat = AudioFormat(SAMPLE_RATE, BITS_PER_SAMPLE, CHANNELS, SIGNED, BIG_ENDIAN)
        val dataLineInfo = DataLine.Info(TargetDataLine::class.java, audioFormat)
        
        targetDataLine = AudioSystem.getLine(dataLineInfo) as TargetDataLine
        targetDataLine?.open(audioFormat)
        targetDataLine?.start()
        
        isRecording = true
        
        val bufferSize = audioFormat.frameSize * (SAMPLE_RATE / 10).toInt() // 100ms buffer
        val buffer = ByteArray(bufferSize)
        
        val audioOutputStream = AudioSystem.getAudioOutputStream(
            AudioFileFormat.Type.WAVE,
            audioFormat,
            outputFile
        )
        
        val startTime = System.currentTimeMillis()
        val endTime = startTime + (durationMinutes * 60 * 1000)
        
        try {
            while (isRecording && System.currentTimeMillis() < endTime) {
                val bytesRead = targetDataLine?.read(buffer, 0, buffer.size) ?: 0
                
                if (bytesRead > 0) {
                    audioOutputStream.write(buffer, 0, bytesRead)
                    
                    // Calculate volume level for monitoring
                    val volume = calculateVolumeLevel(buffer, bytesRead)
                    onVolumeUpdate(volume)
                    
                    // Update elapsed time
                    val elapsedSeconds = ((System.currentTimeMillis() - startTime) / 1000).toInt()
                    onTimeUpdate(elapsedSeconds)
                }
                
                delay(10) // Small delay to prevent excessive CPU usage
            }
            
            audioOutputStream.close()
            stopRecording()
            
            logger.info("Monitored audio recording completed")
            outputFile
            
        } catch (e: Exception) {
            logger.error("Failed during monitored recording", e)
            stopRecording()
            throw AudioRecordingException("Recording failed: ${e.message}")
        }
    }
    
    /**
     * Stop current recording
     */
    fun stopRecording() {
        if (isRecording) {
            isRecording = false
            targetDataLine?.stop()
            targetDataLine?.close()
            targetDataLine = null
            logger.info("Audio recording stopped")
        }
    }
    
    /**
     * Check if currently recording
     */
    fun isRecording(): Boolean = isRecording
    
    /**
     * Get available audio input devices
     */
    fun getAvailableInputDevices(): List<AudioDevice> {
        val devices = mutableListOf<AudioDevice>()
        
        val mixerInfos = AudioSystem.getMixerInfo()
        for (mixerInfo in mixerInfos) {
            val mixer = AudioSystem.getMixer(mixerInfo)
            val targetLineInfos = mixer.targetLineInfo
            
            if (targetLineInfos.isNotEmpty()) {
                devices.add(
                    AudioDevice(
                        name = mixerInfo.name,
                        description = mixerInfo.description,
                        vendor = mixerInfo.vendor,
                        version = mixerInfo.version
                    )
                )
            }
        }
        
        return devices
    }
    
    /**
     * Test audio input level
     */
    suspend fun testAudioInput(durationSeconds: Int = 5): Float = withContext(Dispatchers.IO) {
        val audioFormat = AudioFormat(SAMPLE_RATE, BITS_PER_SAMPLE, CHANNELS, SIGNED, BIG_ENDIAN)
        val dataLineInfo = DataLine.Info(TargetDataLine::class.java, audioFormat)
        
        val testLine = AudioSystem.getLine(dataLineInfo) as TargetDataLine
        testLine.open(audioFormat)
        testLine.start()
        
        val bufferSize = audioFormat.frameSize * (SAMPLE_RATE / 10).toInt()
        val buffer = ByteArray(bufferSize)
        
        var maxVolume = 0f
        val endTime = System.currentTimeMillis() + (durationSeconds * 1000)
        
        try {
            while (System.currentTimeMillis() < endTime) {
                val bytesRead = testLine.read(buffer, 0, buffer.size)
                if (bytesRead > 0) {
                    val volume = calculateVolumeLevel(buffer, bytesRead)
                    maxVolume = maxOf(maxVolume, volume)
                }
                delay(10)
            }
        } finally {
            testLine.stop()
            testLine.close()
        }
        
        maxVolume
    }
    
    private fun calculateVolumeLevel(buffer: ByteArray, length: Int): Float {
        var sum = 0.0
        for (i in 0 until length step 2) {
            val sample = ((buffer[i + 1].toInt() shl 8) or (buffer[i].toInt() and 0xFF)).toShort()
            sum += sample * sample
        }
        
        val rms = kotlin.math.sqrt(sum / (length / 2))
        return (rms / Short.MAX_VALUE).toFloat()
    }
}

data class AudioDevice(
    val name: String,
    val description: String,
    val vendor: String,
    val version: String
)

class AudioRecordingException(message: String) : Exception(message)