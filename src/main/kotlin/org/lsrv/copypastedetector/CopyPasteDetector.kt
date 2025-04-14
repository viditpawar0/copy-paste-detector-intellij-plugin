package org.lsrv.copypastedetector

import com.intellij.codeInsight.editorActions.CopyPastePreProcessor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.RawText
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class CopyPasteDetector : CopyPastePreProcessor {
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
        postSnippet(text, SnippetType.PASTED)
        println("Pasted: $text")
        return text.orEmpty()
    }

    private fun postSnippet(text: String?, type: SnippetType) {
//        if (isSessionActive()) return
        if(sessionData.sessionId == null) return
        val body = buildJsonObject {
            put("session", sessionData.sessionId)
            put("clientName", sessionData.clientName)
            put("content", text)
            put("type", type.name)
        }
        sendToServer(body, ServerEntity.SNIPPET)
    }
}