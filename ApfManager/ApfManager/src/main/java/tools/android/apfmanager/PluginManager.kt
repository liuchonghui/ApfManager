package tools.android.apfmanager

import android.compact.impl.TaskPayload
import android.compact.utils.IntentCompactUtil
import android.compact.utils.MathCompactUtil
import android.content.Context
import android.content.Intent
import android.os.Parcelable
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import com.limpoxe.fairy.core.FairyGlobal
import com.limpoxe.fairy.core.PluginIntentResolver
import com.limpoxe.fairy.manager.PluginManagerHelper
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import tools.android.async2sync.Connection
import tools.android.async2sync.Packet
import java.io.Serializable
/**
 * plugin - management
 */
class PluginManager: Serializable {

    internal var TAG = "PM"
    private var enableLogcat: Boolean = false
    private var baseUrl: String = ""

    companion object {
        private var instance: PluginManager? = null
        fun get(): PluginManager {
            if (instance == null) {
                synchronized(PluginManager::class.java) {
                    if (instance == null) {
                        instance = PluginManager()
                    }
                }
            }
            return instance!!
        }
    }

    fun enableLogcat(enable: Boolean) {
        this.enableLogcat = enable
    }

    fun setBaseUrl(url: String) {
        this.baseUrl = url
    }

    /**
     * 只更新安装过的
     */
    fun checkAndUpdatePlugins(context: Context, checkCache: Boolean) {
        // 渠道插件的检查安装处理
        val cacheExist = CpPluginTransition.get().infoCacheExist()
        if (!checkCache || (checkCache && !cacheExist)) {
            // 1.如果不检查缓存则直接启动下载校验流程
            // 2.如果检查缓存且缓存无效，也启动下载校验流程
            CpPluginTransition.get().checkAndUpdatePlugins(context)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { plugin ->
                        Log.d(TAG, "Result|" + plugin._id + "|" + plugin.hint)
                        if (!TextUtils.isEmpty(plugin.toast)) {
                            Log.d(TAG, "Toast|checkAndUpdatePlugins|" + plugin._id + "|" + plugin.toast)
                            Toast.makeText(context.applicationContext, plugin.toast, Toast.LENGTH_LONG).show()
                        }
                    }
        }
        // 后续类型插件的检查安装处理
    }

    /**
     * 没安装的安装，需要更新的更新
     */
    fun checkAndInstallPlugins(context: Context, checkCache: Boolean) {
        // 渠道插件的检查安装处理
        val cacheExist = CpPluginTransition.get().infoCacheExist()
        if (!checkCache || (checkCache && !cacheExist)) {
            // 1.如果不检查缓存则直接启动下载校验流程
            // 2.如果检查缓存且缓存无效，也启动下载校验流程
            CpPluginTransition.get().checkAndInstallPlugins(context)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { plugin ->
                        Log.d(TAG, "Result|" + plugin._id + "|" + plugin.hint)
                        if (!TextUtils.isEmpty(plugin.toast)) {
                            Log.d(TAG, "Toast|checkAndInstallPlugins|" + plugin._id + "|" + plugin.toast)
                            Toast.makeText(context.applicationContext, plugin.toast, Toast.LENGTH_LONG).show()
                        }
                    }
        }
        // 后续类型插件的检查安装处理
    }

    @Synchronized
    fun syncProcessTaskPayload(context: Context, payload: TaskPayload?, type: String): TaskPayload {
        var result = TaskPayload()
        var networkConnected = PluginUtil.isNetworkConnected(context.applicationContext)
        if (!networkConnected) {
            result.state = TaskPayloadState.NETWORK_INVALID.code()
            return result
        }
        if (payload == null) {
            result.state = TaskPayloadState.NULL.code()
            return result
        }
        if (TextUtils.isEmpty(payload.identify)) {
            payload.state = TaskPayloadState.INIT_INVALID_ID.code()
            return payload
        }
        if (TextUtils.isEmpty(payload.to)) {
            payload.state = TaskPayloadState.INIT_INVALID_TO.code()
            return payload
        }
        val installed = PluginManagerHelper.isInstalled(payload.to)
        if (!installed) {
            payload.state = TaskPayloadState.TARGET_PLUGIN_NOT_FOUND.code()
            return payload
        } else {
            var ver = ""
            var found = false
            for (pd in PluginManagerHelper.getPlugins()) {
                if (pd != null && pd.packageName == payload.to) {
                    ver = pd.version
                    found = true
                    break
                }
            }
            if (!found) {
                payload.state = TaskPayloadState.TARGET_PLUGIN_NOT_FOUND.code()
                Log.d(TAG, "Illegal: " + payload.to + " not in plugin list!")
                return payload
            }
            payload.tag = ver
        }
        if (TextUtils.isEmpty(payload.from)) {
            payload.from = context.packageName
        }
        if (TextUtils.isEmpty(payload.ch)) {
            payload.ch = MathCompactUtil.randomString(8)
        }

        var filter = PacketIDFilter(payload.ch)
        var collector = mConnection.createPacketCollector(filter)

        val success = sendPayloadToPlugin(context, payload, type)
        if (!success) {
            result.state = TaskPayloadState.TARGET_PLUGIN_NOT_FOUND.code()
            return result
        }
        if (mConnection == null) {
            mConnection = Connection(15000L)
        }
        var newPacket = collector.nextResult(mConnection.connectionTimeOut)
        collector.cancel()
        if (newPacket == null || !(newPacket.content is TaskPayload)) {
            Log.d(TAG, "Illegal: newPacket.content")
            return result
        }
        var newContent = newPacket.content as TaskPayload
        if (payload.identify == newContent.identify) {
            result = newContent
        } else {
            Log.d(TAG, "Illegal: newPacket.content.identify")
        }
        return result
    }

    fun receiveFromPlugin(context: Context, payload: TaskPayload?) {
        if (payload == null || TextUtils.isEmpty(payload.identify)) {
            return
        }
        Log.d(TAG, payload.type + "Receive|ch|" + payload.ch + "|id|" + payload.identify + "|to|" + payload.to)
        var packet = Packet<Any>()
        packet.content = payload
        mConnection.processPacket(packet)
    }

    private var mConnection = Connection(20000L)

    fun sendPayloadToPlugin(context: Context, payload: TaskPayload, type : String?): Boolean {
        return if ("submit".equals(type)) {
            submitTask(context, payload, payload.to)
        } else if ("change".equals(type)) {
            changeTask(context, payload, payload.to)
        } else if ("callback".equals(type)) {
            callbackTask(context, payload, payload.to)
        } else if ("query".equals(type)) {
            queryTask(context, payload, payload.to)
        } else {
            submitTask(context, payload, payload.to)
        }
    }

    private fun submitTask(context: Context?, payload: TaskPayload?, pluginPackageName: String?): Boolean {
        if (context == null || payload == null || pluginPackageName == null) {
            Log.d(TAG, "submitTask|invalidInput")
            return false
        }
        var success: Boolean = false
        payload.type = "submit"
        payload.auth = context.packageName + ".auth.HOST_PROVIDER"
        val intent = Intent(pluginPackageName + ".action.PLUGIN_INTENT_SERVICE")
        intent.`package` = pluginPackageName
        if (!IntentCompactUtil.checkIntentHasHandle(context, intent)) {
            Log.d(TAG, "submitTask|IntentHasNoHandle|" + intent.toUri(Intent.URI_INTENT_SCHEME))
            return false
        }
        if (!checkPluginReadyByServiceName(pluginPackageName + ".PluginIntentService")) {
            Log.d(TAG, "submitTask|PluginNotReady|" + pluginPackageName + ".PluginIntentService")
            return false
        }
        intent.putExtra("taskpayload", payload as Parcelable?)
        val enableLogcat = enableLogcat
        if (enableLogcat) {
            intent.putExtra("enableLogcat", true)
        }
        if (baseUrl != null && baseUrl.length > 0) {
            intent.putExtra("baseurl", baseUrl)
        }
        try {
            Log.d(TAG, "submitTask|ch|" + payload.ch + "|id|" + payload.identify + "|to|" + pluginPackageName)
            if (FairyGlobal.hasPluginFilter() && FairyGlobal.filterPlugin(intent)) {
                PluginIntentResolver.resolveService(intent)
            }
            context.startService(intent)
            success = true
        } catch (e: Exception) {
            success = false
            Log.d(TAG, "Exception|" + e.message)
            e.printStackTrace()
        }
        return success
    }

    fun callbackTask(context: Context?, payload: TaskPayload?, pluginPackageName: String?): Boolean {
        if (context == null || payload == null || pluginPackageName == null) {
            Log.d(TAG, "callbackTask|invalidInput")
            return false
        }
        var success: Boolean = false
        payload.type = "callback"
        payload.auth = context.packageName + ".auth.HOST_PROVIDER"
        val intent = Intent(pluginPackageName + ".action.PLUGIN_INTENT_SERVICE")
        intent.`package` = pluginPackageName
        if (!IntentCompactUtil.checkIntentHasHandle(context, intent)) {
            Log.d(TAG, "callbackTask|IntentHasNoHandle|" + intent.toUri(Intent.URI_INTENT_SCHEME))
            return false
        }
        if (!checkPluginReadyByServiceName(pluginPackageName + ".PluginIntentService")) {
            Log.d(TAG, "callbackTask|PluginNotReady|" + pluginPackageName + ".PluginIntentService")
            return false
        }
        intent.putExtra("taskpayload", payload as Parcelable?)
        val enableLogcat = enableLogcat
        if (enableLogcat) {
            intent.putExtra("enableLogcat", true)
        }
        if (baseUrl != null && baseUrl.length > 0) {
            intent.putExtra("baseurl", baseUrl)
        }
        try {
            Log.d(TAG, "callbackTask|ch|" + payload.ch + "|id|" + payload.identify + "|to|" + pluginPackageName)
            if (FairyGlobal.hasPluginFilter() && FairyGlobal.filterPlugin(intent)) {
                PluginIntentResolver.resolveService(intent)
            }
            context.startService(intent)
            success = true
        } catch (e: Exception) {
            success = false
            Log.d(TAG, "Exception|" + e.message)
            e.printStackTrace()
        }
        return success
    }

    private fun changeTask(context: Context?, payload: TaskPayload?, pluginPackageName: String?): Boolean {
        if (context == null || payload == null || pluginPackageName == null) {
            Log.d(TAG, "changeTask|invalidInput")
            return false
        }
        var success: Boolean = false
        payload.type = "change"
        payload.auth = context.packageName + ".auth.HOST_PROVIDER"
        val intent = Intent(pluginPackageName + ".action.PLUGIN_INTENT_SERVICE")
        intent.`package` = pluginPackageName
        if (!IntentCompactUtil.checkIntentHasHandle(context, intent)) {
            Log.d(TAG, "changeTask|IntentHasNoHandle|" + intent.toUri(Intent.URI_INTENT_SCHEME))
            return false
        }
        if (!checkPluginReadyByServiceName(pluginPackageName + ".PluginIntentService")) {
            Log.d(TAG, "changeTask|PluginNotReady|" + pluginPackageName + ".PluginIntentService")
            return false
        }
        intent.putExtra("taskpayload", payload as Parcelable?)
        val enableLogcat = enableLogcat
        if (enableLogcat) {
            intent.putExtra("enableLogcat", true)
        }
        if (baseUrl != null && baseUrl.length > 0) {
            intent.putExtra("baseurl", baseUrl)
        }
        try {
            Log.d(TAG, "changeTask|ch|" + payload.ch + "|id|" + payload.identify + "|to|" + pluginPackageName)
            if (FairyGlobal.hasPluginFilter() && FairyGlobal.filterPlugin(intent)) {
                PluginIntentResolver.resolveService(intent)
            }
            context.startService(intent)
            success = true
        } catch (e: Exception) {
            success = false
            Log.d(TAG, "Exception|" + e.message)
            e.printStackTrace()
        }
        return success
    }

    private fun queryTask(context: Context?, payload: TaskPayload?, pluginPackageName: String?): Boolean {
        if (context == null || payload == null || pluginPackageName == null) {
            Log.d(TAG, "queryTask|invalidInput")
            return false
        }
        var success: Boolean = false
        payload.type = "query"
        payload.auth = context.packageName + ".auth.HOST_PROVIDER"
        val intent = Intent(pluginPackageName + ".action.PLUGIN_INTENT_SERVICE")
        intent.`package` = pluginPackageName
        if (!IntentCompactUtil.checkIntentHasHandle(context, intent)) {
            Log.d(TAG, "queryTask|IntentHasNoHandle|" + intent.toUri(Intent.URI_INTENT_SCHEME))
            return false
        }
        if (!checkPluginReadyByServiceName(pluginPackageName + ".PluginIntentService")) {
            Log.d(TAG, "queryTask|PluginNotReady|" + pluginPackageName + ".PluginIntentService")
            return false
        }
        intent.putExtra("taskpayload", payload as Parcelable?)
        val enableLogcat = enableLogcat
        if (enableLogcat) {
            intent.putExtra("enableLogcat", true)
        }
        if (baseUrl != null && baseUrl.length > 0) {
            intent.putExtra("baseurl", baseUrl)
        }
        try {
            Log.d(TAG, "queryTask|ch|" + payload.ch + "|id|" + payload.identify + "|to|" + pluginPackageName)
            if (FairyGlobal.hasPluginFilter() && FairyGlobal.filterPlugin(intent)) {
                PluginIntentResolver.resolveService(intent)
            }
            context.startService(intent)
            success = true
        } catch (e: Exception) {
            success = false
            Log.d(TAG, "Exception|" + e.message)
            e.printStackTrace()
        }
        return success
    }

    fun getMessageFromTaskPayloadStates(payloadState: Int?): String {
        var message = ""
        var tps: TaskPayloadState? = null
        for (state in TaskPayloadState.values()) {
            if (state.code() == payloadState) {
                tps = state
                break
            }
        }
        if (tps != null) {
            if (TaskPayloadState.SUCCESS == tps) {
                message = "成功"
            } else if (TaskPayloadState.INIT_INVALID_ID == tps) {
                message = "初始化对象非法id"
            } else if (TaskPayloadState.INIT_INVALID_TO == tps) {
                message = "初始化对象非法接收方"
            } else if (TaskPayloadState.PROCESS_FAILURE == tps) {
                message = "插件方解析结果参数非法"
            } else if (TaskPayloadState.TARGET_PLUGIN_NOT_SUPPORT == tps) {
                message = "目标插件不支持操作"
            } else if (TaskPayloadState.TARGET_PLUGIN_NOT_FOUND == tps) {
                message = "没有找到插件"
            } else if (TaskPayloadState.NETWORK_REQUEST_ERR == tps) {
                message = "网络请求失败"
            } else if (TaskPayloadState.NETWORK_INVALID == tps) {
                message = "网络无法连接"
            } else {
                message += tps.name
            }
        }
        return message
    }

    private fun checkPluginReadyByServiceName(clazzName: String): Boolean {
        try {
            return PluginManagerHelper.getPluginDescriptorByClassName(clazzName) != null
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }
}
