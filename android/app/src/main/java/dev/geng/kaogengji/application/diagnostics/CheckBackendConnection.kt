package dev.geng.kaogengji.application.diagnostics

class CheckBackendConnection(private val gateway: BackendHealthGateway) {
    suspend operator fun invoke(): BackendHealth = gateway.check()
}
