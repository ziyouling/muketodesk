package com.muketang.mukescreen;

public enum DeskStatus {
    /**
     * 注册中
     */
    SIGNING,
    /**
     * ICE连接中
     */
    ICE_CONECTING,
    /**
     * ICE连接OK
     */
    ICE_CONNECTED,
    /**
     * 收到视频
     */
    VIDEO_RECEVIED,


    VIDEO_TIMEOUT,

    /**
     * ICE失连
     */
    ICE_DISCONNECTED,

    /**
     * sign失败
     */
    SIGN_FAILED,

    RESET,
}
