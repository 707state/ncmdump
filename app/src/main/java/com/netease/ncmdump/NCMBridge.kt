package com.netease.ncmdump

object NcmBridge {
    init {
        System.loadLibrary("ncmdump")
    }

    external fun convertAll(inputFolder: String, outputFolder: String): Int
}
fun mimeTypeFromName(name: String): String {
    val lower = name.lowercase()
    return when {
        lower.endsWith(".mp3") -> "audio/mpeg"
        lower.endsWith(".flac") -> "audio/flac"
        lower.endsWith(".wav") -> "audio/wav"
        else -> "application/octet-stream"
    }
}
