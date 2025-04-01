package org.lsrv.copypastedetector

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class SecurityLogger {
    private val logFile: File
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    
    init {
        // Create log directory if it doesn't exist
        val logDir = File(System.getProperty("user.home"), ".intellij-exam-logs")
        if (!logDir.exists()) {
            logDir.mkdirs()
        }
        
        // Create a log file for this session
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        logFile = File(logDir, "exam_security_log_$timestamp.txt")
        
        // Write header
        logFile.writeText("IntelliJ Exam Security Log\n")
        logFile.appendText("Started: ${LocalDateTime.now().format(dateTimeFormatter)}\n")
        logFile.appendText("----------------------------------------\n\n")
    }
    
    fun logWarning(event: String, sessionData: SessionData.Session) {
        val logEntry = buildString {
            append("[WARNING] $event\n")
            append("  Time: ${LocalDateTime.now().format(dateTimeFormatter)}\n")
            append("  Session ID: ${sessionData.sessionId}\n")
            append("  User: ${sessionData.clientName} (${sessionData.rollNumber})\n")
            append("----------------------------------------\n")
        }
        
        // Log to console
        println(logEntry)
        
        // Log to file
        synchronized(this) {
            logFile.appendText(logEntry)
        }
    }
    
    fun logCritical(event: String, sessionData: SessionData.Session) {
        val logEntry = buildString {
            append("[CRITICAL] $event\n")
            append("  Time: ${LocalDateTime.now().format(dateTimeFormatter)}\n")
            append("  Session ID: ${sessionData.sessionId}\n")
            append("  User: ${sessionData.clientName} (${sessionData.rollNumber})\n")
            append("----------------------------------------\n")
        }
        
        // Log to console
        println(logEntry)
        
        // Log to file
        synchronized(this) {
            logFile.appendText(logEntry)
        }
    }
    
    fun logInfo(event: String, sessionData: SessionData.Session) {
        val logEntry = buildString {
            append("[INFO] $event\n")
            append("  Time: ${LocalDateTime.now().format(dateTimeFormatter)}\n")
            append("  Session ID: ${sessionData.sessionId}\n")
            append("  User: ${sessionData.clientName} (${sessionData.rollNumber})\n")
            append("----------------------------------------\n")
        }
        
        // Log to console
        println(logEntry)
        
        // Log to file
        synchronized(this) {
            logFile.appendText(logEntry)
        }
    }
    
    companion object {
        fun getInstance(): SecurityLogger = service()
    }
} 