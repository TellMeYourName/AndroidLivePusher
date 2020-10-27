package com.yxt.livepusher.network.rtmp;

import android.text.TextUtils;
import android.util.Log;

public class RtmpPush {


    /**
     * 加载 so库x
     */
    static {
        System.loadLibrary("push");
    }

    /**
     * 服务器连接监听器
     */
    private ConnectListenr connectListenr;


    public void setConnectListenr(ConnectListenr connectListenr) {
        this.connectListenr = connectListenr;
    }


    private void onConnecting() {
        if (connectListenr != null) {
            connectListenr.onConnecting();
        }
    }

    private void onConnectSuccess() {
        if (connectListenr != null) {
            connectListenr.onConnectSuccess();
        }
    }

    private void onConnectFial(String msg) {
        if (connectListenr != null) {
            connectListenr.onConnectFail(msg);
        }
    }


    /**
     * 初始化直播，native方法保存了直播地址，生成了MessageQueue对象，用于存放RTMPPacket
     * @param url rtmp 直播地址
     */
    public void initLivePush(String url) {
        if (!TextUtils.isEmpty(url)) {
            initPush(url);
        }
    }

    /**
     * 将SPS PPS传递给native
     * @param sps
     * @param pps
     */
    public void pushSPSPPS(byte[] sps, byte[] pps) {
        if (sps != null && pps != null) {
            pushSPSPPS(sps, sps.length, pps, pps.length);
        }
    }

    public void pushVideoData(byte[] data, boolean keyframe) {
        if (data != null) {
            pushVideoData(data, data.length, keyframe);
        }
    }

    public void pushAudioData(byte[] data) {
        if (data != null) {
            pushAudioData(data, data.length);
        }
    }

    public void stopPush() {
        pushStop();
    }


    private native void initPush(String pushUrl);

    private native void pushSPSPPS(byte[] sps, int sps_len, byte[] pps, int pps_len);

    private native void pushVideoData(byte[] data, int data_len, boolean keyframe);

    private native void pushAudioData(byte[] data, int data_len);

    private native void pushStop();


}
