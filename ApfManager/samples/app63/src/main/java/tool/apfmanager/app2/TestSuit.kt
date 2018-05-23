package tool.apfmanager.app2

import android.compact.impl.TaskPayload
import android.content.Context
import android.text.TextUtils
import android.widget.Toast
import com.google.gson.Gson
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import tools.android.apfmanager.CpPluginTransition
import tools.android.apfmanager.VideoDefinition
import tools.android.apfmanager.bean.Content

class TestSuit {

    companion object {
        private var instance: TestSuit? = null
        fun get(): TestSuit {
            if (instance == null) {
                synchronized(TestSuit::class.java) {
                    if (instance == null) {
                        instance = TestSuit()
                    }
                }
            }
            return instance!!
        }
    }

    fun testAndToast(appContext: Context, cp: String) {
        testPluginByCp(appContext.applicationContext, cp)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { payload ->
                    var payloadStr = Gson().toJson(payload)
                    if (!TextUtils.isEmpty(payloadStr)) {
                        Toast.makeText(appContext.applicationContext, payloadStr, Toast.LENGTH_LONG).show()
                    }
                }
    }

    fun testPluginByCp(appContext: Context, cp: String): Observable<TaskPayload> {
        var videoId = getTestVideoIdByCp(cp)
        var content = getTestContentByCp(cp)
        return CpPluginTransition.get().submitTaskOnPlugin(appContext, cp, videoId, content, VideoDefinition.DEFINITION_NORMAL)
    }

    private fun getTestVideoIdByCp(cp: String): String {
        return if ("cy".equals(cp)) {
            "3052576"
        } else if ("cs".equals(cp)) {
            "3053745"
        } else if ("el".equals(cp)) {
            "Ym5P9xoRLynw"
        } else if ("fh".equals(cp)) {
            "http://ips.ifeng.com/video19.ifeng.com/video09/2016/08/27/318849-102-998767-191913.mp4?vid=0178355a-1ce8-472a-9def-7b1d474a61b1&ts=" + System.currentTimeMillis()
        } else if ("fs".equals(cp)) {
            "v16214771"
        } else if ("rr".equals(cp)) {
            "http://sh.file.myqcloud.com/files/v2/1252816746/rrsp/2017/10/12/7cef4a7941184f8ea208a47b064ae07e.mp4"
        } else if ("yl".equals(cp)) {
            "qbjQzrp7X5Az"
        } else if ("yk".equals(cp)) {
            "https://api.youku.com/videos/player/file?data=WcEl1o6uUdTRNRGMyTURBNE1BPT18MnwyfDI1MjU5fDIO0O0O"
        } else if ("wb".equals(cp)) {
            "http://f.us.sinaimg.cn/000RtnXqlx07jehw2lDq01040200eYte0k010.mp4?label=hevc_mp4_hd&template=852x480.32&Expires=1522752887&ssig=LnlNAV7ZjX&KID=unistore,video"
        }
        else {
            ""
        }
    }

    private fun getTestContentByCp(cp: String): Content {
        var content = Content()
        return if ("cy".equals(cp)) {
            content.setIMEI("13290wehgoiho2i2982332902")
            content
        } else if ("fh".equals(cp)) {
            content.versionName = "v1.3.2"
            content.imei = "13290wehgoiho2i2982332902"
            content.videoDuration = "406"
            content.setVid("7295d732-0d41-419d-9529-67f8a8d499ab")
            content
        } else if ("fs".equals(cp)) {
            content.authorId = "c1016"
            content
        } else if ("wb".equals(cp)) {
            content.urlExt = "{\"sid\":\"1034:90f87d9248c4e2542c5045e9d29b94f8\"}"
            content
        }
        else {
            content
        }
    }
}