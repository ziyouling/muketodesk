package com.muketang.mukescreen;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

public class DeskFinder {

    private RequestQueue httpReqQueue;

    private  String ipPrefix;

    private  int beign;
    private  int end;

    private  int exclude;

    private  Runnable callback;

    private  Activity owner;

    private  String deskIp;

    private  int current;


    private SharedPreferences prefs;

    private  int savedIp;
    private String ssid="deskIp";

    private String localName;

    private String localId;

    private  boolean serverChecking;

    public DeskFinder(Activity owner)
    {
        this.owner = owner;
        localName = "test_" + System.currentTimeMillis();
        prefs = owner.getSharedPreferences("ssid_desk", Context.MODE_PRIVATE);
        savedIp =  prefs.getInt(ssid, 0);
    }

    public  void  find( String ipPrefix, int exclude, int begin, int end, Runnable callback)
    {
        this.ipPrefix = ipPrefix;
        this.exclude = exclude;
        this.beign = begin;
        this.end     = end;
        this.callback = callback;
        current = --beign;

        httpReqQueue = Volley.newRequestQueue(owner);
        if(savedIp != 0)
        {
            findAt(savedIp);
        }else
        {
            findNext();
        }
    }

    public  void  retry()
    {
        current =this.beign-1;
        if(savedIp != 0)
        {
            findAt(savedIp);
        }else
        {
            findNext();
        }
    }

    public  String getDeskIp()
    {
        return  deskIp;
    }


    private  void findNext()
    {
        //每9秒检查以下server
        if(current % 9 == 0 && !serverChecking && savedIp != 0){
            serverChecking=true;
            findAt(savedIp);
            return;
        }
        serverChecking=false;
        current+=1;
        if(current == exclude)
        {
            current +=1;
        }
        if(current > end)
        {
            onFound(null, -1,false);
            return;
        }
        findAt(current);
    }


    private  void findAt(final int ipInt) {
        String ip = ipPrefix + "." + ipInt;
        String desk = "http://" + ip + ":" + RemoteScreenController.RTC_PORT;
        String url = desk + "/sign_in?" + localName;
        StringRequest req = new StringRequest(Request.Method.GET, url, (p) -> {
            this.localId = getPeerId(p);
            if(this.localId != null)
            {
                onFound(p, ipInt, true);
            }else
            {
                findNext();
            }
        }, (p) -> {
            findNext();
        });
        req.setShouldCache(false);
        req.setRetryPolicy(new DefaultRetryPolicy(
                100,
                1, 1.0f));
        httpReqQueue.add(req);
    }

    private  void signout(String deskIp)
    {
        String server = "http://" + deskIp + ":" + RemoteScreenController.RTC_PORT;
        String url = server + "/sign_out?peer_id=" + this.localId;
        StringRequest req = new StringRequest(Request.Method.GET, url, (p) -> {
        }, (p) -> {

        });
        req.setShouldCache(false);
        httpReqQueue.add(req);
    }

    private void onFound(String data, int ipInt, boolean success) {
        if(success)
        {

            this.deskIp = ipPrefix + "." + ipInt;
            signout(this.deskIp);
            if(savedIp != ipInt)
            {
               SharedPreferences.Editor editor =  prefs.edit();
               editor.putInt(ssid, ipInt);
               editor.commit();
            }
        }
        owner.runOnUiThread(this.callback);
    }

    private String getPeerId(String signInData) {
        String[] items = signInData.split(",");
        int length = items.length;
        String result = null;
        for (int i = 0; i < length; i++) {
            String value = items[i];
            if (value.equals(localName)) {
                result = items[i + 1];
                break;
            }
        }
        return result;
    }
}
