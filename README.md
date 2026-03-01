# 心流番茄 (Flow-Focus) 🍅

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Platform: Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![Kotlin: 1.9+](https://img.shields.io/badge/Kotlin-1.9%2B-purple.svg)](https://kotlinlang.org)

**心流番茄** 是一款专注极致体验的 Android 本地化专注与应用拦截工具。它结合了经典的番茄钟工作法与现代化的数据分析，旨在帮助用户减少干扰，进入深度工作的“心流”状态。

---

## ✨ 核心功能

### 📋 智能清单
- **任务管理**：简洁高效的待办清单，随时记录你的每一个目标。
- **一键专注**：从清单直接发起专注任务，支持番茄钟（Pomodoro）与正计时（Stopwatch）。

### 📊 深度统计 dashboard
- **全本地化分析**：不上传任何数据，提供完全隐私的本地统计分析。
- **多维视图**：支持单日、本周、本月、本年的专注趋势图、分布饼图和时长柱状图。
- **实时刷新**：专注完成后，数据即刻呈现。

### 🛡️ 极简严格模式
- **应用拦截**：通过 Android 无障碍服务实现无缝应用管控。
- **人性化引导**：拦截非白名单应用时提供 3 秒倒计时缓冲，帮助你平稳回归心流环境。
- **IME 兼容**：动态排除系统输入法，确保基础操作不受限。

### 🎵 沉浸式环境
- **高品质白噪音**：内置多款精选环境音（雨声、森林、白噪音等），营造沉浸式专注氛围。

### ☁️ 云端同步
- **WebDAV 支持**：支持通过 WebDAV 进行跨设备配置和数据备份，数据掌握在自己手中。

---

## 🛠️ 技术栈

- **UI 框架**：[Jetpack Compose](https://developer.android.com/jetpack/compose) (现代、响应式的原生声明式 UI)
- **依赖注入**：[Hilt](https://dagger.dev/hilt/)
- **持久化层**：[Room](https://developer.android.com/training/data-storage/room) (SQLite)
- **偏好设置**：[DataStore](https://developer.android.com/topic/libraries/architecture/datastore)
- **异步处理**：[Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) & [Flow](https://kotlinlang.org/docs/flow.html)
- **网络层**：[Retrofit](https://square.github.io/retrofit/) & [OkHttp](https://square.github.io/okhttp/) (用于 WebDAV 同步)

---

## 🚀 编译与运行

1. 克隆本项目：
   ```bash
   git clone https://github.com/EchoRan/Flow-Focus.git
   ```
2. 使用最新版本的 Android Studio (Ladybug 或更新) 打开项目。
3. 等待 Gradle 同步完成。
4. 在设备上开启 **无障碍服务** 以启用严格模式。
5. 在设备上开启 **悬浮窗权限** 以显示拦截反馈。

---

## 📜 许可证

本项目采用 **[GPLv3](LICENSE)** 许可证开源。

> 我们坚信开源的力量。你可以自由地学习、修改和分发本项目，但请务必遵循 GPLv3 的条款，确保派生作品同样保持开源。
