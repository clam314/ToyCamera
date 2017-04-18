package com.clam314.toycamera;


import android.app.Activity;
import android.hardware.Camera;
import android.view.Surface;

/**
 * Created by clam314 on 2017/4/18
 */

public class CameraUtil {

    private volatile static CameraUtil cameraUtil;
    private Camera mCamera;

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

    public Camera getCamera(int id){
        releaseCamera();
        try {
            mCamera = Camera.open(id);
        }catch (Exception e){
            e.printStackTrace();
        }
        return mCamera;
    }

    public void releaseCamera(){
        if(mCamera != null){
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    public void setCameraDisplayOrientation(Activity activity, int cameraId, Camera camera) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
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
