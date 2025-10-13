package com.netease.ncmdump

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun ConvertScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var inputFolder by remember { mutableStateOf<Uri?>(null) }
    var outputFolder by remember { mutableStateOf<Uri?>(null) }

    var isConverting by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("等待选择文件夹...") }
    var deleteAfterConvert by remember { mutableStateOf(false) }
    Column(modifier = Modifier.padding(16.dp)) {
        AllFilesAccessSection(context = context)

        Spacer(modifier = Modifier.height(16.dp))


        // 选择输入文件夹
        val pickInput = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocumentTree()
        ) { uri ->
            if (uri != null) {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                inputFolder = uri
                message = "输入目录: ${DocumentFile.fromTreeUri(context, uri)?.name}"
            }
        }

        // 选择输出文件夹
        val pickOutput = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocumentTree()
        ) { uri ->
            if (uri != null) {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                outputFolder = uri
                message = "输出目录: ${DocumentFile.fromTreeUri(context, uri)?.name}"
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🎵 NCM 文件转换工具", style = MaterialTheme.typography.titleLarge)

            Button(onClick = { pickInput.launch(null) }) {
                Icon(Icons.Default.Folder, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("选择输入文件夹")
            }

            Button(onClick = { pickOutput.launch(null) }) {
                Icon(Icons.Default.Folder, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("选择输出文件夹")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = deleteAfterConvert,
                    onCheckedChange = { deleteAfterConvert = it }
                )
                Text("转换后删除原文件")
            }
            Button(
                onClick = {
                    if (inputFolder != null && outputFolder != null) {
                        isConverting = true
                        message = "正在转换..."
                        scope.launch {
                            val inputDir = DocumentFile.fromTreeUri(context, inputFolder!!)
                            val outputDir = DocumentFile.fromTreeUri(context, outputFolder!!)
                            if (inputDir == null || outputDir == null) {
                                message = "无法访问输入或输出文件夹"
                                isConverting = false
                                return@launch
                            }

                            // 创建临时可访问的 app 内部目录
                            val tmpDir = File(context.filesDir, "ncm_tmp").apply { mkdirs() }

                            var convertedCount = 0

                            withContext(Dispatchers.IO) {
                                try {
                                    // 遍历 NCM 文件
                                    for (file in inputDir.listFiles() ?: emptyArray()) {
                                        if (!file.isFile) continue
                                        if (!file.name.orEmpty().lowercase()
                                                .endsWith(".ncm")
                                        ) continue

                                        // 拷贝到临时目录
                                        val tmpFile = File(tmpDir, file.name!!)
                                        context.contentResolver.openInputStream(file.uri)
                                            ?.use { input ->
                                                tmpFile.outputStream().use { output ->
                                                    input.copyTo(output)
                                                }
                                            }
                                        // 调用 C++ 转换，输出也在临时目录
                                        val res=NcmBridge.convertAll(
                                            tmpFile.absolutePath,
                                            tmpDir.absolutePath
                                        )
                                        if (deleteAfterConvert && res!=0) {
                                            file.delete()  // 删除 DocumentFile
                                        }
                                    }
                                    // 将转换好的文件拷贝回用户选择的输出目录
                                    tmpDir.listFiles()?.forEach { tmpFile ->
                                        val nameLower = tmpFile.name.orEmpty().lowercase()
                                        if (!nameLower.endsWith(".mp3") && !nameLower.endsWith(".flac")) return@forEach
                                        val mimeType = mimeTypeFromName(tmpFile.name!!)
                                        val outFile = outputDir.createFile(mimeType, tmpFile.name!!)
                                        outFile?.uri?.let { outUri ->
                                            context.contentResolver.openOutputStream(outUri)
                                                ?.use { output ->
                                                    tmpFile.inputStream().use { input ->
                                                        input.copyTo(output)
                                                    }
                                                }
                                            convertedCount++
                                        }
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    convertedCount = -1
                                } finally {
                                    // 临时目录清理
                                    tmpDir.deleteRecursively()
                                }
                            }
                            isConverting = false
                            message = if (convertedCount >= 0)
                                "转换完成，共 $convertedCount 个文件"
                            else
                                "转换失败！"
                        }
                    } else {
                        message = "请先选择输入/输出文件夹"
                    }
                },
                enabled = !isConverting
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (isConverting) "转换中..." else "开始转换")
            }
            Text(message)
            if (isConverting) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}