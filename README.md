# 青简阅读

青简阅读是一个从零开始的新本地电子书阅读器项目，使用 Kotlin + Compose Multiplatform 开发。

## 当前版本：0.3.0

已实现：

- Android + Windows/Desktop 双端基础工程
- 书架首页
- TXT / EPUB / PDF 文件导入
- TXT 阅读与章节识别
- EPUB XHTML/HTML 文本解析为章节
- PDF 文件识别、入库和阅读占位入口
- UTF-8 / GBK 文本读取兜底
- 阅读页
- 上一章 / 下一章
- 字号、行距、页边距调节
- 纯白、牛皮纸、羊皮纸、护眼绿、水墨黑主题
- 全书关键词搜索
- 本地书架、阅读进度、书签、设置持久化
- Android 状态栏保留，不覆盖系统状态栏
- Android 音量键翻章
- 检查更新：打开 GitHub Releases 最新版下载页

## 构建

Windows Git Bash 下：

```bash
export JAVA_HOME='/c/Program Files/Android/Android Studio/jbr'
export ANDROID_HOME='/c/Users/Richardyap/AppData/Local/Android/Sdk'
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"

./gradlew :androidApp:assembleDebug --no-daemon
./gradlew :desktopApp:packageUberJarForCurrentOS --no-daemon
```

## 模块

```text
shared      公共 UI / 数据模型 / TXT 解析 / 阅读逻辑
androidApp  Android 壳、文件选择器、沉浸式、音量键入口
desktopApp  Windows 桌面壳、文件选择器
```

## 路线图

下一步：

- 阅读进度持久化
- 书签持久化
- 书架删除 / 置顶 / 分类
- EPUB 解析
- PDF 阅读
- Android 音量键真实翻页
- 正式 Release APK
