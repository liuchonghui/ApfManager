package tools.android.apfmanager;

public enum TaskPayloadState {

    INIT_INVALID_ID(10),
    INIT_INVALID_TO(11),
    PROCESS_FAILURE(20),
    HOST_CALLBACK_SUCCESS(30),
    HOST_CALLBACK_FAILURE(31),
    HOST_CALLBACK_CLEARALL(32),
    HOST_CALLBACK_CHANGETO_ND(40),
    HOST_CALLBACK_CHANGETO_LD(41),
    HOST_CALLBACK_CHANGETO_HD(42),
    HOST_CALLBACK_CHANGETO_SD(43),
    TARGET_PLUGIN_NOT_SUPPORT(-5),
    TARGET_PLUGIN_NOT_FOUND(-4),
    NETWORK_REQUEST_ERR(-3),
    NETWORK_INVALID(-2),
    NULL(-1),
    SUCCESS(1);

    public int code;

    TaskPayloadState(int code) {
        this.code = code;
    }

    public int code() {
        return this.code;
    }

    public static TaskPayloadState parse(int code) {
        for (TaskPayloadState s : TaskPayloadState.values()) {
            if (code == s.code) {
               return s;
            }
        }
        return null;
    }
}
