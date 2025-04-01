package org.lsrv.copypastedetector

import com.intellij.ide.actions.TogglePresentationModeAction
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.ui.Messages
import java.awt.Frame
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.awt.event.WindowStateListener
import java.time.LocalDateTime
import java.util.*
import javax.swing.JFrame
import javax.swing.Timer
import java.awt.event.ActionListener

class WindowFocusEnforcer : StartupActivity {
    private var focusTimer: Timer? = null
    private var minimizeWarningCount = 0
    private var resizeWarningCount = 0
    private val MAX_WARNINGS = 3
    private var lastFrameState = -1
    
    // Flag to ignore initial resize events when entering presentation mode
    private var ignoreNextResize = true
    
    override fun runActivity(project: Project) {
        val sessionData = SessionData.getInstance().state
        if (sessionData.sessionId.isNullOrEmpty()) {
            return
        }
        
        // Show initial warning message about monitoring
        ApplicationManager.getApplication().invokeLater {
            Messages.showWarningDialog(
                project,
                "EXAM SESSION STARTED\n\n" +
                "Please be aware that your activities are being monitored during this exam:\n\n" +
                "• Copy-paste operations are tracked\n" +
                "• Window minimizing is not allowed\n" +
                "• Window resizing is not allowed\n" +
                "• Closing the application will void your exam\n\n" +
                "The application will now enter presentation mode. Please focus on your exam and avoid any actions that could be considered cheating.",
                "Exam Monitoring Active"
            )
        }
        
        // Get the main IDE frame
        val frame = getIdeFrame()
        if (frame != null) {
            // Store initial frame state
            lastFrameState = frame.extendedState
            
            // Use IntelliJ's presentation mode
            ApplicationManager.getApplication().invokeLater {
                // First maximize the window
                frame.extendedState = Frame.MAXIMIZED_BOTH
                lastFrameState = frame.extendedState
                
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
                    
                    // Reset the ignore flag after a delay to allow presentation mode to complete
                    Timer(2000) { ignoreNextResize = false }.start()
                } else {
                    // Fallback to just maximized if presentation mode action not found
                    println("Presentation mode not available, using maximized window")
                    ignoreNextResize = false
                }
            }
            
            // Add component listener to detect manual resizing (dragging window edges)
            frame.addComponentListener(object : ComponentAdapter() {
                private var lastWidth = frame.width
                private var lastHeight = frame.height
                
                override fun componentResized(e: ComponentEvent) {
                    // Skip initial events and state changes
                    if (ignoreNextResize || frame.extendedState != lastFrameState) {
                        lastWidth = frame.width
                        lastHeight = frame.height
                        return
                    }
                    
                    // Detect significant manual resizing
                    val widthChange = Math.abs(frame.width - lastWidth)
                    val heightChange = Math.abs(frame.height - lastHeight)
                    
                    if (widthChange > 20 || heightChange > 20) {
                        println("Manual resize detected: width change=$widthChange, height change=$heightChange")
                        handleResize(project, sessionData)
                    }
                    
                    lastWidth = frame.width
                    lastHeight = frame.height
                }
            })
            
            // Add a more robust window state listener
            frame.addWindowStateListener(object : WindowStateListener {
                private var wasMaximized = (frame.extendedState and Frame.MAXIMIZED_BOTH) != 0
                
                override fun windowStateChanged(e: WindowEvent) {
                    // Skip initial events during setup
                    if (ignoreNextResize) {
                        return
                    }
                    
                    val isNowMaximized = (e.newState and Frame.MAXIMIZED_BOTH) != 0
                    
                    // Detect toggling between maximized and non-maximized states
                    // This catches the green button clicks
                    if (wasMaximized != isNowMaximized) {
                        println("Window maximize/restore state changed: was=$wasMaximized, now=$isNowMaximized")
                        
                        // Only warn when going from maximized to non-maximized
                        // (we don't want to warn when the user maximizes the window)
                        if (wasMaximized && !isNowMaximized) {
                            handleResize(project, sessionData)
                        }
                        
                        wasMaximized = isNowMaximized
                    }
                }
            })

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
                        voidExam(project, sessionData, "minimizing the window", minimizeWarningCount)
                    } else {
                        // Show warning with current count
                        ApplicationManager.getApplication().invokeLater {
                            val warningsLeft = MAX_WARNINGS - minimizeWarningCount
                            Messages.showWarningDialog(
                                project,
                                "WARNING: Minimizing the exam window may be considered cheating.\n\n" +
                                "This is warning ${minimizeWarningCount} of ${MAX_WARNINGS}.\n" +
                                "You have ${warningsLeft} warnings remaining.\n\n" +
                                "If you exceed ${MAX_WARNINGS} warnings, your exam will be voided.\n\n" +
                                "The window will now return to full screen mode. Please do not minimize it again.",
                                "Exam Window Minimized"
                            )
                            
                            // Restore presentation mode after the warning is dismissed
                            restorePresentationMode(project, frame)
                        }
                    }
                }
                
                override fun windowDeiconified(e: WindowEvent) {
                    println("INFO: Window was restored from minimized state")
                    // When restored, maximize again and re-enter presentation mode
                    restorePresentationMode(project, frame)
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
    
    private fun handleResize(project: Project, sessionData: SessionData.Session) {
        // Increment resize warning count
        resizeWarningCount++
        
        // Log the resize event
        logResizeEvent(sessionData, resizeWarningCount)
        
        if (resizeWarningCount > MAX_WARNINGS) {
            // Void the exam after exceeding max warnings
            voidExam(project, sessionData, "resizing the window", resizeWarningCount)
        } else {
            // Show warning with current count
            ApplicationManager.getApplication().invokeLater {
                val warningsLeft = MAX_WARNINGS - resizeWarningCount
                val result = Messages.showWarningDialog(
                    project,
                    "WARNING: Resizing the exam window may be considered cheating.\n\n" +
                    "This is warning ${resizeWarningCount} of ${MAX_WARNINGS}.\n" +
                    "You have ${warningsLeft} warnings remaining.\n\n" +
                    "If you exceed ${MAX_WARNINGS} warnings, your exam will be voided.\n\n" +
                    "The window will now return to full screen mode. Please do not resize it again.",
                    "Exam Window Resized"
                )
                
                // Force presentation mode immediately after dialog is closed
                ApplicationManager.getApplication().invokeLater {
                    // Get the current frame
                    val currentFrame = getIdeFrame()
                    if (currentFrame != null) {
                        // First maximize
                        currentFrame.extendedState = Frame.MAXIMIZED_BOTH
                        
                        // Then directly toggle presentation mode
                        val actionManager = ActionManager.getInstance()
                        val presentationAction = actionManager.getAction("TogglePresentationMode")
                        if (presentationAction != null) {
                            // Check if we're already in presentation mode
                            val dataContext = com.intellij.openapi.actionSystem.impl.SimpleDataContext.getProjectContext(project)
                            val event = com.intellij.openapi.actionSystem.AnActionEvent.createFromAnAction(
                                presentationAction,
                                null,
                                "Exam Mode",
                                dataContext
                            )
                            
                            // Force presentation mode on
                            presentationAction.actionPerformed(event)
                            println("Presentation mode forced after resize warning")
                        }
                    } else {
                        println("ERROR: Could not find IDE frame for restoration after resize warning")
                    }
                }
            }
        }
    }
    
    private fun voidExam(project: Project, sessionData: SessionData.Session, action: String, warningCount: Int) {
        ApplicationManager.getApplication().invokeLater {
            Messages.showErrorDialog(
                project,
                "You have $action ${warningCount} times.\n\n" +
                "This exceeds the maximum allowed warnings (${MAX_WARNINGS}).\n" +
                "Your exam is now considered null and void.",
                "Exam Voided"
            )
            
            // Clear session data
            sessionData.sessionId = null
            sessionData.sessionStartTime = null
            
            // Log the final restriction
            println("CRITICAL: User restricted after ${warningCount} warnings for $action")
            println("  Session ID: ${sessionData.sessionId}")
            println("  User: ${sessionData.clientName} (${sessionData.rollNumber})")
            println("  Timestamp: ${LocalDateTime.now()}")
            println("  Action: EXAM_VOIDED")
        }
    }
    
    private fun restorePresentationMode(project: Project, frame: JFrame) {
        // Set a flag to ignore resize events during restoration
        ignoreNextResize = true
        
        ApplicationManager.getApplication().invokeLater {
            // First maximize
            frame.extendedState = Frame.MAXIMIZED_BOTH
            lastFrameState = frame.extendedState
            
            // Then re-enter presentation mode
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
                println("Presentation mode restored")
                
                // Reset the ignore flag after a delay
                Timer(1000) { ignoreNextResize = false }.start()
            } else {
                // If presentation mode action not found, just maximize
                println("Presentation mode not available, using maximized window")
                ignoreNextResize = false
            }
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
    
    private fun logResizeEvent(sessionData: SessionData.Session, warningCount: Int) {
        println("WARNING: Window resize detected!")
        println("  Session ID: ${sessionData.sessionId}")
        println("  User: ${sessionData.clientName} (${sessionData.rollNumber})")
        println("  Warning count: ${warningCount}/${MAX_WARNINGS}")
        println("  Timestamp: ${LocalDateTime.now()}")
        
        // In the future, this would send data to a server
        val logEntry = """
            {
                "event": "WINDOW_RESIZED",
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