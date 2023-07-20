package com.muketang.mukescreen;

import org.webrtc.VideoFrame;

public interface IDeskFrameListener {
    void  onFrame(VideoFrame frame);
}
