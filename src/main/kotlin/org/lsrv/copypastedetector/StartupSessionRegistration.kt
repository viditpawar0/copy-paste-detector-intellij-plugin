package org.lsrv.copypastedetector

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity

class StartupSessionRegistration : StartupActivity {
    override fun runActivity(project: Project) {
        // Check if session is already registered
        val sessionData = SessionData.getInstance().state
        if (sessionData.sessionId.isNullOrEmpty()) {
            // Show registration dialog if no session is registered
            val dialog = RegisterSessionDialog()
            if (dialog.showAndGet()) {
                // Session registered successfully
                println("Session registered on startup")
            }
        }
    }
} 