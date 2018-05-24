package tool.apfmanager.app2;

import android.app.Activity;
import android.compact.utils.FileCompactUtil;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.limpoxe.fairy.content.PluginDescriptor;
import com.limpoxe.fairy.manager.PluginManagerHelper;

import java.io.File;

import tools.android.apfmanager.PluginUtil;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initData();
    }

    protected void initData() {
        PluginUtil.Companion.checkAndInstallPlugins(getApplicationContext(), false);
    }

    protected void initView() {
        final CheckBox cp_plugin_switcher = (CheckBox) findViewById(R.id.cp_plugin_switcher);
        final ViewGroup cp_plugin_panel = (ViewGroup) findViewById(R.id.cp_plugin_panel);
        cp_plugin_switcher.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean checked) {
                if (checked) {
                    for (PluginDescriptor pd : PluginManagerHelper.getPlugins()) {
                        String pluginPackageName = pd.getPackageName();
                        boolean pluginEnable = pd.isEnabled();
                        boolean pluginStandAlone = pd.isStandalone();
                        String pluginVersion = pd.getVersion();
                        String pluginPath = pd.getInstalledPath();
                        String pluginMd5 = FileCompactUtil.getMD5(new File(pluginPath));
                        String desc = pluginPackageName + "|version|" + pluginVersion + "|enable|" + pluginEnable + "|standalone|" + pluginStandAlone + "|path|" + pluginPath + "|md5|" + pluginMd5;
                        final TextView text = new TextView(cp_plugin_switcher.getContext());
                        text.setText(desc);
                        text.setTag(pluginPackageName);
                        text.setOnLongClickListener(new View.OnLongClickListener() {
                            @Override
                            public boolean onLongClick(View v) {
                                String tag = (String) v.getTag();
                                int code = PluginManagerHelper.remove(tag);
                                Toast.makeText(v.getContext(), PluginUtil.Companion.getPluginErrMsg(v.getContext().getApplicationContext(), code), Toast.LENGTH_LONG).show();
                                cp_plugin_panel.removeView((View) text.getParent());
                                cp_plugin_panel.invalidate();
                                return true;
                            }
                        });

                        final Button btn = new Button(cp_plugin_switcher.getContext());
                        btn.setText("测试");
                        btn.setTag(pluginVersion.substring(0, pluginVersion.indexOf("_")));
                        btn.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                String tag = (String) v.getTag();
                                TestSuit.Companion.get().testAndToast(v.getContext(), tag);
                            }
                        });

                        LinearLayout cont = new LinearLayout(cp_plugin_switcher.getContext());
                        cont.setOrientation(LinearLayout.HORIZONTAL);
                        cont.setGravity(Gravity.CENTER_VERTICAL);
                        LinearLayout.LayoutParams textPl = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                        cont.addView(text, textPl);
                        LinearLayout.LayoutParams btnPl = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.33f);
                        cont.addView(btn, btnPl);
                        cp_plugin_panel.addView(cont);
                    }
                    cp_plugin_panel.invalidate();
                } else {
                    cp_plugin_panel.removeAllViews();
                    cp_plugin_panel.invalidate();
                }
            }
        });
    }
}
