package com.muketang.mukescreen;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;


import org.webrtc.EglBase;
import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoFrame;

import java.util.ArrayList;
import java.util.List;

public class RemoteScreenController implements IRemoteScreenController, IDeskStatusChangeListener, IDeskFrameListener {
    private  static  IRemoteScreenController instance;
    public static final int RTC_PORT = 8888;
    public static final int TOUCH_PORT = 8312;
    public static final int DATA_PORT = 9527;
    public static final int RESTART_PORT = 12346;
    public  static final String  TAG="aiedu";

    private  DeskStatus status;
    private PeerClient peerClient;

    private EglBase eglBase;


    private  String serverIp;

    private  Context context;

    private  boolean firstFrameGot;
    private  volatile  int aliveFrameCount;


    private List<IDeskStatusChangeListener> statusChangeListeners = new ArrayList<IDeskStatusChangeListener>();
    private List<IDeskFrameListener> deskFrameListeners = new ArrayList<IDeskFrameListener>();

    private Handler handler ;

    private  SurfaceViewRenderer videoRenderer;

    private  CheckAliveThread checkVideoAlive;

    private int videoWidth;
    private int fps;
    private long fpsStart;

    public  static  IRemoteScreenController getInstance()
    {
        if(instance == null)
        {
            instance = new RemoteScreenController();
        }
        return  instance;
    }

    public  void  init()
    {
        handler = new Handler();
        firstFrameGot=false;
        status = DeskStatus.ICE_DISCONNECTED;
    }

    @Override
    public void connect(Context context, String serverIp) {
        this.serverIp = serverIp;
        this.context = context;
        if(peerClient != null)
        {
            peerClient.close();
        }
        firstFrameGot = false;
        createPeerClient();
    }

    @Override
    public void close() {
        if(peerClient != null)
        {
            peerClient.close();
        }
        peerClient = null;
        if(eglBase != null)
        {
            eglBase.release();
        }
        eglBase=null;
        if(statusChangeListeners != null)
        {
            statusChangeListeners.clear();
        }
        if(deskFrameListeners != null)
        {
            deskFrameListeners.clear();
        }
        if(checkVideoAlive != null)
        {
            checkVideoAlive.stop();
            checkVideoAlive = null;
        }
        videoRenderer= null;
    }

    @Override
    public DeskStatus getDeskStatus() {
        return status;
    }

    @Override
    public void addDeskStatusChangeListener(IDeskStatusChangeListener listener) {
        statusChangeListeners.add(listener);
    }

    @Override
    public void removeDeskStatusChangeListener(IDeskStatusChangeListener listener) {
        statusChangeListeners.remove(listener);
    }

    @Override
    public void addDeskFrameListener(IDeskFrameListener listener) {
        deskFrameListeners.add(listener);
    }

    @Override
    public void removeDeskFrameListener(IDeskFrameListener listener) {
        deskFrameListeners.remove(listener);
    }

    @Override
    public void onDeskStatusChanged(DeskStatus status) {
        this.status = status;
        MessageController.getInstance().send(MessageController.LOG, "desk status：" + status);
        this.runOnUIThread(()->{
            for(IDeskStatusChangeListener listener : statusChangeListeners)
            {
                listener.onDeskStatusChanged(status);
            }
        });
//        if(status == DeskStatus.ICE_DISCONNECTED || status == DeskStatus.SIGN_FAILED)
//        {
//            Thread thread = new Thread(()->{
//                try {
//                    Thread.sleep(1000);
//                } catch (InterruptedException e) {
//
//                }
//                runOnUIThread(()->{
//                    createPeerClient();
//                });
//            });
//            thread.setDaemon(true);
//            thread.start();
//        }
    }

    @Override
    public void onFrame(VideoFrame frame) {
        if(!firstFrameGot)
        {
            firstFrameGot = true;
            videoWidth = frame.getRotatedWidth();
            onDeskStatusChanged(DeskStatus.VIDEO_RECEVIED);
        }
        if(checkVideoAlive == null)
        {
            checkVideoAlive = new CheckAliveThread(this, 2000);
            checkVideoAlive.start();
        }
        if(videoRenderer != null)
        {
            videoRenderer.onFrame(frame);
            aliveFrameCount ++;
        }
        for(IDeskFrameListener listener : deskFrameListeners)
        {
            listener.onFrame(frame);
        }

        long tick = System.currentTimeMillis();
        fps++;
        if(tick - fpsStart >= 1000)
        {
            System.out.println("-----------------fps: " + fps );
            fps = 0;
            fpsStart = tick;
        }
    }


    public  void config(SurfaceViewRenderer view)
    {
        if(videoRenderer != null)
        {
            return;
        }
        videoRenderer = view;
        view.init(eglBase.getEglBaseContext(), null);
        view.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
        view.setEnableHardwareScaler(true);
    }

    /**
     * 如果在指定时间内，没有视频过来，需要重建
     */
    private  void checkVideoAlive()
    {
        //超时没有收到视频，需要重建
        if(aliveFrameCount == 0 && firstFrameGot)
        {
            onDeskStatusChanged(DeskStatus.VIDEO_TIMEOUT);
        }
        aliveFrameCount = 0;
    }

    private  void  createPeerClient()
    {
        if(checkVideoAlive != null)
        {
            checkVideoAlive.stop();
            checkVideoAlive=null;
        }
        if(peerClient != null)
        {
            peerClient.close();
        }
        if(eglBase == null)
        {
            eglBase = EglBase.create();
        }
        peerClient = new PeerClient(context,eglBase, serverIp,RTC_PORT, this, this);
        peerClient.start();
    }



    private  void  onHearbeatConnected()
    {

    }


    private void runOnUIThread(Runnable r) {
        if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
            r.run();
        } else {
            handler.post(r);
        }

    }

    private  static  class  CheckAliveThread
    {
        private  RemoteScreenController owner;
        private  Thread thread;
        private  boolean isStopped;
        private  int ms;
        private  volatile boolean paused;

        public  CheckAliveThread(RemoteScreenController owner, int ms)
        {
            this.owner = owner;
            this.ms  = ms ;
        }

        public  void  start()
        {
            thread = new Thread(()->{checkAliveInThread();});
            thread.setDaemon(true);
            thread.start();
        }

        public  void  stop()
        {
            isStopped = true;
        }

        private  void  checkAliveInThread()
        {
            while (!isStopped)
            {
                try
                {
                    Thread.sleep(ms);
                    if(!isStopped)
                    {
                        owner.checkVideoAlive();
                    }
                }
                catch (Exception e)
                {
                }
            }
        }
    }
}
