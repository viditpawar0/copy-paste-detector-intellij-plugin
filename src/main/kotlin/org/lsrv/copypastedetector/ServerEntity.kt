package org.lsrv.copypastedetector

enum class ServerEntity(val urlPathSuffix: String) {
    SESSION("session"),
    SNIPPET("snippet"),
    WARNING("warning"),
}