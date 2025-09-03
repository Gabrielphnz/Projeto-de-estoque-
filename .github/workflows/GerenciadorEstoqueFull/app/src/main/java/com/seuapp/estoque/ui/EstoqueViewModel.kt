package com.seuapp.estoque.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class EstoqueViewModel: ViewModel() {
    private val _msg = MutableStateFlow("Pronto")
    val msg: StateFlow<String> = _msg

    companion object {
        fun provideFactory(ctx: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return EstoqueViewModel() as T
                }
            }
    }
}
