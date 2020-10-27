//
// Created by yangw on 2018-9-14.
//



#include "RtmpPush.h"

RtmpPush::RtmpPush(const char *url, CallJava *wlCallJava) {
    this->url = static_cast<char *>(malloc(512));
    strcpy(this->url, url);
    this->queue = new MessageQueue();
    this->wlCallJava = wlCallJava;
}

RtmpPush::~RtmpPush() {
    queue->notifyQueue();
    queue->clearQueue();
    free(url);
}

void *callBackPush(void *data) {

    RtmpPush *rtmpPush = static_cast<RtmpPush *>(data);
    // 没有开始推送
    rtmpPush->startPushing = false;
    // rtmpPush的rtmp
    rtmpPush->rtmp = RTMP_Alloc();
    // RTMP初始化
    RTMP_Init(rtmpPush->rtmp);
    // 设置rtmp超时时长
    rtmpPush->rtmp->Link.timeout = 10;
    // 标识为直播流
    rtmpPush->rtmp->Link.lFlags |= RTMP_LF_LIVE;
    // 设置RTMP的URL
    RTMP_SetupURL(rtmpPush->rtmp, rtmpPush->url);
    RTMP_EnableWrite(rtmpPush->rtmp);

    // RTMP url 连接失败
    if (!RTMP_Connect(rtmpPush->rtmp, NULL)) {
//        LOGE("can not connect the url");
        rtmpPush->wlCallJava->onConnectFail("can not connect the url");
        // 结束线程
        goto end;
    }
    // 检查rtmp流
    if (!RTMP_ConnectStream(rtmpPush->rtmp, 0)) {

        rtmpPush->wlCallJava->onConnectFail("can not connect the stream of service");
        // 结束线程
        goto end;
    }
    // 调用java层，连接成功
    rtmpPush->wlCallJava->onConnectsuccess();
    // 连接成功，开始推送
    rtmpPush->startPushing = true;
    // 开始时间戳
    rtmpPush->startTime = RTMP_GetTime();
    while (true) {
        // 停止推送
        if (!rtmpPush->startPushing) {
            break;
        }

        RTMPPacket *packet = NULL;
        // 获取RtmpPacket,线程会阻塞，直到获取到packet
        packet = rtmpPush->queue->getRtmpPacket();
        if (packet != NULL) {
            // 发送RTMP数据
            int result = RTMP_SendPacket(rtmpPush->rtmp, packet, 1);
            LOGD("RTMP_SendPacket result is %d", result);
            RTMPPacket_Free(packet);
            free(packet);
            packet = NULL;
        } else {
            LOGD("RTMP_SendPacket else %d", 123);

        }
    }
    end:
    RTMP_Close(rtmpPush->rtmp);
    RTMP_Free(rtmpPush->rtmp);
    rtmpPush->rtmp = NULL;
    // 线程结束
    pthread_exit(&rtmpPush->push_thread);
}

void RtmpPush::init() {
    // 回调 连接中
    wlCallJava->onConnectint(WL_THREAD_MAIN);
    // push_thread 创建，创建成功后会调用callBackPush
    pthread_create(&push_thread, NULL, callBackPush, this);

}

void RtmpPush::pushSPSPPS(char *sps, int sps_len, char *pps, int pps_len) {

    int bodysize = sps_len + pps_len + 16;// TODO 为什么要加16？
    RTMPPacket *packet = static_cast<RTMPPacket *>(malloc(sizeof(RTMPPacket)));
    RTMPPacket_Alloc(packet, bodysize);
    RTMPPacket_Reset(packet);

    char *body = packet->m_body;

    int i = 0;

    body[i++] = 0x17;

    body[i++] = 0x00;
    body[i++] = 0x00;
    body[i++] = 0x00;
    body[i++] = 0x00;

    body[i++] = 0x01;
    body[i++] = sps[1];
    body[i++] = sps[2];
    body[i++] = sps[3];

    body[i++] = 0xFF;

    body[i++] = 0xE1;
    body[i++] = (sps_len >> 8) & 0xff;
    body[i++] = sps_len & 0xff;
    memcpy(&body[i], sps, sps_len);
    i += sps_len;

    body[i++] = 0x01;
    body[i++] = (pps_len >> 8) & 0xff;
    body[i++] = pps_len & 0xff;
    memcpy(&body[i], pps, pps_len);

    packet->m_packetType = RTMP_PACKET_TYPE_VIDEO;
    packet->m_nBodySize = bodysize;
    packet->m_nTimeStamp = 0;
    packet->m_hasAbsTimestamp = 0;
    packet->m_nChannel = 0x04;
    packet->m_headerType = RTMP_PACKET_SIZE_MEDIUM;
    packet->m_nInfoField2 = rtmp->m_stream_id;

    queue->putRtmpPacket(packet);
}

void RtmpPush::pushVideoData(char *data, int data_len, bool keyframe) {

    int bodysize = data_len + 9;
    RTMPPacket *packet = static_cast<RTMPPacket *>(malloc(sizeof(RTMPPacket)));
    RTMPPacket_Alloc(packet, bodysize);
    RTMPPacket_Reset(packet);

    char *body = packet->m_body;
    int i = 0;

    if (keyframe) {
        // keyframe
        body[i++] = 0x17;
    } else {
        // inter-frame  inter frame(for AVC, a non-seekable frame) 不是关键帧，比如P帧
        // 故可通过与 0x17 或 0x27 的比较，来判断视频帧是否为关键帧。
        body[i++] = 0x27;
    }

    body[i++] = 0x01;
    body[i++] = 0x00;
    body[i++] = 0x00;
    body[i++] = 0x00;

    body[i++] = (data_len >> 24) & 0xff;
    body[i++] = (data_len >> 16) & 0xff;
    body[i++] = (data_len >> 8) & 0xff;
    body[i++] = data_len & 0xff;
    memcpy(&body[i], data, data_len);

    packet->m_packetType = RTMP_PACKET_TYPE_VIDEO;
    packet->m_nBodySize = bodysize;
    packet->m_nTimeStamp = RTMP_GetTime() - startTime;
    packet->m_hasAbsTimestamp = 0;
    packet->m_nChannel = 0x04;
    packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
    packet->m_nInfoField2 = rtmp->m_stream_id;

    queue->putRtmpPacket(packet);
}

/**
 * 将音频数据压入队列
 * @param data 数据
 * @param data_len 数据长度
 */
void RtmpPush::pushAudioData(char *data, int data_len) {
    int bodysize = data_len + 2;
    RTMPPacket *packet = static_cast<RTMPPacket *>(malloc(sizeof(RTMPPacket)));
    RTMPPacket_Alloc(packet, bodysize);
    RTMPPacket_Reset(packet);
    char *body = packet->m_body;
    // HE-AAC 44 kHz 16 bit stereo
    body[0] = 0xAF;
    body[1] = 0x01;
    memcpy(&body[2], data, data_len);
    // 下面RTMP 头信息
    // Message type ID
    packet->m_packetType = RTMP_PACKET_TYPE_AUDIO;
    // 消息长度
    packet->m_nBodySize = bodysize;
    // 时间戳
    packet->m_nTimeStamp = RTMP_GetTime() - startTime;
    packet->m_hasAbsTimestamp = 0;
    // 块流ID
    packet->m_nChannel = 0x04;
    packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
    // 消息流ID
    packet->m_nInfoField2 = rtmp->m_stream_id;
    queue->putRtmpPacket(packet);
}

void RtmpPush::pushStop() {
    startPushing = false;
    queue->notifyQueue();
    // 以阻塞的方式等待thread指定的线程结束
    pthread_join(push_thread, NULL);
}
