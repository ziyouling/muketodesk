package com.muketang.mukescreen;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.muketang.mukescreen.sender.CameraLoginStatus;
import com.muketang.mukescreen.sender.ISenderController;
import com.muketang.mukescreen.sender.SenderController;

import org.webrtc.SurfaceViewRenderer;

import java.io.IOException;
import java.util.Map;


public class WifiHomeActivity extends Activity{


    private  boolean apOpened;

    private  DeskFinder deskFinder;

    private  final int CHECK_WIFI_TIMEOUT_IN_MS = 10000;

    private  long createTime;

    private PermissionManager _permissionManager;
    private  WifiManager wifi;

    private TextView wifiInput;
    private View progressBar;
    private  TextView progressText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        createTime = System.currentTimeMillis();
        showConnectDeskProgress(false);

        wifiInput =(TextView) findViewById(R.id.wifiInput);
        wifiInput.setText(getSavedDeskName());

        progressBar =findViewById(R.id.progressBar);
        progressText = findViewById(R.id.processInfoText);

        showConnectDeskProgress(false);

        _permissionManager = new PermissionManager(this);

        AppModel.getInstance().init(this.getApplicationContext());

        _permissionManager.getAllPermissionReady();
    }

    public   void  login(View view)
    {
        String ssid =wifiInput.getText().toString();
        if(ssid == null || ssid.length() <= 0)
        {
            return;
        }
        ssid = ssid.toUpperCase();
        //搜索局域网内的讲台，并连接。
        view.setVisibility(View.INVISIBLE);

        showConnectDeskProgress(true);
        saveDeskName(ssid);
        logConnectDeskInfo("登录中...");
        AppModel.getInstance().setDeskName(ssid);

        ISenderController senderController = SenderController.getInstance();

        String deviceId = AppModel.getInstance().getDeviceId();
        String name = "muke_camera_" + deviceId;
        senderController.init(this.getApplicationContext(), name, 1280,720,30);
        senderController.connect( AppModel.getInstance().getServerIp(), AppModel.getInstance().getDeskName(), p->onSignOK(p));
    }

    private  boolean connectDesked;

    private  void onSignOK(CameraLoginStatus status){
        String failedMsg = "";
        if(status == CameraLoginStatus.SIGN_IN){
            logConnectDeskInfo("登录成功...");
            checkLoginTimeout(10000);
            return;
        }else if(status == CameraLoginStatus.PREPARE_CONNECT_DESK) {
            connectDesked=true;
            logConnectDeskInfo("找到了导播台，准备连接...");
            delayGotoMain();
            return;
        }
        else
        {
            failedMsg = "登录失败，请保证网络畅通！";
        }
        logConnectDeskInfo(failedMsg);
        this.runOnUiThread(()->{
            TextView infoText = findViewById(R.id.wifiInput);
            infoText.postDelayed(()->{
                showConnectDeskProgress(false);
            }, 1000);
        });

    }

    private  void onLoginTimeout()
    {
        if(connectDesked)
        {
            return;
        }
        String failedMsg = "找不到导播台，导播是否启动?";
        logConnectDeskInfo(failedMsg);
        this.runOnUiThread(()->{
            TextView infoText = findViewById(R.id.wifiInput);
            infoText.postDelayed(()->{
                showConnectDeskProgress(false);
            }, 2000);
        });
    }

    private  void checkLoginTimeout(int timeoutMs)
    {
        this.runOnUiThread(()->{
            TextView infoText = findViewById(R.id.wifiInput);
            infoText.postDelayed(()->{
                onLoginTimeout();
            }, timeoutMs);
        });
    }


    private String getSavedDeskName()
    {
        SharedPreferences prefs = this.getApplicationContext().getSharedPreferences("muke_desk", Context.MODE_PRIVATE);
        return prefs.getString("name", "");
    }

    private  void saveDeskName(String name)
    {
        SharedPreferences prefs = this.getApplicationContext().getSharedPreferences("muke_desk", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor =  prefs.edit();
        editor.putString("name", name);
        editor.commit();
    }


    private  void delayFindDesk()
    {
        this.runOnUiThread(()->{
            TextView infoText = findViewById(R.id.wifiInput);
            infoText.postDelayed(()->{
               if(deskFinder != null)
               {
                   deskFinder.retry();
               }
            }, 2000);
        });
    }

    private void onDeskGot()
    {
        String ip = deskFinder.getDeskIp();
        if(ip == null || ip.length() <= 0)
        {
            logConnectDeskInfo("找不到导播台");
            delayFindDesk();
            return;
        }
        logConnectDeskInfo("找到了导播台：" + ip);
        AppModel.getInstance().setServerIp(ip);
        delayGotoMain();

    }


    private void logConnectDeskInfo(String text)
    {
        this.runOnUiThread(()->{
            TextView infoText = findViewById(R.id.processInfoText);
            infoText.setText(text);
        });
    }

    private  void showConnectDeskProgress(boolean visible)
    {
        this.runOnUiThread(()->{
            TextView infoText = findViewById(R.id.processInfoText);
            infoText.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
            ProgressBar bar = findViewById(R.id.progressBar);
            bar.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);

            findViewById(R.id.loginBt).setVisibility(!visible ? View.VISIBLE : View.INVISIBLE);
        });
    }

    private  void delayGotoMain()
    {
        this.runOnUiThread(()->{
            TextView infoText = findViewById(R.id.processInfoText);
            infoText.postDelayed(()->{
                Intent intent = new Intent(this, SenderActivity.class);
                startActivity(intent);
                this.finish();
            }, 1000);
        });
    }

}
