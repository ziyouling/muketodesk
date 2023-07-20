package com.muketang.mukescreen.sdp;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.muketang.mukescreen.http.JsonAndRawResponseRequest;
import com.muketang.mukescreen.http.StringAndRawResponseRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SdpCommunication2 implements  ISdpCommunication2{

    private RequestQueue httpReqQueue;

    private List<ISdpListener> listeners;

    private String server;

    private  String name;

    private  String localId;

    private  SignStatus signStatus;

    private StringAndRawResponseRequest waitRequest;

    public  SdpCommunication2(Context context)
    {
        httpReqQueue = Volley.newRequestQueue(context);
        listeners = new ArrayList<ISdpListener>();
    }

    @Override
    public void sign(String server, String name) {
        this.name = name;
        this.server = server;
        String url = server + "/openscreen/sign_in?name=" + name;
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null, (p) -> {
            onSiginResult(p);
        }, (p) -> {
            onSiginFail(p);
        });
        req.setShouldCache(false);
        httpReqQueue.add(req);
    }

    @Override
    public void connectToDesk(String name, String deskName) {
        String url = server + "/openscreen/connect?fromPeerId=" + this.localId + "&fromName=" + name + "&toName=" + deskName;
        StringRequest req = new StringRequest(Request.Method.GET, url, (p) -> {

        }, (p) -> {

        });
        req.setShouldCache(false);
        httpReqQueue.add(req);
    }

    @Override
    public void sigout() {
        //不再支持sign out，因为server有可能重启，peer id可能不对，导致把别人给登出了。
        changeSignStatus(SignStatus.SIGNING_OUT);
        String url = server + "/sign_out?peerId=" + localId;
        StringRequest req = new StringRequest(Request.Method.GET, url, (p) -> {
            dispose();
        }, (p) -> {
            dispose();
        });
        req.setShouldCache(false);
        httpReqQueue.add(req);
        if(waitRequest != null)
        {
            waitRequest.cancel();
        }
    }

    @Override
    public void send(JSONObject value, String remoteId) {
        String url = server + "/openscreen/message?from=" + localId + "&to=" + remoteId;
        Log.i("MUKE_MSG", "send message to remoteid:" + remoteId + " msg:" + value.toString());
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, value, (p) -> {
        }, (p) -> {
            onMsgSentFailed(value,remoteId, p);
        });
        request.setShouldCache(false);
        httpReqQueue.add(request);
    }

    @Override
    public void addSdpListener(ISdpListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeSdpListener(ISdpListener listener) {
        listeners.remove(listener);
    }

    @Override
    public SignStatus getSignStatus() {
        return this.signStatus;
    }

    @Override
    public String getPeerId() {
        return this.localId;
    }


    public void dispose() {
        changeSignStatus(SignStatus.SIGN_OUT);
        httpReqQueue.stop();
        listeners.clear();
    }

    private void onSiginResult(JSONObject data) {
        int code = data.optInt("code");
        if(code != 0)
        {
            changeSignStatus(SignStatus.SIGN_FAILED);
            return;
        }
        localId = "" + data.optInt("result");
        changeSignStatus(SignStatus.SIGN_SUCCESS);
       //fireMsgGot(data);
        getMsg();
    }

    /**
     * 询问是否有自己的消息
     */
    private void getMsg() {
        if(waitRequest != null)
        {
            waitRequest.cancel();
        }
        if(this.signStatus != SignStatus.SIGN_SUCCESS)
        {
            return;
        }
        String url = server + "/openscreen/wait?to=" + localId;
        StringAndRawResponseRequest request = new StringAndRawResponseRequest(Request.Method.GET, url,
                (p) -> onGetMsgSuccess(p), (p) -> onGetMsgFailed(p));
        waitRequest = request;
        request.setShouldCache(false);
        //等待5分钟
        request.setRetryPolicy(new DefaultRetryPolicy(300000,
                0,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        httpReqQueue.add(request);
    }

    private void onSiginFail(VolleyError error) {
       changeSignStatus(SignStatus.SIGN_FAILED);
    }

    private void onSigOutFail(VolleyError error) {
        changeSignStatus(SignStatus.SIGN_FAILED);
    }

    private  void changeSignStatus(SignStatus status)
    {
        this.signStatus = status;
        for(ISdpListener listener : listeners)
        {
            listener.onSignStatus(status);
        }
    }

    private void onGetMsgFailed(VolleyError e) {
        if(e.networkResponse != null && e.networkResponse.statusCode == 500)
        {
            this.dispose();
            return;
        }
        delay(1000, ()->{ getMsg();});
    }

    private void onGetMsgSuccess(String result) {
        Log.i("MUKE_MSG", "received:" + result);
        JSONObject msg = null;
        try {
            msg = new JSONObject(result);
        } catch (JSONException e) {
            // e.printStackTrace();
        }
        int delayMs = 2000;
        if(msg != null && msg.optJSONObject("result") != null)
        {
            delayMs = 10;
        }
        fireMsgGot(result);
        delay(delayMs, ()->{ getMsg();});
    }

    private  void  fireMsgGot(String msg)
    {
        for(ISdpListener listener : listeners)
        {
            listener.onMsg(msg);
        }
    }

    private void onMsgSentFailed(JSONObject req,String remoteId,  VolleyError e) {
//        int retry = req.optInt("retry", 0);
//        if(retry >= 3)
//        {
//            return;
//        }
//        Integer retry2 = retry + 1;
//        try {
//            req.putOpt("retry", retry2);
//        } catch (JSONException jsonException) {
//            jsonException.printStackTrace();
//        }
//        delay(1000, ()->{ send(req, remoteId);});
    }

    /**
     * 延迟10ms获取消息
     */
    private void delay(int ms, Runnable r) {
        new Handler().postDelayed(() -> r.run(), ms);
    }

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
