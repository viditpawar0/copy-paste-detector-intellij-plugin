package org.lsrv.copypastedetector

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.ex.AnActionListener

class ActionListener: AnActionListener {
    override fun beforeActionPerformed(action: AnAction, event: AnActionEvent) {
        super.beforeActionPerformed(action, event)
        println("Before:-")
        println("Action:${action.templateText}")
        println("Event:${event.presentation.text}")
    }

    override fun afterActionPerformed(action: AnAction, event: AnActionEvent, result: AnActionResult) {
        super.afterActionPerformed(action, event, result)
        println("After:-")
        println("Action:${action.templateText}")
        println("Event:${event.presentation.text}")
    }

    override fun beforeEditorTyping(c: Char, dataContext: DataContext) {
        super.beforeEditorTyping(c, dataContext)
    }

    override fun afterEditorTyping(c: Char, dataContext: DataContext) {
        super.afterEditorTyping(c, dataContext)
    }

    override fun beforeShortcutTriggered(shortcut: Shortcut, actions: MutableList<AnAction>, dataContext: DataContext) {
        super.beforeShortcutTriggered(shortcut, actions, dataContext)
    }
}