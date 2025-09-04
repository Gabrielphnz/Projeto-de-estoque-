package com.seuapp.estoque

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.lifecycle.viewmodel.compose.viewModel
import com.seuapp.estoque.ui.AppScaffold
import com.seuapp.estoque.ui.EstoqueViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm: EstoqueViewModel = viewModel()
            Surface(color = MaterialTheme.colorScheme.background) {
                AppScaffold(vm)
            }
        }
    }
}