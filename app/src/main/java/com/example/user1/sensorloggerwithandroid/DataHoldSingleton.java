package com.example.user1.sensorloggerwithandroid;

import android.app.Application;
import android.util.Log;

import org.json.JSONArray;

/**
 * Created by user1 on 2017/06/29.
 */

public class DataHoldSingleton extends Application {

    private static DataHoldSingleton sInstance = null;   //唯一のインスタンスを保持する

    public JSONArray report = new JSONArray();

    @Override
    public void onCreate() {
        super.onCreate();

        sInstance = this;
    }

    //シングルトンパターンでは、インスタンスはこのクラスメソッドを介して行う
    public static DataHoldSingleton getInstance() {
        if (sInstance == null) {
            Log.e("FusedLocationClient", "Singleton instance is not generated.");
        }
        return sInstance;
    }



}
