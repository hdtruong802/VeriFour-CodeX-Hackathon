package com.verifour.app

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.verifour.app.ui.FlysticApp
import com.verifour.app.ui.theme.VeriFourTheme
import com.verifour.app.viewmodel.FlysticViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        setContent {
            VeriFourTheme {
                val flysticViewModel: FlysticViewModel = viewModel()
                FlysticApp(viewModel = flysticViewModel)
            }
        }
    }
}
