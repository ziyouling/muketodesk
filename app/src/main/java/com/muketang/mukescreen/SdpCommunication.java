package com.muketang.mukescreen;

import android.content.Context;
import android.os.Handler;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.muketang.mukescreen.http.JsonAndRawResponseRequest;

import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.PeerConnection;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;

import java.util.Map;

public class SdpCommunication implements SdpObserver {

    private IPeerSdpClient client;
    private RequestQueue httpReqQueue;
    private String name;
    private String server;
    private String localId;
    private String remoteId = "1";

    private PeerConnection peerConnection;
    private boolean signOut;
    private boolean msgPollRequired = true;
    private boolean anwserCreating;
    private SessionDescription anwser;

    private  IDeskStatusChangeListener statusCallback;

    public SdpCommunication(Context context, IPeerSdpClient client, IDeskStatusChangeListener statusCallback, String server, String name) {
        this.server = server;
        this.name = name;
        this.statusCallback = statusCallback;
        httpReqQueue = Volley.newRequestQueue(context);
        this.client = client;
    }

    /**
     * 登录到信令服务器，交换sdp
     */
    public void signIn() {
        String url = server + "/sign_in?" + name;
        StringRequest req = new StringRequest(Request.Method.GET, url, (p) -> {
            onSiginResult(p);
        }, (p) -> {
            onSiginFail(p);
        });
        req.setShouldCache(false);
        httpReqQueue.add(req);
    }

    public void sendIceCandidate(IceCandidate iceCandidate) {
        JSONObject req = new JSONObject();
        try {
            req.put("sdpMid", iceCandidate.sdpMid);
            req.put("sdpMLineIndex", iceCandidate.sdpMLineIndex);
            req.put("candidate", iceCandidate.sdp);
        } catch (Exception e) {
        }
        sendTo(req);
    }

    public void stopPoll() {
        msgPollRequired = false;
    }

    public void signOut() {
        signOut = true;
        String url = server + "/sign_out?peer_id=" + this.localId;
        StringRequest req = new StringRequest(Request.Method.GET, url, (p) -> {
            onSiginResult(p);
        }, (p) -> {
            onSigOutFail(p);
        });
        req.setShouldCache(false);
        httpReqQueue.add(req);
        if(waitRequest != null)
        {
            waitRequest.cancel();
        }
    }

    private void onSiginFail(VolleyError error) {
        statusCallback.onDeskStatusChanged(DeskStatus.SIGN_FAILED);
    }

    private void onSigOutFail(VolleyError error) {

    }

    private void onSiginResult(String data) {
        localId = getPeerId(data);
        if (localId == null || localId.length() <= 0) {
            return;
        }
        statusCallback.onDeskStatusChanged(DeskStatus.ICE_CONECTING);
        client.asyRun(() -> {
            peerConnection = client.createPeerConnection();
            getMsg();
        });
    }

    private  void fireNeedRest()
    {
        if(statusCallback != null)
        {
            statusCallback.onDeskStatusChanged(DeskStatus.RESET);
        }
    }

    private JsonAndRawResponseRequest waitRequest;
    /**
     * 询问是否有自己的消息
     */
    private void getMsg() {
        if(waitRequest != null)
        {
            waitRequest.cancel();
        }
        if(signOut)
        {
            return;
        }
        String url = server + "/wait?keepalive=0&peer_id=" + localId;
        JsonAndRawResponseRequest request = new JsonAndRawResponseRequest(Request.Method.GET, url, null, (p) -> onGetMsgSuccess(p), (p) -> onGetMsgFailed(p));
        waitRequest = request;
        request.setShouldCache(false);
        //等待5分钟
        request.setRetryPolicy(new DefaultRetryPolicy(300000,
                0,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        httpReqQueue.add(request);
    }

    /**
     * 延迟10ms获取消息
     */
    private void delayGetMsg(int ms) {
        if (signOut || !msgPollRequired) {
            return;
        }
        new Handler().postDelayed(() -> getMsg(), ms);
    }

    private void onGetMsgFailed(VolleyError e) {
        delayGetMsg(1000);
    }

    private void onGetMsgSuccess(JSONObject result) {
        NetworkResponse response =  waitRequest.getResponse();
        Map<String, String> headers = response.headers;
        String remotePeerId = headers.get("Pragma");
        if(remotePeerId != null && remotePeerId.length() > 0)
        {
            this.remoteId = remotePeerId;
        }
        String type = result.optString("type", "");
        //和查看方建立连接
        if (type.equals("offer")) {
            onRemoteSdp(result.optString("sdp", ""));
        }else if(type.equals("reset")){
            fireNeedRest();
        }  else{
            onRemoteCandicate(result);
        }
        delayGetMsg(10);
    }

    private void onRemoteSdp(String sdp) {
        client.asyRun(() ->
        {
            SessionDescription offer = new SessionDescription(SessionDescription.Type.OFFER, sdp);
            peerConnection.setRemoteDescription(this, offer);
        });
    }


    private void onRemoteCandicate(JSONObject data) {
        String sdp = data.optString("candidate", "");
        if (sdp.length() == 0) {
            return;
        }
        client.asyRun(() -> {
            IceCandidate candidate = new IceCandidate(data.optString("sdpMid", "0"), data.optInt("sdpMLineIndex", 0), sdp);
            boolean result =  peerConnection.addIceCandidate(candidate);
            if(!result)
            {
              //  AideuApplication.sendMessage(AideuApplication.ERROR, "addIceCandidate失败");
            }
        });
    }

    @Override
    public void onCreateSuccess(SessionDescription sessionDescription) {
        if (!anwserCreating) {
            return;
        }
        this.anwser = sessionDescription;
       // AideuApplication.sendMessage(AideuApplication.ERROR, "create anwser success and set local desc");
        client.asyRun(() -> {
            peerConnection.setLocalDescription(this, sessionDescription);
        });
    }

    @Override
    public void onSetSuccess() {
        if (signOut) {
            return;
        }
       // AideuApplication.sendMessage(AideuApplication.ERROR, "onSetSuccess");
        //awnser set success
        if (anwserCreating) {
            if(anwser != null)
            {
              //  AideuApplication.sendMessage(AideuApplication.ERROR, "send anwser");
                sendSdp(anwser);
            }
            return;
        }
        //remote offer
        anwserCreating = true;
        client.asyRun(() -> {
            MediaConstraints sdpMediaConstraints = new MediaConstraints();
            sdpMediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "false"));
            sdpMediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
            peerConnection.createAnswer(this, sdpMediaConstraints);
        });
    }

    @Override
    public void onCreateFailure(String s) {
      //  AideuApplication.sendMessage(AideuApplication.ERROR, "create fail: " + s);
    }

    @Override
    public void onSetFailure(String s) {
      //  AideuApplication.sendMessage(AideuApplication.ERROR, "set fail: " + s);
    }

    private void sendSdp(SessionDescription sdp) {
        JSONObject req = new JSONObject();
        try {
            req.put("type", sdp.type.toString().toLowerCase());
            req.put("sdp", sdp.description);
        } catch (Exception e) {
        }
        sendTo(req);
    }

    private void sendTo(JSONObject req) {
        String url = server + "/message?peer_id=" + this.localId + "&to=" + remoteId;
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, req, (p) -> {
        }, (p) -> {
            onMsgSentFailed(req, p);
        });
        request.setShouldCache(false);
        httpReqQueue.add(request);
    }

    private void onMsgSentFailed(JSONObject req,VolleyError e) {
      //  AideuApplication.sendMessage(AideuApplication.ERROR, "send  failed");
    }


    private String getPeerId(String signInData) {
        String[] items = signInData.split(",");
        int length = items.length;
        String result = null;
        for (int i = 0; i < length; i++) {
            String value = items[i];
            if (value.equals(name)) {
                result = items[i + 1];
                break;
            }
        }
        return result;
    }
}
