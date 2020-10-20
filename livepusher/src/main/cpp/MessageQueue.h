//
// Created by yangw on 2018-9-14.
//

#ifndef WLLIVEPUSHER_WLQUEUE_H
#define WLLIVEPUSHER_WLQUEUE_H

#include "queue"
#include "pthread.h"
#include "AndroidLog.h"

extern "C"
{
#include "librtmp/rtmp.h"
};


class MessageQueue {

public:
    // 存放RTMPPacket的队列
    std::queue<RTMPPacket *> queuePacket;
    // 互斥锁
    pthread_mutex_t mutexPacket;
    // 条件变量用来自动阻塞一个线程，直到条件(predicate)满足被触发为止
    pthread_cond_t condPacket;

public:
    MessageQueue();
    ~MessageQueue();

    /**
     * 将 RTMPPacket 放入队列中
     * @param packet
     * @return
     */
    int putRtmpPacket(RTMPPacket *packet);

    /**
     * 获取RTMP packet
     * @return
     */
    RTMPPacket* getRtmpPacket();

    void clearQueue();

    /**
     * 通知释放锁，避免死锁
     */
    void notifyQueue();


};


#endif //WLLIVEPUSHER_WLQUEUE_H
