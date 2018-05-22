package tools.android.apfmanager;

public enum PluginQuery {
    QUERY_ABILITY("query_abi"),
    QUERY_PLAY_URLS("query_urls");

    public String what;

    PluginQuery(String what) {
        this.what = what;
    }

    public static PluginQuery parse(String what) {
        for (PluginQuery pq : PluginQuery.values()) {
            if (pq.what.equals(what)) {
                return pq;
            }
        }
        return null;
    }
}
