package com.muketang.mukescreen;

import android.content.Context;
import android.content.SharedPreferences;

public class AppModel {

    private  static AppModel instance;

    private  String deviceId;

   private String serverIp = "http://screen.muketang.com";

  //  private String serverIp = "http://192.168.2.12";

    private  String deskName;


    private  String camera="后置摄像头";


    public  static  AppModel getInstance()
    {
        if(instance == null)
        {
            instance = new AppModel();
        }
        return  instance;
    }

    public  void  init(Context context)
    {
        SharedPreferences prefs = context.getSharedPreferences("device_id", Context.MODE_PRIVATE);
        long devcideId =  prefs.getLong("device_id",0);
        if(devcideId == 0)
        {
            long newDeviceId = System.currentTimeMillis();
            SharedPreferences.Editor editor =  prefs.edit();
            editor.putLong("device_id", newDeviceId);
            editor.commit();
            devcideId = newDeviceId ;
        }
        this.deviceId = "a_"+devcideId;
    }

    public void setDeviceId(String id) {
        this.deviceId = id;
    }

    public String getDeviceId() {
        return deviceId;
    }
    public String getServerIp() {
        return serverIp;
    }

    public void setServerIp(String serverIp) {
        this.serverIp = serverIp;

    }

    public String getCamera() {
        return camera;
    }

    public void setCamera(String camera) {
        this.camera = camera;
    }
    public String getDeskName() {
        return deskName;
    }

    public void setDeskName(String remoteName) {
        this.deskName = remoteName;
    }


}
