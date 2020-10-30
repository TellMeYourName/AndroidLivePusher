package com.yxt.xiaozhenkeji.androidlivepusher;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.yxt.livepusher.test.CameraGLRender;
import com.yxt.livepusher.test.YxtStream;

public class MainActivity extends Activity {
    private static final int PERMISSIONS_REQUEST_CODE = 1;
    YxtStream yxt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //权限申请
        if (!allPermissionsGranted()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(getRequiredPermissions(), PERMISSIONS_REQUEST_CODE);
            }
            return;
        }


        //带预览摄像头
        yxt = new YxtStream((SurfaceView) findViewById(R.id.surfaceView), this);
        //不带预览摄像头
//        yxt = new YxtStream(this);

        //设置前后摄像头
        yxt.setCameraFacing(YxtStream.FACING_FRONT);
        //设置帧率
        yxt.setFps(60);

        WindowManager wm = (WindowManager) this
                .getSystemService(Context.WINDOW_SERVICE);
        int width = wm.getDefaultDisplay().getWidth();
        int height = wm.getDefaultDisplay().getHeight();
        // 设置宽高
        yxt.setWidthAndHeight(320, 480);
        //开始执行
        yxt.star();

        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 开始推流
                yxt.startRtmp(0,"rtmp://10.252.216.144:1935/tv_file");
            }
        });

        //开始录制mp4
//        yxt.startRecord("录制地址+文件名");
        //停止录制
//        yxt.stopRecord();

        //开始推送rtmp
//        yxt.startRtmp(0,"rtmp地址");
        //停止推送rtmp
//        yxt.stopRtmpStream();


        //截图
//        yxt.getCameraRender().requestScreenShot(new CameraGLRender.ScreenShotListener() {
//            @Override
//            public void onBitmapAvailable(Bitmap bitmap) {
//
//            }
//        });


    }

    private String[] getRequiredPermissions() {
        Activity activity = this;
        try {
            PackageInfo info =
                    activity
                            .getPackageManager()
                            .getPackageInfo(activity.getPackageName(), PackageManager.GET_PERMISSIONS);
            String[] ps = info.requestedPermissions;
            if (ps != null && ps.length > 0) {
                return ps;
            } else {
                return new String[0];
            }
        } catch (Exception e) {
            return new String[0];
        }
    }

    private boolean allPermissionsGranted() {
        for (String permission : getRequiredPermissions()) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        yxt.onDestory();
        super.onDestroy();

    }
}
