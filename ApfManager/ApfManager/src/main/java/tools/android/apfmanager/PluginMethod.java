package tools.android.apfmanager;

import java.io.Serializable;

public interface PluginMethod extends Serializable {
    String METHOD_GET_PLUGIN_INFOS = "getPluginInfos";
    String METHOD_SEND_MESSAGES_TO_HOST = "sendMessagesToHost";
}
