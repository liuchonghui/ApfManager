package tools.android.apfmanager.bean;

import java.io.Serializable;
import java.util.HashMap;

public class Content extends HashMap<String, String> implements Serializable {
    private static final long serialVersionUID = 1L;

    public void setAuthorId(String authorId) {
        put("author_id", authorId);
    }

    public String getAuthorId() {
        return get("author_id");
    }

    public void setVersionName(String vername) {
        put("vername", vername);
    }

    public String getVersionName() {
        return get("vername");
    }

    public void setVersionCode(String vercode) {
        put("vercode", vercode);
    }

    public String getVersionCode() {
        return get("vercode");
    }

    public void setIMEI(String imei) {
        put("imei", imei);
    }

    public String getIMEI() {
        return get("imei");
    }

    public void setCry(boolean enCrypt) {
        // true then imei&did will be encrypt
        put("cry", enCrypt ? "t" : "f");
    }

    public boolean getCry() {
        return "t".equals(get("cry"));
    }

    public void setVideoDuration(String videoDuration) {
        put("vd", videoDuration);
    }

    public String getVideoDuration() {
        return get("vd");
    }

    public void setVid(String videoId) {
        put("vid", videoId);
    }

    public String getVid() {
        return get("vid");
    }

    public void setOid(String originalId) {
        put("oid", originalId);
    }

    public String getOid() {
        return get("oid");
    }

    public void setQuery(String value) {
        put("query", value);
    }

    public String getQuery() {
        return get("query");
    }

    public void setDid(String deviceId) {
        put("did", deviceId);
    }

    public String getDid() {
        return get("did");
    }

    public void setUid(String uid) {
        put("uid", uid);
    }

    public String getUid() {
        return get("uid");
    }

    public void setUtk(String utk) {
        put("utk", utk);
    }

    public String getUtk() {
        return get("utk");
    }

    public void setScreen(String screen) {
        put("screen", screen);
    }

    public String getScreen() {
        return get("screen");
    }

    public void setUrlExt(String urlExt) {
        put("url_ext", urlExt);
    }

    public String getUrlExt() {
        return get("url_ext");
    }
}
