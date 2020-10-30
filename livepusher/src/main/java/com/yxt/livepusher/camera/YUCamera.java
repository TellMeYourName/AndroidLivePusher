package com.yxt.livepusher.camera;

import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.Log;

import java.io.IOException;
import java.util.List;

/**
 * 相机类
 */
public class YUCamera {

    /**
     * 纹理id
     */
    private SurfaceTexture surfaceTexture;

    private Camera camera;

    private int width;
    private int height;

    public YUCamera(int width, int height) {
//        Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
//        width = display.getWidth();
//        height = display.getHeight();
        this.width = width;
        this.height = height;
        Log.e("width", width + "  " + height);
    }

    /**
     * 设置相机宽高
     * @param width
     * @param height
     */
    public void setWidthAndHeight(int width, int height) {
        this.width = width;
        this.height = height;
    }

    /**
     * 初始化相机
     * @param surfaceTexture SurfaceTexture
     * @param cameraId 相机的Id 前置还是后置
     */
    public void initCamera(SurfaceTexture surfaceTexture, int cameraId) {
        this.surfaceTexture = surfaceTexture;
        setCameraParme(cameraId);
    }

    private void setCameraParme(int cameraId) {
        try {
            // 打开camera
            camera = Camera.open(cameraId);
            // Sets the SurfaceTexture to be used for live preview
            camera.setPreviewTexture(surfaceTexture);
            // 设置相机参数
            Camera.Parameters parameters = camera.getParameters();
            parameters.setFlashMode("off");
            // YCrCb format used for images, which uses the NV21 encoding format.
            parameters.setPreviewFormat(ImageFormat.NV21);
            // 设置相机尺寸
            Camera.Size size = getFitSize(parameters.getSupportedPictureSizes());
            if (width > height)
                parameters.setPictureSize(size.width, size.height);
            else
                parameters.setPictureSize(size.width, size.height);


            size = getFitSize(parameters.getSupportedPreviewSizes());
            if (width > height)
                parameters.setPreviewSize(size.width, size.height);
            else
                parameters.setPreviewSize(size.width, size.height);
            // 设置相机参数
            camera.setParameters(parameters);
            // 开启预览
            camera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stopPreview() {
        if (camera != null) {
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }

    public void changeCamera(int cameraId) {
        if (camera != null) {
            stopPreview();
        }
        setCameraParme(cameraId);
    }

    /**
     * 调整相机尺寸
     * @param sizes
     * @return
     */
    private Camera.Size getFitSize(List<Camera.Size> sizes) {
        int widtha = width;
        int heightt = height;
        if (widtha < heightt) {
            int t = heightt;
            heightt = widtha;
            widtha = t;
        }
        for (Camera.Size size : sizes) {
            if (1.0f * size.width / size.height == 1.0f * widtha / heightt) {
                return size;
            }
        }
        return sizes.get(0);
    }
}
