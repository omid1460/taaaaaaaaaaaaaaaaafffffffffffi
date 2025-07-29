package com.voiceclone.audio

import mu.KotlinLogging
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.FFmpegFrameRecorder
import org.bytedeco.javacv.Frame
import java.io.File
import java.nio.ByteBuffer
import java.nio.ShortBuffer
import javax.sound.sampled.*
import kotlin.math.*

private val logger = KotlinLogging.logger {}

class AudioProcessor {
    
    /**
     * Enhance audio quality by applying noise reduction and normalization
     */
    fun enhanceAudio(inputFile: File): File {
        logger.info("Enhancing audio quality for ${inputFile.name}")
        
        val outputFile = File(inputFile.parent, "${inputFile.nameWithoutExtension}_enhanced.wav")
        
        try {
            // Use FFmpeg for advanced audio processing
            val grabber = FFmpegFrameGrabber(inputFile.absolutePath)
            grabber.start()
            
            val recorder = FFmpegFrameRecorder(outputFile.absolutePath, grabber.audioChannels)
            recorder.audioCodec = org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_PCM_S16LE
            recorder.sampleRate = grabber.sampleRate
            recorder.audioBitrate = 256000
            recorder.start()
            
            var frame: Frame?
            while (grabber.grab().also { frame = it } != null) {
                frame?.let { f ->
                    if (f.samples != null) {
                        // Apply audio enhancements
                        val enhancedFrame = applyAudioEnhancements(f)
                        recorder.record(enhancedFrame)
                    }
                }
            }
            
            recorder.stop()
            recorder.release()
            grabber.stop()
            grabber.release()
            
            logger.info("Audio enhancement completed")
            return outputFile
            
        } catch (e: Exception) {
            logger.error("Failed to enhance audio", e)
            return inputFile // Return original if enhancement fails
        }
    }
    
    /**
     * Combine multiple audio files into one
     */
    fun combineAudioFiles(audioFiles: List<File>, outputFile: File): File {
        logger.info("Combining ${audioFiles.size} audio files into ${outputFile.name}")
        
        try {
            val recorder = FFmpegFrameRecorder(outputFile.absolutePath, 1)
            recorder.audioCodec = org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_PCM_S16LE
            recorder.sampleRate = 22050
            recorder.audioBitrate = 256000
            recorder.start()
            
            for (audioFile in audioFiles) {
                val grabber = FFmpegFrameGrabber(audioFile.absolutePath)
                grabber.start()
                
                var frame: Frame?
                while (grabber.grab().also { frame = it } != null) {
                    frame?.let { f ->
                        if (f.samples != null) {
                            recorder.record(f)
                        }
                    }
                }
                
                grabber.stop()
                grabber.release()
            }
            
            recorder.stop()
            recorder.release()
            
            logger.info("Audio combination completed")
            return outputFile
            
        } catch (e: Exception) {
            logger.error("Failed to combine audio files", e)
            throw AudioProcessingException("Failed to combine audio files: ${e.message}")
        }
    }
    
    /**
     * Extract audio features for voice analysis
     */
    fun extractAudioFeatures(audioFile: File): AudioFeatures {
        logger.info("Extracting audio features from ${audioFile.name}")
        
        try {
            val grabber = FFmpegFrameGrabber(audioFile.absolutePath)
            grabber.start()
            
            val features = AudioFeatures()
            var frameCount = 0
            val amplitudes = mutableListOf<Double>()
            val frequencies = mutableListOf<Double>()
            
            var frame: Frame?
            while (grabber.grab().also { frame = it } != null) {
                frame?.let { f ->
                    if (f.samples != null && f.samples.isNotEmpty()) {
                        val samples = f.samples[0] as ShortBuffer
                        val audioData = ShortArray(samples.remaining())
                        samples.get(audioData)
                        
                        // Calculate amplitude
                        val amplitude = calculateRMS(audioData)
                        amplitudes.add(amplitude)
                        
                        // Calculate dominant frequency
                        val frequency = calculateDominantFrequency(audioData, grabber.sampleRate)
                        frequencies.add(frequency)
                        
                        frameCount++
                    }
                }
            }
            
            features.averageAmplitude = amplitudes.average()
            features.maxAmplitude = amplitudes.maxOrNull() ?: 0.0
            features.minAmplitude = amplitudes.minOrNull() ?: 0.0
            features.averageFrequency = frequencies.average()
            features.fundamentalFrequency = frequencies.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key ?: 0.0
            features.duration = frameCount / grabber.frameRate
            features.sampleRate = grabber.sampleRate
            
            grabber.stop()
            grabber.release()
            
            logger.info("Audio features extracted successfully")
            return features
            
        } catch (e: Exception) {
            logger.error("Failed to extract audio features", e)
            throw AudioProcessingException("Failed to extract audio features: ${e.message}")
        }
    }
    
    /**
     * Convert audio to specific format and quality
     */
    fun convertAudioFormat(inputFile: File, outputFile: File, targetSampleRate: Int = 22050): File {
        logger.info("Converting audio format: ${inputFile.name} -> ${outputFile.name}")
        
        try {
            val grabber = FFmpegFrameGrabber(inputFile.absolutePath)
            grabber.start()
            
            val recorder = FFmpegFrameRecorder(outputFile.absolutePath, 1)
            recorder.audioCodec = org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_PCM_S16LE
            recorder.sampleRate = targetSampleRate
            recorder.audioBitrate = 256000
            recorder.start()
            
            var frame: Frame?
            while (grabber.grab().also { frame = it } != null) {
                frame?.let { f ->
                    if (f.samples != null) {
                        recorder.record(f)
                    }
                }
            }
            
            recorder.stop()
            recorder.release()
            grabber.stop()
            grabber.release()
            
            logger.info("Audio format conversion completed")
            return outputFile
            
        } catch (e: Exception) {
            logger.error("Failed to convert audio format", e)
            throw AudioProcessingException("Failed to convert audio format: ${e.message}")
        }
    }
    
    /**
     * Apply noise reduction to audio
     */
    fun reduceNoise(inputFile: File): File {
        logger.info("Applying noise reduction to ${inputFile.name}")
        
        val outputFile = File(inputFile.parent, "${inputFile.nameWithoutExtension}_denoised.wav")
        
        try {
            val grabber = FFmpegFrameGrabber(inputFile.absolutePath)
            grabber.start()
            
            val recorder = FFmpegFrameRecorder(outputFile.absolutePath, grabber.audioChannels)
            recorder.audioCodec = org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_PCM_S16LE
            recorder.sampleRate = grabber.sampleRate
            recorder.audioBitrate = 256000
            recorder.start()
            
            var frame: Frame?
            while (grabber.grab().also { frame = it } != null) {
                frame?.let { f ->
                    if (f.samples != null) {
                        val denoisedFrame = applyNoiseReduction(f)
                        recorder.record(denoisedFrame)
                    }
                }
            }
            
            recorder.stop()
            recorder.release()
            grabber.stop()
            grabber.release()
            
            logger.info("Noise reduction completed")
            return outputFile
            
        } catch (e: Exception) {
            logger.error("Failed to reduce noise", e)
            return inputFile // Return original if denoising fails
        }
    }
    
    private fun applyAudioEnhancements(frame: Frame): Frame {
        if (frame.samples == null || frame.samples.isEmpty()) return frame
        
        val samples = frame.samples[0] as ShortBuffer
        val audioData = ShortArray(samples.remaining())
        samples.get(audioData)
        
        // Apply normalization
        val normalizedData = normalizeAudio(audioData)
        
        // Apply high-pass filter to remove low-frequency noise
        val filteredData = applyHighPassFilter(normalizedData, 80.0, 22050.0)
        
        // Create new frame with enhanced audio
        val enhancedFrame = Frame()
        enhancedFrame.sampleRate = frame.sampleRate
        enhancedFrame.audioChannels = frame.audioChannels
        enhancedFrame.samples = arrayOf(ShortBuffer.wrap(filteredData))
        
        return enhancedFrame
    }
    
    private fun applyNoiseReduction(frame: Frame): Frame {
        if (frame.samples == null || frame.samples.isEmpty()) return frame
        
        val samples = frame.samples[0] as ShortBuffer
        val audioData = ShortArray(samples.remaining())
        samples.get(audioData)
        
        // Simple spectral subtraction for noise reduction
        val denoisedData = spectralSubtraction(audioData)
        
        val denoisedFrame = Frame()
        denoisedFrame.sampleRate = frame.sampleRate
        denoisedFrame.audioChannels = frame.audioChannels
        denoisedFrame.samples = arrayOf(ShortBuffer.wrap(denoisedData))
        
        return denoisedFrame
    }
    
    private fun normalizeAudio(audioData: ShortArray): ShortArray {
        val maxValue = audioData.maxOfOrNull { abs(it.toInt()) } ?: 1
        val scaleFactor = Short.MAX_VALUE.toDouble() / maxValue * 0.95 // Leave some headroom
        
        return audioData.map { (it * scaleFactor).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort() }.toShortArray()
    }
    
    private fun applyHighPassFilter(audioData: ShortArray, cutoffFreq: Double, sampleRate: Double): ShortArray {
        val rc = 1.0 / (2.0 * PI * cutoffFreq)
        val dt = 1.0 / sampleRate
        val alpha = rc / (rc + dt)
        
        val filteredData = ShortArray(audioData.size)
        filteredData[0] = audioData[0]
        
        for (i in 1 until audioData.size) {
            filteredData[i] = (alpha * (filteredData[i - 1] + audioData[i] - audioData[i - 1])).toInt().toShort()
        }
        
        return filteredData
    }
    
    private fun spectralSubtraction(audioData: ShortArray): ShortArray {
        // Simple noise reduction using spectral subtraction
        // This is a simplified implementation
        val windowSize = 1024
        val denoisedData = audioData.copyOf()
        
        for (i in 0 until audioData.size - windowSize step windowSize / 2) {
            val window = audioData.sliceArray(i until minOf(i + windowSize, audioData.size))
            val processedWindow = processWindow(window)
            
            for (j in processedWindow.indices) {
                if (i + j < denoisedData.size) {
                    denoisedData[i + j] = processedWindow[j]
                }
            }
        }
        
        return denoisedData
    }
    
    private fun processWindow(window: ShortArray): ShortArray {
        // Apply simple smoothing filter
        val smoothed = ShortArray(window.size)
        smoothed[0] = window[0]
        
        for (i in 1 until window.size - 1) {
            smoothed[i] = ((window[i - 1] + window[i] + window[i + 1]) / 3).toShort()
        }
        
        smoothed[window.size - 1] = window[window.size - 1]
        return smoothed
    }
    
    private fun calculateRMS(audioData: ShortArray): Double {
        var sum = 0.0
        for (sample in audioData) {
            sum += sample * sample
        }
        return sqrt(sum / audioData.size)
    }
    
    private fun calculateDominantFrequency(audioData: ShortArray, sampleRate: Int): Double {
        // Simple zero-crossing rate based frequency estimation
        var crossings = 0
        for (i in 1 until audioData.size) {
            if ((audioData[i - 1] >= 0) != (audioData[i] >= 0)) {
                crossings++
            }
        }
        
        return (crossings * sampleRate.toDouble()) / (2.0 * audioData.size)
    }
}

data class AudioFeatures(
    var averageAmplitude: Double = 0.0,
    var maxAmplitude: Double = 0.0,
    var minAmplitude: Double = 0.0,
    var averageFrequency: Double = 0.0,
    var fundamentalFrequency: Double = 0.0,
    var duration: Double = 0.0,
    var sampleRate: Int = 0
)

class AudioProcessingException(message: String) : Exception(message)