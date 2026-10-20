package com.example.bodega

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.bodega.ui.navigation.AppNavigation
import com.example.bodega.ui.theme.BodegaTheme
import com.example.bodega.viewmodel.AppViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as BodegaApp).container
        val factory = AppViewModelFactory(container)

        setContent {
            BodegaTheme {
                AppNavigation(factory = factory, session = container.session)
            }
        }
    }
}
