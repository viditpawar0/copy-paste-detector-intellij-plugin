package org.lsrv.copypastedetector

import com.intellij.ide.actions.TogglePresentationModeAction
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.ui.Messages
import java.awt.Frame
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.time.LocalDateTime
import java.util.*
import javax.swing.JFrame
import javax.swing.Timer

class WindowFocusEnforcer : StartupActivity {
    private var focusTimer: Timer? = null
    private var minimizeWarningCount = 0
    private val MAX_WARNINGS = 3
    
    override fun runActivity(project: Project) {
        val sessionData = SessionData.getInstance().state
        if (sessionData.sessionId.isNullOrEmpty()) {
            return
        }
        
        // Get the main IDE frame
        val frame = getIdeFrame()
        if (frame != null) {
            // Use IntelliJ's presentation mode
            ApplicationManager.getApplication().invokeLater {
                // First maximize the window
                frame.extendedState = Frame.MAXIMIZED_BOTH
                
                // Then enter presentation mode
                val actionManager = ActionManager.getInstance()
                val presentationAction = actionManager.getAction("TogglePresentationMode")
                if (presentationAction != null) {
                    val dataContext = com.intellij.openapi.actionSystem.impl.SimpleDataContext.getProjectContext(project)
                    val event = com.intellij.openapi.actionSystem.AnActionEvent.createFromAnAction(
                        presentationAction,
                        null,
                        "Exam Mode",
                        dataContext
                    )
                    presentationAction.actionPerformed(event)
                    println("Presentation mode enabled for exam session")
                } else {
                    // Fallback to just maximized if presentation mode action not found
                    println("Presentation mode not available, using maximized window")
                }
            }
            
            // Add window listener with warnings
            frame.defaultCloseOperation = JFrame.DO_NOTHING_ON_CLOSE
            frame.addWindowListener(object : WindowAdapter() {
                override fun windowIconified(e: WindowEvent) {
                    // Increment warning count
                    minimizeWarningCount++
                    
                    // Log the minimize event
                    logMinimizeEvent(sessionData, minimizeWarningCount)
                    
                    if (minimizeWarningCount > MAX_WARNINGS) {
                        // Void the exam after exceeding max warnings
                        ApplicationManager.getApplication().invokeLater {
                            Messages.showErrorDialog(
                                project,
                                "You have minimized the exam window ${minimizeWarningCount} times.\n\n" +
                                "This exceeds the maximum allowed warnings (${MAX_WARNINGS}).\n" +
                                "Your exam is now considered null and void.",
                                "Exam Voided"
                            )
                            
                            // Clear session data
                            sessionData.sessionId = null
                            sessionData.sessionStartTime = null
                            
                            // Log the final restriction
                            println("CRITICAL: User restricted after ${minimizeWarningCount} minimize warnings")
                            println("  Session ID: ${sessionData.sessionId}")
                            println("  User: ${sessionData.clientName} (${sessionData.rollNumber})")
                            println("  Timestamp: ${LocalDateTime.now()}")
                            println("  Action: EXAM_VOIDED")
                        }
                    } else {
                        // Show warning with current count
                        ApplicationManager.getApplication().invokeLater {
                            val warningsLeft = MAX_WARNINGS - minimizeWarningCount
                            Messages.showWarningDialog(
                                project,
                                "WARNING: Minimizing the exam window may be considered cheating.\n\n" +
                                "This is warning ${minimizeWarningCount} of ${MAX_WARNINGS}.\n" +
                                "You have ${warningsLeft} warnings remaining.\n\n" +
                                "If you exceed ${MAX_WARNINGS} warnings, your exam will be voided.",
                                "Exam Window Minimized"
                            )
                        }
                    }
                }
                
                override fun windowDeiconified(e: WindowEvent) {
                    println("INFO: Window was restored from minimized state")
                    // When restored, maximize again and re-enter presentation mode
                    ApplicationManager.getApplication().invokeLater {
                        frame.extendedState = Frame.MAXIMIZED_BOTH
                        
                        // Re-enter presentation mode
                        val actionManager = ActionManager.getInstance()
                        val presentationAction = actionManager.getAction("TogglePresentationMode")
                        if (presentationAction != null) {
                            val dataContext = com.intellij.openapi.actionSystem.impl.SimpleDataContext.getProjectContext(project)
                            val event = com.intellij.openapi.actionSystem.AnActionEvent.createFromAnAction(
                                presentationAction,
                                null,
                                "Exam Mode",
                                dataContext
                            )
                            presentationAction.actionPerformed(event)
                        }
                    }
                }
                
                override fun windowClosing(e: WindowEvent) {
                    // Show warning when trying to close
                    val result = Messages.showYesNoDialog(
                        project,
                        "WARNING: You are attempting to close the exam window.\n\n" +
                        "If you close this window, your exam will be considered null and void.\n" +
                        "This action cannot be undone.\n\n" +
                        "Are you sure you want to exit the exam?",
                        "Exit Exam Confirmation",
                        "Exit Exam (Void Results)",
                        "Continue Exam",
                        Messages.getWarningIcon()
                    )
                    
                    if (result == Messages.YES) {
                        // User confirmed exit - clear session and allow close
                        println("User confirmed exam exit - clearing session")
                        sessionData.sessionId = null
                        sessionData.sessionStartTime = null
                        
                        // Exit presentation mode before closing
                        val actionManager = ActionManager.getInstance()
                        val presentationAction = actionManager.getAction("TogglePresentationMode")
                        if (presentationAction != null) {
                            val dataContext = com.intellij.openapi.actionSystem.impl.SimpleDataContext.getProjectContext(project)
                            val event = com.intellij.openapi.actionSystem.AnActionEvent.createFromAnAction(
                                presentationAction,
                                null,
                                "Exam Mode",
                                dataContext
                            )
                            presentationAction.actionPerformed(event)
                        }
                        
                        // Allow the window to close
                        frame.defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
                        frame.dispatchEvent(WindowEvent(frame, WindowEvent.WINDOW_CLOSING))
                    }
                }
            })
            
            // Monitor focus changes
            focusTimer = Timer(2000) { // Check every 2 seconds
                if (!frame.isFocused) {
                    println("INFO: Window lost focus")
                }
            }
            focusTimer?.start()
        }
    }
    
    private fun logMinimizeEvent(sessionData: SessionData.Session, warningCount: Int) {
        println("WARNING: Window minimize detected!")
        println("  Session ID: ${sessionData.sessionId}")
        println("  User: ${sessionData.clientName} (${sessionData.rollNumber})")
        println("  Warning count: ${warningCount}/${MAX_WARNINGS}")
        println("  Timestamp: ${LocalDateTime.now()}")
        
        // In the future, this would send data to a server
        val logEntry = """
            {
                "event": "WINDOW_MINIMIZED",
                "sessionId": "${sessionData.sessionId}",
                "clientName": "${sessionData.clientName}",
                "rollNumber": "${sessionData.rollNumber}",
                "warningCount": ${warningCount},
                "maxWarnings": ${MAX_WARNINGS},
                "timestamp": "${LocalDateTime.now()}"
            }
        """.trimIndent()
        
        println("Log entry: $logEntry")
    }
    
    private fun getIdeFrame(): JFrame? {
        for (frame in Frame.getFrames()) {
            if (frame is JFrame && frame.isVisible) {
                return frame
            }
        }
        return null
    }
} 