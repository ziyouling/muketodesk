package com.muketang.mukescreen.sdp;

import org.json.JSONObject;

public interface ISdpCommunication2 {
    void sign(String server, String name);

    void connectToDesk(String name, String deskName);

    void sigout();

    /**
     * 释放
     */
    void dispose();

    void send(JSONObject value,  String remoteId);

    void addSdpListener(ISdpListener listener);

    void removeSdpListener(ISdpListener listener);

    SignStatus getSignStatus();

    String getPeerId();

}
