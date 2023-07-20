package com.muketang.mukescreen;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.hardware.usb.UsbDevice;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.view.TextureView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.muketang.mukescreen.capture.UsbCapture;
import com.muketang.mukescreen.sender.ISenderController;
import com.muketang.mukescreen.sender.SenderController;
import com.serenegiant.usb.DeviceFilter;
import com.serenegiant.usb.USBMonitor;

import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerationAndroid;
import org.webrtc.CameraEnumerator;
import org.webrtc.EglBase;
import org.webrtc.ScreenCapturerAndroid;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class SenderActivity extends Activity implements AdapterView.OnItemClickListener {

    private  ListView menuUI;

    private   ISenderController sender1Controller;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Utils.fullscreen(this);
        setContentView(R.layout.activity_sender);
        initView();
        initSender();
        MessageController.getInstance().init(this.getApplicationContext());
        onOrientation(true);
    }


    @Override
    public void onStart()
    {
        super.onStart();
        doCommand(AppModel.getInstance().getCamera());

    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        if(this.sender1Controller != null)
        {
            this.sender1Controller.close();
        }
        this.sender1Controller = null;
    }


    public   void showMenu(View view)
    {
        menuUI.setVisibility(View.VISIBLE);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        TextView textView = view.findViewById(R.id.mi_text);
        String name = textView.getText().toString();
        doCommand(name);
        menuUI.setVisibility(View.INVISIBLE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == 222 && resultCode == RESULT_OK){
            onScreenGranted(data);
        }
    }
    @Override
    public void onConfigurationChanged(Configuration newConfig){
        super.onConfigurationChanged(newConfig );
        this.onOrientation(newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE);
    }

    private  void  onOrientation(boolean landscape)
    {
        if(sender1Controller != null)
        {
            sender1Controller.changeOrientation(landscape);
        }
        ConstraintLayout cl =  findViewById(R.id.cl);
        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(cl);
        constraintSet.setDimensionRatio(R.id.localpreview,landscape? "16:9": "9:16");
        constraintSet.applyTo(cl);
    }

    private  void  initView()
    {
        MenuAdapter _barApdapter = new MenuAdapter(this);
        menuUI = findViewById(R.id.menu);
        menuUI.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        menuUI.setAdapter(_barApdapter);
        menuUI.setOnItemClickListener(this);
        menuUI.setVisibility(View.INVISIBLE);
    }


    private  void doCommand(String name)
    {
        VideoCapturer capturer = null;
        View surface = findViewById(R.id.localpreview);
        TextView infoText = findViewById(R.id.infoText);
        switch (name)
        {
            case "后置摄像头":
                AppModel.getInstance().setCamera(name);
                capturer = getVideoCapture(false, false);
                if(capturer == null)
                {
                    Toast.makeText(this, "没有摄像头", Toast.LENGTH_LONG).show();
                    break;
                }
                sender1Controller.changeCapture(capturer);
                surface.setVisibility(View.VISIBLE);
                infoText.setText("");
                break;
            case "前置摄像头":
                AppModel.getInstance().setCamera(name);
                capturer = getVideoCapture(true, false);
                if(capturer == null)
                {
                    Toast.makeText(this, "没有摄像头", Toast.LENGTH_LONG).show();
                    break;
                }
                sender1Controller.changeCapture(capturer);
                surface.setVisibility(View.VISIBLE);
                infoText.setText("");
                break;
            case "共享屏幕":
                AppModel.getInstance().setCamera(name);
                if(Build.VERSION.SDK_INT < 20)
                {
                    Toast.makeText(this, "共享不了屏幕", Toast.LENGTH_LONG).show();
                    break;
                }
                requestScreen();
                break;
            case "重连":

                break;
            case "退出":
                if(this.sender1Controller != null)
                {
                    sender1Controller.changeCapture(null);
                    this.sender1Controller.signout();
                }
                Utils.delay(300, ()->{
                    this.finish();
                });
                break;
        }
    }




    private  void initSender()
    {
        sender1Controller = SenderController.getInstance();
        SurfaceViewRenderer localpreview = findViewById(R.id.localpreview);
        sender1Controller.initSurface(localpreview);
    }


    private void requestScreen()
    {
        MediaProjectionManager manager = (MediaProjectionManager) this.getSystemService(MEDIA_PROJECTION_SERVICE);
        if(manager == null)
        {
            return;
        }
        Intent intent = manager.createScreenCaptureIntent();
        startActivityForResult(intent, 222);
    }



    private  void  onScreenGranted(Intent grantedData)
    {
        ScreenCapturerAndroid screenCapture = new ScreenCapturerAndroid(grantedData, new MediaProjection.Callback() {
            @Override
            public void onStop() {
                super.onStop();
            }
        });
        sender1Controller.changeCapture(screenCapture);
        this.moveTaskToBack(true);

        View surface = findViewById(R.id.localpreview);
        surface.setVisibility(View.INVISIBLE);
        TextView infoText = findViewById(R.id.infoText);
        infoText.setVisibility(View.VISIBLE);
        infoText.setText("共享屏幕中...");
    }


    private  VideoCapturer getVideoCapture(boolean frontCamera, boolean screenCast){
        if(screenCast){
            if(Build.VERSION.SDK_INT < 20)
            {
                return  null;
            }
           return  null;
        }
        return  createVideoCaptuer(this, frontCamera);
    }


    private  VideoCapturer createVideoCaptuer(Context context, boolean frontCamera)
    {
        CameraEnumerator enumerator = Camera2Enumerator.isSupported(context)
                ? new Camera2Enumerator(context)
                : new Camera1Enumerator();
        String[] names = enumerator.getDeviceNames();
        if(names == null || names.length <= 0)
        {
            return null;
        }
        for(String name : names)
        {
            if(enumerator.isFrontFacing(name) && frontCamera){
                return enumerator.createCapturer(name, null);
            }else   if(enumerator.isBackFacing(name) && !frontCamera){
                return enumerator.createCapturer(name, null);
            }
        }
        return  null;
    }


}
