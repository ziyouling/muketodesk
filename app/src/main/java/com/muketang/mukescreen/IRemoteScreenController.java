package com.muketang.mukescreen;

import android.content.Context;


import org.webrtc.SurfaceViewRenderer;

/**
 * 和屏幕相关的操作
 */
public interface IRemoteScreenController {

    void  init();
    /**
     * 连接到讲桌
     * @param serverIp 讲台ip
     */
    void connect(Context context, String serverIp);

    /**
     * 关闭和讲桌的连接
     */
    void close();

    /**
     * 获取课桌状态
     */
    DeskStatus getDeskStatus();

    /**
     * 添加状态监听
     */
    void addDeskStatusChangeListener(IDeskStatusChangeListener listener);

    /**
     * 删除状态监听
     */
    void removeDeskStatusChangeListener(IDeskStatusChangeListener listener);

    /**
     * 监听桌面帧数据
     */
    void addDeskFrameListener(IDeskFrameListener listener);

    /**
     * 删除桌面帧数据监听
     */
    void removeDeskFrameListener(IDeskFrameListener listener);


    /**
     * 配置视频
     * @param view
     */
    void config(SurfaceViewRenderer view);

}
