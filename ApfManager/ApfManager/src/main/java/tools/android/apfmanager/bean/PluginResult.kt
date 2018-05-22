package com.kuaiest.video.data.models.jsondata.plugins

import java.util.*

class PluginResult {
    var data: CpPluginData = CpPluginData()
    var times: CommonTimes = CommonTimes()
}

class CpPluginData {
    var result: String = ""
    var cp_plugin  = ArrayList<Plugin>()
}

class CommonTimes {
    val created: Int = 0
    val updated: Int = 0
}

class Plugin {
    var _id: String = ""
    var md5: String = ""
    var cp: String = ""
    var url: String = ""
    var path: String = ""
    var hint: String = ""
    var toast: String = ""
}

class PluginInfo {
    var id: String = ""
    var pluginEnable: Boolean = false
    var pluginStandAlone: Boolean = false
    var pluginVersion: String = ""
    var pluginPath: String = ""
    var pluginMd5: String = ""
}