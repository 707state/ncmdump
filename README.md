# ncmdump

使用本程序可将下载的网易云音乐缓存文件（ncm）转换为 mp3 或 flac 格式

## 简介

这是ncmdump的安卓应用，提供了一个播放器页面和NCM转换页面。

libncmdump通过JNI提供，对原版ncmdump做了一些修改，代码可见app/src/main/cpp，这里不再使用vcpkg来管理taglib，而是直接作为依赖添加进来。