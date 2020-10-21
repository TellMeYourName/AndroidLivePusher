package com.yxt.livepusher.network.rtmp;

/***
 * rtmp 服务器连接监听器
 */
public interface ConnectListenr {

    /**
     * rtmp 连接中
     */
    void onConnecting();

    /**
     * 连接成功
     */
    void onConnectSuccess();

    /**
     * 连接失败
     * @param msg 失败信息
     */
    void onConnectFail(String msg);

}
