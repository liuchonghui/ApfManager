package tool.apfmanager.app;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Button btn1 = (Button) findViewById(R.id.btn1);
        final TextView btn1ret = (TextView) findViewById(R.id.btn1_ret);
        final Button btn2 = (Button) findViewById(R.id.btn2);
        final TextView btn2ret = (TextView) findViewById(R.id.btn2_ret);

        btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
            }
        });

        btn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
            }
        });
    }
}
