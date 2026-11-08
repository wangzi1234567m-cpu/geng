package dev.geng.kaogengji

import dev.geng.kaogengji.application.diagnostics.CheckBackendConnection
import dev.geng.kaogengji.core.BackendOrigin
import dev.geng.kaogengji.data.diagnostics.HttpBackendHealthGateway

/** Composition root only. UI and use cases do not construct network adapters. */
class AppContainer {
    val checkBackendConnection = CheckBackendConnection(
        HttpBackendHealthGateway(BackendOrigin.parse(BuildConfig.BACKEND_ORIGIN, BuildConfig.DEBUG)),
    )
}
