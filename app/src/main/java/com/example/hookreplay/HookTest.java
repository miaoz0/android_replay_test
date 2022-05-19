package com.example.hookreplay;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.widget.*;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;

public class HookTest implements IXposedHookLoadPackage {

    static final String TARGET_APP_NAME = "com.example.myapplication";
    static final String HOOP_APP_NAME = "com.example.hookreplay";

    static final String EDIT_TEXT = "edit_text";
    static final String TEXT_VIEW = "text_view";
    static final String BUTTON = "button";

    public void makeActionLog(View v){
        makeActionLog(v,"");
    }
    public void makeActionLog(View v, String extra){
        long curTime = System.currentTimeMillis();
        String action = "";
        if(v instanceof EditText){
            action = v.getId() + " " +  EDIT_TEXT + " " +  Long.toString(curTime) + " " +  extra;
        }else if(v instanceof Button){
            action = v.getId() + " " + BUTTON + " " + Long.toString(curTime);
        }else if(v instanceof TextView){
            action = v.getId() + " " + TEXT_VIEW + " " + Long.toString(curTime);
        }
        XposedBridge.log("make log...: " + action);

        // 发送广播
        Intent intent = new Intent(TARGET_APP_NAME);
        intent.putExtra("action", action);
        v.getContext().sendBroadcast(intent);
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if(lpparam.packageName.equals(TARGET_APP_NAME)){
            XposedBridge.log("Hook succeeded");
            XposedBridge.log("Loaded app: "+ lpparam.packageName);
            XposedHelpers.findAndHookMethod(
            TARGET_APP_NAME + ".MainActivity",
            lpparam.classLoader, "onCreate", Bundle.class,
            new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Class<?> c = lpparam.classLoader.loadClass(TARGET_APP_NAME + ".MainActivity");

                    Field field1 = c.getDeclaredField("et11");
                    Field field2 = c.getDeclaredField("tv11");
                    Field field3 = c.getDeclaredField("bt11");
                    Field field4 = c.getDeclaredField("bt12");
                    Field field5 = c.getDeclaredField("rg11");
                    field1.setAccessible(true);
                    field2.setAccessible(true);
                    field3.setAccessible(true);
                    field4.setAccessible(true);
                    field5.setAccessible(true);
                    EditText et11 = (EditText) field1.get(param.thisObject);
                    TextView tv11 = (TextView) field2.get(param.thisObject);
                    Button bt11 = (Button) field3.get(param.thisObject);
                    Button bt12 = (Button) field4.get(param.thisObject);
                    RadioGroup rg11 = (RadioGroup) field5.get(param.thisObject);

                    et11.addTextChangedListener(new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                        @Override
                        public void onTextChanged(CharSequence s, int start, int before, int count) {}
                        @Override
                        public void afterTextChanged(Editable s) {
                            String ss = s.toString();
                            XposedBridge.log("id: "+ et11.getId() + " text after change...");
                            // action log
                            makeActionLog(et11, ss);
                        }
                    });

                    // et11, tv11, bt11, bt12
                    ArrayList<View> onClickList = new ArrayList<>(Arrays.asList(et11, tv11, bt11, bt12));
                    for(View v: onClickList){
                        if(v.hasOnClickListeners()){
                            Method method = View.class.getDeclaredMethod("getListenerInfo");
                            method.setAccessible(true);
                            Object mListenerInfo = method.invoke(v);

                            Class<?> listenerInfoClz = Class.forName("android.view.View$ListenerInfo");
                            Field field = listenerInfoClz.getDeclaredField("mOnClickListener");
                            final View.OnClickListener onClickListenerInstance = (View.OnClickListener) field.get(mListenerInfo);
                            View.OnClickListener proxyOnClickListener = v1 -> {
                                XposedBridge.log("id1: "+ v1.getId());
                                onClickListenerInstance.onClick(v1);

                                // action log
                                makeActionLog(v1);
                            };
                            // 设置代理类
                            field.set(mListenerInfo, proxyOnClickListener);
                        }else{
                            //如果没有click listener，添加 onClickListeners
                            v.setOnClickListener(v1->{
                                XposedBridge.log("id2: " + v1.getId());

                                // action log
                                makeActionLog(v1);
                            });
                        }
                    }
                    // rg11
                    Class<?> radioGroupClz = Class.forName("android.widget.RadioGroup");
                    Field field = radioGroupClz.getDeclaredField("mOnCheckedChangeListener");
                    field.setAccessible(true);
                    RadioGroup.OnCheckedChangeListener onCheckedChangeListenerInstance = (RadioGroup.OnCheckedChangeListener) field.get(rg11);
                    RadioGroup.OnCheckedChangeListener proxyOnCheckedChangeListener = (group, checkedId) -> {
                        XposedBridge.log("id3: " + checkedId);
                        onCheckedChangeListenerInstance.onCheckedChanged(group, checkedId);

                        // action log
                        View v = ((Activity)param.thisObject).findViewById(checkedId);
                        makeActionLog(v);
                    };
                    field.set(rg11, proxyOnCheckedChangeListener);


                    // add a broadcast receiver to target app
                    class TargetReceiver extends BroadcastReceiver{
                        public static final String SENDER_NAME = "com.example.hookreplay";
                        public static final long ACTION_DELAY = 1000;
                        private void performTouch(View v){
                            long downTime = SystemClock.uptimeMillis();
                            MotionEvent downEvent = MotionEvent.obtain(downTime, downTime+200, MotionEvent.ACTION_DOWN, 0, 0 , 0);
                            downTime += 600;
                            MotionEvent upEvent = MotionEvent.obtain(downTime, downTime+100, MotionEvent.ACTION_UP, 0, 0 , 0);
                            v.onTouchEvent(downEvent);
                            v.onTouchEvent(upEvent);
                            upEvent.recycle();
                            downEvent.recycle();
                        }
                        @Override
                        public void onReceive(Context context, Intent intent) {
                            if(!SENDER_NAME.equals(intent.getAction())){
                                return;
                            }
                            Bundle bundle = intent.getExtras();
                            if(bundle == null){
                                return;
                            }
                            String[] actionArray = bundle.getStringArray("actionArray");

                            if(actionArray.length == 0){
                                Toast.makeText((Activity)param.thisObject,"请先录制",Toast.LENGTH_SHORT).show();
                                return;
                            }
                            // 遍历action列表，每隔ACTION_DELAY执行一次动作
                            Toast.makeText((Activity)param.thisObject,"开始播放",Toast.LENGTH_SHORT).show();
                            final Handler handler = new Handler(Looper.getMainLooper());
                            long startTime = Long.parseLong(actionArray[0].split(" ")[2]);
                            long endTime = Long.parseLong(actionArray[actionArray.length-1].split(" ")[2]);
                            for(String action: actionArray) {
                                System.out.println("perform script:  "+ action);
                                String[] e = action.split(" ");
                                if (e.length == 0) {
                                    continue;
                                }
                                int id = Integer.parseInt(e[0]);
                                String compName = e[1];
                                long curTime = Long.parseLong(e[2]) + ACTION_DELAY;  // 所有动作延后ACTION_DELAY

                                View v = ((Activity)context).findViewById(id);
                                handler.postDelayed(() -> {
                                    System.out.println("execute action: " + action);
                                    switch (compName) {
                                        case HookTest.BUTTON:
                                            performTouch(v);
                                            break;
                                        case HookTest.TEXT_VIEW:
                                            performTouch(v);
                                            break;
                                        case HookTest.EDIT_TEXT:
                                            if(e.length >= 4){
                                                String text = e[3];
                                                ((EditText) v).setText(text);
                                            }else{
                                                performTouch(v);
                                            }
                                            break;
                                        default:
                                            break;
                                    }
                                }, (curTime - startTime));
                            }

                            handler.postDelayed(()->{
                                Toast.makeText((Activity)param.thisObject,"回放结束",Toast.LENGTH_SHORT).show();
                            }, (endTime-startTime) + ACTION_DELAY*2);
                        }
                    }
                    // 注册receiver
                    BroadcastReceiver bcr = new TargetReceiver();
                    IntentFilter intentFilter = new IntentFilter();
                    intentFilter.addAction(TargetReceiver.SENDER_NAME);
                    ((Activity)param.thisObject).registerReceiver(bcr,intentFilter);
                }
            });

        }
    }
}
