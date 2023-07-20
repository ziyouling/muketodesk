package com.muketang.mukescreen;

import android.content.Context;

import org.webrtc.DataChannel;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.Loggable;
import org.webrtc.Logging;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.PlatformSoftwareVideoDecoderFactory;
import org.webrtc.RtpReceiver;
import org.webrtc.VideoDecoderFactory;
import org.webrtc.VideoTrack;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class PeerClient implements IPeerSdpClient, PeerConnection.Observer, Loggable {

    private  static  boolean factoryInit;

    private SdpCommunication sdpCommunication;
    private  IDeskStatusChangeListener statusCallback;
    private IDeskFrameListener frameListener;

    private  Context context;

    private PeerConnection rtcConn;
    private    PeerConnectionFactory factory;
    private  EglBase eglBase;

    private  boolean closed;

    PeerClient(Context context, EglBase eglbase, String serverIp, int port, IDeskStatusChangeListener statusCallback, IDeskFrameListener frameListener) {
        this.context = context;
        this.eglBase = eglbase;
        String server = "http://" + serverIp + ":" + port;
        this.statusCallback = statusCallback;
        this.frameListener = frameListener;
        String mukeId = "Muke@" + AppModel.getInstance().getDeviceId();
        sdpCommunication = new SdpCommunication(context, this,statusCallback, server,mukeId);
    }

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    /**
     * 打开视图
     */
    public void start() {
        sdpCommunication.signIn();
    }

    /**
     * 关闭视图
     */
    public void stop() {
        sdpCommunication.stopPoll();
        sdpCommunication.signOut();
    }

    public  void  close()
    {
        closed=true;
        if (rtcConn != null) {
            stop();
            rtcConn.close();
            rtcConn.dispose();
            rtcConn = null;
        }
        if(sdpCommunication != null)
        {
            sdpCommunication.stopPoll();
            sdpCommunication.signOut();
            sdpCommunication=null;
        }
        if(factory != null)
        {
            factory.dispose();
            factory = null;
        }
    }


    @Override
    public void asyRun(Runnable task) {
        if(closed)
        {
            return;
        }
        executor.execute(task);
    }

    @Override
    public PeerConnection createPeerConnection() {
        if(!factoryInit)
        {
            PeerConnectionFactory.initialize(PeerConnectionFactory.InitializationOptions.builder(this.context).setInjectableLogger(this,
                    Logging.Severity.LS_WARNING).createInitializationOptions());
            factoryInit=true;
        }
        VideoDecoderFactory decoderFactory = new PlatformSoftwareVideoDecoderFactory(eglBase.getEglBaseContext());//new DefaultVideoDecoderFactory(eglBase.getEglBaseContext());
        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();

        //encoder、decoder都要设置，不然会出错
        factory = PeerConnectionFactory.builder()
                .setOptions(options)
                .setVideoEncoderFactory(new DefaultVideoEncoderFactory(eglBase.getEglBaseContext(), true, true))
                .setVideoDecoderFactory(decoderFactory)
                .createPeerConnectionFactory();

        List<PeerConnection.IceServer> servers = new ArrayList<PeerConnection.IceServer>();
        PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(servers);
        rtcConfig.continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY;
        rtcConfig.keyType = PeerConnection.KeyType.ECDSA;
        rtcConfig.enableDtlsSrtp = true;
        rtcConfig.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN;
        rtcConfig.tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED;
        rtcConn = factory.createPeerConnection(rtcConfig, this);


        return rtcConn;
    }

    @Override
    public void onSignalingChange(PeerConnection.SignalingState signalingState) {

    }

    @Override
    public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
        MessageController.getInstance().send(MessageController.LOG, "ice connetion status：" + iceConnectionState);
        switch (iceConnectionState)
        {
            case FAILED:
                if(statusCallback != null) {
                    statusCallback.onDeskStatusChanged(DeskStatus.ICE_DISCONNECTED);
                }
                break;
            case CONNECTED:
                if(statusCallback != null)
                {
                    statusCallback.onDeskStatusChanged(DeskStatus.ICE_CONNECTED);
                }
                break;
            case DISCONNECTED:
                if(statusCallback != null) {
                   // statusCallback.onDeskStatusChanged(DeskStatus.ICE_DISCONNECTED);
                }
                break;
            case CLOSED:
                if(statusCallback != null)
                {
                  //  statusCallback.onDeskStatusChanged(DeskStatus.ICE_DISCONNECTED);
                }
                break;
            case NEW:
                break;
            case CHECKING:
                break;
            case COMPLETED:
                sdpCommunication.stopPoll();
                if(statusCallback != null)
                {
                    statusCallback.onDeskStatusChanged(DeskStatus.ICE_CONNECTED);
                }
                break;
        }
    }

    @Override
    public void onIceConnectionReceivingChange(boolean b) {

    }

    @Override
    public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {

    }

    @Override
    public void onIceCandidate(IceCandidate iceCandidate) {
        sdpCommunication.sendIceCandidate(iceCandidate);
    }

    @Override
    public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {

    }

    @Override
    public void onAddStream(MediaStream mediaStream) {
        VideoTrack track = mediaStream.videoTracks.get(0);
        track.addSink(videoFrame -> {
            if(frameListener != null)
            {
                frameListener.onFrame(videoFrame);
            }
        });
    }

    @Override
    public void onRemoveStream(MediaStream mediaStream) {

    }

    @Override
    public void onDataChannel(DataChannel dc) {
    }

    @Override
    public void onRenegotiationNeeded() {

    }

    @Override
    public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {

    }

    @Override
    public void onLogMessage(String s, Logging.Severity severity, String s1) {
        System.out.println(s + severity + ":" + s1);
    }
}
