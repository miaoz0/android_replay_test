package com.example.hookreplay;

import android.content.*;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;


public class MainActivity extends AppCompatActivity {
    Button bt1,bt2,bt3;
    TextView tv1;
    static String TARGET_APP_NAME = "com.example.myapplication";
    static String HOOP_APP_NAME = "com.example.hookreplay";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 获取对象
        bt1 = (Button) this.findViewById(R.id.button1);
        bt2 = (Button) this.findViewById(R.id.button2);
        bt3 = (Button) this.findViewById(R.id.button3);
        tv1 = (TextView) this.findViewById(R.id.tv1);

        // 注册事件
        bt1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tv1.setText("开始录制");
                jumpApp();
            }
        });

        bt2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tv1.setText("清空录制");

                MyBroadcastReceiver.actionList.clear();
            }
        });

        bt3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tv1.setText("回放");

                jumpApp();
                performScript();

                MyBroadcastReceiver.actionList.clear();
            }
        });

        // 动态注册广播接收器
        BroadcastReceiver myReceiver = MyBroadcastReceiver.getInstance();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(MyBroadcastReceiver.SENDER_NAME);
        registerReceiver(myReceiver, intentFilter);

    }

    public void jumpApp(){
        String packageName = HookTest.TARGET_APP_NAME;
        String activityName = HookTest.TARGET_APP_NAME +".MainActivity";
        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ComponentName componentName = new ComponentName(packageName, activityName);
        intent.setComponent(componentName);
        startActivity(intent);
    }

    public void performScript(){
        // 广播 在该app中存储的动作列表
        Intent intent = new Intent(HOOP_APP_NAME);
        intent.putExtra("actionArray", MyBroadcastReceiver.actionList.toArray(new String[0]));
        this.sendBroadcast(intent);
    }
}