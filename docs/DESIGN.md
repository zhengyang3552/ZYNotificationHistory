# 设计系统说明（Material 3）

本文档记录本次界面改造的设计依据、色彩方案与落地约束，供后续维护时保持一致。

## 1. 设计目标

| 维度 | 目标 | 落地方式 |
| --- | --- | --- |
| 一致性 | 明暗两套模式、两个页面使用同一套语义角色 | `values/colors.xml` + `values-night/colors.xml` 同名角色，主题只做「角色 → 属性」转接 |
| 层次 | 用色彩容器与字号建立层级，而不是堆阴影 | `surfaceContainerLow/High/Highest` 阶梯 + M3 字号（titleMedium / bodyMedium / labelSmall） |
| 可维护 | 改一处生效全局，禁止把十六进制散落在布局里 | 布局与代码只引用 `?attr/color*` 与 `@dimen/*` 令牌 |
| 可访问 | 触控区 ≥ 48dp，文字对比度 ≥ 4.5:1 | 见第 6 节实测数据 |

## 2. 色彩方案

### 2.1 种子色的选择依据

应用定位是「系统通知的本地档案馆」：长时间浏览、以信息阅读为主。主色需要冷静可信，同时与 Android 上大量使用 M3 默认紫（`#6750A4`）的应用区分开。

最终选取 **深青（Lagoon Teal）`#00696E`** 作为种子色（HCT：色相 202°，彩度 34.5）：

1. **语义贴合**：青蓝系在屏幕上天然关联「系统 / 监控 / 日志」，与「历史记录」的产品语境一致。
2. **状态可分辨**：主色（202°）、第三色（约 250°）、错误色（25°）在色相环上彼此间隔 ≥ 45°，其中主色与错误色相隔 177°，因此「常驻 / 临时 / 已移除」三种状态标签不会互相混淆，红色也只留给真正的错误。
3. **明暗都可读**：该色相在深色模式下能推导出 `#80D4D9` 这种高明度、低刺激的主色，暗色环境下不刺眼；浅色模式下 `#00696E` 对白字对比度 6.47:1，可直接承载主按钮。

> 全部 47 个角色由 **Material Color Utilities** 的 `SchemeTonalSpot(seed, isDark, contrastLevel = 0)` 计算得出，
> 而非手工挑选十六进制值。这样可保证色调板（tone 0–100）间距、彩度与 sRGB 色域裁剪都符合 M3 规范，
> 后续若要支持动态取色（Material You），只需替换种子色重新生成本文件即可。

### 2.2 语义角色对照

命名规则：`md_theme_<role>`，`values` 为浅色、`values-night` 为深色，角色名与 M3 官方一致。

| 语义角色 | 浅色 | 深色 |
| --- | --- | --- |
| `primary` | `#00696E` | `#80D4D9` |
| `on_primary` | `#FFFFFF` | `#003739` |
| `primary_container` | `#9CF0F6` | `#004F53` |
| `on_primary_container` | `#004F53` | `#9CF0F6` |
| `inverse_primary` | `#80D4D9` | `#00696E` |
| `secondary` | `#4A6364` | `#B1CCCD` |
| `on_secondary` | `#FFFFFF` | `#1B3436` |
| `secondary_container` | `#CCE8E9` | `#324B4D` |
| `on_secondary_container` | `#324B4D` | `#CCE8E9` |
| `tertiary` | `#4E5F7D` | `#B6C7E9` |
| `on_tertiary` | `#FFFFFF` | `#20314C` |
| `tertiary_container` | `#D6E3FF` | `#364764` |
| `on_tertiary_container` | `#364764` | `#D6E3FF` |
| `error` | `#BA1A1A` | `#FFB4AB` |
| `on_error` | `#FFFFFF` | `#690005` |
| `error_container` | `#FFDAD6` | `#93000A` |
| `on_error_container` | `#93000A` | `#FFDAD6` |
| `background` | `#F4FAFA` | `#0E1415` |
| `on_background` | `#161D1D` | `#DDE4E4` |
| `surface` | `#F4FAFA` | `#0E1415` |
| `on_surface` | `#161D1D` | `#DDE4E4` |
| `surface_variant` | `#DAE4E5` | `#3F4949` |
| `on_surface_variant` | `#3F4949` | `#BEC8C9` |
| `surface_dim` | `#D5DBDB` | `#0E1415` |
| `surface_bright` | `#F4FAFA` | `#343A3B` |
| `surface_container_lowest` | `#FFFFFF` | `#090F10` |
| `surface_container_low` | `#EFF5F5` | `#161D1D` |
| `surface_container` | `#E9EFEF` | `#1A2121` |
| `surface_container_high` | `#E3E9E9` | `#252B2B` |
| `surface_container_highest` | `#DDE4E4` | `#303636` |
| `outline` | `#6F7979` | `#899393` |
| `outline_variant` | `#BEC8C9` | `#3F4949` |
| `inverse_surface` | `#2B3232` | `#DDE4E4` |
| `inverse_on_surface` | `#ECF2F2` | `#2B3232` |
| `scrim` | `#000000` | `#000000` |

此外还提供 `primary/secondary/tertiary_fixed(_dim)` 与对应的 `on_*_fixed(_variant)` 共 12 个固定角色，
用于「不随明暗翻转的同色块」（例如未来做分享卡片、图表配色）；当前界面未直接使用，但作为完整角色集保留。

### 2.3 状态色语义映射

| 状态 | 背景角色 | 前景角色 | 设计依据 |
| --- | --- | --- | --- |
| 临时（默认） | `secondary_container` | `on_secondary_container` | 默认态是「绝大多数」，应用低饱和容器色，避免每条通知都在争夺注意力（改造前用 `primary_container`，导致所有条目同等高亮） |
| 常驻 | `tertiary_container` | `on_tertiary_container` | 「持续存在」是一种特殊但非异常的状态，用第三色与默认态区分 |
| 已移除 | `surface_container_highest` | `on_surface_variant` | 「已从通知栏移除」是信息性状态而非错误，用中性容器色；红色只留给权限缺失等真正的异常 |
| 权限缺失提示条 | `error_container` | `on_error_container` | 阻塞型状态，需要用户立即处理 |

### 2.4 对比度实测（WCAG 2.1）

| 前景 / 背景 | 浅色 | 深色 |
| --- | --- | --- |
| `on_surface` / `surface` | 16.21:1 | 14.42:1 |
| `on_surface_variant` / `surface` | 8.80:1 | 10.88:1 |
| `on_primary` / `primary` | 6.47:1 | 7.68:1 |
| `on_primary_container` / `primary_container` | 7.23:1 | 7.23:1 |
| `on_secondary_container` / `secondary_container` | 7.24:1 | 7.24:1 |
| `on_tertiary_container` / `tertiary_container` | 7.27:1 | 7.27:1 |
| `on_error_container` / `error_container` | 7.24:1 | 7.24:1 |
| `primary` / `surface`（当文字用） | 6.13:1 | 10.92:1 |
| `error` / `surface`（当文字用） | 6.12:1 | 10.95:1 |

全部组合 ≥ 4.5:1，正文与次要文字均满足 AA；主要阅读文本（16:1 左右）接近 AAA。

### 2.5 明暗模式策略

- **单一主题，派生覆盖**：`Theme.NotificationHistory.Base` 定义全部语义色映射，`Theme.NotificationHistory`
  继承它；导航栏这类受 API 限制的属性只在 `values-v27` / `values-night-v27` 中覆盖，避免用 `tools:ignore` 掩盖问题。
  低版本设备沿用系统默认导航栏（黑底白图标），不会出现「透明栏 + 白色图标」不可见的组合。
- **冷启动即正确**：`App.onCreate()` 读取已保存的模式并调用 `AppCompatDelegate.setDefaultNightMode`，
  修正了改造前「只有设置页应用主题、冷启动会先按系统主题绘制一帧」的闪烁问题。
- **关闭框架二次调制**：`elevationOverlayEnabled=false`（M3 用容器色表达高度，不再叠加 elevation overlay）、
  `forceDarkAllowed=false`（避免系统强制深色改写我们已定义的色值）。

## 3. 设计令牌

间距采用 4dp 栅格；圆角对齐 M3 shape scale（4 / 8 / 12 / 16 / 28）。

| 类别 | 令牌 | 值 | 用途 |
| --- | --- | --- | --- |
| 间距 | `space_xxs` … `space_3xl` | 2 / 4 / 8 / 12 / 16 / 24 / 32 / 48dp | 页面留白、卡片内边距、元素间隔 |
| 圆角 | `radius_xs` … `radius_xl` | 4 / 8 / 12 / 16 / 28dp | 标签 8dp、卡片 16dp、FAB 28dp |
| 页面 | `screen_margin` | 16dp | 页面左右安全边距 |
| 列表 | `list_horizontal_inset` / 条目外边距 | 12dp + 4dp | 视觉上仍是 16dp 页边距，同时给卡片留出涟漪与阴影空间 |
| 图标 | `app_icon_size` / `icon_size_md` | 44dp / 24dp | 应用图标容器、行内图标 |
| 无障碍 | `min_touch_target` | 48dp | 设置项、按钮最小高度 |

字号全部使用 M3 语义字号，不自造 sp 值：`titleLarge`(22) 页面标题、`titleMedium`(16) 通知标题、
`bodyMedium`(14) 正文、`labelLarge`(14) 应用名、`labelSmall`(11) 时间与包名。

## 4. 组件规范

| 组件 | 规范 |
| --- | --- |
| 内容卡片 | `surfaceContainerLow` + 16dp 圆角 + 0 阴影 + 无描边（`Widget.ZYNH.Card`） |
| 列表项 | 同上，纵向间距 8dp；涟漪由 `selectableItemBackground` 提供 |
| 状态标签 | `labelSmall` + 8dp 圆角 + 水平 8dp / 垂直 2dp 内边距，颜色由状态注入 |
| 顶部栏 | 透明背景、无阴影，标题 `titleLarge`，副标题 `labelMedium` 显示「正在监听 · N 条记录」 |
| 底部导航 | `BottomNavigationView` + `secondaryContainer` 活动指示器，容器色 `surfaceContainer` |
| 空状态 | 88dp 圆形 `secondaryContainer` 底 + 32dp 图标 + 标题/说明/主行动按钮 |
| 权限提示条 | `error_container` 卡片，图标 + 标题 + 说明 + 文本按钮，未授权时常驻 |

## 5. 布局与交互改进

| 位置 | 改造前 | 改造后 | 目的 |
| --- | --- | --- | --- |
| 列表容器 | `NestedScrollView` 内嵌 `RecyclerView` 且 `wrap_content` | `RecyclerView` 独立滚动 + `clipToPadding=false` | 恢复 ViewHolder 复用，条目多时不再一次性布局全部内容 |
| 条目层级 | 应用名与标题同为 `titleMedium` 加粗，卡片内 1dp 分隔线 | 应用名 `labelLarge`/次级色 → 标题 `titleMedium`/主色 → 正文 `bodyMedium`/次级色 → 元信息 `labelSmall` | 形成「来源 → 时间 → 标题 → 正文 → 状态」的阅读顺序 |
| 时间位置 | 右下角标签行内 | 标题行右上角，与 M3 通知样式一致 | 扫读时先看到时间，减少视线折返 |
| 设置页配色 | 硬编码深棕（`#2A2420`/`#D4A87C`），与主界面紫色主题冲突，且不随明暗切换 | 与主界面同一套语义角色 | 跨页面一致性；深色/浅色都可读 |
| 设置项控件 | 3 个 `RadioGroup` + 手写 3 按钮底栏 | `MaterialButtonToggleGroup` 分段按钮 + `MaterialSwitch` + 可点击行 + `BottomNavigationView` | 减少点击次数，控件密度更合理，底栏与主界面同构 |
| 返回键 | 未授权时拦截返回并弹 Snackbar，用户无法退出 | 正常退出；搜索展开时先收起搜索 | 修复可用性缺陷 |
| 主题切换 | `recreate()` + `onResume` 再次应用主题 | 一次 `setDefaultNightMode` 由框架统一重建 | 消除重复重建与闪烁 |
| 空状态 | 灰色信息图标 + 两行文字 | 圆形容器图标 + 标题/说明/「去开启」按钮 | 空状态可操作 |
| 搜索 | 无 | 顶部栏 `SearchView`，按应用/标题/正文/包名过滤 | 历史记录量大时的核心诉求 |

## 6. 可访问性

- 所有可点击元素（设置行、标签按钮、列表项）最小高度 48dp。
- 图标默认 `contentDescription=null`（装饰性），信息由相邻文字承担；纯图标按钮（清空 FAB）均带 `contentDescription`。
- 状态不只依赖颜色：标签同时有文字（常驻 / 临时 / 已移除）。
- 文字对比度见 2.4 节，全部 ≥ 4.5:1。

## 7. 维护约定

1. **不要在布局或 Kotlin 中写死颜色**，只引用 `?attr/color*` 或 `@color/md_theme_*`；
   需要按状态取色时用 `MaterialColors.getColor(view, attrRes)`（见 `NotificationAdapter`）。
2. **不要在布局中写死间距/圆角**，只引用 `@dimen/*` 令牌。
3. 新增颜色 → 改 `docs/` 中的生成流程（Material Color Utilities）重新生成
   `values/colors.xml` 与 `values-night/colors.xml`，不要单独补一个色值。
4. 组件级视觉规则（卡片、标签、设置行、分段按钮）放在 `values/styles.xml`，布局只描述结构。
