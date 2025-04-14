package org.lsrv.copypastedetector

import com.intellij.openapi.application.ApplicationListener

class AppListener: ApplicationListener {
    override fun canExitApplication(): Boolean {
        println("AppListener canExitApplication")
        return super.canExitApplication()
    }

    override fun beforeWriteActionStart(action: Any) {
        super.beforeWriteActionStart(action)
        println("beforeWriteActionStart")
    }

    override fun writeActionStarted(action: Any) {
        super.writeActionStarted(action)
        println("writeActionStarted")
    }

    override fun writeActionFinished(action: Any) {
        super.writeActionFinished(action)
        println("writeActionFinished")
    }

    override fun afterWriteActionFinished(action: Any) {
        super.afterWriteActionFinished(action)
        println("afterWriteActionFinished")
    }
}