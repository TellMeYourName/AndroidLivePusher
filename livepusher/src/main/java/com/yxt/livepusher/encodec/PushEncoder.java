package com.yxt.livepusher.encodec;

import android.content.Context;

public class PushEncoder extends BasePushEncoder {
    private EncodecRender encodecRender;

    /**
     * 推流编码器
     * @param context 上下文
     * @param textureId 纹理id
     */
    public PushEncoder(Context context, int textureId) {
        super();
        encodecRender = new EncodecRender(context, textureId);
        setRender(encodecRender);
    }
    public EncodecRender getRender(){
        return encodecRender;
    }
}
