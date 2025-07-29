package com.voiceclone

import com.voiceclone.api.VoiceCloneServer
import com.voiceclone.core.VoiceCloneEngine
import com.voiceclone.gui.VoiceCloneGUI
import com.voiceclone.updater.AutoUpdater
import kotlinx.coroutines.*
import mu.KotlinLogging
import kotlin.system.exitProcess

private val logger = KotlinLogging.logger {}

fun main(args: Array<String>) {
    logger.info("Starting Voice Clone Application v1.0.0")
    
    try {
        // Initialize core components
        val engine = VoiceCloneEngine()
        val autoUpdater = AutoUpdater()
        
        // Check for updates on startup
        runBlocking {
            autoUpdater.checkForUpdates()
        }
        
        // Parse command line arguments
        val mode = when {
            args.contains("--server") -> "server"
            args.contains("--gui") -> "gui"
            args.contains("--cli") -> "cli"
            else -> "gui" // Default to GUI mode
        }
        
        when (mode) {
            "server" -> {
                logger.info("Starting in server mode")
                val server = VoiceCloneServer(engine)
                server.start()
            }
            "gui" -> {
                logger.info("Starting in GUI mode")
                VoiceCloneGUI.launch(engine, autoUpdater)
            }
            "cli" -> {
                logger.info("Starting in CLI mode")
                startCLI(engine)
            }
        }
        
    } catch (e: Exception) {
        logger.error("Failed to start application", e)
        exitProcess(1)
    }
}

private fun startCLI(engine: VoiceCloneEngine) {
    println("Voice Clone CLI - Type 'help' for commands")
    
    while (true) {
        print("> ")
        val input = readlnOrNull() ?: break
        
        when (input.trim().lowercase()) {
            "help" -> showHelp()
            "exit", "quit" -> break
            else -> {
                if (input.startsWith("record ")) {
                    val duration = input.substringAfter("record ").toIntOrNull() ?: 10
                    println("Recording for $duration minutes...")
                    // Implementation will be added
                } else if (input.startsWith("clone ")) {
                    val text = input.substringAfter("clone ")
                    println("Cloning voice for text: $text")
                    // Implementation will be added
                } else {
                    println("Unknown command. Type 'help' for available commands.")
                }
            }
        }
    }
}

private fun showHelp() {
    println("""
        Available commands:
        - record <minutes>  : Record voice sample (10-20 minutes recommended)
        - clone <text>      : Generate speech from text using cloned voice
        - help              : Show this help message
        - exit/quit         : Exit the application
    """.trimIndent())
}