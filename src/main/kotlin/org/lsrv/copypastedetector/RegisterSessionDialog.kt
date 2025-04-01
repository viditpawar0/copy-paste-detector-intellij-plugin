package org.lsrv.copypastedetector

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.remoteDev.util.addPathSuffix
import com.intellij.ui.dsl.builder.panel
import java.net.ConnectException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.*
import javax.swing.JComponent
import javax.swing.JFrame
import com.intellij.openapi.application.ApplicationManager
import java.awt.Frame

class RegisterSessionDialog : DialogWrapper(true) {
    private val infoDialog = InfoDialog()

    private var sessionId: Int? = null
    private var clientName: String? = null
    private var rollNumber: String? = null

    private var isSessionIdInvalid = false

    init {
        title = "Register Session"
        init()
    }

    override fun createCenterPanel(): JComponent? {
        return panel {
            row("Session ID:") {
                textField()
                    .onChanged {
                        try {
                            sessionId = it.text.toInt()
                            isSessionIdInvalid = false
                        } catch (e: NumberFormatException) {
                            isSessionIdInvalid = true
                        }
                    }
                    .errorOnApply("Session ID is invalid!") { isSessionIdInvalid }
            }
            row("Name:") {
                textField()
                    .onChanged {
                        clientName = it.text
                    }
            }
            row("Roll Number:") {
                textField()
                    .onChanged {
                        rollNumber = it.text
                    }
            }
        }
    }

    override fun doOKAction() {
        if (sessionId == null || clientName.isNullOrBlank() || rollNumber.isNullOrBlank()) {
            infoDialog.show("Please fill all fields")
            return
        }
        
        // Save to session data
        val sessionData = SessionData.getInstance().state
        sessionData.sessionId = sessionId.toString()
        sessionData.clientName = clientName
        sessionData.rollNumber = rollNumber
        sessionData.sessionStartTime = System.currentTimeMillis().toString()
        
        // Log session start
        println("Session started: ID=${sessionId}, Name=${clientName}, Roll=${rollNumber}")
        
        // Accept any session ID without server validation
        super.doOKAction()
        
        // Maximize the window after session starts
        ApplicationManager.getApplication().invokeLater {
            maximizeIdeWindow()
        }
    }

    private fun maximizeIdeWindow() {
        for (frame in Frame.getFrames()) {
            if (frame is JFrame && frame.isVisible) {
                frame.extendedState = Frame.MAXIMIZED_BOTH
                break
            }
        }
    }
}