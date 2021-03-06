#include <jni.h>
#include <string>

#include "RtmpPush.h"
#include "CallJava.h"

RtmpPush *rtmpPush = NULL;
CallJava *wlCallJava = NULL;
JavaVM *javaVM = NULL;
bool exit = true;

extern "C"
// extern "C"后，会指示编译器这部分代码按C语言的进行编译，而不是C++的。
// 由于C++支持函数重载，因此编译器编译函数的过程中会将函数的参数类型也加到编译后的代码中，
// 而不仅仅是函数名；而C语言并不支持函数重载，
// 因此编译C语言代码的函数时不会带上函数的参数类型，一般之包括函数名。
JNIEXPORT void JNICALL
Java_com_yxt_livepusher_network_rtmp_RtmpPush_initPush(JNIEnv *env, jobject instance,
                                                       jstring pushUrl_) {
    const char *pushUrl = env->GetStringUTFChars(pushUrl_, 0);

    // TODO
    if (wlCallJava == NULL) {
        exit = false;
        wlCallJava = new CallJava(javaVM, env, &instance);
        rtmpPush = new RtmpPush(pushUrl, wlCallJava);
        rtmpPush->init();
    }
    env->ReleaseStringUTFChars(pushUrl_, pushUrl);
}


extern "C"
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    javaVM = vm;
    JNIEnv *env;
    if (vm->GetEnv((void **) &env, JNI_VERSION_1_4) != JNI_OK) {
        if (LOG_SHOW) {
            LOGE("GetEnv failed!");
        }
        return -1;
    }
    return JNI_VERSION_1_4;
}

extern "C"
JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *vm, void *reserved) {
    javaVM = NULL;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_yxt_livepusher_network_rtmp_RtmpPush_pushSPSPPS(JNIEnv *env, jobject instance,
                                                          jbyteArray sps_, jint sps_len,
                                                          jbyteArray pps_, jint pps_len) {
    jbyte *sps = env->GetByteArrayElements(sps_, NULL);
    jbyte *pps = env->GetByteArrayElements(pps_, NULL);

    // TODO
    if (rtmpPush != NULL && !exit) {
        rtmpPush->pushSPSPPS(reinterpret_cast<char *>(sps), sps_len, reinterpret_cast<char *>(pps),
                             pps_len);
    }

    env->ReleaseByteArrayElements(sps_, sps, 0);
    env->ReleaseByteArrayElements(pps_, pps, 0);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_yxt_livepusher_network_rtmp_RtmpPush_pushVideoData(JNIEnv *env, jobject instance,
                                                             jbyteArray data_, jint data_len,
                                                             jboolean keyframe) {
    jbyte *data = env->GetByteArrayElements(data_, NULL);
    // TODO
    if (rtmpPush != NULL && !exit) {
        rtmpPush->pushVideoData(reinterpret_cast<char *>(data), data_len, keyframe);
    }
    env->ReleaseByteArrayElements(data_, data, 0);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_yxt_livepusher_network_rtmp_RtmpPush_pushAudioData(JNIEnv *env, jobject instance,
                                                             jbyteArray data_, jint data_len) {
    jbyte *data = env->GetByteArrayElements(data_, NULL);

    // TODO
    if (rtmpPush != NULL && !exit) {
        rtmpPush->pushAudioData(reinterpret_cast<char *>(data), data_len);
    }

    env->ReleaseByteArrayElements(data_, data, 0);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_yxt_livepusher_network_rtmp_RtmpPush_pushStop(JNIEnv *env, jobject instance) {

    // TODO
    if (rtmpPush != NULL) {
        exit = true;
        // 停止放入RTMP数据
        rtmpPush->pushStop();
        delete (rtmpPush);
        delete (wlCallJava);
        rtmpPush = NULL;
        wlCallJava = NULL;
    }

}