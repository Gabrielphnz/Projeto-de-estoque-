package com.seuapp.estoque

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.seuapp.estoque.ui.AppScaffold
import com.seuapp.estoque.ui.EstoqueViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm: EstoqueViewModel = viewModel(factory = EstoqueViewModel.provideFactory(this))
            AppScaffold(vm)
        }
    }
}
