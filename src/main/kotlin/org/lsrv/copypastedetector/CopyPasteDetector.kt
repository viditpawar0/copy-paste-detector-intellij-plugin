package org.lsrv.copypastedetector

import com.intellij.codeInsight.editorActions.CopyPastePreProcessor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.RawText
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.remoteDev.util.addPathSuffix
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodySubscribers
import java.time.LocalDateTime
import java.util.*

class CopyPasteDetector : CopyPastePreProcessor {
    private val serverUrl = URI(ResourceBundle.getBundle("plugin").getString("ServerUrl"))
    private val client = HttpClient.newBuilder().build()
    private val sessionData = SessionData.getInstance().state

    override fun preprocessOnCopy(
        file: PsiFile?,
        startOffsets: IntArray?,
        endOffsets: IntArray?,
        text: String?
    ): String? {
        println("Copied: $text")
        postSnippet(text, SnippetType.COPIED)
        return text
    }

    override fun preprocessOnPaste(
        project: Project?,
        file: PsiFile?,
        editor: Editor?,
        text: String?,
        rawText: RawText?
    ): String {
        if (sessionData.sessionId == null) {
            return text.orEmpty()
        }
        postSnippet(text, SnippetType.PASTED)
        println("Pasted: $text")
        return text.orEmpty()
    }

    private fun postSnippet(text: String?, type: SnippetType) {
        val body = buildJsonObject {
            put("session", sessionData.sessionId)
            put("clientName", sessionData.clientName)
            put("content", text)
            put("type", type.toString())
            put("createdAt", LocalDateTime.now().toString())
        }
        println(LocalDateTime.now().toString())
        val request = HttpRequest
            .newBuilder()
            .uri(serverUrl.addPathSuffix("snippet"))
            .setHeader("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
            .build()
        client.sendAsync(request) {
            println("Status code: ${it.statusCode()}")
            BodySubscribers.ofString(charset("UTF-8"))
        }.handleAsync {response, throwable ->
            println(response.body())
            println(throwable.message)
        }
    }
}