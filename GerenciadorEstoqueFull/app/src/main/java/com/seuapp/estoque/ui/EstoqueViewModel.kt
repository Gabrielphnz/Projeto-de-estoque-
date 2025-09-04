package com.seuapp.estoque.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class EstoqueViewModel : ViewModel() {
    var quantidade by mutableStateOf(0)
        private set

    fun incrementar() { quantidade++ }
}
