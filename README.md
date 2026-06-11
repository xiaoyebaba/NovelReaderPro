# 青简阅读

**青简阅读** 是一款正在打磨中的纯净本地小说阅读器，面向 Android 与 Windows 双端，使用 Kotlin + Compose Multiplatform 开发。

它的目标很简单：**打开本地书，安静地读下去。**  不强制联网，不做广告，不堆复杂入口，把重点放在本地书架、阅读排版、护眼显示和稳定的阅读进度保存上。

## 当前版本

**v0.6.1 · 阅读交互修正版**

这一版修复阅读页菜单和设置页的交互问题：去掉重复书签入口、移除阅读页检查更新按钮、设置改成可滚动弹窗，并让 Android 原生返回键优先关闭弹层或回到书架。

## 功能亮点

### 本地书架

- 支持本地 TXT / EPUB / PDF 文件导入
- 书架信息、阅读进度、书签和阅读设置本地保存
- 支持置顶、删除和格式识别
- 纯本地优先，不强制联网阅读

### 小说阅读体验

- 默认正文优先，点击阅读区域呼出顶部 / 底部菜单
- 支持上一章 / 下一章、目录快速跳转、搜索、书签
- 实时显示阅读百分比和预计剩余阅读时间
- 支持音量键翻章，适合单手阅读

### 排版与显示

- 字体选择：衬线、无衬线、等宽、手写风格
- 字号、行距、段距、页边距自由调节
- 翻页模式设置：无动画、滑动、仿真翻页占位
- 支持纯白、牛皮纸、羊皮纸、护眼绿、水墨黑主题

### 护眼与沉浸

- 支持亮度调节
- 支持冷暖色温调节
- 支持护眼柔光设置
- 默认保留 Android 状态栏，不遮挡系统信息
- 可手动开启全屏阅读，开启后隐藏状态栏

### 格式支持进度

| 格式 | 当前状态 |
| --- | --- |
| TXT | 已支持导入、章节识别、阅读、搜索、进度保存 |
| EPUB | 已支持基础 XHTML/HTML 文本解析、章节生成、标题提取、章节缓存保存 |
| PDF | 已支持导入识别和书架入口，真实页面渲染后续接入 |

## 下载

最新版本请查看：

https://github.com/xiaoyebaba/NovelReaderPro/releases/latest

每个版本都会创建独立 Release，历史版本不会被覆盖。

## 构建

Windows Git Bash 下：

```bash
export JAVA_HOME='/c/Program Files/Android/Android Studio/jbr'
export ANDROID_HOME='/c/Users/Richardyap/AppData/Local/Android/Sdk'
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"

./gradlew :androidApp:assembleDebug --no-daemon
./gradlew :desktopApp:packageUberJarForCurrentOS --no-daemon
```

## 项目结构

```text
shared      公共 UI、数据模型、阅读逻辑、TXT/EPUB/PDF 抽象
androidApp  Android 壳、系统文件选择器、状态栏控制、音量键入口
desktopApp  Windows 桌面壳、文件选择器
```

## 后续计划

- 真正的 PDF 页面渲染、缩放和页码记忆
- EPUB 目录顺序进一步按 OPF spine 优化
- 自定义图片背景
- 书签分组和批注笔记
- 手动修正章节目录
- 批量导入与本地文件夹扫描
- TTS 听书
