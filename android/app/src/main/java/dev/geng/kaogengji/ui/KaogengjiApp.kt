package dev.geng.kaogengji.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.geng.kaogengji.AppContainer
import dev.geng.kaogengji.BuildConfig
import dev.geng.kaogengji.R
import dev.geng.kaogengji.ui.foundation.ConnectionState
import dev.geng.kaogengji.ui.foundation.FoundationViewModel

@Composable
fun KaogengjiApp() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineLarge)
                Text(stringResource(R.string.bootstrap_subtitle))
                if (BuildConfig.DEBUG) {
                    DevelopmentConnection()
                }
            }
        }
    }
}

@Composable
private fun DevelopmentConnection() {
    val container = remember { AppContainer() }
    val model: FoundationViewModel = viewModel(factory = FoundationViewModel.Factory(container.checkBackendConnection))
    val state by model.state.collectAsState()
    val label = when (state) {
        ConnectionState.NOT_CHECKED -> R.string.connection_not_checked
        ConnectionState.CHECKING -> R.string.connection_checking
        ConnectionState.REACHABLE -> R.string.connection_reachable
        ConnectionState.UNREACHABLE -> R.string.connection_unreachable
    }
    Text(stringResource(R.string.bootstrap_status), style = MaterialTheme.typography.labelMedium)
    Text(stringResource(label))
    Button(onClick = model::checkConnection, enabled = state != ConnectionState.CHECKING) {
        Text(stringResource(R.string.connection_check))
    }
    Text(stringResource(R.string.connection_scope), style = MaterialTheme.typography.bodySmall)
}
