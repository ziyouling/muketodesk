package com.muketang.mukescreen;

import android.Manifest;
import android.app.Activity;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class PermissionManager {

    private Activity _activity;
    private String[] _permissions;
    private static final int REQUESR_CODE=1;

    public PermissionManager(Activity activity) {
        _activity = activity;
        _permissions = new String[]{Manifest.permission.CAMERA,
                Manifest.permission.INTERNET, Manifest.permission.ACCESS_NETWORK_STATE};
    }

    public boolean getAllPermissionReady() {
        if (!checkPermission()) {
            askPermission();
            return false;
        }
        return true;
    }

    public boolean checkPermission() {
        for (int i = 0; i < _permissions.length; i++) {
           if(ContextCompat.checkSelfPermission(_activity, _permissions[i])!=0) {
               return false;
           }
        }
        return true;
    }


    public void askPermission() {
        ActivityCompat.requestPermissions(_activity,
                _permissions,REQUESR_CODE);
    }


}
