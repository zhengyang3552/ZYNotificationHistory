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

### 2.1 种子色与统一设计规范

本应用的配色属于一套**跨应用统一设计规范**：与 ZYEmail、RemoteFileManager 使用同一品牌种子色 **`#0B57D0`（HCT 色相 270.3°，彩度 65，tone 40.3）**，色板由 Material Color Utilities 的 `SchemeTonalSpot` 统一推导，保证几个应用的观感一致、可被用户当作同一套产品识别。

选择蓝色系作为品牌色的依据：

1. **语义贴合**：蓝色承载「系统、可信、归档」的心智，与「通知历史」这类工具型应用契合，长时间阅读不疲劳；
2. **状态可分辨**：主色（270°）、第三色（约 335° 的紫红）、错误色（25° 的红）在色相环上相互远离，「常驻 / 临时 / 已移除」三类状态标签不会混淆，红色只留给真正的异常；
3. **明暗都可读**：深色模式推导出 `#B2C5FF` 这类高明度主色，暗色环境下不刺眼；浅色模式 `#0856CF` 对白字对比度 6.48:1，可直接承载主按钮与彩色文字。

> 全部 47 个语义角色由 **Material Color Utilities**（`SchemeTonalSpot(seed, isDark, contrastLevel = 0)`）计算生成，
> 其中 26 个核心角色与统一规范基线值完全一致，surface container 阶梯、fixed 角色等补充角色按同一种子色推导，
> 保证与规范同源且满足本应用对容器色阶的完整需求。后续如需换品牌色，只需替换种子色重新生成两个 colors.xml。

**动态取色（Material You）**：Android 12+ 上可在「设置 → 外观」开启「动态取色」，由 `DynamicColors.applyToActivityIfAvailable`
以壁纸取色包装当前主题；关闭或低版本时回退到上述统一品牌色方案。开关状态存于 `notification_prefs`，
切换后 `recreate()` 立即生效。

### 2.2 语义角色对照

命名规则：`md_theme_<role>`（snake_case），`values` 为浅色、`values-night` 为深色，角色名与 M3 官方一致。

| 语义角色 | 浅色 | 深色 |
| --- | --- | --- |
| `primary` | `#0856CF` | `#B2C5FF` |
| `on_primary` | `#FFFFFF` | `#002B72` |
| `primary_container` | `#DAE2FF` | `#0040A1` |
| `on_primary_container` | `#001847` | `#DAE2FF` |
| `inverse_primary` | `#B2C5FF` | `#495D92` |
| `secondary` | `#585E71` | `#C0C6DD` |
| `on_secondary` | `#FFFFFF` | `#2A3042` |
| `secondary_container` | `#DCE2F9` | `#404659` |
| `on_secondary_container` | `#151B2C` | `#DCE2F9` |
| `tertiary` | `#735471` | `#E1BBDD` |
| `on_tertiary` | `#FFFFFF` | `#412741` |
| `tertiary_container` | `#FED7F9` | `#5A3D59` |
| `on_tertiary_container` | `#2B122B` | `#FED7F9` |
| `error` | `#B4271F` | `#FFB4AA` |
| `on_error` | `#FFFFFF` | `#690003` |
| `error_container` | `#FFDAD5` | `#910809` |
| `on_error_container` | `#410001` | `#FFDAD5` |
| `background` | `#FBF8FD` | `#131316` |
| `on_background` | `#1B1B1F` | `#E4E2E6` |
| `surface` | `#FBF8FD` | `#131316` |
| `on_surface` | `#1B1B1F` | `#E4E2E6` |
| `surface_variant` | `#E1E2EC` | `#45464F` |
| `on_surface_variant` | `#45464F` | `#C5C6D0` |
| `surface_dim` | `#DAD9E0` | `#121318` |
| `surface_bright` | `#FAF8FF` | `#38393F` |
| `surface_container_lowest` | `#FFFFFF` | `#0D0E13` |
| `surface_container_low` | `#F4F3FA` | `#1A1B21` |
| `surface_container` | `#EEEDF4` | `#1E1F25` |
| `surface_container_high` | `#E8E7EF` | `#282A2F` |
| `surface_container_highest` | `#E2E2E9` | `#33343A` |
| `outline` | `#757780` | `#8F909A` |
| `outline_variant` | `#C5C6D0` | `#45464F` |
| `inverse_surface` | `#303034` | `#E4E2E6` |
| `inverse_on_surface` | `#F2F0F4` | `#303034` |
| `scrim` | `#000000` | `#000000` |
| `primary_fixed` | `#DAE2FF` | `#DAE2FF` |
| `primary_fixed_dim` | `#B2C5FF` | `#B2C5FF` |
| `on_primary_fixed` | `#001847` | `#001847` |
| `on_primary_fixed_variant` | `#304578` | `#304578` |
| `secondary_fixed` | `#DCE2F9` | `#DCE2F9` |
| `secondary_fixed_dim` | `#C0C6DD` | `#C0C6DD` |
| `on_secondary_fixed` | `#151B2C` | `#151B2C` |
| `on_secondary_fixed_variant` | `#404659` | `#404659` |
| `tertiary_fixed` | `#FED7F9` | `#FED7F9` |
| `tertiary_fixed_dim` | `#E1BBDD` | `#E1BBDD` |
| `on_tertiary_fixed` | `#2B122B` | `#2B122B` |
| `on_tertiary_fixed_variant` | `#5A3D59` | `#5A3D59` |

此外还提供 `primary/secondary/tertiary_fixed(_dim)` 与对应的 `on_*_fixed(_variant)` 共 12 个固定角色，
用于「不随明暗翻转的同色块」（例如未来做分享卡片、图表配色）；当前界面未直接使用，但作为完整角色集保留。

### 2.3 状态色语义映射

| 状态 | 背景角色 | 前景角色 | 设计依据 |
| --- | --- | --- | --- |
| 临时（默认） | `secondary_container` | `on_secondary_container` | 默认态是「绝大多数」，应用低饱和容器色，避免每条通知都在争夺注意力 |
| 常驻 | `tertiary_container` | `on_tertiary_container` | 「持续存在」是一种特殊但非异常的状态，用第三色与默认态区分 |
| 已移除 | `surface_container_highest` | `on_surface_variant` | 「已从通知栏移除」是信息性状态而非错误，用中性容器色；红色只留给权限缺失等真正的异常 |
| 权限缺失提示条 | `error_container` | `on_error_container` | 阻塞型状态，需要用户立即处理 |

### 2.4 对比度实测（WCAG 2.1）

| 前景 / 背景 | 浅色 | 深色 |
| --- | --- | --- |
| `on_surface` / `surface` | 16.31:1 | 14.41:1 |
| `on_surface_variant` / `surface` | 8.90:1 | 10.92:1 |
| `on_primary` / `primary` | 6.48:1 | 7.76:1 |
| `on_primary_container` / `primary_container` | 13.35:1 | 7.25:1 |
| `on_secondary_container` / `secondary_container` | 13.30:1 | 7.28:1 |
| `on_tertiary_container` / `tertiary_container` | 13.32:1 | 7.26:1 |
| `on_error_container` / `error_container` | 13.26:1 | 7.24:1 |
| `on_error` / `error` | 6.46:1 | 7.72:1 |
| `primary` / `surface` | 6.15:1 | 10.88:1 |
| `error` / `surface` | 6.14:1 | 10.92:1 |

全部组合 ≥ 4.5:1，正文与次要文字均满足 AA；主要阅读文本（16:1 左右）接近 AAA。

### 2.5 明暗模式策略

- **单一主题，派生覆盖**：`Theme.NotificationHistory.Base` 定义全部语义色映射，`Theme.NotificationHistory`
  继承它；导航栏这类受 API 限制的属性只在 `values-v27` / `values-night-v27` 中覆盖，避免用 `tools:ignore` 掩盖问题。
  低版本设备沿用系统默认导航栏（黑底白图标），不会出现「透明栏 + 白色图标」不可见的组合。
- **冷启动即正确**：`App.onCreate()` 读取已保存的模式并调用 `AppCompatDelegate.setDefaultNightMode`，
  修正了改造前「只有设置页应用主题、冷启动会先按系统主题绘制一帧」的闪烁问题。
- **动态取色按需启用**：默认关闭（保持统一品牌识别），开启后由 Material DynamicColors 接管，
  关闭立即回退品牌色，两种模式都保证文字对比度达标。
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
3. 新增颜色 → 用统一规范的生成流程（Material Color Utilities，种子色 `#0B57D0`）重新生成
   `values/colors.xml` 与 `values-night/colors.xml`，不要单独补一个色值；
   26 个核心角色的取值必须与统一设计规范基线保持一致。
4. 组件级视觉规则（卡片、标签、设置行、分段按钮）放在 `values/styles.xml`，布局只描述结构。
