package com.muketang.mukescreen;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.os.Bundle;
import android.os.Message;
import android.view.View;
import android.widget.TextView;

import org.webrtc.SurfaceViewRenderer;

public class MainActivity extends Activity implements IDeskStatusChangeListener {

    private SurfaceViewRenderer touchView;

    private TextView infoText;

    private  IRemoteScreenController screenController;

    private  boolean connecting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Utils.fullscreen(this);
        setContentView(R.layout.activity_main);
        touchView = this.findViewById(R.id.video_view);
        infoText = findViewById(R.id.infoText);
        MessageController.getInstance().init(this.getApplicationContext());

    }

    @Override
    protected void onResume()
    {
        super.onResume();
        connectScreen();
    }

    @Override
    protected void onDestroy()
    {
        RemoteScreenController.getInstance().close();
        super.onDestroy();
        dispose();
    }

    public void dispose() {
        touchView.clearImage();
        touchView.release();
        touchView = null;
    }

    private void connectScreen()
    {
        if(connecting)
        {
            return;
        }
        connecting=true;
        logLogin("连接中...");
        screenController = RemoteScreenController.getInstance();
        screenController.init();
        screenController.removeDeskStatusChangeListener(this);
        screenController.addDeskStatusChangeListener(this);
        screenController.connect(this.getApplicationContext(), AppModel.getInstance().getServerIp());

    }

    @Override
    public void onDeskStatusChanged(DeskStatus status) {
        logLogin("连接状态：" + status);
        if(status == DeskStatus.VIDEO_RECEVIED)
        {
            logLogin("");
            RemoteScreenController.getInstance().config(touchView);
            return;
        }
        if(status == DeskStatus.RESET || status == DeskStatus.SIGN_FAILED || status == DeskStatus.ICE_DISCONNECTED || status == DeskStatus.VIDEO_TIMEOUT)
        {
            connecting=false;
            infoText.postDelayed(()->{connectScreen();}, 1000);
        }
    }

    private  void  logLogin(String msg)
    {
       this.runOnUiThread(()->{
           infoText.setText(msg);
       });
    }

}