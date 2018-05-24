package tools.android.apfmanager

import android.compact.impl.TaskPayload
import android.compact.utils.FileCompactUtil
import android.content.Context
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import com.google.gson.Gson
import com.kuaiest.video.data.models.jsondata.plugins.Plugin
import com.kuaiest.video.data.models.jsondata.plugins.PluginInfo
import com.kuaiest.video.data.models.jsondata.plugins.PluginResult
import com.limpoxe.fairy.manager.PluginManagerHelper
import okhttp3.*
import java.util.concurrent.CopyOnWriteArrayList
import java.io.Serializable
import rx.Observable
import tools.android.apfmanager.bean.Content
import tools.android.apfmanager.bean.Ex
import tools.android.simpledownloader.SimpleDownloadManager
import tools.android.simpledownloader.DownloadAdapter
import java.io.File
import java.io.IOException

/**
 * Cp插件的具体业务
 */
class CpPluginTransition: Serializable {

    internal var TAG = "CPT"
    // CMS没有测试地址，所以用下面的测试地址，必须测试验证正常再上CMS，因为CMS一旦部署就是现网数据，切记
    internal var DEBUG_UPDATE_URL = "http://45.32.40.65/g/fetch_plugin"
    private var fetchUrl: String = DEBUG_UPDATE_URL
    private var fetchUrlAppend: String = ""

    companion object {
        private var instance: CpPluginTransition? = null
        fun get(): CpPluginTransition {
            if (instance == null) {
                synchronized(CpPluginTransition::class.java) {
                    if (instance == null) {
                        instance = CpPluginTransition()
                    }
                }
            }
            return instance!!
        }
    }

    private var savedPlugin: CopyOnWriteArrayList<Plugin> = CopyOnWriteArrayList()

    /**
     * 替换请求地址
     */
    fun setFetchUrl(url: String?) {
        if (url != null && url.length > 0) {
            fetchUrl = url
        }
    }

    /**
     * 替换请求地址公共参数
     */
    fun setFetchUrlAppend(append: String?) {
        if (append != null && append.length > 0) {
            fetchUrlAppend = append
        }
    }

    fun getFetchUrl(): String {
        return fetchUrl
    }

    fun getFetchUrlAppend(): String {
        return fetchUrlAppend
    }

    fun infoCacheExist(): Boolean {
        return savedPlugin != null && savedPlugin.size > 0
    }

    fun checkCpNeedPlugin(context: Context, cp: String): Observable<Boolean> {
        Log.d(TAG, "checkCpNeedPlugin|cp|" + cp)
        // 如何判断一个视频是否需要插件支持播放？
        // 如果只判断本地，那么进入时断网，点击播放时没有安装插件，点击几次失败几次
        // 如果只判断网络，那么网络接口挂掉时，点击播放，即使有旧插件，点击几次失败几次
        // 所以有本地匹配插件时，走本地判断；有网络合法结果时，走网络判断
        // 这样最坏情况只有当渠道插件从未安装过，才会播放失败

        // 如果本地存在，即使旧版本，也认为匹配
        if (checkLocalExistCpPlugin(context, cp)) {
            return Observable.create { subscriber ->
                Log.d(TAG, "checkCpNeedPlugin|existPlugin|valid&match")
                subscriber.onNext(true)
                subscriber.onCompleted()
            }
        }

        // 如果缓存info存在，检查是否匹配
        if (checkLocalCacheCpPlugin(context, cp)) {
            return Observable.create { subscriber ->
                Log.d(TAG, "checkCpNeedPlugin|savedPlugin|valid&match")
                subscriber.onNext(true)
                subscriber.onCompleted()
            }
        }
        // 如果缓存info不为空，且上一步的匹配没有匹配上，说明该cp不是特殊cp，不需要走插件
        if (!savedPlugin?.isEmpty()) {
            return Observable.create { subscriber ->
                Log.d(TAG, "checkCpNeedPlugin|savedPlugin|valid&notMatch")
                subscriber.onNext(false)
                subscriber.onCompleted()
            }
        }
        // 这种情况就是缓存info为空，那么去请求获取缓存数据同时填充缓存info
        Log.d(TAG, "checkCpNeedPlugin|savedPlugin|invalid|needRequest")
        return requestPluginInfo(context)
                .flatMap { pluginResult -> checkPluginResult(cp, pluginResult) }
    }

    private fun checkLocalExistCpPlugin(context: Context, cp: String?): Boolean {
        Log.d(TAG, "checkLocalExistCpPlugin|cp|" + cp)
        var catched = false
        for (pd in PluginManagerHelper.getPlugins()) {
            var pluginPackageName = pd.packageName
            var pluginVersion = pd.version
            Log.d(TAG, "checkLocalExistCpPlugin|plugin|" + pluginPackageName + "|cp|" + pluginVersion)
            if (!TextUtils.isEmpty(pluginVersion) && pluginVersion.startsWith(cp + "_")) {
                Log.d(TAG, "checkLocalExistCpPlugin|cp|" + cp + "|catched|" + pd.packageName)
                catched = true
                break
            }
        }
        return catched
    }

    private fun checkLocalCacheCpPlugin(context: Context, cp: String?): Boolean {
        Log.d(TAG, "checkLocalCacheCpPlugin|cp|" + cp)
        var catched = false
        if (savedPlugin != null) {
            for (plugin in savedPlugin) {
                Log.d(TAG, "checkLocalCacheCpPlugin|plugin|" + plugin._id + "|cp|" + plugin.cp)
                if (cp != null && cp == plugin.cp) {
                    Log.d(TAG, "checkLocalCacheCpPlugin|cp|" + cp + "|catched|" + plugin._id)
                    catched = true
                    break
                }
            }
        }
        return catched
    }

    fun checkPluginResult(cp: String?, pluginResult: PluginResult?): Observable<Boolean> {
        Log.d(TAG, "checkPluginResult|cp|" + cp)
        return Observable.create { subscriber ->
            var handle = false
            if (pluginResult == null || pluginResult.data == null
                    || !"success".equals(pluginResult.data.result)) {
                // 各种网络失败，数据失败的情况
                Log.d(TAG, "checkPluginResult|cp|" + cp + "|plugin data denied")
            } else {
                // 网络成功
                Log.d(TAG, "checkPluginResult|cp|" + cp + "|plugin data ok")
                val pluginList = pluginResult?.data?.cp_plugin
                if (pluginList != null) {
                    for (plugin in pluginList) {
                        if (cp == plugin.cp) {
                            Log.d(TAG, "checkPluginResult|cp|" + cp + "|match|" + plugin._id)
                            // 当前网络返回数据有效的话，比对键值是否一致，一致的话验证通过
                            handle = true
                        }
                    }
                }
            }
            Log.d(TAG, "checkPluginResult|ret|" + handle)
            subscriber.onNext(handle)
            subscriber.onCompleted()
        }
    }

    fun submitTaskOnPlugin(context: Context, cp: String?, videoId: String?, content: Content?, targetDefinition: VideoDefinition?): Observable<TaskPayload> {
        Log.d(TAG, "submitTaskOnPlugin|cp|" + cp + "|videoId|" + videoId)
        return Observable.create { subscriber ->
            var result: TaskPayload? = null
            val payload = TaskPayload()
            payload.identify = videoId
            payload.cp = cp
            payload.content = Gson().toJson(content)
            payload.color = parseDefinitionCode(targetDefinition)
            payload.to = getPluginIdentifyByCpName(cp)

            Log.d(TAG, ">submitTaskOnPlugin|id|" + payload.identify + "|cp|" + payload.cp + "|to|" + payload.to + "|content|" + payload.content + "|state|" + payload.state + "|ex|" + payload.ex)
            result = PluginManager.get().syncProcessTaskPayload(context, payload, "submit")
            Log.d(TAG, "<submitTaskOnPlugin|ch|" + result.ch + "|id|" + result.identify + "|cp|" + result.cp + "|to|" + result.to + "|content|" + result.content + "|state|" + result.state + "|ex|" + result.ex)

            if (TaskPayloadState.SUCCESS.code() == result?.state) {
                subscriber.onNext(result)
            } else {
                var message = PluginManager.get().getMessageFromTaskPayloadStates(result.state)
                Log.d(TAG, "submitTaskOnPlugin|failure|" + message)
                subscriber.onNext(result)
            }
            subscriber.onCompleted()
        }
    }

    fun changeTaskOnPlugin(context: Context, cp: String?, videoId: String?, definition: Int): Observable<TaskPayload> {
        Log.d(TAG, "changeTaskOnPlugin|cp|" + cp + "|videoId|" + videoId + "|definition|" + definition)
        return Observable.create { subscriber ->
            var result: TaskPayload? = null
            val payload = TaskPayload()
            payload.identify = videoId
            payload.cp = cp
            payload.color = definition
            payload.to = getPluginIdentifyByCpName(cp)

            Log.d(TAG, ">changeTaskOnPlugin|id|" + payload.identify + "|cp|" + payload.cp + "|to|" + payload.to + "|content|" + payload.content + "|state|" + payload.state + "|ex|" + payload.ex)
            result = PluginManager.get().syncProcessTaskPayload(context, payload, "change")
            Log.d(TAG, "<changeTaskOnPlugin|ch|" + result.ch + "|id|" + result.identify + "|cp|" + result.cp + "|to|" + result.to + "|content|" + result.content + "|state|" + result.state + "|ex|" + result.ex)

            if (TaskPayloadState.SUCCESS.code() == result?.state) {
                subscriber.onNext(result)
            } else {
                var message = PluginManager.get().getMessageFromTaskPayloadStates(result.state)
                Log.d(TAG, "changeTaskOnPlugin|failure|" + message)
                subscriber.onNext(result)
            }
            subscriber.onCompleted()
        }
    }

    fun queryTaskOnPlugin(context: Context, cp: String?, videoId: String?, content: Content, targetDefinition: VideoDefinition?, queryWhat: PluginQuery): Observable<TaskPayload> {
        Log.d(TAG, "queryTaskOnPlugin|cp|" + cp + "|videoId|" + videoId + "|query|" + queryWhat)
        return Observable.create { subscriber ->
            var result: TaskPayload? = null
            val payload = TaskPayload()
            payload.identify = videoId
            payload.cp = cp
            content.query = queryWhat.what
            payload.content = Gson().toJson(content)
            payload.color = parseDefinitionCode(targetDefinition)
            payload.to = getPluginIdentifyByCpName(cp)

            Log.d(TAG, ">queryTaskOnPlugin|id|" + payload.identify + "|cp|" + payload.cp + "|to|" + payload.to + "|content|" + payload.content + "|state|" + payload.state + "|ex|" + payload.ex)
            result = PluginManager.get().syncProcessTaskPayload(context, payload, "query")
            Log.d(TAG, "<queryTaskOnPlugin|ch|" + result.ch + "|id|" + result.identify + "|cp|" + result.cp + "|to|" + result.to + "|content|" + result.content + "|state|" + result.state + "|ex|" + result.ex)

            if (TaskPayloadState.SUCCESS.code() == result?.state) {
                subscriber.onNext(result)
            } else {
                var message = PluginManager.get().getMessageFromTaskPayloadStates(result.state)
                Log.d(TAG, "queryTaskOnPlugin|failure|" + message)
                subscriber.onNext(result)
            }
            subscriber.onCompleted()
        }
    }

    /**
     * checkAndRequestPlugin中的第4步
     * 详细查看：checkAndRequestPlugin
     */
    fun installPlugin(context: Context, plugin: Plugin): Observable<Plugin> {
        return Observable.create { subscriber ->
            val alreadyInstalled = PluginManagerHelper.isInstalled(plugin._id)
            if (alreadyInstalled) {
                for (pd in PluginManagerHelper.getPlugins()) {
                    if (pd != null && pd.packageName == plugin._id) {
                        val pluginVersion = pd.version
                        Log.d(TAG, "alreadyInstalledPlugin|${plugin._id}|version|$pluginVersion")
                    }
                }
            }
            val resultCode = PluginUtil.installPlugin(plugin._id, plugin.path)
            val resultMsg = PluginUtil.getPluginErrMsg(context.applicationContext, resultCode)
            val resultToast = PluginUtil.getPluginErrToast(context.applicationContext, resultCode)
            var appendMsg = ""
            for (pd in PluginManagerHelper.getPlugins()) {
                if (pd != null && pd.packageName == plugin._id) {
                    val pluginPackageName = pd.packageName
                    val pluginEnable = pd.isEnabled
                    val pluginStandAlone = pd.isStandalone
                    val pluginVersion = pd.version
                    val pluginPath = pd.installedPath
                    val pluginMd5 = FileCompactUtil.getMD5(File(pluginPath))
                    appendMsg = "|updatePlugin|packageName|$pluginPackageName|version|$pluginVersion|enable|$pluginEnable|standalone|$pluginStandAlone|path|$pluginPath|md5|$pluginMd5"
                }
            }
            plugin.hint = resultMsg + appendMsg
            plugin.toast = resultToast
            val installSuccess = PluginManagerHelper.isInstalled(plugin._id)
            if (installSuccess) {
                Log.d(TAG, "installPlugin|packageName|${plugin._id}|installSuccess|delete cache")
                val cache = File(plugin.path)
                if (cache.exists() && cache.isFile) {
                    cache.delete()
                }
            }
            subscriber.onNext(plugin)
            subscriber.onCompleted()
        }
    }

    /**
     * checkAndRequestPlugin中的第3步
     * 详细查看：checkAndRequestPlugin
     */
    fun downloadPlugin(context: Context, plugin: Plugin): Observable<Plugin> {
        return Observable.create { subscriber ->
            SimpleDownloadManager.get().downloadSimpleFile(context.applicationContext,
                    plugin._id, "apk", plugin.md5, plugin.url, object : DownloadAdapter() {
                override fun onDownloadSuccess(url: String, path: String) {
                    val p = Plugin()
                    p._id = plugin._id
                    p.md5 = plugin.md5
                    p.url = plugin.url
                    p.path = path
                    Log.d(TAG, "onDownloadSuccess|" + p._id + "|" + p.path + "|" + hashCode() + "|" + releaseCode)
                    subscriber.onNext(p)
                }
                override fun onDownloadFailure(url: String, message: String?) {
                    Log.d(TAG, "onDownloadFailure|" + url + "|" + message)
                }
            })
        }
    }

    /**
     * 升级策略（只更新安装过的）：
     * 1.如果本地存在的plugin，平台没有部署，那就是要删除（删除不需要的）
     * 2.如果本地存在的plugin，和平台存在的md5对不上，那就是要在本地的基础上升级（本地不存在的不管）
     */
    private fun checkPluginOnlyUpdate(context: Context, pluginResult: PluginResult?): Observable<List<Plugin>> {
        return Observable.create(Observable.OnSubscribe { subscriber ->
            val needToDown = ArrayList<Plugin>()
            Log.d(TAG, "check plugin need to update+")
            if (pluginResult == null || pluginResult.data == null
                    || !"success".equals(pluginResult.data.result)) {
                Log.d(TAG, "plugin data denied+")
                subscriber.onNext(needToDown)
                subscriber.onCompleted()
                return@OnSubscribe
            }
            val remotePlugins = HashMap<String, Plugin>()
            val pluginList = pluginResult?.data?.cp_plugin
            if (pluginList != null) {
                for (plugin in pluginList) {
                    remotePlugins.put(plugin._id, plugin)
                }
            }
            Log.d(TAG, "remote plugin size+" + remotePlugins.size)
            if (PluginManagerHelper.getPlugins() == null || PluginManagerHelper.getPlugins().size == 0) {
                Log.d(TAG, "local plugin not exist+")
            }
            var localPlugins = HashMap<String, PluginInfo>()
            for (pd in PluginManagerHelper.getPlugins()) {
                val pluginPackageName = pd.packageName
                val pluginEnable = pd.isEnabled
                val pluginStandAlone = pd.isStandalone
                val pluginVersion = pd.version
                val pluginPath = pd.installedPath
                val pluginMd5 = FileCompactUtil.getMD5(File(pluginPath))
                Log.d(TAG, "installedPlugin+|packageName|$pluginPackageName|version|$pluginVersion|enable|$pluginEnable|standalone|$pluginStandAlone|path|$pluginPath|md5|$pluginMd5")
                val info = PluginInfo()
                info.id = pluginPackageName
                info.pluginEnable = pluginEnable
                info.pluginStandAlone = pluginStandAlone
                info.pluginVersion = pluginVersion
                info.pluginPath = pluginPath
                info.pluginMd5 = pluginMd5
                localPlugins.put(pluginPackageName, info)
            }
            // 禁止删除支持
//            val needToDeleteIds = ArrayList<String>()
//            for (local in localPlugins.values) {
//                // 如果本地存在的plugin，平台没有部署，那就是要删除
//                if (remotePlugins[local.id] == null) {
//                    Log.d(TAG, "needDelete+|" + local.id + "|" + local.pluginPath)
//                    PluginManagerHelper.remove(local.id)
//                    needToDeleteIds.add(local.id)
//                }
//            }
//            // 上面删除了平台端没有的，所以删除后需要更新本地数据
//            for (deletedId in needToDeleteIds) {
//                localPlugins.remove(deletedId)
//            }
            for (local in localPlugins.values) {
                // 如果本地存在的plugin，和平台存在的md5对不上，那就是要在本地的基础上升级
                val remotePlugin = remotePlugins[local.id]
                if (remotePlugin == null || remotePlugin.md5 == local.pluginMd5) {
                    // 平台没有，或者平台的md5与本地一致，不用更新
                    continue
                }
                needToDown.add(remotePlugin)
                Log.d(TAG, "needToDown+|" + remotePlugin._id + "|" + remotePlugin.url)
            }
            subscriber.onNext(needToDown)
            subscriber.onCompleted()
        })
    }

    /**
     * 升级策略（没安装的安装，需要更新的更新）：
     * 1.如果本地存在的plugin，平台没有部署，那就是要删除（删除不需要的）
     * 2.如果平台部署的plugin，本地不存在，或者和本地存在的md5对不上，那就是要更新
     *
     * checkAndRequestPlugin中的第2步
     * 详细查看：checkAndRequestPlugin
     */
    private fun checkPluginInstallOrUpdate(context: Context, pluginResult: PluginResult?): Observable<List<Plugin>> {
        return Observable.create(Observable.OnSubscribe { subscriber ->
            val needToDown = ArrayList<Plugin>()
            Log.d(TAG, "check plugin need to update:")
            if (pluginResult == null || pluginResult.data == null
                    || !"success".equals(pluginResult.data.result)) {
                Log.d(TAG, "plugin data denied:")
                subscriber.onNext(needToDown)
                subscriber.onCompleted()
                return@OnSubscribe
            }
            val remotePlugins = HashMap<String, Plugin>()
            val pluginList = pluginResult?.data?.cp_plugin
            if (pluginList != null) {
                for (plugin in pluginList) {
                    remotePlugins.put(plugin._id, plugin)
                }
            }
            Log.d(TAG, "remote plugin size:" + remotePlugins.size)
            if (PluginManagerHelper.getPlugins() == null || PluginManagerHelper.getPlugins().size == 0) {
                Log.d(TAG, "local plugin not exist:")
            }
            var localPlugins = HashMap<String, PluginInfo>()
            for (pd in PluginManagerHelper.getPlugins()) {
                val pluginPackageName = pd.packageName
                val pluginEnable = pd.isEnabled
                val pluginStandAlone = pd.isStandalone
                val pluginVersion = pd.version
                val pluginPath = pd.installedPath
                val pluginMd5 = FileCompactUtil.getMD5(File(pluginPath))
                Log.d(TAG, "installedPlugin:|packageName|$pluginPackageName|version|$pluginVersion|enable|$pluginEnable|standalone|$pluginStandAlone|path|$pluginPath|md5|$pluginMd5")
                val info = PluginInfo()
                info.id = pluginPackageName
                info.pluginEnable = pluginEnable
                info.pluginStandAlone = pluginStandAlone
                info.pluginVersion = pluginVersion
                info.pluginPath = pluginPath
                info.pluginMd5 = pluginMd5
                localPlugins.put(pluginPackageName, info)
            }
            // 禁止删除支持
//            val needToDeleteIds = ArrayList<String>()
//            for (local in localPlugins.values) {
//                // 如果本地存在的plugin，平台没有部署，那就是要删除
//                if (remotePlugins[local.id] == null) {
//                    Log.d(TAG, "needDelete:|" + local.id + "|" + local.pluginPath)
//                    PluginManagerHelper.remove(local.id)
//                    needToDeleteIds.add(local.id)
//                }
//            }
//            // 上面删除了平台端没有的，所以删除后需要更新本地数据
//            for (deletedId in needToDeleteIds) {
//                localPlugins.remove(deletedId)
//            }
            for (remotePlugin in remotePlugins.values) {
                // 如果平台部署的plugin，和本地plugin的md5对不上，那就是要下载安装
                val local = localPlugins[remotePlugin._id]
                if (local != null && local.pluginMd5 == remotePlugin.md5) {
                    // 本地存在，并且本地的md5与平台一致，不用更新
                    continue
                }
                needToDown.add(remotePlugin)
                Log.d(TAG, "needToDown:|" + remotePlugin._id + "|" + remotePlugin.url)
            }
            subscriber.onNext(needToDown)
            subscriber.onCompleted()
        })
    }

    /**
     * checkAndRequestPlugin中的第1步
     * 详细查看：checkAndRequestPlugin
     */
    fun requestPluginInfo(context: Context): Observable<PluginResult> {
        var fetchUrl = getFetchUrl() + getFetchUrlAppend()
        Log.d(TAG, "requestPlugin|" + fetchUrl)
        return Observable.create { subscriber ->
            val client = OkHttpClient().newBuilder().build()
            val request = Request.Builder().url(fetchUrl).build()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.d(TAG, "checkPlugin|retJson|invalid")
                    subscriber.onNext(PluginResult())
                    subscriber.onCompleted()
                }

                @Throws(Exception::class)
                override fun onResponse(call: Call, response: Response) {
                    var retJson: String? = null
                    var pluginResult: PluginResult? = null
                    if (response.isSuccessful) {
                        retJson = response.body().string()
                        if (retJson != null) {
                            try {
                                pluginResult = Gson().fromJson(retJson, PluginResult::class.java)
                            } catch (e: Exception) {
                            }
                        }
                    }
                    Log.d(TAG, "checkPlugin|retJson|" + retJson)
                    if (pluginResult?.data?.cp_plugin != null
                            && "success" == pluginResult?.data?.result) {
                        var pluginList = pluginResult?.data?.cp_plugin
                        if (pluginList != null && savedPlugin != null) {
                            // 快速调试
//                            for (p in pluginList) {
//                                if (p._id == "fengxing.plugin") {
//                                    p.md5 = "74f477ca2ab9d42ac8fa5e28d2ca850e"
//                                    p.url = "https://gist.github.com/liuchonghui/b9757b65748eb42548213ec7b9572116/raw/167fb55ef0374c438794f4acc47099a8566bd558/fengxing.1.5_8.74f477ca2ab9d42ac8fa5e28d2ca850e.apk"
//                                } else if (p._id == "renren.plugin") {
//                                    p.md5 = "fd793d3215aec8810709480cf510f4db"
//                                    p.url = "https://gist.github.com/liuchonghui/b9757b65748eb42548213ec7b9572116/raw/167fb55ef0374c438794f4acc47099a8566bd558/renren.1.5_8.fd793d3215aec8810709480cf510f4db.apk"
//                                } else if (p._id == "yilan.plugin") {
//                                    p.md5 = "b1d8b3835c6c740bbf5f8664bf2403a7"
//                                    p.url = "https://gist.github.com/liuchonghui/b9757b65748eb42548213ec7b9572116/raw/167fb55ef0374c438794f4acc47099a8566bd558/yilan.1.5_8.b1d8b3835c6c740bbf5f8664bf2403a7.apk"
//                                } else if (p._id == "chushou.plugin") {
//                                    p.md5 = "a13eaf0dda181759d2bdc541d0de8b9d"
//                                    p.url = "https://gist.github.com/liuchonghui/b9757b65748eb42548213ec7b9572116/raw/167fb55ef0374c438794f4acc47099a8566bd558/chuoshou.1.5_8.a13eaf0dda181759d2bdc541d0de8b9d.apk"
//                                }
//                            }
                            savedPlugin.clear()
                            savedPlugin.addAll(pluginList)
                        }
                        subscriber.onNext(pluginResult)
                    } else {
                        subscriber.onNext(PluginResult())
                    }
                    subscriber.onCompleted()
                }
            })
        }
    }

    /**
     * 只更新安装过的
     */
    fun checkAndUpdatePlugins(context: Context): Observable<Plugin> {
        return requestPluginInfo(context)
                .flatMap { pluginResult -> checkPluginOnlyUpdate(context, pluginResult) }
                .flatMap { plugins -> Observable.from(plugins) }
                .flatMap { plugin -> downloadPlugin(context, plugin) }
                .flatMap { plugin -> installPlugin(context, plugin) }
    }

    /**
     * 没安装的安装，需要更新的更新
     * checkAndRequestPlugin中的前第4步
     * 详细查看：checkAndRequestPlugin
     * 顺序：检查-下载-安装
     */
    fun checkAndInstallPlugins(context: Context): Observable<Plugin> {
        return requestPluginInfo(context)
                .flatMap { pluginResult -> checkPluginInstallOrUpdate(context, pluginResult) }
                .flatMap { plugins -> Observable.from(plugins) }
                .flatMap { plugin -> downloadPlugin(context, plugin) }
                .flatMap { plugin -> installPlugin(context, plugin) }
    }

    /**
     * 顺序：检查-（下载单个-安装单个）-执行
     */
    fun checkAndRequestPlugin(context: Context, cp: String, videoId: String, content: Content?, targetDefinition: VideoDefinition?): Observable<TaskPayload> {
        val pluginIdentify = getPluginIdentifyByCpName(cp)
        val installed = PluginManagerHelper.isInstalled(pluginIdentify)
        // 一切都以cp插件是否被安装为前提
        // 如果安装，直接执行向插件发送命令的语句(即直接执行第5步)，内部仍旧有是否存在接收方的判断
        // 如果未安装：
        // 第1步. 发起一次新的联网请求，此时如果另外线程已经安装完毕，则这次联网也会执行
        // 第2步. 联网后分析数据，安装或卸载或维持现状，此时如果另外线程已经安装完毕，则这次分析也会执行
        // 第3步. 逐一进行下载，此时如果另外线程已经安装完毕，则这次下载也会执行，如果另外线程正在下载，则多个线程会走唯一的下载任务
        // 第4步. 逐一进行安装，此时如果另外线程已经安装完毕，则此次安装会提示已有相同版本无需安装，线程间不受影响
        // 第5步. 向插件发送命令(此处是submit命令)，发请求和处理是单一线程执行，各线程之间会异步处理结果
        return if (installed) {
            submitTaskOnPlugin(context, cp, videoId, content, targetDefinition)
        } else {
            requestPluginInfo(context) // 第1步
                    .flatMap { pluginResult -> checkPluginInstallOrUpdate(context, pluginResult) } // 第2步
                    .flatMap { plugins -> Observable.from(plugins) }
                    .filter { plugin -> plugin._id == pluginIdentify }
                    .flatMap { plugin -> downloadPlugin(context, plugin) } // 第3步
                    .flatMap { plugin -> installPlugin(context, plugin) } // 第4步
                    .flatMap { plugin ->
                        Log.d(TAG, "Result|" + plugin._id + "|" + plugin.hint)
                        if (!TextUtils.isEmpty(plugin.toast)) {
                            Log.d(TAG, "Toast|checkAndRequestPlugin|" + plugin._id + "|" + plugin.toast)
                            Toast.makeText(context.applicationContext, plugin.toast, Toast.LENGTH_LONG).show()
                        }
                        submitTaskOnPlugin(context, cp, videoId, content, targetDefinition)
                    } // 第5步

        }
    }

    private fun getPluginIdentifyByCpName(cp: String?): String? {
        var pluginIdentify: String? = null
        if (savedPlugin != null) {
            for (plugin in savedPlugin) {
                if (plugin.cp == cp) {
                    pluginIdentify = plugin._id
                    break
                }
            }
        }
        if (TextUtils.isEmpty(pluginIdentify)) {
            // 如果网络缓存中没有找到，去插件框架中找
            for (pd in PluginManagerHelper.getPlugins()) {
                var pluginVersion = pd.version
                if (!TextUtils.isEmpty(pluginVersion) && pluginVersion.startsWith(cp + "_")) {
                    pluginIdentify = pd.packageName
                    break
                }
            }
        }
        return pluginIdentify
    }

    fun getPlayUrlListByDefinition(ex: Ex): ArrayList<String> {
        var retList = ArrayList<String>()
        var defStr = ex.definition
        if (VideoDefinition.DEFINITION_LOW.desc() == defStr) {
            retList.addAll(ex.lowDefList)
        } else if (VideoDefinition.DEFINITION_NORMAL.desc() == defStr) {
            retList.addAll(ex.normalDefList)
        } else if (VideoDefinition.DEFINITION_HIGH.desc() == defStr) {
            retList.addAll(ex.highDefList)
        } else if (VideoDefinition.DEFINITION_SUPER.desc() == defStr) {
            retList.addAll(ex.superDefList)
        }
        return retList
    }

    @Synchronized
    fun callbackCpPlugin(context: Context, identify: String, cp: String, state: TaskPayloadState) {
        Log.d(TAG, "callbackCpPlugin|cp|" + cp + "|identify|" + identify + "|state|" + state)
        if (TextUtils.isEmpty(identify)) {
            return
        }
        val pluginIdentify = getPluginIdentifyByCpName(cp)
        if (TextUtils.isEmpty(pluginIdentify)) {
            return
        }
        val payload = TaskPayload()
        payload.identify = identify
        payload.to = pluginIdentify
        payload.state = state.code()
        if (TextUtils.isEmpty(payload.from)) {
            payload.from = context.packageName
        }
        PluginManager.get().callbackTask(context, payload, payload.to)
    }

    @Synchronized
    fun clearAllCpPlugin(context: Context) {
        Log.d(TAG, "clearAllCpPlugin")
        for (pd in PluginManagerHelper.getPlugins()) {
            val payload = TaskPayload()
            payload.identify = "universe"
            payload.to = pd.packageName
            payload.state = TaskPayloadState.HOST_CALLBACK_CLEARALL.code()
            if (TextUtils.isEmpty(payload.from)) {
                payload.from = context.packageName
            }
            PluginManager.get().callbackTask(context, payload, payload.to)
        }
    }

    fun getDefinitionByCurrentNetwork(isWifi: Boolean): VideoDefinition {
        return if (isWifi) {
            // wifi下选择最高分辨率
            VideoDefinition.DEFINITION_SUPER
        } else {
            // 非wifi下选择最低分辨率
            VideoDefinition.DEFINITION_LOW
        }
    }

    fun parseDefinitionCode(definition: VideoDefinition?): Int {
        return if (definition != null) {
            definition.code()
        } else {
            VideoDefinition.DEFINITION_LOW.code()
        }
    }

    fun checkAndQueryPlugin(context: Context, cp: String, videoId: String, content: Content, targetDefinition: VideoDefinition?, queryWhat: PluginQuery): Observable<TaskPayload> {
        val pluginIdentify = getPluginIdentifyByCpName(cp)
        val installed = PluginManagerHelper.isInstalled(pluginIdentify)
        // 一切都以cp插件是否被安装为前提
        // 如果安装，直接执行向插件发送命令的语句(即直接执行第5步)，内部仍旧有是否存在接收方的判断
        // 如果未安装：
        // 第1步. 发起一次新的联网请求，此时如果另外线程已经安装完毕，则这次联网也会执行
        // 第2步. 联网后分析数据，安装或卸载或维持现状，此时如果另外线程已经安装完毕，则这次分析也会执行
        // 第3步. 逐一进行下载，此时如果另外线程已经安装完毕，则这次下载也会执行，如果另外线程正在下载，则多个线程会走唯一的下载任务
        // 第4步. 逐一进行安装，此时如果另外线程已经安装完毕，则此次安装会提示已有相同版本无需安装，线程间不受影响
        // 第5步. 向插件发送命令(此处是query命令)，发请求和处理是单一线程执行，各线程之间异步处理结果
        return if (installed) {
            queryTaskOnPlugin(context, cp, videoId, content, targetDefinition, queryWhat)
        } else {
            requestPluginInfo(context) // 第1步
                    .flatMap { pluginResult -> checkPluginInstallOrUpdate(context, pluginResult) } // 第2步
                    .flatMap { plugins -> Observable.from(plugins) }
                    .filter { plugin -> plugin._id == pluginIdentify }
                    .flatMap { plugin -> downloadPlugin(context, plugin) } // 第3步
                    .flatMap { plugin -> installPlugin(context, plugin) } // 第4步
                    .flatMap { plugin ->
                        Log.d(TAG, "Result|" + plugin._id + "|" + plugin.hint)
                        if (!TextUtils.isEmpty(plugin.toast)) {
                            Log.d(TAG, "Toast|checkAndQueryPlugin|" + plugin._id + "|" + plugin.toast)
                            Toast.makeText(context.applicationContext, plugin.toast, Toast.LENGTH_LONG).show()
                        }
                        queryTaskOnPlugin(context, cp, videoId, content, targetDefinition, queryWhat)
                    } // 第5步

        }
    }
}
