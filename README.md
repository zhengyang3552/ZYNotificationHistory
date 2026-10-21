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
- **设置中心**：主题模式（跟随系统 / 浅色 / 深色）、动态取色（Android 12+ 壁纸取色）、时间格式（相对 / 精确）、是否显示包名、权限与存储管理。
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

配色遵循**跨应用统一设计规范**，与 ZYEmail / RemoteFileManager 使用同一品牌种子色 **`#0B57D0`**，由 Material Color Utilities 的 `SchemeTonalSpot` 推导出 47 个语义色角色（浅色 / 深色各一套），全部组合对比度 ≥ 4.5:1。Android 12+ 支持开启「动态取色」跟随壁纸。

| 角色 | 浅色 | 深色 |
| --- | --- | --- |
| Primary | `#0856CF` | `#B2C5FF` |
| Primary Container | `#DAE2FF` | `#0040A1` |
| Secondary Container | `#DCE2F9` | `#404659` |
| Tertiary Container | `#FED7F9` | `#5A3D59` |
| Surface | `#FBF8FD` | `#131318` |
| Surface Container Low | `#F4F3FA` | `#1B1B21` |
| Error Container | `#FFDAD5` | `#93000A` |

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

Release 一律由 CI 在推送 `v*` 标签时构建并发布到 GitHub Releases（签名固定的 keystore 通过仓库
Secrets 注入，保证所有版本证书一致、可覆盖升级）。本地如需自签，可设置环境变量后执行：

```bash
export KEYSTORE_PATH="path/to/keystore"   # 相对路径以仓库根目录为基准
export KEYSTORE_PASSWORD="your_password"
export KEY_ALIAS="your_alias"
export KEY_PASSWORD="your_key_password"
./gradlew assembleRelease
```

签名配置方法见 [SIGNING.md](SIGNING.md)。

## ⚙️ CI/CD

`.github/workflows/ci.yml` 采用三段式流水线（与 ZYEmail / RemoteFileManager 同构），本地无需安装 Android SDK 也能拿到 APK。

**触发条件**：推送到 `main`、提交 PR 到 `main`、推送 `v*` 标签（触发发布）、手动 `workflow_dispatch`；
同一分支新提交会自动取消上一次未完成的运行。

| 阶段 | 内容 | 产物 |
| --- | --- | --- |
| `build` | 单元测试 + 构建 Debug APK | `ZYNotificationHistory-debug` |
| `lint` | `lintDebug`（有 error 即失败，作为质量门禁） | `ZYNotificationHistory-lint-report` |
| `release` | 仅 `v*` 标签：固定签名构建 Release APK → 创建 GitHub Release 并附加 APK | Release 页面下载 |

日常构建的 APK 在对应运行详情页的 **Artifacts** 区域下载；正式发版打 `v1.3` 这类标签即可。

**固定发布签名**：`release` 阶段从仓库 Secrets 读取 `KEYSTORE_BASE64` / `KEYSTORE_PASSWORD` /
`KEY_ALIAS` / `KEY_PASSWORD`（配置方法见 [SIGNING.md](SIGNING.md)）。未配置时该阶段直接失败，
不会产出签名不稳定的包；构建摘要会输出签名证书 SHA-256 供每次发版核对。

## 📝 变更记录

### v1.3

- 对齐跨应用统一设计规范：品牌种子色切换为 `#0B57D0`（与 ZYEmail / RemoteFileManager 同源），26 个核心角色与规范基线一致，并补全 surface container 阶梯与 fixed 角色（共 47 个语义角色）。
- 新增「动态取色」开关（Android 12+ 壁纸取色，关闭回退统一品牌色），设置 → 外观。
- CI 采用三段式规范（build / lint / release），Release 固定签名（Secrets 注入，签名指纹写入构建摘要），并修复 keystore 相对路径解析问题。

### v1.2

- 移除底部导航栏（右上角已有设置入口，且底栏与「清空」按钮重叠）；「清空」改为右下角悬浮按钮，列表底部留白同步调整。
- 相对时间实时刷新：列表可见期间每 15 秒对时间文案做局部重绑（payload 复用，不触发布局/动画/滚动位置），不再停留在绑定那一刻的旧值；「精确时间」模式自动跳过刷新。

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
