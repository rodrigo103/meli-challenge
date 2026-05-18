package com.example.myandroidapp.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myandroidapp.MainNavigation

@Composable
fun ResponsiveApp(modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        if (maxWidth < 840.dp) {
            MainNavigation()
        } else {
            DualPaneScreen(modifier = Modifier.fillMaxSize())
        }
    }
}
