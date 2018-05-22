package com.kuaiest.video.cpplugin

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Environment
import android.text.TextUtils
import android.util.Log
import com.limpoxe.fairy.core.FairyGlobal
import com.limpoxe.fairy.core.PluginLoader
import com.limpoxe.fairy.manager.PluginManagerHelper
import java.io.File
import java.lang.reflect.Method
import android.app.ActivityManager
import android.compact.utils.FileCompactUtil
import android.net.ConnectivityManager
import com.limpoxe.fairy.util.ProcessUtil
import tools.android.apfmanager.PluginManager
import tools.android.apfmanager.R


class PluginUtil {
    companion object {

        fun applicationAttachBaseContext(application: Application?, base: Context?) {
            FairyGlobal.setNeedVerifyPlugin(false) // 不需要比对插件和宿主签名是否一致
            FairyGlobal.setAllowDowngrade(false) // 不允许插件版本出现downgrade
            if (!FairyGlobal.hasPluginFilter()) {
                FairyGlobal.setPluginFilter { input ->
                    input.endsWith(".plugin")
                }
            }
//            com.limpoxe.fairy.util.LogUtil.setEnable(true) // enable logcat
            var success = false
            try {
                PluginLoader.initLoader(application)
                success = true
            } catch (t: Throwable) {
                t.printStackTrace()
                success = false
            }
            if (!success) {
                Log.d("PM", "PluginLoader.initLoader error!")
            }
        }

        fun applicationGetBaseContext(application: Application?, base: Context?): Context {
            FairyGlobal.setNeedVerifyPlugin(false) // 不需要比对插件和宿主签名是否一致
            FairyGlobal.setAllowDowngrade(false) // 不允许插件版本出现downgrade
            if (!FairyGlobal.hasPluginFilter()) {
                FairyGlobal.setPluginFilter { input ->
                    input.endsWith(".plugin")
                }
            }
//            com.limpoxe.fairy.util.LogUtil.setEnable(true) // enable logcat
            return PluginLoader.fixBaseContextForReceiver(base)
        }

        /**
         * 只更新安装过的
         */
        fun checkAndUpdatePlugins(context: Context, checkCache: Boolean) {
            PluginManager.get().checkAndUpdatePlugins(context, checkCache)
        }

        /**
         * 没安装的安装，需要更新的更新
         */
        fun checkAndInstallPlugins(context: Context, checkCache: Boolean) {
            PluginManager.get().checkAndInstallPlugins(context, checkCache)
        }

        val CODE_ID_LOST = 400
        val CODE_PATH_LOST = 401
        val CODE_CACHE_NOT_FOUND = 402
        val CODE_ID_ALREADY_INSTALLED = 403

        @Synchronized
        fun installPlugin(id: String?, path: String?): Int {
            if (TextUtils.isEmpty(id)) {
                return PluginUtil.CODE_ID_LOST
            }
            if (TextUtils.isEmpty(path)) {
                return PluginUtil.CODE_PATH_LOST
            }
            var cacheFile = File(path)
            if (!cacheFile.exists()) {
                return PluginUtil.CODE_CACHE_NOT_FOUND
            }
            val cacheFileMd5 = FileCompactUtil.getMD5(cacheFile)
            val pluginIdentify = id
            for (descriptor in PluginManagerHelper.getPlugins()) {
                val pluginPackageName = descriptor.packageName
                if (pluginPackageName == pluginIdentify) {
                    val installedPluginMd5 = FileCompactUtil.getMD5(File(descriptor.installedPath))
                    if (installedPluginMd5 == cacheFileMd5) {
                        return PluginUtil.CODE_ID_ALREADY_INSTALLED
                    }
                }
            }
            val code = PluginManagerHelper.installPlugin(path)
            try {
                // 增加一点延迟保证框架中新增组件更新完毕
                Thread.sleep(50L)
            } catch (t: Throwable) {
                // ignore
            }
            return code
        }

        fun getPluginErrMsg(context: Context, code: Int?): String {
            return if (code == PluginUtil.CODE_ID_LOST) {
                context.getString(R.string.plugin_warning_id_lost) // "安装前检查：插件标识丢失" // warning
            } else if (code == PluginUtil.CODE_PATH_LOST) {
                context.getString(R.string.plugin_warning_path_lost) // "安装前检查：缓存路径丢失" // warning
            } else if (code == PluginUtil.CODE_CACHE_NOT_FOUND) {
                context.getString(R.string.plugin_warning_cache_not_fount) // "安装前检查：缓存文件不存在" // warning
            } else if (code == PluginUtil.CODE_ID_ALREADY_INSTALLED) {
                context.getString(R.string.plugin_warning_id_already_installed) // "安装前检查：相同文件已安装" // warning
            } else if (code == PluginManagerHelper.SUCCESS) {
                context.getString(R.string.plugin_warning_success) // "成功"
            } else if (code == PluginManagerHelper.SRC_FILE_NOT_FOUND) {
                context.getString(R.string.plugin_warning_src_file_not_found) // "失败: 安装文件未找到"
            } else if (code == PluginManagerHelper.COPY_FILE_FAIL) {
                context.getString(R.string.plugin_warning_copy_file_fail) // "失败: 复制安装文件到安装目录失败"
            } else if (code == PluginManagerHelper.SIGNATURES_INVALIDATE) {
                context.getString(R.string.plugin_warning_signatures_invalidate) // "失败: 安装文件验证失败"
            } else if (code == PluginManagerHelper.VERIFY_SIGNATURES_FAIL) {
                context.getString(R.string.plugin_warning_verify_signatures_fail) // "失败: 插件和宿主签名串不匹配"
            } else if (code == PluginManagerHelper.PARSE_MANIFEST_FAIL) {
                context.getString(R.string.plugin_warning_parse_manifest_fail) // "失败: 插件Manifest文件解析出错"
            } else if (code == PluginManagerHelper.FAIL_BECAUSE_SAME_VER_HAS_LOADED) {
                context.getString(R.string.plugin_warning_same_ver_has_loaded) // "失败: 同版本插件已加载,无需安装"
            } else if (code == PluginManagerHelper.FAIL_BECAUSE_HIGH_VER_HAS_LOADED) {
                context.getString(R.string.plugin_warning_high_ver_has_loaded) // "失败: 高版本插件已加载,无需安装"
            } else if (code == PluginManagerHelper.MIN_API_NOT_SUPPORTED) {
                context.getString(R.string.plugin_warning_min_api_not_supported) // "失败: 当前系统版本过低,不支持此插件"
            } else if (code == PluginManagerHelper.PLUGIN_NOT_EXIST) {
                context.getString(R.string.plugin_warning_plugin_not_exist) // "失败: 插件不存在"
            } else if (code == PluginManagerHelper.REMOVE_FAIL) {
                context.getString(R.string.plugin_warning_remove_fail) // "失败: 删除插件失败"
            } else if (code == PluginManagerHelper.HOST_VERSION_NOT_SUPPORT_CURRENT_PLUGIN) {
                context.getString(R.string.plugin_warning_host_version_not_support_current_plugin) // "失败: 插件要求的宿主版本和当前宿主版本不匹配"
            } else {
                context.getString(R.string.plugin_warning_others) + "code=" + code // "失败: 其他 code=" + code
            }
        }

        fun getPluginErrToast(context: Context, code: Int?): String {
            // warning 不需要 toast
            return if (code == PluginUtil.CODE_ID_LOST) {
                "" // "安装前检查：插件标识丢失"
            } else if (code == PluginUtil.CODE_PATH_LOST) {
                "" // "安装前检查：缓存路径丢失"
            } else if (code == PluginUtil.CODE_CACHE_NOT_FOUND) {
                "" // "安装前检查：缓存文件不存在"
            } else if (code == PluginUtil.CODE_ID_ALREADY_INSTALLED) {
                "" // "安装前检查：相同文件已安装"
            } else if (code == PluginManagerHelper.SUCCESS) {
                "" // "成功" // 成功不用显示toast
            } else if (code == PluginManagerHelper.SRC_FILE_NOT_FOUND) {
                "" // "插件安装失败: 安装文件未找到" // 属于安装前环境检查，不用显示toast
            } else if (code == PluginManagerHelper.COPY_FILE_FAIL) {
                context.getString(R.string.plugin_error_copy_file_fail) // "插件安装失败: 复制安装文件到安装目录失败"
            } else if (code == PluginManagerHelper.SIGNATURES_INVALIDATE) {
                context.getString(R.string.plugin_error_signatures_invalidate) // "插件安装失败: 安装文件验证失败"
            } else if (code == PluginManagerHelper.VERIFY_SIGNATURES_FAIL) {
                context.getString(R.string.plugin_error_verify_signatures_fail) // "插件安装失败: 插件和宿主签名串不匹配"
            } else if (code == PluginManagerHelper.PARSE_MANIFEST_FAIL) {
                context.getString(R.string.plugin_error_parse_manifest_fail) // "插件安装失败: 插件Manifest文件解析出错"
            } else if (code == PluginManagerHelper.FAIL_BECAUSE_SAME_VER_HAS_LOADED) {
                "" // ""插件安装失败: 同版本插件已加载,无需安装" // 属于安装前环境检查，不用显示toast
            } else if (code == PluginManagerHelper.FAIL_BECAUSE_HIGH_VER_HAS_LOADED) {
                "" // ""插件安装失败: 高版本插件已加载,无需安装" // 属于安装前环境检查，不用显示toast
            } else if (code == PluginManagerHelper.MIN_API_NOT_SUPPORTED) {
                context.getString(R.string.plugin_error_min_api_not_supported) // "插件安装失败: 当前系统版本过低,不支持此插件"
            } else if (code == PluginManagerHelper.PLUGIN_NOT_EXIST) {
                context.getString(R.string.plugin_error_plugin_not_exist) // "插件安装失败: 插件不存在"
            } else if (code == PluginManagerHelper.REMOVE_FAIL) {
                context.getString(R.string.plugin_error_remove_fail) // "插件安装失败: 删除插件失败"
            } else if (code == PluginManagerHelper.HOST_VERSION_NOT_SUPPORT_CURRENT_PLUGIN) {
                context.getString(R.string.plugin_error_host_version_not_support_current_plugin) // "插件安装失败: 插件要求的宿主版本和当前宿主版本不匹配"
            } else {
                "" // ""插件安装失败: 其他 code=" + code
            }
        }

        fun getPatchDirPath(context: Context): String {
            return getDirPathByName(context, "patch")
        }

        protected fun getDirPathByName(context: Context, name: String): String {
            var path: String? = null
            var dir: File? = null

            path = getExternalFilesDir(context, Environment.DIRECTORY_PICTURES, name)
            dir = File(path!!)
            if (dir != null && dir.exists() && dir.isDirectory) {
                path = dir.absolutePath
                return path
            }
            return ""
        }

        protected fun getExternalFilesDir(context: Context, environment: String, childOfEnvironment: String): String? {
            var dir: File? = null
            var path: String? = null
            try {
                dir = context.getExternalFilesDir(environment)
                if (dir == null) {
                    path = context.filesDir.toString() + File.separator + childOfEnvironment
                } else {
                    path = dir.absolutePath + File.separator + childOfEnvironment
                }
            } catch (t: Throwable) {
                t.printStackTrace()
            }

            ensureDirectoryExistAndAccessable(path)
            return path
        }

        protected fun ensureDirectoryExistAndAccessable(path: String?): Boolean {
            if (path == null || path.length == 0) {
                return false
            }
            val target = File(path)
            if (!target.exists()) {
                target.mkdirs()
                chmodCompatV23(target, 493)
                return true
            } else if (!target.isDirectory) {
                return false
            }

            chmodCompatV23(target, 493)
            return true
        }

        protected fun chmodCompatV23(path: File, mode: Int): Int {
            return if (Build.VERSION.SDK_INT > 23) {
                0
            } else chmod(path, mode)
        }

        protected fun chmod(path: File, mode: Int): Int {
            val fileUtils: Class<*>
            var setPermissions: Method? = null
            try {
                fileUtils = Class.forName("android.os.FileUtils")
                setPermissions = fileUtils.getMethod("setPermissions",
                        String::class.java, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
                return setPermissions!!.invoke(null, path.absolutePath,
                        mode, -1, -1) as Int
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return 0
        }

        fun getCurrentProcessName(context: Context): String {
            val pid = android.os.Process.myPid()
            val activityManager = context.applicationContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            for (appProcess in activityManager.runningAppProcesses) {
                if (appProcess.pid == pid) {
                    if (appProcess.processName != null) {
                        return appProcess.processName
                    }
                }
            }
            return ""
        }

        fun isPluginProcess(context: Context): Boolean {
            var isPluginProcess = false
            try {
                isPluginProcess = ProcessUtil.isPluginProcess(context.applicationContext)
                        || getCurrentProcessName(context.applicationContext).contains("plugin")
            } catch (t: Throwable) {
                // ignore
                isPluginProcess = false
            }
            return isPluginProcess
        }

        fun isNetworkConnected(context: Context): Boolean {
            try {
                val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val info = cm.activeNetworkInfo
                return info != null && info.isConnectedOrConnecting
            } catch (e: Exception) {
                return false
            }

        }

        fun isNetworkAvailable(context: Context): Boolean {
            return if (isWifiAvailable(context) || isMobileAvailable(context)) {
                true
            } else {
                false
            }
        }

        fun isWifiAvailable(context: Context): Boolean {
            try {
                val connectManager = context
                        .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

                val wifi = connectManager
                        .getNetworkInfo(ConnectivityManager.TYPE_WIFI)
                if (wifi != null && wifi.isAvailable) {
                    return true
                }
            } catch (e: Exception) {
            }

            return false
        }

        fun isMobileAvailable(context: Context): Boolean {
            try {
                val connectManager = context
                        .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

                val mobile = connectManager
                        .getNetworkInfo(ConnectivityManager.TYPE_MOBILE)
                if (mobile != null && mobile.isAvailable) {
                    return true
                }
            } catch (e: Exception) {
            }

            return false
        }
    }
}
