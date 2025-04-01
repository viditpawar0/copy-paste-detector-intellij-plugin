package org.lsrv.copypastedetector

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.remoteDev.util.addPathSuffix
import com.intellij.ui.dsl.builder.panel
import kotlinx.serialization.json.Json.Default.parseToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.ConnectException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.*
import javax.swing.JComponent

class RegisterSessionDialog : DialogWrapper(true) {
    private val infoDialog = InfoDialog()

    private var sessionId: Int? = null

    private var clientName: String? = null

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
            row("Client Name:") {
                textField()
                    .onChanged {
                        clientName = it.text
                    }
            }
        }
    }

    override fun doOKAction() {
        val serverUrl = URI(ResourceBundle.getBundle("plugin").getString("ServerUrl"))
        val client = HttpClient.newBuilder().build()
        if (sessionId == null) {
            isSessionIdInvalid = true
            return
        }
        val response : HttpResponse<String>
        try {
            response = client.send(
                HttpRequest
                    .newBuilder()
                    .uri(serverUrl.addPathSuffix("session/${sessionId}"))
                    .GET()
                    .build(),
                HttpResponse.BodyHandlers.ofString())
        } catch (e: ConnectException) {
            infoDialog.show("Connection error")
            return
        }
        if (response.statusCode() == 400) {
            isSessionIdInvalid = true
            return
        }
        if (response.statusCode() != 200) {
            infoDialog.show("Http Error: ${response.statusCode()}")
            return
        }
        println("Response: ${response.body()}")
        val sessionState = SessionData.getInstance().state
        sessionState.clientName = clientName
        parseToJsonElement(response.body()).jsonObject.also {
            sessionState.sessionId = it["id"]?.jsonPrimitive?.content
            sessionState.endsAt = it["endsAt"]?.jsonPrimitive?.content
        }
        println(sessionState.toString())
        super.doOKAction()
    }
}