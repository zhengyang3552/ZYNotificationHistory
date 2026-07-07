# ZYNotificationHistory

<div align="center">

![CI](https://github.com/zhengyang3552/ZYNotificationHistory/workflows/CI/badge.svg)
![Android](https://img.shields.io/badge/Platform-Android-brightgreen)
![Kotlin](https://img.shields.io/badge/Language-Kotlin-orange)

</div>

## 📖 简介

ZYNotificationHistory 是一款 Android 应用程序，用于记录和查看系统通知历史。该应用通过 `NotificationListenerService` 实时捕获通知事件，并使用本地数据库持久化存储，方便用户回溯查看历史通知内容。

## ✨ 功能特点

- **实时监听**：捕获并记录系统推送的通知消息。
- **本地存储**：使用 Room 数据库本地持久化存储通知记录。
- **列表展示**：通过 RecyclerView 展示历史通知列表。
- **设置中心**：支持查看应用配置及权限管理。
- **时间线视图**：按时间倒序排列通知记录。

## 🛠 技术栈

- **语言**: Kotlin
- **架构**: MVVM (Model-View-ViewModel)
- **UI 框架**: Android Views + ViewBinding
- **数据库**: Room (SQLite)
- **并发**: Kotlin Coroutines
- **构建工具**: Gradle 8.2
- **UI 组件**: Material Design, RecyclerView, ConstraintLayout

## 🚀 快速开始

### 环境要求

- Android Studio (推荐最新版)
- JDK 17 或更高版本
- Android SDK 34 (API 34)

### 构建步骤

1. 克隆仓库
   ```bash
   git clone https://github.com/zhengyang3552/ZYNotificationHistory.git
   ```
2. 使用 Android Studio 打开项目
3. 同步 Gradle 依赖 (`Sync Project with Gradle Files`)
4. 运行应用 (`Run 'app'`) 或在终端执行：
   ```bash
   ./gradlew assembleDebug
   ```

### 生成签名包 (Release Build)

项目配置了签名环境，需要配置签名文件才能生成 Release APK。
请在项目根目录创建 `release.keystore` 文件，或在环境变量中设置：

```bash
export KEYSTORE_PATH="path/to/keystore"
export KEYSTORE_PASSWORD="your_password"
export KEY_ALIAS="your_alias"
export KEY_PASSWORD="your_key_password"
```

然后执行：
```bash
./gradlew assembleRelease
```

## ⚙️ CI/CD

本项目使用 GitHub Actions 进行持续集成。
每次推送到 `main` 分支或提交 Pull Request 时，CI 会自动执行以下操作：

1. 构建 Debug 和 Release 版本
2. 运行 Lint 代码检查
3. 上传构建产物作为 Artifact

## 📄 许可证

[Apache License 2.0](LICENSE)

