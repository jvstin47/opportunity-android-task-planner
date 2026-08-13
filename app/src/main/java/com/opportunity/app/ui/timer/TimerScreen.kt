package com.opportunity.app.ui.timer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.opportunity.app.timer.TimerRunState

/** Section 7 "Timers": countdown/stopwatch UI backed by TimerController + TimerForegroundService. */
@Composable
fun TimerScreen(
    opportunityId: String,
    onDone: () -> Unit,
    viewModel: TimerScreenViewModel = hiltViewModel()
) {
    val timerState by viewModel.timerState.collectAsState()
    val opportunityTitle by viewModel.opportunityTitle.collectAsState()

    LaunchedEffect(opportunityId) {
        viewModel.beginIfNeeded(opportunityId)
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(24.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(opportunityTitle, style = MaterialTheme.typography.headlineMedium)

            val state = timerState
            if (state != null) {
                Text(
                    "${state.elapsedMinutes} min" + (state.targetMinutes?.let { " / $it min" } ?: ""),
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
                if (state.targetMinutes != null) {
                    CircularProgressIndicator(
                        progress = { state.progressFraction },
                        modifier = Modifier.size(160.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                }
                Row(modifier = Modifier.padding(top = 32.dp)) {
                    when (state.runState) {
                        TimerRunState.RUNNING -> OutlinedButton(onClick = viewModel::pause) { Text("Pause") }
                        TimerRunState.PAUSED -> OutlinedButton(onClick = viewModel::resume) { Text("Resume") }
                        else -> {}
                    }
                    Button(
                        onClick = { viewModel.finish(); onDone() },
                        modifier = Modifier.padding(start = 12.dp)
                    ) { Text("Done") }
                }
            }
        }
    }
}
