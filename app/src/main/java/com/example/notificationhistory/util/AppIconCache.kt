package com.example.notificationhistory.util

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.LruCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 应用图标内存缓存 + 异步加载。
 *
 * 背景：列表绑定时直接调用 `PackageManager.getApplicationIcon()` 是一次 binder IPC，
 * 且每行、每次全量重绑（如切换「显示包名/时间格式」后返回主界面）都会重复执行，
 * 通知来源多时会造成明显掉帧。
 *
 * 方案：
 * - 内存 LruCache 按条目数上限缓存 Drawable（应用数量有限，128 个足够覆盖）；
 * - 未命中时先展示系统默认图标占位，再在 IO 线程加载，回主线程按「行-包名」校验后填充，
 *   避免滚动复用过程中把图标画到已被复用的行上；
 * - 加载结果写入缓存，之后所有绑定都不再有 IPC。
 */
object AppIconCache {

    private const val MAX_ENTRIES = 128

    private val cache = LruCache<String, Drawable>(MAX_ENTRIES)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** 同步取缓存；未命中返回 null（由调用方先展示占位图并异步加载） */
    fun get(packageName: String): Drawable? = cache.get(packageName)

    fun loadAsync(context: Context, packageName: String, onLoaded: (Drawable) -> Unit) {
        val appContext = context.applicationContext
        scope.launch {
            val drawable = runCatching { appContext.packageManager.getApplicationIcon(packageName) }
                .getOrNull() ?: return@launch
            cache.put(packageName, drawable)
            withContext(Dispatchers.Main) { onLoaded(drawable) }
        }
    }
}
