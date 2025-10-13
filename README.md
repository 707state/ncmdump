# ncmdump

使用本程序可将下载的网易云音乐缓存文件（ncm）转换为 mp3 或 flac 格式

## 简介

这是ncmdump的安卓应用，提供了一个播放器页面和NCM转换页面。

libncmdump通过JNI提供，对原版ncmdump做了一些修改，代码可见app/src/main/cpp，这里不再使用vcpkg来管理taglib，而是直接作为依赖添加进来。

## 使用

注意：网易云音乐 3.0 之后的某些版本，下载的 ncm 文件会出现不内置歌曲专辑的封面图片的情况，所需的封面图数据需要从网络获取，介于在一个小工具中嵌入庞大网络库的非必要性，可以移步我的另一个仓库 [ncmdump-go](https://git.taurusxin.com/taurusxin/ncmdump-go) 或者使用基于此项目开发的可视化 GUI 程序 [ncmdump-gui](https://git.taurusxin.com/taurusxin/ncmdump-gui)，其中后者基于前者，均完全使用 Golang 重写，并支持自动从元数据读取封面信息后从网络获取封面图并嵌入到目标音乐文件。
