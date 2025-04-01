package org.lsrv.copypastedetector

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.ui.Messages
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.LocalDateTime
import java.util.Timer
import java.util.TimerTask
import kotlin.concurrent.thread

class ExternalAppMonitor : StartupActivity {
    private var monitoringTimer: Timer? = null
    private val suspiciousApps = listOf(
        "chrome", "firefox", "safari", "edge", // Browsers
        "telegram", "whatsapp", "slack", "discord", // Messaging
        "notepad", "textedit", "word", "excel" // Text editors
    )
    
    override fun runActivity(project: Project) {
        val sessionData = SessionData.getInstance().state
        if (sessionData.sessionId.isNullOrEmpty()) {
            return
        }
        
        // Start monitoring in a separate thread
        monitoringTimer = Timer(true)
        monitoringTimer?.schedule(object : TimerTask() {
            override fun run() {
                checkRunningApplications(project, sessionData)
            }
        }, 10000, 30000) // Check every 30 seconds after a 10-second initial delay
    }
    
    private fun checkRunningApplications(project: Project, sessionData: SessionData.Session) {
        thread {
            try {
                val runningApps = getRunningApplications()
                val detectedApps = suspiciousApps.filter { app ->
                    runningApps.any { it.contains(app, ignoreCase = true) }
                }
                
                if (detectedApps.isNotEmpty()) {
                    // Log the suspicious apps
                    println("WARNING: Suspicious applications detected!")
                    println("  Session ID: ${sessionData.sessionId}")
                    println("  User: ${sessionData.clientName} (${sessionData.rollNumber})")
                    println("  Applications: ${detectedApps.joinToString(", ")}")
                    println("  Timestamp: ${LocalDateTime.now()}")
                    
                    // Notify the user
                    ApplicationManager.getApplication().invokeLater {
                        Messages.showWarningDialog(
                            project,
                            "The following applications have been detected running:\n\n" +
                            "${detectedApps.joinToString("\n")}\n\n" +
                            "Using external applications during the exam may be considered cheating.\n" +
                            "This activity has been logged.",
                            "External Applications Detected"
                        )
                    }
                }
            } catch (e: Exception) {
                println("Error checking running applications: ${e.message}")
            }
        }
    }
    
    private fun getRunningApplications(): List<String> {
        val result = mutableListOf<String>()
        val os = System.getProperty("os.name").lowercase()
        
        try {
            val process = when {
                os.contains("win") -> Runtime.getRuntime().exec("tasklist")
                os.contains("mac") || os.contains("nix") || os.contains("nux") -> 
                    Runtime.getRuntime().exec("ps -e")
                else -> return emptyList()
            }
            
            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    line?.let { result.add(it) }
                }
            }
        } catch (e: Exception) {
            println("Error getting running applications: ${e.message}")
        }
        
        return result
    }
} 