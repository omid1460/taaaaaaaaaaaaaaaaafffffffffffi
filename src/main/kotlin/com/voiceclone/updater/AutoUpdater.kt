package com.voiceclone.updater

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.system.exitProcess

private val logger = KotlinLogging.logger {}

@Serializable
data class UpdateInfo(
    val version: String,
    val downloadUrl: String,
    val releaseNotes: String,
    val mandatory: Boolean = false,
    val size: Long,
    val checksum: String,
    val releaseDate: String
)

@Serializable
data class UpdateResponse(
    val available: Boolean,
    val current: String,
    val latest: String,
    val updateInfo: UpdateInfo? = null
)

class AutoUpdater {
    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            })
        }
    }
    
    private val currentVersion = "1.0.0"
    private val updateCheckUrl = "https://api.github.com/repos/voiceclone/releases/latest" // Example URL
    private val updateDir = File("updates")
    private val backupDir = File("backup")
    
    companion object {
        const val UPDATE_CHECK_INTERVAL = 24 * 60 * 60 * 1000L // 24 hours
        const val AUTO_UPDATE_ENABLED_KEY = "auto_update_enabled"
        const val LAST_UPDATE_CHECK_KEY = "last_update_check"
    }
    
    init {
        updateDir.mkdirs()
        backupDir.mkdirs()
    }
    
    /**
     * Check for available updates
     */
    suspend fun checkForUpdates(force: Boolean = false): UpdateResponse = withContext(Dispatchers.IO) {
        logger.info("Checking for updates...")
        
        try {
            // Check if auto-update is enabled
            if (!force && !isAutoUpdateEnabled()) {
                logger.info("Auto-update is disabled")
                return@withContext UpdateResponse(
                    available = false,
                    current = currentVersion,
                    latest = currentVersion
                )
            }
            
            // Check if enough time has passed since last check
            if (!force && !shouldCheckForUpdates()) {
                logger.info("Skipping update check - not enough time passed")
                return@withContext UpdateResponse(
                    available = false,
                    current = currentVersion,
                    latest = currentVersion
                )
            }
            
            // Fetch latest version info
            val latestVersion = fetchLatestVersionInfo()
            
            // Update last check timestamp
            updateLastCheckTimestamp()
            
            if (isNewerVersion(latestVersion.version, currentVersion)) {
                logger.info("New version available: ${latestVersion.version} (current: $currentVersion)")
                
                return@withContext UpdateResponse(
                    available = true,
                    current = currentVersion,
                    latest = latestVersion.version,
                    updateInfo = latestVersion
                )
            } else {
                logger.info("Application is up to date")
                return@withContext UpdateResponse(
                    available = false,
                    current = currentVersion,
                    latest = currentVersion
                )
            }
            
        } catch (e: Exception) {
            logger.error("Failed to check for updates", e)
            return@withContext UpdateResponse(
                available = false,
                current = currentVersion,
                latest = currentVersion
            )
        }
    }
    
    /**
     * Download and install update
     */
    suspend fun downloadAndInstallUpdate(updateInfo: UpdateInfo): Boolean = withContext(Dispatchers.IO) {
        logger.info("Downloading update: ${updateInfo.version}")
        
        try {
            // Download update file
            val updateFile = downloadUpdate(updateInfo)
            
            // Verify checksum
            if (!verifyChecksum(updateFile, updateInfo.checksum)) {
                logger.error("Update file checksum verification failed")
                updateFile.delete()
                return@withContext false
            }
            
            // Create backup of current version
            createBackup()
            
            // Install update
            val success = installUpdate(updateFile)
            
            if (success) {
                logger.info("Update installed successfully")
                
                // Schedule restart
                scheduleRestart()
                return@withContext true
            } else {
                logger.error("Failed to install update")
                
                // Restore from backup
                restoreFromBackup()
                return@withContext false
            }
            
        } catch (e: Exception) {
            logger.error("Failed to download and install update", e)
            
            // Restore from backup if installation was attempted
            restoreFromBackup()
            return@withContext false
        }
    }
    
    /**
     * Enable or disable auto-updates
     */
    fun setAutoUpdateEnabled(enabled: Boolean) {
        val configFile = File("config.properties")
        val properties = java.util.Properties()
        
        if (configFile.exists()) {
            configFile.inputStream().use { properties.load(it) }
        }
        
        properties.setProperty(AUTO_UPDATE_ENABLED_KEY, enabled.toString())
        
        configFile.outputStream().use { properties.store(it, "Voice Clone Configuration") }
        
        logger.info("Auto-update ${if (enabled) "enabled" else "disabled"}")
    }
    
    /**
     * Check if auto-updates are enabled
     */
    fun isAutoUpdateEnabled(): Boolean {
        val configFile = File("config.properties")
        if (!configFile.exists()) return true // Default to enabled
        
        val properties = java.util.Properties()
        configFile.inputStream().use { properties.load(it) }
        
        return properties.getProperty(AUTO_UPDATE_ENABLED_KEY, "true").toBoolean()
    }
    
    /**
     * Get update history
     */
    fun getUpdateHistory(): List<UpdateHistoryEntry> {
        val historyFile = File("update_history.json")
        if (!historyFile.exists()) return emptyList()
        
        return try {
            val json = historyFile.readText()
            Json.decodeFromString<List<UpdateHistoryEntry>>(json)
        } catch (e: Exception) {
            logger.error("Failed to read update history", e)
            emptyList()
        }
    }
    
    /**
     * Rollback to previous version
     */
    suspend fun rollbackToPreviousVersion(): Boolean = withContext(Dispatchers.IO) {
        logger.info("Rolling back to previous version")
        
        try {
            val backupFiles = backupDir.listFiles()?.filter { it.name.startsWith("backup_") }
            if (backupFiles.isNullOrEmpty()) {
                logger.error("No backup found for rollback")
                return@withContext false
            }
            
            // Find most recent backup
            val latestBackup = backupFiles.maxByOrNull { it.lastModified() }
            if (latestBackup == null) {
                logger.error("No valid backup found")
                return@withContext false
            }
            
            // Extract backup
            val success = extractBackup(latestBackup)
            
            if (success) {
                logger.info("Rollback completed successfully")
                scheduleRestart()
                return@withContext true
            } else {
                logger.error("Failed to rollback")
                return@withContext false
            }
            
        } catch (e: Exception) {
            logger.error("Failed to rollback", e)
            return@withContext false
        }
    }
    
    private suspend fun fetchLatestVersionInfo(): UpdateInfo {
        // This is a simplified implementation
        // In a real application, you would fetch from your update server or GitHub releases
        
        val response = httpClient.get(updateCheckUrl)
        val releaseInfo = response.body<Map<String, Any>>()
        
        return UpdateInfo(
            version = releaseInfo["tag_name"] as? String ?: "1.0.0",
            downloadUrl = releaseInfo["zipball_url"] as? String ?: "",
            releaseNotes = releaseInfo["body"] as? String ?: "",
            mandatory = false,
            size = 1024 * 1024, // 1MB placeholder
            checksum = "placeholder_checksum",
            releaseDate = releaseInfo["published_at"] as? String ?: ""
        )
    }
    
    private fun isNewerVersion(latest: String, current: String): Boolean {
        // Simple version comparison (you might want to use a more sophisticated method)
        val latestParts = latest.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }
        val currentParts = current.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }
        
        for (i in 0 until maxOf(latestParts.size, currentParts.size)) {
            val latestPart = latestParts.getOrNull(i) ?: 0
            val currentPart = currentParts.getOrNull(i) ?: 0
            
            if (latestPart > currentPart) return true
            if (latestPart < currentPart) return false
        }
        
        return false
    }
    
    private fun shouldCheckForUpdates(): Boolean {
        val configFile = File("config.properties")
        if (!configFile.exists()) return true
        
        val properties = java.util.Properties()
        configFile.inputStream().use { properties.load(it) }
        
        val lastCheck = properties.getProperty(LAST_UPDATE_CHECK_KEY, "0").toLongOrNull() ?: 0
        val now = System.currentTimeMillis()
        
        return (now - lastCheck) > UPDATE_CHECK_INTERVAL
    }
    
    private fun updateLastCheckTimestamp() {
        val configFile = File("config.properties")
        val properties = java.util.Properties()
        
        if (configFile.exists()) {
            configFile.inputStream().use { properties.load(it) }
        }
        
        properties.setProperty(LAST_UPDATE_CHECK_KEY, System.currentTimeMillis().toString())
        
        configFile.outputStream().use { properties.store(it, "Voice Clone Configuration") }
    }
    
    private suspend fun downloadUpdate(updateInfo: UpdateInfo): File {
        val updateFile = File(updateDir, "update_${updateInfo.version}.zip")
        
        logger.info("Downloading update from: ${updateInfo.downloadUrl}")
        
        val response = httpClient.get(updateInfo.downloadUrl)
        val bytes = response.body<ByteArray>()
        
        updateFile.writeBytes(bytes)
        
        logger.info("Update downloaded: ${updateFile.absolutePath}")
        return updateFile
    }
    
    private fun verifyChecksum(file: File, expectedChecksum: String): Boolean {
        // Simplified checksum verification
        // In a real implementation, you would use SHA-256 or similar
        return true // Placeholder
    }
    
    private fun createBackup(): Boolean {
        try {
            val timestamp = System.currentTimeMillis()
            val backupFile = File(backupDir, "backup_$timestamp.zip")
            
            // Create ZIP archive of current installation
            // This is a simplified implementation
            logger.info("Creating backup: ${backupFile.absolutePath}")
            
            return true
        } catch (e: Exception) {
            logger.error("Failed to create backup", e)
            return false
        }
    }
    
    private fun installUpdate(updateFile: File): Boolean {
        try {
            logger.info("Installing update from: ${updateFile.absolutePath}")
            
            // Extract update files
            val extractDir = File(updateDir, "extract")
            extractDir.mkdirs()
            
            extractZip(updateFile, extractDir)
            
            // Copy files to application directory
            copyUpdateFiles(extractDir, File("."))
            
            // Clean up
            updateFile.delete()
            extractDir.deleteRecursively()
            
            // Add to update history
            addToUpdateHistory(updateFile.nameWithoutExtension)
            
            return true
            
        } catch (e: Exception) {
            logger.error("Failed to install update", e)
            return false
        }
    }
    
    private fun extractZip(zipFile: File, destDir: File) {
        zipFile.inputStream().use { fis ->
            ZipArchiveInputStream(fis).use { zis ->
                var entry: ZipArchiveEntry? = zis.nextZipEntry
                
                while (entry != null) {
                    val destFile = File(destDir, entry.name)
                    
                    if (entry.isDirectory) {
                        destFile.mkdirs()
                    } else {
                        destFile.parentFile.mkdirs()
                        FileOutputStream(destFile).use { fos ->
                            zis.copyTo(fos)
                        }
                    }
                    
                    entry = zis.nextZipEntry
                }
            }
        }
    }
    
    private fun copyUpdateFiles(sourceDir: File, destDir: File) {
        sourceDir.walkTopDown().forEach { sourceFile ->
            if (sourceFile.isFile) {
                val relativePath = sourceFile.relativeTo(sourceDir)
                val destFile = File(destDir, relativePath.path)
                
                destFile.parentFile.mkdirs()
                Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        }
    }
    
    private fun restoreFromBackup(): Boolean {
        try {
            val backupFiles = backupDir.listFiles()?.filter { it.name.startsWith("backup_") }
            if (backupFiles.isNullOrEmpty()) return false
            
            val latestBackup = backupFiles.maxByOrNull { it.lastModified() }
            return latestBackup?.let { extractBackup(it) } ?: false
            
        } catch (e: Exception) {
            logger.error("Failed to restore from backup", e)
            return false
        }
    }
    
    private fun extractBackup(backupFile: File): Boolean {
        try {
            val extractDir = File(updateDir, "restore")
            extractDir.mkdirs()
            
            extractZip(backupFile, extractDir)
            copyUpdateFiles(extractDir, File("."))
            
            extractDir.deleteRecursively()
            
            return true
        } catch (e: Exception) {
            logger.error("Failed to extract backup", e)
            return false
        }
    }
    
    private fun scheduleRestart() {
        logger.info("Scheduling application restart...")
        
        // Create restart script
        val restartScript = when {
            System.getProperty("os.name").lowercase().contains("windows") -> {
                File("restart.bat").apply {
                    writeText("""
                        @echo off
                        timeout /t 3 /nobreak > nul
                        start "" java -jar voice-clone.jar
                        del "%~f0"
                    """.trimIndent())
                }
            }
            else -> {
                File("restart.sh").apply {
                    writeText("""
                        #!/bin/bash
                        sleep 3
                        java -jar voice-clone.jar &
                        rm "$0"
                    """.trimIndent())
                    setExecutable(true)
                }
            }
        }
        
        // Schedule shutdown and restart
        GlobalScope.launch {
            delay(2000) // Give time for cleanup
            
            try {
                ProcessBuilder(restartScript.absolutePath).start()
            } catch (e: Exception) {
                logger.error("Failed to start restart script", e)
            }
            
            exitProcess(0)
        }
    }
    
    private fun addToUpdateHistory(version: String) {
        val historyFile = File("update_history.json")
        val history = getUpdateHistory().toMutableList()
        
        history.add(
            UpdateHistoryEntry(
                version = version,
                timestamp = System.currentTimeMillis(),
                success = true
            )
        )
        
        // Keep only last 10 entries
        if (history.size > 10) {
            history.removeAt(0)
        }
        
        try {
            val json = Json.encodeToString(history)
            historyFile.writeText(json)
        } catch (e: Exception) {
            logger.error("Failed to save update history", e)
        }
    }
}

@Serializable
data class UpdateHistoryEntry(
    val version: String,
    val timestamp: Long,
    val success: Boolean,
    val notes: String = ""
)