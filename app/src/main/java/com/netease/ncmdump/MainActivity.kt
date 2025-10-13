package com.netease.ncmdump

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.netease.ncmdump.ui.theme.Player2Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Player2Theme {
                PlayerApp()
            }
        }
    }
}
enum class PlayMode { SEQUENTIAL, RANDOM }
data class AudioMetadata(
    val title: String?,
    val artist: String?,
    val album: String?,
    val artwork: android.graphics.Bitmap?
)

@Composable
fun PlayerApp() {
    var selectedTab by remember { mutableStateOf(0) }

    val tabs = listOf("播放", "NCM转换")

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, title ->
                    NavigationBarItem(
                        icon = {
                            if (index == 0) Icon(Icons.Default.PlayArrow, contentDescription = null)
                            else Icon(Icons.Default.Folder, contentDescription = null)
                        },
                        label = { Text(title) },
                        selected = selectedTab == index,
                        onClick = { selectedTab = index }
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (selectedTab) {
                0 -> MusicPlayerScreen()
                1 -> ConvertScreen()
            }
        }
    }
}
