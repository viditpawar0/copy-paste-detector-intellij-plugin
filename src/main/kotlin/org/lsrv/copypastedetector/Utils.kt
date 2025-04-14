package org.lsrv.copypastedetector

import com.intellij.remoteDev.util.addPathSuffix
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodySubscribers
import java.time.Instant
import java.util.*

private val serverUrl = URI(ResourceBundle.getBundle("plugin").getString("ServerUrl"))
private val client = HttpClient.newBuilder().build()
private val sessionData = SessionData.getInstance().state

fun isSessionActive(): Boolean {
    return Instant.now().isBefore(Instant.parse(sessionData.endsAt)) && sessionData.sessionId != null
}

fun reportWarning(text: String) {
//    if (isSessionActive()) return
    if(sessionData.sessionId == null) return
    val body = buildJsonObject {
        put("session", sessionData.sessionId)
        put("clientName", sessionData.clientName)
        put("text", text)
        put("severity", WarningSeverity.HIGH.name)
    }
    sendToServer(body, ServerEntity.WARNING)
}

fun sendToServer(body: JsonObject, entity: ServerEntity) {
    val request = HttpRequest
        .newBuilder()
        .uri(serverUrl.addPathSuffix(entity.urlPathSuffix))
        .setHeader("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
        .build()
    client.sendAsync(request) {
        println("Status code: ${it.statusCode()}")
        BodySubscribers.ofString(charset("UTF-8"))
    }.handleAsync { response, throwable ->
        println("Response:${response.body()}")
        println("Throwable:${throwable.message}")
    }
}