package dev.geng.kaogengji.application.diagnostics

enum class BackendHealth { REACHABLE, UNREACHABLE }

interface BackendHealthGateway {
    suspend fun check(): BackendHealth
}
