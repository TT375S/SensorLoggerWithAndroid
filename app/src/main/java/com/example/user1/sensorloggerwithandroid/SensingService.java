package com.example.user1.sensorloggerwithandroid;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Places;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by user1 on 2017/06/29.
 */

public class SensingService extends Service implements SensorEventListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener
{

    private long loggingStartedTime = 0;

    private class ReportDataSet{
        public double latitude;
        public double longitude;
        public double gpsSpeed;
        public double acceleration[] = new double[3];
        public double linearAcceleration[] = new double[3];
        public double gyroscope[] = new double[3];
        public double magneticField[] = new double[3];
        public double sensorVelocity[] = new double[3];
        public double sensorRotateVelocity[] = new double[3];

        private final static int initNum = -1;

        public ReportDataSet(){
            //値の初期化
            latitude = initNum; longitude = initNum; gpsSpeed = initNum;
            for(int i=0; i<3; i++){
                acceleration[i]=initNum;
                linearAcceleration[i]=initNum;
                gyroscope[i]=initNum;
                magneticField[i]=initNum;
                sensorVelocity[i]=initNum;
                sensorRotateVelocity[i]=initNum;
            }
        }

        @Override
        public String toString(){
            String ret = latitude + " "+ longitude +" "+ gpsSpeed;
            for(int i=0; i<3; i++) {
                ret += " ";
                ret += acceleration[i];
            }
            for(int i=0; i<3; i++) {
                ret += " ";
                ret += linearAcceleration[i];
            }
            for(int i=0; i<3; i++) {
                ret += " ";
                ret += gyroscope[i];
            }
            for(int i=0; i<3; i++) {
                ret += " ";
                ret += magneticField[i];
            }
            for(int i=0; i<3; i++) {
                ret += " ";
                ret += sensorVelocity[i];
            }
            for(int i=0; i<3; i++) {
                ret += " ";
                ret += sensorRotateVelocity[i];
            }
            return ret;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("SensingService", "STARTSERVICE!");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        writer.close();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private SensorManager sensorManager;
    private float sensorX;
    private float sensorY;
    private float sensorZ;

    private LocationManager locationManager;

    // LocationClient の代わりにGoogleApiClientを使います
    private GoogleApiClient mGoogleApiClient;
    private boolean mResolvingError = false;

    private FusedLocationProviderApi fusedLocationProviderApi;

    private LocationRequest locationRequest;
    private Location location;
    private long lastLocationTime = 0;

    private ReportDataSet reportDataSet = new ReportDataSet();
    FileOutputStream fileOutputstream = null;
    PrintWriter writer = null;

    @Override
    public void onCreate()  {
        Log.d("SensingService", "STARTSERVICE!");
        super.onCreate();

        // Get an instance of the SensorManager
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        // Listenerの登録
        Sensor accel = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION); //TYPE_LINEAR_ACCELERATIONは重力の影響を除いた加速度を得る
        Sensor magne = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);  //ドリフトを考慮したジャイロセンサー値を得る
        Sensor magneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, magne, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, magneticField, SensorManager.SENSOR_DELAY_FASTEST);

        locationStart();

        //ファイルオープン
        final DateFormat df = new SimpleDateFormat("yyyyMMdd-HHmmss");
        final Date date = new Date(System.currentTimeMillis());

        try {
            fileOutputstream = openFileOutput("senslog" + df.format(date)+".txt", Context.MODE_PRIVATE);
            writer = new PrintWriter(fileOutputstream);
        } catch (IOException e) {
            e.printStackTrace();
        }

        loggingStartedTime = System.currentTimeMillis();
    }

    private void locationStart(){
        Log.d("debug","locationStart()");

        // LocationRequest を生成して精度、インターバルを設定
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(100);
        locationRequest.setFastestInterval(16);

        fusedLocationProviderApi = LocationServices.FusedLocationApi;

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        startFusedLocation();
    }

    private void startFusedLocation(){
        Log.d("LocationActivity", "onStart");

        // Connect the client.
        if (!mResolvingError) {
            // Connect the client.
            mGoogleApiClient.connect();
        } else {

        }
    }

    private void stopFusedLocation(){
        // Disconnecting the client invalidates it.
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onConnected(Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationProviderApi.requestLocationUpdates(mGoogleApiClient, locationRequest, this);
    }

    //-----センサー値取得関係-----
    final private float k = 0.1f;   //値が大きいほどローパスフィルタの効きが強くなる

    private double rawAcceleration[] = new double[3];   //ハイパスフィルタを通した値

    long oldTime = 0;   //前回、センサの値が変更されたとき

    long oldTime_rotate = 0;    //前回更新時刻
    final private float k_rotate = 0.1f;    //回転センサーの値へのローパスフィルターの効きの強さ

    final private float k_magnetic = 0.1f;

    //このメソッドは、リスナーとして登録してあるどのセンサーの値が変化しても呼ばれる
    //ので中でどのセンサーか場合分けしなくてはならない
    @Override
    public void onSensorChanged(SensorEvent event) {
        //このメソッドが呼ばれた理由となる、値の変わったセンサのタイプを確かめる必要がある

        //重力の影響を除いた加速度センサの場合
        if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            //LPF
            for(int i=0; i<3; i++){
                reportDataSet.linearAcceleration[i] += (event.values[i] - reportDataSet.linearAcceleration[i]) * k;
            }

            // High Pass Filter
            for(int i=0; i<3; i++){
                rawAcceleration[i] = event.values[i] - reportDataSet.linearAcceleration[i];
            }

            if(oldTime == 0) oldTime = System.currentTimeMillis();
            long nowTime = System.currentTimeMillis();
            long interval = nowTime - oldTime;
            oldTime = nowTime;

            for(int i=0; i<3; i++){
                reportDataSet.sensorVelocity[i] += rawAcceleration[i] *interval / 10;// [cm/s] にする
            }
        }

        //回転センサーの場合
        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            if(oldTime_rotate == 0) oldTime_rotate = System.currentTimeMillis();
            long nowTime = System.currentTimeMillis();
            long interval = nowTime - oldTime_rotate;
            oldTime_rotate = nowTime;

            //LPF
            for(int i=0; i<3; i++){
                reportDataSet.gyroscope[i] += (event.values[i] - reportDataSet.gyroscope[i]) * k_rotate;
            }

            // High Pass Filter
            double hiPass[] = new double[3];
            for(int i=0; i<3; i++){
                hiPass[i] = event.values[i] - reportDataSet.gyroscope[i];
            }

            //向きの変化速度
            for(int i=0; i<3; i++){
                reportDataSet.sensorRotateVelocity[i] += hiPass[i] * interval / 10;
            }
        }

        if(event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD){
            //LPF
            for(int i=0; i<3; i++){
                reportDataSet.magneticField[i] += (event.values[i] - reportDataSet.magneticField[i]) * k_magnetic;
            }
        }

        saveReportDataSet();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onLocationChanged(Location location) {
        lastLocationTime = location.getTime() - lastLocationTime;

        //スピードや位置などデータ更新
        reportDataSet.gpsSpeed = location.getSpeed();
        reportDataSet.latitude = location.getLatitude();
        reportDataSet.longitude = location.getLongitude();
        saveReportDataSet();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e("LocationClient", "ConnectionFailed!");
        if (mResolvingError) {
            // Already attempting to resolve an error.
            Log.d("", "Already attempting to resolve an error");

            return;
        } else if (connectionResult.hasResolution()) {

        } else {
            mResolvingError = true;
        }
    }

    private long lastSaveTime = 0;
    private final long savaInterval = 500;
    public void saveReportDataSet(){
        long now = System.currentTimeMillis();
        long interval = now - lastSaveTime;
        if(interval < savaInterval) return;
        lastSaveTime = now;

        double  duration = (now - loggingStartedTime)/1000;
        writer.append(duration +" "+ reportDataSet.toString() + "\n");
    }
}