package com.opportunity.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: RightNowWearViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = RightNowWearViewModel(applicationContext)
        viewModel.refresh()

        setContent {
            MaterialTheme {
                val state by viewModel.uiState.collectAsState()

                Column(
                    modifier = Modifier.fillMaxSize().padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("RIGHT NOW", style = MaterialTheme.typography.caption2)
                    Text(
                        state.title ?: "Nothing urgent",
                        style = MaterialTheme.typography.title3,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                    state.estimatedMinutes?.let {
                        Text("~$it min", style = MaterialTheme.typography.body2)
                    }
                    if (state.opportunityId != null) {
                        Button(
                            onClick = { viewModel.complete() },
                            modifier = Modifier.padding(top = 12.dp)
                        ) { Text("Done") }
                    }
                }
            }
        }
    }
}
