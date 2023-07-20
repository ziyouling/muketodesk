package com.muketang.mukescreen.sender;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.view.TextureView;
import android.view.View;

import com.serenegiant.usb.USBMonitor;

import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;

import java.util.function.Predicate;

public interface ISenderController {

    void init(Context context, String name, int width, int height, int fps);


    void initSurface(SurfaceViewRenderer view);

    void reconnect();

    /**
     * 连接到server
     * @param serverIp 讲台ip
     */
    void connect(String serverIp, String deskName, ActionCallback<CameraLoginStatus> callback);

    void changeCapture(VideoCapturer capturer);

    void changeOrientation(boolean landscape);

    void signout();

    /**
     * 关闭和讲桌的连接
     */
    void close();
}
