package org.lsrv.copypastedetector

import com.intellij.openapi.components.*

@State(name = "SessionData", storages = [Storage(StoragePathMacros.CACHE_FILE)])
@Service
class SessionData : SimplePersistentStateComponent<SessionData.Session>(Session()){
    class Session: BaseState() {
        var sessionId by string()
        var clientName by string()
        var endsAt by string()
    }
    companion object {
        fun getInstance(): SessionData = service()
    }
}