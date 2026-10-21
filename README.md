# ZYNotificationHistory

<div align="center">

![CI](https://github.com/zhengyang3552/ZYNotificationHistory/workflows/CI/badge.svg)
![Android](https://img.shields.io/badge/Platform-Android-brightgreen)
![Kotlin](https://img.shields.io/badge/Language-Kotlin-orange)
![Material 3](https://img.shields.io/badge/UI-Material%203-6750A4)

</div>

## 📖 简介

ZYNotificationHistory 是一款 Android 应用程序，用于记录和查看系统通知历史。该应用通过 `NotificationListenerService` 实时捕获通知事件，并使用本地数据库持久化存储，方便用户回溯查看历史通知内容。

## ✨ 功能特点

- **实时监听**：捕获并记录系统推送的通知消息，支持 BigText / Inbox / MessagingStyle 等富文本通知。
- **本地存储**：使用 Room 数据库本地持久化存储通知记录，不申请网络权限。
- **列表展示**：时间线视图按「常驻优先 + 时间倒序」排列，卡片式布局区分来源、标题、正文与状态。
- **搜索**：按应用名、标题、正文、包名实时过滤。
- **记录管理**：侧滑或长按删除（可撤销）、查看详情、复制正文、一键清空。
- **保留上限**：可设置 500 / 1000 / 5000 条或不限制，超出后自动清理最早记录。
- **设置中心**：主题模式（跟随系统 / 浅色 / 深色）、时间格式（相对 / 精确）、是否显示包名、权限与存储管理。
- **Material 3 主题**：完整语义色角色、明暗双模式、统一设计令牌。

## 🛠 技术栈

- **语言**: Kotlin
- **架构**: MVVM（`MainViewModel` + `NotificationRepository` + Room Flow 单向数据流）
- **UI 框架**: Android Views + ViewBinding + Material Components 1.12
- **设计体系**: Material 3（语义色角色 / 设计令牌 / 明暗模式）
- **数据库**: Room (SQLite)
- **并发**: Kotlin Coroutines + Flow
- **构建工具**: Gradle 8.2 / AGP 8.2
- **UI 组件**: Material 3, RecyclerView, CoordinatorLayout, SearchView, ViewBinding

## 🎨 设计系统

配色以 **深青 `#00696E`** 为种子色，由 *Material Color Utilities* 的 `SchemeTonalSpot` 推导出 47 个语义色角色（浅色 / 深色各一套），全部组合对比度 ≥ 4.5:1。

| 角色 | 浅色 | 深色 |
| --- | --- | --- |
| Primary | `#00696E` | `#80D4D9` |
| Primary Container | `#9CF0F6` | `#004F53` |
| Secondary Container | `#CCE8E9` | `#324B4D` |
| Tertiary Container | `#D6E3FF` | `#364764` |
| Surface | `#F4FAFA` | `#0E1415` |
| Surface Container Low | `#EFF5F5` | `#161D1D` |
| Error Container | `#FFDAD6` | `#93000A` |

完整角色表、对比度实测、设计令牌与组件规范见 **[docs/DESIGN.md](docs/DESIGN.md)**。

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

### 使用说明

1. 首次启动会提示「需要通知使用权」，点击 **去开启** 跳转系统设置。
2. 在系统列表中找到「通知历史查看器」并授权。
3. 返回应用，新通知会自动出现在列表中；点击条目查看详情，侧滑删除（可撤销）。

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

## 📝 变更记录

### v1.1 — Material 3 界面重构与功能梳理

**界面与配色**

- 引入由种子色推导的完整 M3 语义色板（47 个角色 × 明暗两套），替换原先「浅色用 M3 默认紫、设置页硬编码深棕」的混搭配色。
- 新增 `values/dimens.xml` 与 `values/styles.xml` 设计令牌层，布局不再出现魔法数字。
- 修复冷启动主题闪烁：主题在 `Application.onCreate()` 中应用，而非只在设置页应用。
- 修复深色模式导航栏图标不可见：导航栏按 API 27 分级（透明 + 图标明暗），低版本回退系统默认。
- 统一两页面的顶栏 / 底栏 / 卡片 / 分段按钮样式。

**布局与交互**

- 列表改为 `RecyclerView` 独立滚动（原先嵌在 `NestedScrollView` 中，失去复用、条目多时卡顿）。
- 条目重排信息层级：来源 → 时间 → 标题 → 正文 → 状态标签；移除卡片内分隔线。
- 新增搜索、详情弹窗、复制、侧滑删除 + 撤销、长按操作菜单。
- 设置页改用分段按钮 / 开关 / 对话框，替换 3 个 RadioGroup 与手写底栏；补齐「显示包名」「保留上限」配置。
- 修复未授权时返回键被拦截、无法退出应用的问题。

**功能与健壮性**

- 修复通知更新（同 key 重复上报）触发唯一索引冲突导致记录丢失的问题：改为事务内 upsert。
- 通知正文解析支持 BigText / Inbox / MessagingStyle，标题支持 `CharSequence` 子类。
- 应用信息读取失败（应用被卸载、包可见性受限）时回退为包名，不再整条丢弃。
- 通知移除后常驻记录降级为「已移除」；postTime 异常回退当前时间；空文本通知不入库。
- 新增保留上限，超出自动裁剪最早记录，避免数据库无限增长。
- 通知使用权判断改用 `NotificationManagerCompat`，修复短类名导致的误判；跳转设置页增加三级兜底。
- 相对时间处理未来时间戳（时钟回拨），跨天显示「昨天 HH:mm」，超过一周显示日期。
- 搜索词与数据在旋转屏幕 / 主题切换后不丢失（ViewModel + StateFlow）。

## 📄 许可证

[Apache License 2.0](LICENSE)
