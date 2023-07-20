package com.muketang.mukescreen.capture;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.usb.UsbDevice;
import android.os.Handler;
import android.os.SystemClock;
import android.view.Surface;
import android.view.TextureView;

import com.serenegiant.usb.IFrameCallback;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.UVCCamera;

import org.webrtc.CapturerObserver;
import org.webrtc.EglBase;
import org.webrtc.JavaI420Buffer;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoFrame;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

public class UsbCapture implements VideoCapturer,USBMonitor.OnDeviceConnectListener, IFrameCallback {

    private  Context context;
    private CapturerObserver capturerObserver;
    private  USBMonitor monitor;
    private  UsbDevice device;
    private UVCCamera uvcCamera;

    private  USBMonitor.UsbControlBlock ctrlBlock;

    private  boolean captureStarted;

    private  int width;
    private  int height;
    private  int fps;


    private  TextureView view;

    private  CaptureThread captureThred;
    private  boolean disposed;

    private SurfaceViewRenderer localpreview;


    public  UsbCapture(USBMonitor monitor, UsbDevice device, TextureView view, SurfaceViewRenderer localpreview)
    {
        this.monitor = monitor;
        this.device = device;
        this.view = view;
        captureStarted=false;
        monitor.requestPermission(device);
        this.localpreview = localpreview;
    }

    @Override
    public void initialize(SurfaceTextureHelper surfaceTextureHelper, Context context, CapturerObserver capturerObserver) {
        this.context = context;
        this.capturerObserver = capturerObserver;

    }

    @Override
    public void startCapture(int i, int i1, int i2) {
        this.width = i;
        this.height = i1;
        this.fps = i2;
        captureStarted = true;
        capturerObserver.onCapturerStarted(true);
        captureThred  = new CaptureThread(localpreview,this.width, this.height, this.fps);
        captureThred.start(capturerObserver);
    }

    @Override
    public void stopCapture() throws InterruptedException {
        captureStarted = false;
        if(capturerObserver != null)
        {
            capturerObserver.onCapturerStopped();
        }
        if(captureThred != null)
        {
            captureThred.stop();
        }
        captureThred=null;
    }

    @Override
    public void changeCaptureFormat(int i, int i1, int i2) {
        this.width = i;
        this.height = i1;
        this.fps = i2;
    }

    @Override
    public void dispose() {
         try{
             stopCapture();
         }catch (Exception ex){}
        disposed=true;
    }

    @Override
    public boolean isScreencast() {
        return true;
    }

    @Override
    public void onAttach(UsbDevice device) {

    }

    @Override
    public void onDettach(UsbDevice device) {

    }

    @Override
    public void onConnect(UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock, boolean createNew) {
        this.ctrlBlock = ctrlBlock;
        startCaptureIfValid();
    }

    @Override
    public void onDisconnect(UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock) {

    }

    @Override
    public void onCancel(UsbDevice device) {

    }


    private  void startCaptureIfValid()
    {
        SurfaceTexture surfaceTexture = view.getSurfaceTexture();
        if(surfaceTexture == null || !captureStarted)
        {
            delay(1000,()->{startCaptureIfValid();});
            return;
        }
        startCapture();
    }

    private  void startCapture()
    {
        uvcCamera = new UVCCamera();
        try {
            uvcCamera.open(ctrlBlock);
            uvcCamera.setFrameCallback(this, UVCCamera.PIXEL_FORMAT_RAW);
            uvcCamera.setPreviewSize(this.width, this.height);
            SurfaceTexture surfaceTexture = view.getSurfaceTexture();
            Surface mPreviewSurface = new Surface(surfaceTexture);
            uvcCamera.setPreviewDisplay(mPreviewSurface);
            uvcCamera.startPreview(false);
        } catch (final IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

//
//    private  void doCaptureInThread()
//    {
//        while (!disposed)
//        {
//            if(captureStarted)
//            {
//                JavaI420Buffer i420Buffer = JavaI420Buffer.allocate(this.width, this.height);
//                long timeNs = TimeUnit.MILLISECONDS.toNanos(SystemClock.elapsedRealtime());
//                VideoFrame videoFrame = new VideoFrame(i420Buffer,0,timeNs);
//                dispatchFrame(videoFrame);
//                i420Buffer.release();
//            }
//            try {
//                Thread.sleep(30);
//            } catch (InterruptedException e) {
//
//            }
//        }
//    }


    @Override
    public void onFrame(ByteBuffer frame) {
        if(!captureStarted){
            return;
        }
        if(captureThred != null)
        {
            captureThred.onFrame(frame);
        }
       // localpreview.onFrame(videoFrame);
    }




    private void delay(int ms, Runnable r) {
        new Handler().postDelayed(() -> r.run(), ms);
    }
}
