package com.example.hookreplay;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import java.util.ArrayList;

public class MyBroadcastReceiver extends BroadcastReceiver {
    public static final String SENDER_NAME = "com.example.myapplication";
    public static ArrayList<String> actionList = new ArrayList<>();

    private static MyBroadcastReceiver myBroadcastReceiver = null;
    public static MyBroadcastReceiver getInstance(){
        if(myBroadcastReceiver == null){
            myBroadcastReceiver = new MyBroadcastReceiver();
        }
        return myBroadcastReceiver;
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
        String action = bundle.getString("action");
        System.out.println("MyReceiver: " + action);
        actionList.add(action);
    }
}
