package com.netease.ncmdump

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.delay

fun flattenAudioFiles(folder: FileNode.Folder): List<FileNode.AudioFile> {
    val result = mutableListOf<FileNode.AudioFile>()
    folder.children.forEach {
        when (it) {
            is FileNode.AudioFile -> result.add(it)
            is FileNode.Folder -> result.addAll(flattenAudioFiles(it))
        }
    }
    return result
}


sealed class FileNode {
    data class Folder(val name: String, val children: List<FileNode>) : FileNode()
    data class AudioFile(val name: String, val uri: Uri) : FileNode()
}

fun buildFileTree(dir: DocumentFile): FileNode.Folder {
    val children = mutableListOf<FileNode>()
    for (file in dir.listFiles()) {
        when {
            file.isDirectory -> children.add(buildFileTree(file))
            file.isFile && isAudioFile(file.name ?: "") ->
                children.add(FileNode.AudioFile(file.name ?: "未知", file.uri))
        }
    }
    return FileNode.Folder(dir.name ?: "根目录", children)
}

fun isAudioFile(name: String): Boolean {
    val lower = name.lowercase()
    return lower.endsWith(".mp3") || lower.endsWith(".flac") || lower.endsWith(".wav")
}

/** 可折叠文件夹 UI */
@Composable
fun FolderNodeView(
    node: FileNode.Folder,
    currentTrackUri: Uri?,
    onPlay: (Uri, String) -> Unit,
    depth: Int = 0
) {
    val indent = 16.dp * depth
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(start = indent, top = 4.dp, bottom = 4.dp)
        ) {
            Icon(
                imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(node.name, style = MaterialTheme.typography.titleSmall)
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column {
                node.children.forEach { child ->
                    when (child) {
                        is FileNode.Folder ->
                            FolderNodeView(child, currentTrackUri, onPlay, depth + 1)

                        is FileNode.AudioFile -> {
                            val isPlaying = child.uri == currentTrackUri
                            Text(
                                text = "🎵 ${child.name}",
                                color = if (isPlaying)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onPlay(child.uri, child.name) }
                                    .padding(start = indent + 32.dp, top = 2.dp, bottom = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ArtworkDisplay(metadata: AudioMetadata) {
    if (metadata.artwork != null) {
        Image(
            bitmap = metadata.artwork.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.size(200.dp)
        )
    } else {
        Text("🎵", style = MaterialTheme.typography.displayLarge)
    }
}

fun extractMetadata(context: Context, uri: Uri): AudioMetadata {
    val retriever = MediaMetadataRetriever()
    try {
        retriever.setDataSource(context, uri)

        val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
        val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
        val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)

        val artworkData = retriever.embeddedPicture
        val artwork = artworkData?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }

        return AudioMetadata(title, artist, album, artwork)
    } catch (e: Exception) {
        e.printStackTrace()
        return AudioMetadata(null, null, null, null)
    } finally {
        retriever.release()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicPlayerScreen() {
    val context = LocalContext.current
    var showFolderDialog by remember { mutableStateOf(false) }
    var rootFolder by remember { mutableStateOf<FileNode.Folder?>(null) }
    var allSongs by remember { mutableStateOf<List<FileNode.AudioFile>>(emptyList()) } // 扁平化后的音乐列表
    var currentUri by remember { mutableStateOf<Uri?>(null) }
    var folderUri by remember { mutableStateOf<Uri?>(null) }
    var currentSong by remember { mutableStateOf("未选择文件") }
    var isPlaying by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var playMode by remember { mutableStateOf(PlayMode.SEQUENTIAL) }
    var currentIndex by remember { mutableIntStateOf(0) }

    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var currentMetadata by remember { mutableStateOf<AudioMetadata?>(null) }

    // 文件夹选择器
    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            folderUri = uri
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            val pickedDir = DocumentFile.fromTreeUri(context, uri)
            if (pickedDir != null) {
                val folder = buildFileTree(pickedDir)
                rootFolder = folder
                allSongs = flattenAudioFiles(folder) // 收集所有歌曲
                showFolderDialog = true
            }
        }
    }

    // 自动同步播放进度
    LaunchedEffect(mediaPlayer, isPlaying) {
        while (isPlaying && mediaPlayer != null) {
            val mp = mediaPlayer!!
            if (mp.isPlaying && mp.duration > 0) {
                progress = mp.currentPosition.toFloat() / mp.duration.toFloat()
            }
            delay(200)
        }
    }

    // 播放函数封装
    fun playAt(index: Int) {
        if (index !in allSongs.indices) return
        val song = allSongs[index]
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            val newPlayer = MediaPlayer().apply {
                setDataSource(context, song.uri)
                prepare()
                start()
                setOnCompletionListener {
                    // 播放完自动切下一首
                    if (playMode == PlayMode.SEQUENTIAL) {
                        playAt((index + 1) % allSongs.size)
                    } else {
                        playAt((allSongs.indices).random())
                    }
                }
            }
            mediaPlayer = newPlayer
            currentUri = song.uri
            currentSong = song.name
            isPlaying = true
            progress = 0f
            currentIndex = index
            currentMetadata = extractMetadata(context, song.uri)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 下一首
    fun playNext() {
        if (allSongs.isEmpty()) return
        val nextIndex = when (playMode) {
            PlayMode.SEQUENTIAL -> (currentIndex + 1) % allSongs.size
            PlayMode.RANDOM -> (allSongs.indices).random()
        }
        playAt(nextIndex)
    }

    // 上一首
    fun playPrevious() {
        if (allSongs.isEmpty()) return
        val prevIndex = when (playMode) {
            PlayMode.SEQUENTIAL -> if (currentIndex - 1 >= 0) currentIndex - 1 else allSongs.size - 1
            PlayMode.RANDOM -> (allSongs.indices).random()
        }
        playAt(prevIndex)
    }

    // 刷新
    fun refreshFolder() {
        folderUri?.let { uri ->
            val pickedDir = DocumentFile.fromTreeUri(context, uri)
            if (pickedDir != null) {
                val folder = buildFileTree(pickedDir)
                rootFolder = folder
                allSongs = flattenAudioFiles(folder)
            }
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("音乐播放器") },
                actions = {
                    if (rootFolder != null) {
                        IconButton(onClick = { refreshFolder() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "刷新")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (rootFolder == null) folderPicker.launch(null)
                    else showFolderDialog = true
                }
            ) {
                Icon(Icons.Filled.Folder, contentDescription = "选择文件夹")
            }
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (currentMetadata != null) {
                    ArtworkDisplay(currentMetadata!!)
                } else {
                    Text("🎵", style = MaterialTheme.typography.displayLarge)
                }
            }
            Text(currentSong, style = MaterialTheme.typography.titleMedium)
            Slider(
                value = progress,
                onValueChange = { newValue ->
                    progress = newValue
                    mediaPlayer?.let { mp ->
                        if (mp.duration > 0) {
                            mp.seekTo((newValue * mp.duration).toInt())
                        }
                    }
                },
                modifier = Modifier.padding(horizontal = 32.dp)
            )

            // 播放控制栏
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                IconButton(onClick = { playPrevious() }, enabled = mediaPlayer != null) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "上一首")
                }
                IconButton(onClick = {
                    if (mediaPlayer != null) {
                        if (isPlaying) mediaPlayer?.pause() else mediaPlayer?.start()
                        isPlaying = !isPlaying
                    }
                }, enabled = mediaPlayer != null) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "播放/暂停",
                        modifier = Modifier.size(48.dp)
                    )
                }
                IconButton(onClick = { playNext() }, enabled = mediaPlayer != null) {
                    Icon(Icons.Default.SkipNext, contentDescription = "下一首")
                }
            }

            Spacer(Modifier.height(16.dp))

            // 播放模式切换
            Button(onClick = {
                playMode = if (playMode == PlayMode.SEQUENTIAL)
                    PlayMode.RANDOM else PlayMode.SEQUENTIAL
            }) {
                Text("模式: ${if (playMode == PlayMode.SEQUENTIAL) "顺序" else "随机"}")
            }
        }
        // 文件夹弹窗
        if (showFolderDialog && rootFolder != null) {
            AlertDialog(
                onDismissRequest = { showFolderDialog = false },
                confirmButton = {},
                title = { Text("音乐文件列表") },
                text = {
                    LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                        item {
                            FolderNodeView(
                                node = rootFolder!!,
                                currentTrackUri = currentUri,
                                onPlay = { uri, name ->
                                    val index = allSongs.indexOfFirst { it.uri == uri }
                                    if (index >= 0) playAt(index)
                                    showFolderDialog = false
                                }
                            )
                        }
                    }
                }
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }
}