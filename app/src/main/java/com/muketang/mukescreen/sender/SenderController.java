package com.muketang.mukescreen.sender;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.usb.UsbDevice;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TimeUtils;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;

import androidx.annotation.Nullable;

import com.muketang.mukescreen.Utils;
import com.muketang.mukescreen.capture.UsbCapture;
import com.muketang.mukescreen.sdp.ISdpListener;
import com.muketang.mukescreen.sdp.SdpCommunication2;
import com.muketang.mukescreen.sdp.SignStatus;
import com.serenegiant.usb.IFrameCallback;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.UVCCamera;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.CapturerObserver;
import org.webrtc.DataChannel;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.GlRectDrawer;
import org.webrtc.HardwareVideoEncoderFactory;
import org.webrtc.IceCandidate;
import org.webrtc.JavaI420Buffer;
import org.webrtc.JniCommon;
import org.webrtc.Loggable;
import org.webrtc.Logging;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.PlatformSoftwareVideoDecoderFactory;
import org.webrtc.RtpReceiver;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.SoftwareVideoEncoderFactory;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoCodecInfo;
import org.webrtc.VideoDecoderFactory;
import org.webrtc.VideoFileRenderer;
import org.webrtc.VideoFrame;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

public class SenderController implements ISenderController, ISdpListener ,PeerConnection.Observer, Loggable, SdpObserver {

    private  static  final  String Tag = "MUKE_RTC";

    private  static  ISenderController instance;
    private static  boolean factoryInited;

    private Context context;
    private  SurfaceViewRenderer view;
    private  String name;

    private  String serverIp;
    private  String deskName;

    private PeerConnection rtcConn;
    private    PeerConnectionFactory factory;
    private EglBase eglBase;

    private String remoteId;
    private  String connectingRemoteId;
    private  String localId;

    private SdpCommunication2 sdpCommunication2;

    private CapturerObserver videoSourceObserver;

    private  int width;
    private  int height;
    private  int fps;
    private  boolean landscape;

    private  boolean iceConnected;

    private  boolean isClosed;

    private VideoCapturer captuer;
    private  VideoSource videoSource;
    private  SurfaceTextureHelper videoCapturerSurfaceTextureHelper;

    private  ActionCallback<CameraLoginStatus> signcallback;

    public  static  ISenderController getInstance()
    {
        if(instance == null)
        {
            instance = new SenderController();
        }
        return  instance;
    }

    @Override
    public void init(Context context, String name, int width, int height, int fps) {
        this.context = context;

        this.name = name;

        this.width = width;
        this.height = height;
        this.fps = fps;
        eglBase = EglBase.create(null, EglBase.CONFIG_PLAIN);
        videoCapturerSurfaceTextureHelper = SurfaceTextureHelper.create("VideoCapturerThread", eglBase.getEglBaseContext());

    }

    @Override
    public void initSurface(SurfaceViewRenderer view)
    {
        this.view = view;
        view.init(eglBase.getEglBaseContext(), null);
        if(this.connectingRemoteId != null)
        {
            this.connectToPeer(this.connectingRemoteId);
            connectingRemoteId=null;
        }
    }

    @Override
    public void connect(String serverIp, String deskName, ActionCallback<CameraLoginStatus> callback) {
//        if(isClosed){
//            return;
//        }
        this.signcallback = callback;
        this.deskName = deskName;
        this.serverIp = serverIp;
        Log.i(Tag, "connect to server:" + serverIp);
        if(sdpCommunication2 != null)
        {
            sdpCommunication2.removeSdpListener(this);
            sdpCommunication2.dispose();
        }
        localId=null;
        sdpCommunication2 = new SdpCommunication2(this.context);
        sdpCommunication2.addSdpListener(this);
        sdpCommunication2.sign(serverIp, this.name);
    }

    @Override
    public  void  reconnect()
    {
        this.connect(serverIp, this.deskName, this.signcallback);
    }

    @Override
    public void changeCapture(VideoCapturer capturer)
    {
        if(this.captuer !=  null)
        {
            try{
                this.captuer.stopCapture();
            }catch (Exception ex){}
            this.captuer.dispose();
        }
        this.captuer = capturer;
        if(videoSourceObserver == null)
        {
            return;
        }
        if(captuer == null)
        {
            return;
        }
        Utils.delay(1000, ()->{
            captuer.initialize(videoCapturerSurfaceTextureHelper, context, videoSourceObserver);
            captuer.startCapture(this.width, this.height, this.fps);
            videoSource.setIsScreencast(capturer.isScreencast());
        });
    }

    @Override
    public     void changeOrientation(boolean landscape)
    {
        this.landscape = landscape;
    }

    @Override
    public void signout()
    {
        isClosed=true;
        if(sdpCommunication2 != null)
        {
            sdpCommunication2.sigout();
        }
    }

    @Override
    public void close() {
        isClosed=true;
        if(sdpCommunication2 != null)
        {
           sdpCommunication2.sigout();
           sdpCommunication2.removeSdpListener(this);
           sdpCommunication2.dispose();
        }
        if(captuer != null)
        {
            try
            {
                captuer.stopCapture();
            }catch (Exception ex){}
            captuer.dispose();
        }
        captuer = null;
        if(eglBase != null)
        {
            eglBase.release();
        }
        eglBase = null;
        view = null;
        sdpCommunication2=null;
    }

    private void reConnect()
    {
        closePeerConnection();
        delay(100, ()->{ connect(this.serverIp, this.deskName, this.signcallback);});
    }

    @Override
    public void onSignStatus(SignStatus status) {
        if(status == SignStatus.SIGN_SUCCESS){
            this.localId = sdpCommunication2.getPeerId();
            sdpCommunication2.connectToDesk(this.name, this.deskName);
            if(signcallback != null)
            {
                signcallback.onAction(CameraLoginStatus.SIGN_IN);
            }
        }else{
            if(signcallback != null)
            {
                signcallback.onAction(CameraLoginStatus.SIGN_FIALED);
            }
        }
        Log.i(Tag, "on sign status:" + status + "  localid: " + this.localId);
        if(status == SignStatus.SIGN_OUT || status == SignStatus.SIGN_OUT_FAILED || status == SignStatus.SIGN_FAILED){
            reConnect();
        }

    }



    @Override
    public void onMsg(String msgStr) {

        JSONObject msg = null;
        try {
            msg = new JSONObject(msgStr);
        } catch (JSONException e) {
           // e.printStackTrace();
        }
        if(msg == null)
        {
            return;
        }
        JSONObject resultJson = msg.optJSONObject("result");
        if(resultJson == null)
        {
            return;
        }
        //handle sdp msg;
        String type = resultJson.optString("type", "");
        //收到了客户端连接请求
        if(type.equals("connect")){
            String from = resultJson.optString("from", "");
            if(from.length() != 0)
            {
                closePeerConnection();
                if(signcallback != null)
                {
                    signcallback.onAction(CameraLoginStatus.PREPARE_CONNECT_DESK);
                }
                if(this.view != null)
                {
                    delay(100, ()->{ this.connectToPeer(from);});
                }else
                {
                    this.connectingRemoteId = from;
                }
            }
            return;
        }
        if(rtcConn == null){
            return;
        }

        //和查看方建立连接
        if (type.equals("offer-loopback")) {

        }
        else if (type.length() > 0) {
            String sdp = resultJson.optString("sdp", "");
            if(sdp.length() > 0)
            {
                onRemoteSdp(sdp);
            }
            if(type.equals("offer")){
                rtcConn.createAnswer(this, new MediaConstraints());
            }
        } else{
            onRemoteCandicate(resultJson);
        }

    }



    private  void maybeUserInfo(String value)
    {
        if(value == null){
            return;
        }
        Log.i(Tag, "on user msg:" + value);
        if(localId == null || localId.length()<= 0)
        {
            localId = getPeerId(value);
        }
        String[] items = value.split("\n");
        if(items.length <= 0){
            return;
        }
        int length = items.length;
        while(--length >= 0){
            String item  = items[length];
            String all = item.trim();
            if(all.length() <= 0){
                continue;
            }
            String[] itemOneNames = all.split(",");
            if(itemOneNames.length < 3)
            {
                continue;
            }
            String name = itemOneNames[0];
            String peerId = itemOneNames[1];
            String connected = itemOneNames[2];
            if(name.equals(this.name + "@s")){
                if(connected.equals("1")){
                    if(!peerId.equals(this.remoteId)){
                        closePeerConnection();
                        delay(100, ()->{ this.connectToPeer(peerId);});
                    }
                }else if(peerId != null && peerId.equals(this.remoteId))
                {
                    closePeerConnection();
                }
            }
        }
    }


    private void createPeerConnection() {
        if(!factoryInited){
            PeerConnectionFactory.initialize(PeerConnectionFactory.InitializationOptions.builder(this.context).setInjectableLogger(this,
                    Logging.Severity.LS_INFO).createInitializationOptions());
            factoryInited=true;
        }
      //  VideoDecoderFactory decoderFactory = new PlatformSoftwareVideoDecoderFactory(eglBase.getEglBaseContext());//new DefaultVideoDecoderFactory(eglBase.getEglBaseContext());
       PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();

        //vp8,vp9
        HardwareVideoEncoderFactory hardwareVideoEncoderFactory = new HardwareVideoEncoderFactory(eglBase.getEglBaseContext(),false,true);
        VideoCodecInfo[] codecInfos = hardwareVideoEncoderFactory.getSupportedCodecs();

        //encoder、decoder都要设置，不然会出错
        factory = PeerConnectionFactory.builder()
                .setOptions(options)
//                .setVideoEncoderFactory(new SoftwareVideoEncoderFactory())
                .setVideoEncoderFactory(new DefaultVideoEncoderFactory(eglBase.getEglBaseContext(),false,true))
                .setVideoDecoderFactory(new DefaultVideoDecoderFactory(eglBase.getEglBaseContext()))
                .createPeerConnectionFactory();

        List<PeerConnection.IceServer> servers = new ArrayList<PeerConnection.IceServer>();
        PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(servers);
        rtcConfig.tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED;
        rtcConfig.continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY;
        rtcConfig.keyType = PeerConnection.KeyType.ECDSA;
        rtcConfig.enableDtlsSrtp = true;
        rtcConfig.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN;

        MediaConstraints constraints = new MediaConstraints();
//        constraints.mandatory.add(new MediaConstraints.KeyValuePair("googCpuOveruseDetection", "true"));
        rtcConn = factory.createPeerConnection(rtcConfig, constraints,this);
        rtcConn.setAudioRecording(false);
//        rtcConn.setBitrate(1000*1000, 2000*1000, 3000*1000);
//        rtcConn.setBitrate()
        videoSource = factory.createVideoSource(false);
        videoSource.adaptOutputFormat(this.width, this.height, this.fps);

//        videoSource.adaptOutputFormat();
        videoSourceObserver = videoSource.getCapturerObserver();

        VideoTrack videoTrack = factory.createVideoTrack("1", videoSource);
        videoTrack.setEnabled(true);
        videoTrack.addSink(view);
        //MediaStream localMediaStream = factory.createLocalMediaStream("local1");
        List<String> mediaStreamLabels = Collections.singletonList("ARDAMS");
        rtcConn.addTrack(videoTrack, mediaStreamLabels);


        if(this.captuer != null)
        {
            captuer.initialize(videoCapturerSurfaceTextureHelper, context, videoSourceObserver);
            captuer.startCapture(this.width, this.height, this.fps);
        }
    }




    @Override
    public void onLogMessage(String s, Logging.Severity severity, String s1) {
        Log.i("MUKE_LOG",s1 + " "  + s);
    }

    @Override
    public void onSignalingChange(PeerConnection.SignalingState signalingState) {
        Log.i(Tag, "onSignalingChange:" + signalingState);
    }

    @Override
    public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
        Log.i(Tag, "ice connection state:" + iceConnectionState);
        if(iceConnectionState == PeerConnection.IceConnectionState.CONNECTED){
            iceConnected=true;
        }
        else if(iceConnectionState == PeerConnection.IceConnectionState.FAILED){
            if(view == null){
                return;
            }
            view.post(()->{
                reConnect();
            });
        }
    }

    @Override
    public void onIceConnectionReceivingChange(boolean b) {

    }

    @Override
    public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
        Log.i(Tag, "iceGatheringState:" + iceGatheringState);
    }

    @Override
    public void onIceCandidate(IceCandidate iceCandidate) {
      try{
          JSONObject json = new JSONObject();
          json.put("sdpMid", iceCandidate.sdpMid);
          json.put("sdpMLineIndex", iceCandidate.sdpMLineIndex);
          json.put("candidate", iceCandidate.sdp);
          sdpCommunication2.send(json, remoteId);
      }catch (Exception ex){}
    }

    @Override
    public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {

    }

    @Override
    public void onAddStream(MediaStream mediaStream) {

    }

    @Override
    public void onRemoveStream(MediaStream mediaStream) {

    }

    @Override
    public void onDataChannel(DataChannel dataChannel) {

    }

    @Override
    public void onRenegotiationNeeded() {

    }

    @Override
    public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {

    }

    private void delay(int ms, Runnable r) {
        new Handler().postDelayed(() -> r.run(), ms);
    }



    private void connectToPeer(String remoteId) {
        if (this.remoteId != null && this.remoteId.equals(remoteId) )
        {
            return;
        }
        if (rtcConn != null) {
            Log.w(Tag, "We only support connecting to one peer at a time");
            return;
        }
        Log.i(Tag, "begin to connect to remoteId:" + remoteId);
        this.remoteId = remoteId;
        this.createPeerConnection();
        MediaConstraints videoConstraints = new MediaConstraints();
//        videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("maxFrameRate", ""+fps));
//        videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("minFrameRate", ""+fps));
        rtcConn.createOffer(this, videoConstraints);
        Log.i(Tag, "end to connect to remoteId:" + remoteId);
    }

    private  void closePeerConnection()
    {
        Log.i(Tag, "close peer connect, localid: " + this.localId);
        if(rtcConn != null){
            rtcConn.dispose();
        }
        if(factory != null){
            factory.dispose();
        }

        if(captuer != null){
            try {
                captuer.stopCapture();
            } catch (InterruptedException e) {

            }
        }
        remoteId =null;
        iceConnected=false;
        factory = null;
        rtcConn = null;
    }


    private void onRemoteSdp(String sdp) {
        SessionDescription offer = new SessionDescription(SessionDescription.Type.ANSWER, sdp);
        rtcConn.setRemoteDescription(new SdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {

            }

            @Override
            public void onSetSuccess() {

            }

            @Override
            public void onCreateFailure(String s) {

            }

            @Override
            public void onSetFailure(String s) {

            }
        }, offer);
    }

    private void onRemoteCandicate(JSONObject data) {
        String sdp = data.optString("candidate", "");
        if (sdp.length() == 0) {
            return;
        }
        IceCandidate candidate = new IceCandidate(data.optString("sdpMid", "0"), data.optInt("sdpMLineIndex", 0), sdp);
        rtcConn.addIceCandidate(candidate);
    }

    //sdp observer begin
    @Override
    public void onCreateSuccess(SessionDescription sessionDescription) {
        if(rtcConn == null){
            return;
        }
        rtcConn.setLocalDescription(new SdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {

            }

            @Override
            public void onSetSuccess() {

            }

            @Override
            public void onCreateFailure(String s) {

            }

            @Override
            public void onSetFailure(String s) {

            }
        }, sessionDescription);
        JSONObject json = new JSONObject();
        try {
            json.put("type", sessionDescription.type.toString().toLowerCase());
            json.put("sdp", sessionDescription.description);
        } catch (Exception e) {
        }
        sdpCommunication2.send(json, this.remoteId);
    }

    @Override
    public void onSetSuccess() {

    }

    @Override
    public void onCreateFailure(String s) {

    }

    @Override
    public void onSetFailure(String s) {

    }
    //sdp observer end


    private String getPeerId(String signInData) {
        String[] items = signInData.split(",");
        int length = items.length;
        String result = null;
        for (int i = 0; i < length; i++) {
            String value = items[i];
            if (value.equals(this.name)) {
                result = items[i + 1];
                break;
            }
        }
        return result;
    }
}
