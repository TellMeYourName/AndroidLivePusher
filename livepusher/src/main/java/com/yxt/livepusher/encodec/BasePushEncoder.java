package com.yxt.livepusher.encodec;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import com.yxt.livepusher.egl.EglHelper;
import com.yxt.livepusher.egl.YUEGLSurfaceView;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;

import javax.microedition.khronos.egl.EGLContext;

public abstract class BasePushEncoder {

    /**
     * 视频Surface，输入输出的缓冲区
     */
    private Surface surface;
    /**
     * EGLContext
     */
    private EGLContext eglContext;


    private int width;
    private int height;

    /**
     * 视频编码器
     */
    private MediaCodec videoEncodec;

    /**
     * 媒体格式 AVC eg:MediaFormat.MIMETYPE_VIDEO_AVC、视频宽高
     */
    private MediaFormat videoFormat;

    /**
     * 视频Buffer信息，pts就在这里面
     */
    private MediaCodec.BufferInfo videoBufferinfo;

    /**
     * 音频编码器
     */
    private MediaCodec audioEncodec;

    /**
     * 音频格式 ，编码格式MediaFormat.MIMETYPE_AUDIO_AAC, 采样率 sampleRate, 声道数 channelCount
     */
    private MediaFormat audioFormat;

    /**
     * 音频信息
     */
    private MediaCodec.BufferInfo audioBufferinfo;

    /**
     * 音频时间戳
     */
    private long audioPts = 0;

    /**
     * 采样率
     */
    private int sampleRate;

    /**
     * EGLMediaThread EGL媒体线程
     */
    private EGLMediaThread mEGLMediaThread;
    /**
     * 视频编码线程
     */
    private VideoEncodecThread videoEncodecThread;
    /**
     * 音频编码线程
     */
    private AudioEncodecThread audioEncodecThread;

    /**
     * gLRender
     */
    private YUEGLSurfaceView.YuGLRender gLRender;

    public final static int RENDERMODE_WHEN_DIRTY = 0;
    public final static int RENDERMODE_CONTINUOUSLY = 1;
    private int mRenderMode = RENDERMODE_CONTINUOUSLY;

    /**
     * 帧率
     */
    private int fps = 15;
    /**
     * 比特率 250,000 B/s = 2,000,000 (250,000 * 8)b/s = 2 Mb/s
     */
    private int bitrate = 250000;

    /**
     * 媒体信息监听器
     */
    private OnMediaInfoListener onMediaInfoListener;

    /**
     * 音频锁
     */
    private final Object audioLock = new Object();
    /**
     * 视频锁
     */
    private final Object videoLock = new Object();

    public static final int STREAM_TYPE_JT1078 = 1;
    public static final int STREAM_TYPE_RTMP = 0;
    private int streamType = 0;

    public BasePushEncoder() {
    }

    public void setRender(YUEGLSurfaceView.YuGLRender gLRender) {
        this.gLRender = gLRender;
    }

    public void setmRenderMode(int mRenderMode) {
        if (gLRender == null) {
            throw new RuntimeException("must set render before");
        }
        this.mRenderMode = mRenderMode;
    }

    public void setOnMediaInfoListener(OnMediaInfoListener onMediaInfoListener) {
        this.onMediaInfoListener = onMediaInfoListener;
    }

    public void initEncodec(EGLContext eglContext, int width, int height) {
        this.width = width;
        this.height = height;
        this.eglContext = eglContext;
        initMediaEncodec(width, height, 44100, 2);
    }

    /**
     * 开启录制
     */
    public void startRecord() {
        if (surface != null && eglContext != null) {

            audioPts = 0;
            mEGLMediaThread = new EGLMediaThread(new WeakReference<BasePushEncoder>(this));
            videoEncodecThread = new VideoEncodecThread(new WeakReference<BasePushEncoder>(this));
            audioEncodecThread = new AudioEncodecThread(new WeakReference<BasePushEncoder>(this));
            mEGLMediaThread.isCreate = true;
            mEGLMediaThread.isChange = true;
            mEGLMediaThread.start();
            videoEncodecThread.start();
            audioEncodecThread.start();
        }
    }

    public void setFps(int fps) {
        this.fps = fps;
    }

    /**
     * 停止录制
     */
    public void stopRecord() {
        if (mEGLMediaThread != null && videoEncodecThread != null && audioEncodecThread != null) {
            videoEncodecThread.exit();
            audioEncodecThread.exit();
            mEGLMediaThread.onDestory();
            synchronized (audioLock) {
                audioLock.notifyAll();
            }
            synchronized (videoLock) {
                videoLock.notifyAll();
            }
            videoEncodecThread = null;
            mEGLMediaThread = null;
            audioEncodecThread = null;
        }
    }

    private void initMediaEncodec(int width, int height, int sampleRate, int channelCount) {
        initVideoEncodec(MediaFormat.MIMETYPE_VIDEO_AVC, width, height);
        initAudioEncodec(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channelCount);
    }


    private void initVideoEncodec(String mimeType, int width, int height) {
        try {
            videoBufferinfo = new MediaCodec.BufferInfo();
            videoFormat = MediaFormat.createVideoFormat(mimeType, width, height);
            // A key describing the color format of the content in a video format，
            // COLOR_FormatSurface indicates that the data will be a GraphicBuffer metadata reference.
            videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            videoFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);
            videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, fps);
            // 描述关键帧的间隔，如果设置为负数，第一帧之后就没有关键帧；
            videoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 3);
            // 创建视频编码器
            videoEncodec = MediaCodec.createEncoderByType(mimeType);
            // 使MediaCodec 进入configured状态。 MediaCodec.CONFIGURE_FLAG_ENCODE指明为编码器
            videoEncodec.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
           // Requests a Surface to use as the input to an encoder, in place of input buffers
            surface = videoEncodec.createInputSurface();

        } catch (IOException e) {
            e.printStackTrace();
            videoEncodec = null;
            videoFormat = null;
            videoBufferinfo = null;
        }

    }

    public void setBitrate(int bitrate) {
        this.bitrate = bitrate;
    }

    public void setStreamType(int streamType) {
        this.streamType = streamType;
    }

    private void initAudioEncodec(String mimeType, int sampleRate, int channelCount) {
        try {
            this.sampleRate = sampleRate;
            audioBufferinfo = new MediaCodec.BufferInfo();
            audioFormat = MediaFormat.createAudioFormat(mimeType, sampleRate, channelCount);
            audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, 96000);
            // AAC描述
            audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            // 输入最大字节数
            audioFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 4096 * 10);

            audioEncodec = MediaCodec.createEncoderByType(mimeType);
            audioEncodec.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

        } catch (IOException e) {
            e.printStackTrace();
            audioBufferinfo = null;
            audioFormat = null;
            audioEncodec = null;
        }
    }

    public void putPCMData(byte[] buffer, int size) {
        try {
            if (audioEncodecThread != null && !audioEncodecThread.isExit && buffer != null && size > 0) {
                int inputBufferindex = audioEncodec.dequeueInputBuffer(0);
                if (inputBufferindex >= 0) {
                    ByteBuffer byteBuffer = audioEncodec.getInputBuffers()[inputBufferindex];
                    byteBuffer.clear();
                    byteBuffer.put(buffer);
                    // 设置音频的显示时间戳
                    long pts = getAudioPts(size, sampleRate);
                    audioEncodec.queueInputBuffer(inputBufferindex, 0, size, pts, 0);
                }
                synchronized (audioLock) {
                    // 放了数据后通知等待的线程
                    audioLock.notifyAll();
                }
            }
        } catch (Exception e) {
            synchronized (audioLock) {
                audioLock.notifyAll();
            }
        }
    }

    static class EGLMediaThread extends Thread {
        private WeakReference<BasePushEncoder> encoder;
        private EglHelper eglHelper;
        private Object object;

        private boolean isExit = false;
        private boolean isCreate = false;
        private boolean isChange = false;
        private boolean isStart = false;
        private int width;
        private int height;

        private int fps;
        private int mRenderMode;
        private YUEGLSurfaceView.YuGLRender gLRender;

        /**
         * 这个线程用于将纹理id的内容渲染到surface上面
         * @param encoder
         */
        public EGLMediaThread(WeakReference<BasePushEncoder> encoder) {
            this.encoder = encoder;
            width = encoder.get().width;
            height = encoder.get().height;
            mRenderMode = encoder.get().mRenderMode;
            fps = encoder.get().fps;
            gLRender = encoder.get().gLRender;
        }

        @Override
        public void run() {
            super.run();
            isExit = false;
            isStart = false;
            object = new Object();
            eglHelper = new EglHelper();
            // 这个surface在初始化过程中产生的    surface = videoEncodec.createInputSurface();
            eglHelper.initEgl(encoder.get().surface, encoder.get().eglContext, width, height);

            while (true) {
                if (isExit) {
                    release();
                    break;
                }

                if (isStart) {
                    if (mRenderMode == RENDERMODE_WHEN_DIRTY) {
                        synchronized (object) {
                            try {
                                object.wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    } else if (mRenderMode == RENDERMODE_CONTINUOUSLY) {
                        try {
                            Thread.sleep(1000 / fps);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } else {
                        throw new RuntimeException("mRenderMode is wrong value");
                    }
                }
                // 创建opengl
                onCreate();
                onChange(width, height);
                // 开始绘制
                onDraw();
                isStart = true;
                if (encoder != null && encoder.get() != null) {
                    synchronized (encoder.get().videoLock) {
                        if (encoder != null && encoder.get() != null) {
                            // 这里已经把视频帧绘制到surface上面去了，通知视频编码线程取数据
                            encoder.get().videoLock.notifyAll();
                        }
                    }
                }
            }

        }

        private void onCreate() {
            if (isCreate && gLRender != null) {
                isCreate = false;
                encoder.get().gLRender.onSurfaceCreated();
            }
        }

        private void onChange(int width, int height) {
            if (isChange && gLRender != null) {
                isChange = false;
                gLRender.onSurfaceChanged(width, height);
            }
        }

        private void onDraw() {
            if (gLRender != null && eglHelper != null) {
                gLRender.onDrawFrame();
                if (!isStart) {
                    gLRender.onDrawFrame();
                }
                eglHelper.swapBuffers();

            }
        }

        private void requestRender() {
            if (object != null) {
                synchronized (object) {
                    object.notifyAll();
                }
            }
        }

        public void onDestory() {
            isExit = true;
            requestRender();
        }

        public void release() {
            if (gLRender != null) {
                gLRender.onDeleteTextureId();
            }
            if (eglHelper != null) {
                eglHelper.onDestoryEgl();
                eglHelper = null;
                object = null;
                encoder = null;
            }

        }
    }

    /**
     * 视频编码线程
     */
    static class VideoEncodecThread extends Thread {
        private WeakReference<BasePushEncoder> encoder;
        /**
         * 控制是否退出线程
         */
        private boolean isExit;
        /**
         * codec processes input data to generate output data
         */
        private MediaCodec videoEncodec;
        /**
         * BufferInfo
         */
        private MediaCodec.BufferInfo videoBufferinfo;
        /**
         * pts
         *
         */
        private long pts;
        /**
         * sps 字节数组
         */
        private byte[] sps;
        /**
         * pps字节数组
         */
        private byte[] pps;
        /**
         * 关键帧
         */
        private boolean keyFrame = false;

        private boolean isFirst;

        public VideoEncodecThread(WeakReference<BasePushEncoder> encoder) {
            this.encoder = encoder;
            videoEncodec = encoder.get().videoEncodec;
            videoBufferinfo = encoder.get().videoBufferinfo;
            isFirst = false;
        }

        @Override
        public void run() {
            super.run();
            pts = 0;
            isExit = false;
            // mediacodec 在父类创建的时候调用了configure，所以这里可以start
            videoEncodec.start();
            isFirst = false;
            while (true) {
                if (isExit) {
                    isFirst = false;
                    // 退出要调用stop和release
                    videoEncodec.stop();
                    videoEncodec.release();
                    videoEncodec = null;
                    Log.d("yxt", "录制完成");
                    break;
                }

                int outputBufferIndex = videoEncodec.dequeueOutputBuffer(videoBufferinfo, 0);
                keyFrame = false;
                // The output format has changed, subsequent data will follow the new format.:
                // 输出格式已经改变,后续数据将按照新的格式。
                if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    Log.d("yxt", "INFO_OUTPUT_FORMAT_CHANGED");
                    // 对于H.264来说，"csd-0"和"csd-1"分别对应sps和pps；对于AAC来说，"csd-0"对应ADTS
                    ByteBuffer spsb = videoEncodec.getOutputFormat().getByteBuffer("csd-0");
                    sps = new byte[spsb.remaining()];
                    spsb.get(sps, 0, sps.length);

                    ByteBuffer ppsb = videoEncodec.getOutputFormat().getByteBuffer("csd-1");
                    pps = new byte[ppsb.remaining()];
                    ppsb.get(pps, 0, pps.length);

                    Log.d("yxt", "sps:" + byteToHex(sps));
                    Log.d("yxt", "pps:" + byteToHex(pps));

                } else {
                    // 能拿到buffer
                    if (outputBufferIndex >= 0) {
                        while (outputBufferIndex >= 0) {
                            ByteBuffer outputBuffer = videoEncodec.getOutputBuffers()[outputBufferIndex];
                            // Sets this buffer's position
                            outputBuffer.position(videoBufferinfo.offset);
                          // Sets this buffer's limit.
                            outputBuffer.limit(videoBufferinfo.offset + videoBufferinfo.size);
                            //
                            if (pts == 0) {
                                pts = videoBufferinfo.presentationTimeUs;
                            }
                            // 设置当前pts
                            videoBufferinfo.presentationTimeUs = videoBufferinfo.presentationTimeUs - pts;
                            byte[] data = new byte[0];
                            // 是RTMP协议
                            if (encoder != null && encoder.get() != null && encoder.get().streamType == BasePushEncoder.STREAM_TYPE_RTMP) {
                                data = new byte[outputBuffer.remaining()];
                                // 获取到视频数据
                                outputBuffer.get(data, 0, data.length);
                                Log.d("yxt", "data:" + byteToHex(data));

                            } else if (encoder != null && encoder.get() != null && encoder.get().streamType == BasePushEncoder.STREAM_TYPE_JT1078) {
                                if (outputBuffer.get(5) == -120) {
                                    data = new byte[outputBuffer.remaining() + sps.length + pps.length];
                                    outputBuffer.get(data, sps.length + pps.length, data.length - sps.length - pps.length);
                                    System.arraycopy(sps, 0, data, 0, sps.length);
                                    System.arraycopy(pps, 0, data, sps.length, pps.length);
                                    outputBuffer.position(videoBufferinfo.offset);
                                } else {
                                    data = new byte[outputBuffer.remaining()];
                                    outputBuffer.get(data, 0, data.length);
                                    outputBuffer.position(videoBufferinfo.offset);
                                }
                            }


//                            if (encoder.get().streamType == BasePushEncoder.STREAM_TYPE_RTMP && !isFirst) {
////                                if (videoBufferinfo.flags == MediaCodec.BUFFER_FLAG_KEY_FRAME) {
//                                Log.e("asdfasdf", "1111111");
//                                isFirst = true;
//                                keyFrame = true;
//                                if (encoder.get().onMediaInfoListener != null) {
//                                    encoder.get().onMediaInfoListener.onSPSPPSInfo(sps, pps);
//                                }
////                                }
//                            } else if (encoder.get().streamType == BasePushEncoder.STREAM_TYPE_JT1078) {
//                            if (outputBuffer.get(5) == -120) {
                            // 是否是关键帧
                            if (videoBufferinfo.flags == MediaCodec.BUFFER_FLAG_KEY_FRAME) {
                                keyFrame = true;
                                if (encoder.get().onMediaInfoListener != null) {
                                    encoder.get().onMediaInfoListener.onSPSPPSInfo(sps, pps);
                                }
                            }
//                            }

                            if (encoder != null && encoder.get() != null && encoder.get().onMediaInfoListener != null) {
                                if (encoder.get().streamType == BasePushEncoder.STREAM_TYPE_RTMP) {
                                    // 非关键帧数据
                                    encoder.get().onMediaInfoListener.onVideoInfo(data, keyFrame);
                                } else if (encoder.get().streamType == BasePushEncoder.STREAM_TYPE_JT1078) {
                                    encoder.get().onMediaInfoListener.onVideoSPSPPS(data);
                                }
                                encoder.get().onMediaInfoListener.onMediaTime((int) (videoBufferinfo.presentationTimeUs / 1000000));
                            }
                            // return the buffer to the codec
                            videoEncodec.releaseOutputBuffer(outputBufferIndex, false);
                            // Dequeue an output buffer
                            outputBufferIndex = videoEncodec.dequeueOutputBuffer(videoBufferinfo, 0);
                        }
                    } else {
                        synchronized (encoder.get().videoLock) {
                            try {
                                encoder.get().videoLock.wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }

        public void exit() {
            isExit = true;
        }

    }

    static class AudioEncodecThread extends Thread {

        private WeakReference<BasePushEncoder> encoder;
        private boolean isExit;

        private MediaCodec audioEncodec;
        private MediaCodec.BufferInfo bufferInfo;

        long pts;


        public AudioEncodecThread(WeakReference<BasePushEncoder> encoder) {
            this.encoder = encoder;
            audioEncodec = encoder.get().audioEncodec;
            bufferInfo = encoder.get().audioBufferinfo;
        }

        @Override
        public void run() {
            super.run();
            // 显示时间戳
            pts = 0;
            // 控制线程退出
            isExit = false;
            // After successfully configuring the component, call start.
            audioEncodec.start();
            while (true) {
                if (isExit) {
                    audioEncodec.stop();
                    audioEncodec.release();
                    audioEncodec = null;
                    break;
                }
                // Dequeue an output buffer, block at most "timeoutUs" microseconds.
                // Returns the index of an output buffer that has been successfully decoded
                int outputBufferIndex = audioEncodec.dequeueOutputBuffer(bufferInfo, 0);
                if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    // The output format has changed, subsequent data will follow the new format.
                } else {
                    if (outputBufferIndex >= 0) {
                        while (outputBufferIndex >= 0) {
                            // Retrieve the set of output buffers.
                            ByteBuffer outputBuffer = audioEncodec.getOutputBuffers()[outputBufferIndex];
                            outputBuffer.position(bufferInfo.offset);
                            outputBuffer.limit(bufferInfo.offset + bufferInfo.size);
                            if (pts == 0) {
                                pts = bufferInfo.presentationTimeUs;
                            }
                            bufferInfo.presentationTimeUs = bufferInfo.presentationTimeUs - pts;

                            byte[] data = new byte[outputBuffer.remaining()];
                            outputBuffer.get(data, 0, data.length);
                            if (encoder != null && encoder.get() != null && encoder.get().onMediaInfoListener != null) {
                                encoder.get().onMediaInfoListener.onAudioInfo(data);
                            }

                            audioEncodec.releaseOutputBuffer(outputBufferIndex, false);
                            outputBufferIndex = audioEncodec.dequeueOutputBuffer(bufferInfo, 0);
                        }
                    } else {
                        synchronized (encoder.get().audioLock) {
                            try {
                                encoder.get().audioLock.wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }

            }

        }

        public void exit() {
            isExit = true;
        }
    }

    public interface OnMediaInfoListener {
        void onMediaTime(int times);

        void onSPSPPSInfo(byte[] sps, byte[] pps);

        void onVideoInfo(byte[] data, boolean keyframe);

        void onVideoSPSPPS(byte[] spsppsdata);

        void onAudioInfo(byte[] data);

    }

    /**
     * 此缓冲区的显示时间戳（以微秒为单位）
     *
     * @param size       当前大小
     * @param sampleRate 采样率
     * @return
     */
    private long getAudioPts(int size, int sampleRate) {
        audioPts += (long) (1.0 * size / (sampleRate * 2 * 2) * 1000000.0);
        return audioPts;
    }

    public static String byteToHex(byte[] bytes) {
        StringBuffer stringBuffer = new StringBuffer();
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(bytes[i]);
            if (hex.length() == 1) {
                stringBuffer.append("0" + hex);
            } else {
                stringBuffer.append(hex);
            }
            if (i > 20) {
                break;
            }
        }
        return stringBuffer.toString();
    }

}
