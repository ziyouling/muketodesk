package com.muketang.mukescreen.sdp;

import org.json.JSONObject;

public interface ISdpListener {
    void onSignStatus(SignStatus status);

    void onMsg(String msg);

}
