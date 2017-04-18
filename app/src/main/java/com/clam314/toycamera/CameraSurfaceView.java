package com.clam314.toycamera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Created by clam314 on 2017/4/18
 */

public class CameraSurfaceView extends SurfaceView implements SurfaceHolder.Callback,Camera.AutoFocusCallback {
    private static final String TAG = CameraSurfaceView.class.getSimpleName();
    private SurfaceHolder holder;
    private Camera mCamera;

    private int mScreenWidth;
    private int mScreenHeight;

    private int mWidth;
    private int mHeight;

    public CameraSurfaceView(Context context) {
        this(context,null);
    }

    public CameraSurfaceView(Context context, AttributeSet attrs) {
        this(context,attrs,0);
    }

    public CameraSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        getScreenMetrics(context);
        initView();
    }

    private void getScreenMetrics(Context context) {
        WindowManager WM = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        WM.getDefaultDisplay().getMetrics(outMetrics);
        mScreenWidth = outMetrics.widthPixels;
        mScreenHeight = outMetrics.heightPixels;
    }

    private void initView(){
        holder = getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }


    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mWidth = w;
        mHeight = h;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.i(TAG, "surfaceCreated");
        CameraUtil.getInstance().init(holder, "",CameraUtil.Type_Camera_Front);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.i(TAG,"surfaceChanged");
        CameraUtil.getInstance().setCameraParams(this,mWidth,mHeight);
        CameraUtil.getInstance().startPreView();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.i(TAG, "surfaceDestroyed");
        holder = null;
        CameraUtil.getInstance().release();
    }

    @Override
    public void onAutoFocus(boolean success, Camera camera) {
        if(success){
            Log.i(TAG,"onAutoFocus success");
        }
    }


    public void takePicture(){
        CameraUtil.getInstance().setCameraParams(this,mWidth,mHeight);
        CameraUtil.getInstance().takePicture(new CameraUtil.TakePhotoListener() {
            @Override
            public void onTakePhoto(String info) {
                if("没有检测到内存卡".equals(info)){
                    Toast.makeText(getContext(),"没有检测到内存卡", Toast.LENGTH_SHORT).show();
                }else if(!"error".equals(info)){
                    Toast.makeText(getContext(),"照片已经保存到"+info, Toast.LENGTH_SHORT).show();
                }else {
                    Toast.makeText(getContext(),"发生异常", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
