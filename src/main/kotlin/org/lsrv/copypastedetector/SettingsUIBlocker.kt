package org.lsrv.copypastedetector

import com.intellij.ide.actions.ShowSettingsAction
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.ui.Messages
import java.lang.reflect.Proxy
import java.time.LocalDateTime

@Service
class SettingsUIBlocker : StartupActivity {
    override fun runActivity(project: Project) {
        val sessionData = SessionData.getInstance().state
        if (sessionData.sessionId.isNullOrEmpty()) {
            return
        }
        
        // Block settings by replacing the ShowSettings action
        try {
            // 1. Replace all settings-related actions
            replaceSettingsActions(sessionData)
            
            // 2. Replace the ShowSettingsUtil instance with a proxy
            replaceShowSettingsUtil(sessionData)
            
            // 3. Add a global action listener to catch any settings-related actions
            addGlobalActionListener(sessionData)
            
            println("Settings access blocking initialized successfully")
        } catch (e: Exception) {
            println("Failed to initialize settings blocking: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private fun replaceSettingsActions(sessionData: SessionData.Session) {
        val actionManager = ActionManager.getInstance()
        
        // List of all settings-related action IDs
        val settingsActions = listOf(
            "ShowSettings",
            "WelcomeScreen.Settings",
            "ShowProjectStructureSettings",
            "ShowApplicationSettings",
            "Preferences",
            "Configure",
            "PluginSettings",
            "ConfigurePlugins",
            "Configure.Plugins.Selection"
        )
        
        for (actionId in settingsActions) {
            val action = actionManager.getAction(actionId)
            if (action != null) {
                // Create a blocking version
                val blockingAction = object : AnAction() {
                    override fun actionPerformed(e: AnActionEvent) {
                        val currentProject = e.project ?: ProjectManager.getInstance().openProjects.firstOrNull()
                        if (currentProject != null) {
                            ApplicationManager.getApplication().invokeLater {
                                Messages.showWarningDialog(
                                    currentProject,
                                    "Access to settings is restricted during the exam.\n\n" +
                                    "This activity has been logged.",
                                    "Restricted Action"
                                )
                            }
                        }
                        
                        // Log the attempt
                        println("WARNING: Settings access attempted via $actionId")
                        println("  Session ID: ${sessionData.sessionId}")
                        println("  User: ${sessionData.clientName} (${sessionData.rollNumber}}")
                        println("  Timestamp: ${LocalDateTime.now()}")
                        
                        // Consume the event - don't let it propagate
                        e.presentation.isEnabled = false
                    }
                    
                    override fun update(e: AnActionEvent) {
                        // Keep the same presentation as the original action
                        e.presentation.copyFrom(action.templatePresentation)
                    }
                }
                
                // Replace the action
                actionManager.unregisterAction(actionId)
                actionManager.registerAction(actionId, blockingAction)
                
                println("Replaced settings action: $actionId")
            }
        }
    }
    
    private fun replaceShowSettingsUtil(sessionData: SessionData.Session) {
        try {
            // Get the current instance field
            val showSettingsUtilClass = ShowSettingsUtil::class.java
            val instanceField = showSettingsUtilClass.getDeclaredField("ourInstance")
            instanceField.isAccessible = true
            
            // Get the current instance
            val originalInstance = instanceField.get(null)
            
            // Create a proxy that blocks all method calls
            val proxyInstance = Proxy.newProxyInstance(
                showSettingsUtilClass.classLoader,
                arrayOf(showSettingsUtilClass)
            ) { _, method, args ->
                // For any method call, show warning and block
                val methodName = method.name
                if (methodName.startsWith("show") && methodName.contains("Settings", ignoreCase = true)) {
                    // This is a settings-related method
                    val project = args?.firstOrNull() as? Project
                    val currentProject = project ?: ProjectManager.getInstance().openProjects.firstOrNull()
                    
                    if (currentProject != null) {
                        ApplicationManager.getApplication().invokeLater {
                            Messages.showWarningDialog(
                                currentProject,
                                "Access to settings is restricted during the exam.\n\n" +
                                "This activity has been logged.",
                                "Restricted Action"
                            )
                        }
                    }
                    
                    // Log the attempt
                    println("WARNING: Settings access attempted via ShowSettingsUtil.$methodName")
                    println("  Session ID: ${sessionData.sessionId}")
                    println("  User: ${sessionData.clientName} (${sessionData.rollNumber}}")
                    println("  Timestamp: ${LocalDateTime.now()}")
                    
                    // Return a default value based on return type
                    return@newProxyInstance when (method.returnType) {
                        Boolean::class.java -> false
                        Void.TYPE -> null
                        else -> null
                    }
                }
                
                // For non-settings methods, call the original
                method.invoke(originalInstance, *(args ?: emptyArray()))
            }
            
            // Replace the instance
            instanceField.set(null, proxyInstance)
            println("Successfully replaced ShowSettingsUtil with blocking proxy")
            
        } catch (e: Exception) {
            println("Failed to replace ShowSettingsUtil: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private fun addGlobalActionListener(sessionData: SessionData.Session) {
        // This would add a global action listener to catch any settings-related actions
        // that might slip through our other defenses
        // Implementation depends on the specific IntelliJ Platform version
    }
} 