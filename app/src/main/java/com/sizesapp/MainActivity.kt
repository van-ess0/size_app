package com.sizesapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.sizesapp.ui.navigation.SizesNavGraph
import com.sizesapp.ui.theme.SizesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SizesTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SizesNavGraph()
                }
            }
        }
    }
}
