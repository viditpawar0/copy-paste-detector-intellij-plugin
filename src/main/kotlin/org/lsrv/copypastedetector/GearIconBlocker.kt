package org.lsrv.copypastedetector

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.ui.Messages
import java.time.LocalDateTime

class GearIconBlocker : StartupActivity {
    override fun runActivity(project: Project) {
        val sessionData = SessionData.getInstance().state
        if (sessionData.sessionId.isNullOrEmpty()) {
            return
        }
        
        try {
            // Block the gear icon actions
            val gearActions = listOf(
                "GearButton", 
                "ShowSettings", 
                "OptionsMenu",
                "ToolbarSettingsGroup"
            )
            
            val actionManager = ActionManager.getInstance()
            
            for (actionId in gearActions) {
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
                            println("WARNING: Settings access attempted via gear icon ($actionId)")
                            println("  Session ID: ${sessionData.sessionId}")
                            println("  User: ${sessionData.clientName} (${sessionData.rollNumber}}")
                            println("  Timestamp: ${LocalDateTime.now()}")
                        }
                        
                        override fun update(e: AnActionEvent) {
                            // Keep the same presentation as the original action
                            e.presentation.copyFrom(action.templatePresentation)
                        }
                    }
                    
                    // Replace the action
                    actionManager.unregisterAction(actionId)
                    actionManager.registerAction(actionId, blockingAction)
                    
                    println("Blocked gear icon action: $actionId")
                }
            }
        } catch (e: Exception) {
            println("Failed to block gear icon: ${e.message}")
            e.printStackTrace()
        }
    }
} 