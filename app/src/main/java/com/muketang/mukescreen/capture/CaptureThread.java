package com.muketang.mukescreen.capture;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import org.webrtc.CapturerObserver;
import org.webrtc.JavaI420Buffer;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoFrame;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class CaptureThread implements  Runnable {

    private  CapturerObserver observer;


    private  long camerafpsTime;
    private int camreaFps;

    private  int captureFps;
    private  long captureFpsTime;

    private  int width;
    private  int height;
    private  int fps;

    private  boolean stopped;

    private  Thread thread;

    private  int sleepMs;


    private  byte[] frameData ;
    private  ByteBuffer frameBuffer;

    private SurfaceViewRenderer localpreview;

    public  CaptureThread(SurfaceViewRenderer localpreview, int width, int height, int fps)
    {
        this.width = width;
        this.height = height;
        this.fps = fps;
        sleepMs = 1000/ fps;

        this.localpreview = localpreview;

        frameData = new byte[width * height * 3/2];
        frameBuffer =ByteBuffer.allocateDirect(frameData.length);
    }

    public  void  start(CapturerObserver observer)
    {
        this.observer = observer;
        thread = new Thread(this);
        thread.setDaemon(true);
        thread.start();
    }



    public  void  stop(){
        stopped=true;
    }


    public  synchronized void onFrame(ByteBuffer i420Buffer)
    {
        i420Buffer.get(frameData, 0, frameData.length);
        long current = System.currentTimeMillis();
        camreaFps++;
        if(current - camerafpsTime >= 1000)
        {
            System.out.println("camera fps：" + camreaFps);
            camreaFps = 0;
            camerafpsTime = current;
        }
    }

    private synchronized  void dispatchCapturedFrame()
    {
        long current = System.currentTimeMillis();
        frameBuffer.clear();
        frameBuffer.put(frameData);
        int chromaHeight = (height + 1) / 2;
        int strideY = width;
        int strideUV = (width + 1) / 2;
        int yPos = 0;
        int uPos = yPos + width * height;
        int vPos = uPos + strideUV * chromaHeight;
        ByteBuffer buffer = frameBuffer;
        buffer.position(yPos);
        buffer.limit(uPos);
        ByteBuffer dataY = buffer.slice();

        buffer.position(uPos);
        buffer.limit(vPos);
        ByteBuffer dataU = buffer.slice();

        buffer.position(vPos);
        buffer.limit(vPos + strideUV * chromaHeight);
        ByteBuffer dataV = buffer.slice();
        JavaI420Buffer i420Buffer = JavaI420Buffer.wrap(this.width, this.height, dataY, strideY, dataU, strideUV, dataV, strideUV,null );
        long timeNs = TimeUnit.MILLISECONDS.toNanos(SystemClock.elapsedRealtime()-100);
        VideoFrame videoFrame = new VideoFrame(i420Buffer,0,timeNs);
        captureFps++;
        if(current - captureFpsTime >= 1000)
        {
            System.out.println("capture fps:" + captureFps);
            captureFps = 0;
            captureFpsTime = current;
        }
        observer.onFrameCaptured(videoFrame);
     //   localpreview.onFrame(videoFrame);
    }

    @Override
    public void run() {
        while (!stopped){
            dispatchCapturedFrame();
            try
            {
                Thread.sleep(sleepMs);
            }catch (Exception e){}
        }
    }


}
