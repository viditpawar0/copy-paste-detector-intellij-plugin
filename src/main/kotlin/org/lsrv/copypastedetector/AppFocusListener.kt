package org.lsrv.copypastedetector

import com.intellij.openapi.application.ApplicationActivationListener
import com.intellij.openapi.wm.IdeFrame
import java.awt.Window

class AppFocusListener: ApplicationActivationListener {
    override fun applicationActivated(ideFrame: IdeFrame) {
        super.applicationActivated(ideFrame)
        println("AppFocusListener: applicationActivated")
    }

    override fun applicationDeactivated(ideFrame: IdeFrame) {
        super.applicationDeactivated(ideFrame)
        println("AppFocusListener: applicationDeactivated")
        reportWarning("Window Switched!")
    }

    override fun delayedApplicationDeactivated(ideFrame: Window) {
        super.delayedApplicationDeactivated(ideFrame)
        println("AppFocusListener: delayedApplicationDeactivated")
    }
}