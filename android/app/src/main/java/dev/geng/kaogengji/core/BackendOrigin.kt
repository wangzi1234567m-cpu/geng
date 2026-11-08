package dev.geng.kaogengji.core

import java.net.URI

class BackendOrigin private constructor(private val uri: URI) {
    val healthUri: URI get() = uri.resolve("/actuator/health")

    companion object {
        fun parse(value: String, development: Boolean): BackendOrigin {
            val uri = URI(value)
            require(uri.host != null && uri.userInfo == null && uri.query == null && uri.fragment == null) {
                "Backend origin must be an origin without credentials"
            }
            require(uri.path.isNullOrEmpty() || uri.path == "/") { "Backend origin must not include a path" }
            require(uri.port == -1 || uri.port in 1..65535) { "Invalid backend port" }
            val localHosts = setOf("localhost", "127.0.0.1", "10.0.2.2")
            require(uri.scheme == "https" || (development && uri.scheme == "http" && uri.host in localHosts)) {
                "HTTPS is required outside local development"
            }
            return BackendOrigin(uri)
        }
    }
}
