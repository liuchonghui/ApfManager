package tool.apfmanager.app2;

import android.app.Application;
import android.content.Context;

import tools.android.apfmanager.PluginUtil;

public class MainApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        if (PluginUtil.Companion.isPluginProcess(this)) {
            return;
        }
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        PluginUtil.Companion.applicationAttachBaseContext(this, base);
    }

    @Override
    public Context getBaseContext() {
        return PluginUtil.Companion.applicationGetBaseContext(this, super.getBaseContext());
    }
}
