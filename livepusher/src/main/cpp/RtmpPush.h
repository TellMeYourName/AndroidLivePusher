

#ifndef WLLIVEPUSHER_RTMPPUSH_H
#define WLLIVEPUSHER_RTMPPUSH_H

#include <malloc.h>
#include <string.h>
#include "MessageQueue.h"
#include "pthread.h"
#include "CallJava.h"

extern "C"
{
#include "librtmp/rtmp.h"
};

class RtmpPush {

public:
    RTMP *rtmp = NULL;
    char *url = NULL;
    MessageQueue *queue = NULL;
    pthread_t push_thread;
    CallJava *wlCallJava = NULL;
    bool startPushing = false;
    long startTime = 0;
public:
    RtmpPush(const char *url, CallJava *wlCallJava);

    ~RtmpPush();

    void init();


    /**
        H264帧
        对于H.264而言，每帧的界定符为00 00 00 01 或者00 00 01。

        例如下面是一个H264的文件片段
        00 00 00 01 67 42 C0 28 DA 01 E0 08 9F 96 10 00
        00 03 00 10 00 00 03 01 48 F1 83 2A 00 00 00 01
        68 CE 3C 80 00 00 01 06 05 FF FF 5D DC 45 E9 BD
        E6 D9 48 B7 96 2C D8 20 D9 23 EE EF …

        第一帧是00 00 00 01 67 42 C0 28 DA 01 E0 08 9F 96 10 00 00 03 00 10 00 00 03 01 48 F1 83 2A
        第二帧是00 00 00 01 68 CE 3C 80
        第三帧是00 00 01 06 05 FF FF 5D DC 45 E9 BD E6 D9 48 B7 96 2C D8 20 D9 23 EE EF ..

        帧类型有：

        NAL_SLICE = 1 非关键帧
        NAL_SLICE_DPA = 2
        NAL_SLICE_DPB = 3
        NAL_SLICE_DPC =4
        NAL_SLICE_IDR =5 关键帧
        NAL_SEI = 6
        NAL_SPS = 7 SPS帧
        NAL_PPS = 8 PPS帧
        NAL_AUD = 9
        NAL_FILLER = 12

        SPS 对于H264而言，就是编码后的第一帧，如果是读取的H264文件，就是第一个帧界定符和第二个帧界定符之间的数据的长度是4

        PPS 就是编码后的第二帧，如果是读取的H264文件，就是第二帧界定符和第三帧界定符中间的数据长度不固定。
        */




    /**
     * 压入 SPS、PPS数据
     * @param sps 编码后的第一帧
     * @param sps_len 第一帧的长度
     * @param pps 编码后的第二帧
     * @param pps_len 编码后的第二帧长度
     */
    void pushSPSPPS(char *sps, int sps_len, char *pps, int pps_len);


    void pushVideoData(char *data, int data_len, bool keyframe);

    void pushAudioData(char *data, int data_len);

    void pushStop();


};


#endif //WLLIVEPUSHER_RTMPPUSH_H
