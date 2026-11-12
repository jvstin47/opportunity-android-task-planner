package com.opportunity.app.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val themeStore: ThemeStore
) : ViewModel() {

    val accent: StateFlow<AccentPalette> = themeStore.accent
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AccentPalette.CORAL)

    fun setAccent(accent: AccentPalette) = viewModelScope.launch { themeStore.setAccent(accent) }
}
