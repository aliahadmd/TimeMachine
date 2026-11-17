package me.aliahad.timemanager.core.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import me.aliahad.timemanager.TimeManagerApp

@Composable
inline fun <reified VM : ViewModel> appViewModel(
    key: String? = null,
    crossinline creator: (AppContainer) -> VM
): VM {
    val context = LocalContext.current.applicationContext
    val container = (context as TimeManagerApp).container
    val factory = remember(container) {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return creator(container) as T
            }
        }
    }
    return viewModel(modelClass = VM::class.java, key = key, factory = factory)
}
