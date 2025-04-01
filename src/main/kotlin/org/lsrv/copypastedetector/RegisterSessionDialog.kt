package org.lsrv.copypastedetector

import com.intellij.openapi.ui.DialogPanel
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

class RegisterSessionDialog : DialogWrapper(true) {
    private val infoDialog = InfoDialog()

    private var sesId: Int? = null

    private var isSessionIdInvalid = false

    init {
        title = "Register Session"
        init()
    }

    override fun createCenterPanel(): JComponent? {
        val panel: DialogPanel = panel {
            row {
                label("Enter Session ID:")
                textField()
                    .onChanged {
                        try {
                            sesId = it.text.toInt()
                            isSessionIdInvalid = false
                        } catch (e: NumberFormatException) {
                            isSessionIdInvalid = true
                        }
                    }
                    .errorOnApply("Session ID is invalid!") { isSessionIdInvalid }
            }
        }
        return panel
    }

    override fun doOKAction() {
        println(sesId)
        val serverUrl = URI(ResourceBundle.getBundle("plugin").getString("ServerUrl"))
        val client = HttpClient.newBuilder().build()
        if (sesId == null) {
            isSessionIdInvalid = true
            return
        }
        val response : HttpResponse<String>
        try {
            response = client.send(HttpRequest.newBuilder().uri(serverUrl.addPathSuffix("session/${sesId}")).GET().build(), HttpResponse.BodyHandlers.ofString())
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
        sessionState.sessionId = sesId.toString()
        super.doOKAction()
    }
}