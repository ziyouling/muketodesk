package com.muketang.mukescreen;

import android.content.Context;
import android.os.Handler;
import android.os.Message;

import java.util.ArrayList;
import java.util.List;


public class MessageController {
    public  static  MessageController controller;

    public static final int HEART_BEAT_DISCONNECTED = 1;
    public static final int HEART_BEAT_CONNECTED = 2;

    public static final int BAR_ITEM_CHECKED = 3;
    public static final  int CAMERA = 4;
    public  static final int LOG = 5;

    private Handler msgHandler;

    private List<IMessageListener> messageListeners;

    public  static  MessageController getInstance()
    {
        if(controller == null)
        {
            controller = new MessageController();
        }
        return  controller;
    }

    public  void  init(Context context)
    {
        msgHandler = new android.os.Handler(){
            @Override
            public void handleMessage(Message msg) {
                onMsgGot(msg);
            }
        };
        messageListeners = new ArrayList<IMessageListener>();
    }

    public  void  send(int messageType, String msg)
    {
        Message message = new Message();
        message.what = messageType;
        message.obj = msg;
        msgHandler.sendMessage(message);
    }

    public void addMessageListener(IMessageListener listener)
    {
        messageListeners.add(listener);
    }

    public void removeMessageListener(IMessageListener listener)
    {
        messageListeners.remove(listener);
    }


    private  void  onMsgGot(Message msg)
    {
        for(IMessageListener item : messageListeners)
        {
            item.onMsgGot(msg.what, msg.obj.toString());
        }
    }
}
