package com.clam314.toycamera;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.FrameLayout;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

import static android.content.ContentValues.TAG;

/**
 * Created by clam314 on 2017/4/18
 */

public class CameraUtil {
    public static final int Type_Camera_Front = 1;
    public static final int Type_Camera_Back = 0;


    private volatile static CameraUtil cameraUtil;
    private Camera mCamera;
    private Camera.CameraInfo mCameraInfo;
    private int mCameraId;
    private String savePathDir;
    private TakePhotoListener takePhotoListener;

    public interface TakePhotoListener {
        void onTakePhoto(String info);
    }


    public static CameraUtil getInstance(){
        if(cameraUtil == null){
            synchronized (CameraUtil.class){
                if(cameraUtil == null){
                    cameraUtil = new CameraUtil();
                }
            }
        }
        return cameraUtil;
    }

    public void init(SurfaceHolder holder, String saveDir, int typeCamera){
        try {
            if(TextUtils.isEmpty(saveDir)){
                savePathDir = getDefaultSaveDir();
            }else {
                savePathDir = saveDir;
            }
            if(Camera.getNumberOfCameras() == 0){
                throw new Exception("don't have camera");
            }
            int id;
            if(typeCamera == Type_Camera_Front){
                id = 1;
            }else {
                id = 0;
            }
            mCamera = Camera.open(id);
            mCameraId = id;
            mCameraInfo = new Camera.CameraInfo();
            Camera.getCameraInfo(mCameraId,mCameraInfo);
            mCamera.setPreviewDisplay(holder);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private static String getDefaultSaveDir()throws Exception{
        if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
            String filePath = Environment.getExternalStorageDirectory()+"/ToyCamera";//照片保存路径
            File path = new File(filePath);
            if(!path.exists()){
                path.mkdirs();
            }
            return filePath;
        }else {
            throw new Exception("don't have sdcard");
        }
    }

    public void startPreView(){
        mCamera.startPreview();
    }

    public void release(){
        if(mCamera != null){
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
            mCameraId = 0;
        }
        takePhotoListener = null;
    }

    public void takePicture(TakePhotoListener listener){
        this.takePhotoListener = listener;
        if(mCamera!=null) mCamera.takePicture(null,null,jpegCallback);
    }

    private Camera.PictureCallback jpegCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera Camera) {
            BufferedOutputStream bos = null;
            Bitmap bm = null;
            try {
                // 获得图片
                bm = BitmapFactory.decodeByteArray(data, 0, data.length);
                bm = rotateBitmap(mCameraId, mCameraInfo.orientation,bm);
                if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                    String name = System.currentTimeMillis()+".jpg";
                    File file = new File(savePathDir,name);
                    if (!file.exists()){
                        file.createNewFile();
                    }
                    bos = new BufferedOutputStream(new FileOutputStream(file));
                    bm.compress(Bitmap.CompressFormat.JPEG, 100, bos);//将图片压缩到流中
                    if(takePhotoListener != null)takePhotoListener.onTakePhoto(file.getAbsolutePath());
                }else{
                    if(takePhotoListener != null)takePhotoListener.onTakePhoto("没有检测到内存卡");
                }
            } catch (Exception e) {
                e.printStackTrace();
                if(takePhotoListener != null)takePhotoListener.onTakePhoto("error");
            } finally {
                try {
                    if(bos != null){
                        bos.flush();//输出
                        bos.close();//关闭
                    }
                    if(bm != null){
                        bm.recycle();// 回收bitmap空间
                    }
                    mCamera.stopPreview();// 关闭预览
                    mCamera.startPreview();// 开启预览
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    };


    /**
     *根据surfaceView的比例选择合适的预览和实际拍照的大小，并重新设置surfaceView的大小。这里按FrameLayout的父布局
     *
     * @param surfaceView 预览照片的surfaceView
     * @param width surfaceView的宽度
     * @param height surfaceView的高度
     */
    public void setCameraParams(SurfaceView surfaceView,int width, int height) {
        Log.i(TAG,"setCameraParams  width="+width+"  height="+height);
        Camera.Parameters parameters = mCamera.getParameters();
        // 获取摄像头支持的PictureSize列表
        List<Camera.Size> pictureSizeList = parameters.getSupportedPictureSizes();
        for (Camera.Size size : pictureSizeList) {
            Log.i(TAG, "pictureSizeList size.width=" + size.width + "  size.height=" + size.height);
        }
        /**从列表中选取合适的分辨率*/
        Camera.Size picSize = getProperSize(pictureSizeList, ((float) height / width));
        if (null == picSize) {
            Log.i(TAG, "null == picSize");
            picSize = parameters.getPictureSize();
        }
        Log.i(TAG, "picSize.width=" + picSize.width + "  picSize.height=" + picSize.height);
        // 根据选出的PictureSize重新设置SurfaceView大小
        float w = picSize.width;
        float h = picSize.height;
        parameters.setPictureSize(picSize.width,picSize.height);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams((int) (height*(h/w)), height);
        lp.gravity = Gravity.CENTER;
        surfaceView.setLayoutParams(lp);

        // 获取摄像头支持的PreviewSize列表
        List<Camera.Size> previewSizeList = parameters.getSupportedPreviewSizes();

        for (Camera.Size size : previewSizeList) {
            Log.i(TAG, "previewSizeList size.width=" + size.width + "  size.height=" + size.height);
        }
        Camera.Size preSize = getProperSize(previewSizeList, ((float) height) / width);
        if (null != preSize) {
            Log.i(TAG, "preSize.width=" + preSize.width + "  preSize.height=" + preSize.height);
            parameters.setPreviewSize(preSize.width, preSize.height);
        }

        parameters.setJpegQuality(100); // 设置照片质量
        if (parameters.getSupportedFocusModes().contains(android.hardware.Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            parameters.setFocusMode(android.hardware.Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);// 连续对焦模式
        }

        mCamera.cancelAutoFocus();//自动对焦。
        // 设置PreviewDisplay的方向，效果就是将捕获的画面旋转多少度显示
        setCameraDisplayOrientation(surfaceView.getContext(),mCameraId,mCamera);
        mCamera.setParameters(parameters);
    }

    /**
     *根据比例获取系统支持的接近的分辨率
     */
    private static Camera.Size getProperSize(List<Camera.Size> pictureSizeList, float screenRatio) {
        Log.i(TAG, "getProperSize screenRatio=" + screenRatio);
        Camera.Size result = findProperSize(pictureSizeList,screenRatio);
        if(result == null){
            result = findProperSize(pictureSizeList,4f/3);// 没有找到支持的比例，转而采用默认w:h = 4:3
        }

        Log.i(TAG,"getProperSize result.height:"+result.height+" result.width:"+result.width);
        return result;
    }

    private static Camera.Size findProperSize(List<Camera.Size> list, float condition){
        for(int i = list.size() - 1; i >= 0 ; i--){
            Camera.Size size = list.get(i);
            float ratio = ((float) size.width) / size.height;
            if(ratio == condition){
                return size;
            }
        }
        return null;
    }

    /**
     *将相机返回的照片旋正
     *
     * @param cameraId 相机的id，1一般是前置摄像头
     * @param angle 旋转的角度
     * @param bitmap 相机返回的照片
     */
    public static Bitmap rotateBitmap(int cameraId, int angle, Bitmap bitmap){
        //旋转图片 动作
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        //加入翻转 把前置照相头返回的照片转正
        if (cameraId == 1) {
            matrix.postScale(-1, 1);
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }



    public static void setCameraDisplayOrientation(Context context, int cameraId, Camera camera) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        WindowManager WM = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        int rotation = WM.getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }
}
