package org.lsrv.copypastedetector

import com.intellij.openapi.ui.DialogWrapper
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class InfoDialog : DialogWrapper(false) {
    private val centerPanel = JPanel()

    init {
        init()
    }

    override fun createCenterPanel(): JComponent? {
        return centerPanel
    }
    fun show(message: String) {
        centerPanel.removeAll()
        centerPanel.add(JLabel(message))
        show()
    }
}