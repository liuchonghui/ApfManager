package tools.android.apfmanager;

import java.io.Serializable;

public interface PluginAction extends Serializable {
    String NAME = "action";
    String ACTION_GET_IDENTIFY = "getIdentify";
    String ACTION_SET_TASK_PAYLOAD = "setTaskPayload";
}
