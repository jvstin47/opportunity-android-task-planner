package com.opportunity.app.ui.settings

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.opportunity.app.ui.common.GlassCard

/** Vault tab (formerly "Settings"): Places + cross-device sync -- opt-in, off by default (section 21). */
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val places by viewModel.places.collectAsState()
    var email by remember { mutableStateOf("") }
    var placeName by remember { mutableStateOf("") }
    var pendingPlaceName by remember { mutableStateOf<String?>(null) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val name = pendingPlaceName
        pendingPlaceName = null
        if (granted && name != null) viewModel.addPlaceFromCurrentLocation(name)
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Vault") }) }) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Places", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Name a place (\"College\", \"Gym\") and tag tasks with a matching category " +
                                "so Focus can tell you what's relevant when you're there.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(modifier = Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = placeName,
                                onValueChange = { placeName = it },
                                label = { Text("Place name") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedButton(
                                onClick = {
                                    pendingPlaceName = placeName
                                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                                },
                                enabled = placeName.isNotBlank() && !state.isAddingPlace,
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                if (state.isAddingPlace) CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                                Text("Add here")
                            }
                        }
                        state.placeError?.let {
                            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 4.dp))
                        }
                        places.forEach { place ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(place.name)
                                IconButton(onClick = { viewModel.removePlace(place.id) }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Remove ${place.name}")
                                }
                            }
                        }
                    }
                }
            }

            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Sync across devices", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Optional. Opportunity works fully offline without this.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email") },
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                        )
                        Button(
                            onClick = { viewModel.enableSync(email) },
                            enabled = email.isNotBlank() && !state.isSyncing,
                            modifier = Modifier.padding(top = 12.dp)
                        ) {
                            if (state.isSyncing) CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                            Text(if (state.syncEnabled) "Synced" else "Enable Sync")
                        }
                        state.error?.let {
                            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                }
            }
        }
    }
}
