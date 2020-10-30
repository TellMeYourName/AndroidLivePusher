package com.yxt.livepusher.egl;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES20;


import com.yxt.livepusher.livepusher.R;
import com.yxt.livepusher.utils.ShaderUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public abstract class BaseRendeer {

    protected Context context;
    protected float[] vertexData = {
            -1f, -1f,
            1f, -1f,
            -1f, 1f,
            1f, 1f,


            -1f, 0.75f,
            1f, 0.75f,
            -1f, 1f,
            1f, 1f,
    };
    protected float[] fragmentData = {
            0f, 1f,
            1f, 1f,
            0f, 0f,
            1f, 0f
    };
    protected FloatBuffer vertexBuffer;
    protected FloatBuffer fragmentBuffer;
    protected int program = -1;
    protected int vPosition;
    protected int fPosition;
    protected int sampler;
    protected int vboId = -1;
    private int bitmapTextureId = -1;
    private Bitmap bitmap;
    int vertexShader = -1;
    int fragmentShader = -1;

    public BaseRendeer(Context context) {
        this.context = context;


//        bitmap = ShaderUtils.createTextImage(15, "#ffffffff", "#4D000000", 5, "50", "沪A123456", "上海市陆家嘴金融中心金茂大厦十楼", "2019-2-5 10:13:31");
        vertexBuffer = ByteBuffer.allocateDirect(vertexData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(vertexData);
        vertexBuffer.position(0);
        fragmentBuffer = ByteBuffer.allocateDirect(fragmentData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(fragmentData);
        fragmentBuffer.position(0);
    }

    public void onCreate() {
        try {
            // 设置透明渲染
            GLES20.glEnable(GLES20.GL_BLEND);
            // 设置BlendFunc，第一个参数为源混合因子，第二个参数为目的混合因子
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
            // 顶点着色器
            String vertexSource = ShaderUtils.getRawResource(context, R.raw.vertex_shader_screen);
            // 片元着色器
            String fragmentSource = ShaderUtils.getRawResource(context, R.raw.fragment_shader_screen);
            int[] id = ShaderUtils.createProgram(vertexSource, fragmentSource);
            if (id != null) {
                vertexShader = id[0];
                fragmentShader = id[1];
                program = id[2];
            }
            // 获取顶点着色器的位置的句柄
            vPosition = GLES20.glGetAttribLocation(program, "v_Position");
            // 获取片元着色器的位置的句柄
            fPosition = GLES20.glGetAttribLocation(program, "f_Position");
            // 获取着色器程序中，指定为uniform类型变量的id
            sampler = GLES20.glGetUniformLocation(program, "sTexture");

            // Vertex Buffer object
            // 不使用VBO时，我们每次绘制（ glDrawArrays ）图形时都是从本地内存处获取顶点数据然后传输给OpenGL来绘制，
            // 这样就会频繁的操作CPU->GPU增大开销，从而降低效率。
            // 使用VBO，我们就能把顶点数据缓存到GPU开辟的一段内存中，然后使用时不必再从本地获取，
            // 而是直接从显存中获取，这样就能提升绘制的效率。
            int[] vbos = new int[1];
            GLES20.glGenBuffers(1, vbos, 0);
            // 1. 创建VBO得到vboId
            vboId = vbos[0];
            // 2. 根据id绑定VBO
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId);
            // 3. 分配VBO需要的缓存大小
            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vertexData.length * 4 + fragmentData.length * 4, null, GLES20.GL_STATIC_DRAW);
            // 4. 为VBO设置顶点数据的值
            GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, 0, vertexData.length * 4, vertexBuffer);
            // 5. 为VBO设置片元数据的值
            GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, vertexData.length * 4, fragmentData.length * 4, fragmentBuffer);
            // 6. 解绑VBO
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onChange(int width, int height) {
        GLES20.glViewport(0, 0, width, height);
    }

    public void onDrawFrame(int textureId) {
        //清屏
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        //  设置背景的颜色
        GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        //使用program
        GLES20.glUseProgram(program);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId);

        //绑定fbo纹理
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        // 启用顶点位置的句柄
        GLES20.glEnableVertexAttribArray(vPosition);
        // 准备坐标数据
        GLES20.glVertexAttribPointer(vPosition, 2, GLES20.GL_FLOAT, false, 8, 0);
        GLES20.glEnableVertexAttribArray(fPosition);
        GLES20.glVertexAttribPointer(fPosition, 2, GLES20.GL_FLOAT, false, 8, vertexData.length * 4);
        // // 绘制三角形
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);


        if (bitmap != null && !bitmap.isRecycled()) {
            if (bitmapTextureId != -1) {
                GLES20.glDeleteTextures(1, new int[]{bitmapTextureId}, 0);
            }

            bitmapTextureId = ShaderUtils.loadBitmapTexture(bitmap);
            if (bitmap != null) {
                bitmap.recycle();
                bitmap = null;
            }
        }
        if (bitmapTextureId != -1) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, bitmapTextureId);
            GLES20.glEnableVertexAttribArray(vPosition);
            GLES20.glVertexAttribPointer(vPosition, 2, GLES20.GL_FLOAT, false, 8, 32);
            GLES20.glEnableVertexAttribArray(fPosition);
            GLES20.glVertexAttribPointer(fPosition, 2, GLES20.GL_FLOAT, false, 8, vertexData.length * 4);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        }
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
    }

    public void onDeleteTextureId() {
        if (bitmapTextureId != -1) {
            GLES20.glDeleteTextures(1, new int[]{bitmapTextureId}, 0);
        }
        if (vboId != -1)
            GLES20.glDeleteBuffers(1, new int[]{vboId}, 0);
        if (program != -1)
            GLES20.glDeleteProgram(program);
        if (vertexShader != -1)
            GLES20.glDeleteShader(vertexShader);
        if (fragmentShader != -1)
            GLES20.glDeleteShader(fragmentShader);
    }

    public void setBitmap(int textSize, String textColor, String bgColor, int padding, String speed, String vehicleLicence, String address, String time) {
        if (bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
            bitmap = null;
        } else {
            bitmap = null;
        }
        bitmap = ShaderUtils.createTextImage(textSize, textColor, bgColor, padding, speed, vehicleLicence, address, time);
    }
}
