package org.lsrv.copypastedetector

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.ui.Messages
import java.util.Timer
import java.util.TimerTask

@Service
class PluginProtectionService {
    private val PLUGIN_ID = "org.lsrv.copy-paste-detector"
    private var monitoringTimer: Timer? = null
    
    fun startMonitoring(project: Project) {
        monitoringTimer = Timer(true)
        monitoringTimer?.schedule(object : TimerTask() {
            override fun run() {
                checkPluginEnabled(project)
            }
        }, 5000, 5000) // Check every 5 seconds
    }
    
    private fun checkPluginEnabled(project: Project) {
        val pluginId = PluginId.findId(PLUGIN_ID)
        val plugin = if (pluginId != null) PluginManagerCore.getPlugin(pluginId) else null
        
        if (plugin == null || !plugin.isEnabled) {
            // Plugin was disabled or uninstalled
            ApplicationManager.getApplication().invokeLater {
                Messages.showErrorDialog(
                    project,
                    "Attempting to disable the exam monitoring plugin is not allowed.\n\n" +
                    "This incident has been logged and your exam may be voided.\n\n" +
                    "Please contact your instructor immediately.",
                    "Exam Security Violation"
                )
                
                // Log the violation
                println("CRITICAL: User attempted to disable the exam monitoring plugin")
                println("  Timestamp: ${java.time.LocalDateTime.now()}")
                
                // You could also void the exam here
            }
        }
    }
    
    companion object {
        fun getInstance(): PluginProtectionService = service()
    }
}

class PluginProtectionStarter : StartupActivity {
    override fun runActivity(project: Project) {
        val sessionData = SessionData.getInstance().state
        if (!sessionData.sessionId.isNullOrEmpty()) {
            PluginProtectionService.getInstance().startMonitoring(project)
        }
    }
} 