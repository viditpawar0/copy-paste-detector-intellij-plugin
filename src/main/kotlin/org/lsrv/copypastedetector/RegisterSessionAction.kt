package org.lsrv.copypastedetector

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class RegisterSessionAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        println("Registering session...")
        RegisterSessionDialog().show()
    }
}