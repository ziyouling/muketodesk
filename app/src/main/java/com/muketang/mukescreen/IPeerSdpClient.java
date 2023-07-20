package com.muketang.mukescreen;

import org.webrtc.PeerConnection;

public interface IPeerSdpClient {
    void asyRun(Runnable task);

    PeerConnection createPeerConnection();

    void  close();
}


