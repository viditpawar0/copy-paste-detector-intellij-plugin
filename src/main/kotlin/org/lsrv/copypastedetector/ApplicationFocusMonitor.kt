package org.lsrv.copypastedetector

import com.intellij.openapi.application.ApplicationActivationListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.IdeFrame
import com.intellij.remoteDev.util.addPathSuffix
import com.intellij.ui.AppUIUtil
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.LocalDateTime
import java.util.*

class ApplicationFocusMonitor : ApplicationActivationListener {
    private val serverUrl = URI(ResourceBundle.getBundle("plugin").getString("ServerUrl"))
    private val client = HttpClient.newBuilder().build()
    private val sessionData = SessionData.getInstance().state
    
    private var warningCount = 0
    private val MAX_WARNINGS = 3
    
    override fun applicationActivated(ideFrame: IdeFrame) {
        // IDE gained focus - no action needed
    }

    override fun applicationDeactivated(ideFrame: IdeFrame) {
        // IDE lost focus - log this event and show warning
        if (!isSessionActive()) {
            return
        }
    
        warningCount++
        // Log the focus loss event
        logFocusLossEvent()
        
        // Show warning when user returns to IDE
        ApplicationManager.getApplication().invokeLater {
            val project = ProjectManager.getInstance().openProjects.firstOrNull()
            if (project != null) {
                val message = if (warningCount >= MAX_WARNINGS) {
                    "WARNING: You have switched away from the IDE ${warningCount} times.\n" +
                    "This activity is being logged and may be considered a violation of exam rules.\n" +
                    "Further violations may result in automatic termination of your exam session."
                } else {
                    "WARNING: Switching away from the IDE during an exam is not allowed.\n" +
                    "This activity has been logged. (Warning ${warningCount}/${MAX_WARNINGS})"
                }
                
                Messages.showWarningDialog(
                    project,
                    message,
                    "Exam Session Violation"
                )
                
                // If too many warnings, consider terminating the session
                if (warningCount > MAX_WARNINGS) {
                    considerTerminatingSession(project)
                }
            }
        }
    }
    
    private fun isSessionActive(): Boolean {
        // Simply check if we have a session ID
        return !sessionData.sessionId.isNullOrEmpty()
    }
    
    private fun logFocusLossEvent() {
        // Log to console for now
        println("WARNING: Focus loss detected!")
        println("  Session ID: ${sessionData.sessionId}")
        println("  User: ${sessionData.clientName} (${sessionData.rollNumber})")
        println("  Warning count: ${warningCount}")
        println("  Timestamp: ${LocalDateTime.now()}")
        
        // Still build the JSON for future server integration
        val body = buildJsonObject {
            put("session", sessionData.sessionId)
            put("clientName", sessionData.clientName)
            put("rollNumber", sessionData.rollNumber)
            put("activity", "IDE_FOCUS_LOST")
            put("warningCount", warningCount)
            put("timestamp", LocalDateTime.now().toString())
        }
        
        // Print the JSON that would be sent to server
        println("Would send to server: ${body.toString()}")
    }
    
    private fun considerTerminatingSession(project: Project) {
        val result = Messages.showYesNoDialog(
            project,
            "You have repeatedly violated exam rules by switching away from the IDE.\n" +
            "Your session may be terminated. Do you want to continue with the exam?",
            "Exam Session Violation",
            "Continue Exam",
            "End Session",
            Messages.getWarningIcon()
        )
        
        if (result == Messages.NO) {
            // User chose to end session
            sessionData.sessionId = null
            sessionData.sessionStartTime = null
            
            Messages.showMessageDialog(
                project,
                "Your exam session has been terminated.",
                "Session Ended",
                Messages.getInformationIcon()
            )
        }
    }
} 