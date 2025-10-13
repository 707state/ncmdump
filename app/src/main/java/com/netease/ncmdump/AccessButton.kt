package com.netease.ncmdump

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri

fun hasAllFilesAccess(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Environment.isExternalStorageManager()
    } else {
        true // Android 10以下没有这个限制
    }
}
@Composable
fun RequestAllFilesAccessButton(context: Context) {
    Button(onClick = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:" + context.packageName)
                context.startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }) {
        Text("开启访问所有文件权限")
    }
}
@Composable
fun AllFilesAccessView(context: Context) {
    if (!hasAllFilesAccess()) {
        Text("App 当前没有访问所有文件权限，需要手动开启")
        RequestAllFilesAccessButton(context)
    } else {
        Text("已拥有访问所有文件权限")
    }
}

@Composable
fun AllFilesAccessSection(context: Context) {
    // 用 remember 来保持状态
    val hasAccess = remember { mutableStateOf(hasAllFilesAccess()) }

    Column {
        if (!hasAccess.value) {
            Text("App 当前没有访问所有文件权限，需要手动开启")
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    try {
                        if (!Environment.isExternalStorageManager()) {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            intent.data = ("package:" + context.packageName).toUri()
                            context.startActivity(intent)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }) {
                Text("开启访问所有文件权限")
            }

            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                // 用户点击“检查权限”，刷新状态
                hasAccess.value = hasAllFilesAccess()
            }) {
                Text("检查权限")
            }
        } else {
            Text("已拥有访问所有文件权限")
        }
    }
}
