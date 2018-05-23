package tools.android.apfmanager;

import android.compact.impl.TaskPayload;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.google.gson.Gson;

import tools.android.apfmanager.bean.ResultBean;

public class HostProvider extends ContentProvider {
    protected String TAG = "HP";
    protected static Gson mGson = new Gson();
    protected static boolean enableLogcat = false;

    @Override
    public boolean onCreate() {
        if (mGson == null) {
            mGson = new Gson();
        }
        return true;
    }

    protected boolean isEnableLogcat() {
        return enableLogcat;
    }

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        enableLogcat = isEnableLogcat();

        Bundle ret = new Bundle();
        ResultBean result = handleAll(getContext(), method, arg, extras);
        if (result != null) {
            ret.putString("result_json", mGson.toJson(result));
        } else {
            ret = super.call(method, arg, extras);
        }
        return ret;
    }

    private final ResultBean handleAll(Context context, String method, String arg, Bundle extras) {
        String action = null;
        ResultBean resultBean = null;
        if (enableLogcat) {
            Log.d(TAG, getClass().getSimpleName() + "|handleAll|method|" + method + "|extras|" + extras.toString() + "|start|" + System.currentTimeMillis());
        }
        if (PluginMethod.METHOD_GET_PLUGIN_INFOS.equals(method)) {
            action = extras.getString(PluginAction.NAME);
            resultBean = getPluginInfosByAction(context, action, extras);
        } else if (PluginMethod.METHOD_SEND_MESSAGES_TO_HOST.equals(method)) {
            action = extras.getString(PluginAction.NAME);
            resultBean = getPluginMessages(context, action, extras);
        } else {
            // TODO: other methods
        }
        if (enableLogcat) {
            Log.d(TAG, getClass().getSimpleName() + "|handleAll|method|" + method + "|extras|" + extras.toString() + "|end|" + System.currentTimeMillis());
        }
        return resultBean;
    }

    private final ResultBean getPluginInfosByAction(Context context, String action, Bundle extras) {
        ResultBean resultBean = null;
        if (PluginAction.ACTION_GET_IDENTIFY.equals(action)) {
            resultBean = new ResultBean();
            resultBean.setIdentify(context.getPackageName());
            resultBean.setTimestamp(System.currentTimeMillis());
        } else {
            // TODO: other actions
        }
        return resultBean;
    }

    private final ResultBean getPluginMessages(Context context, String action, Bundle extras) {
        ResultBean resultBean = null;
        if (PluginAction.ACTION_SET_TASK_PAYLOAD.equals(action)) {
            String strJson = extras.getString("extra_json");
            TaskPayload payload = mGson.fromJson(strJson, TaskPayload.class);
            if (enableLogcat) {
                Log.d(TAG, "HOST|getPluginMessages|TASK_PAYLOAD|" + payload.identify + "|" + payload.ex + "|" + payload.state + "|" + payload.timestamp);
            }

            PluginManager.Companion.get().receiveFromPlugin(context, payload);

            resultBean = new ResultBean();
            resultBean.setIdentify(context.getPackageName());
            resultBean.setTimestamp(System.currentTimeMillis());
        } else {
            // TODO: other actions
        }
        return resultBean;
    }

    @Override
    public Cursor query(Uri uri, String[] strings, String s, String[] strings1, String s1) {
        return null;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        return null;
    }

    @Override
    public int delete(Uri uri, String s, String[] strings) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String s, String[] strings) {
        return 0;
    }
}