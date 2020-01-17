package org.easydarwin.push;

import android.content.Context;
import android.content.Intent;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Process;
import android.preference.PreferenceManager;
import android.util.Log;

import org.easydarwin.audio.AudioObserver;
import org.easydarwin.audio.AudioStream;
import org.easydarwin.audio.AudioSubject;
import org.easydarwin.bus.SupportResolution;
import org.easydarwin.config.Config;
import org.easydarwin.easypusher.BackgroundCameraService;
import org.easydarwin.easypusher.EasyApplication;
import org.easydarwin.easyrtmp.push.EasyRTMP;
import org.easydarwin.muxer.EasyMuxer;
import org.easydarwin.sw.JNIUtil;
import org.easydarwin.util.Util;
import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import static android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar;
import static android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar;
import static android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar;
import static android.media.MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar;

public class MediaStream implements AudioSubject {
    private static final int SWITCH_CAMERA = 11;
    private final boolean enanleVideo;
    Pusher mEasyPusher;
    static final String TAG = "MediaStream";
    int width = Config.defaultWidth, height = Config.defaultHeight;
    int mCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
    WeakReference<SurfaceTexture> mSurfaceHolderRef;
    Camera mCamera;
    boolean pushStream = false;//是否要推送数据
    AudioStream audioStream;
    private boolean isCameraBack = true;
    private int mDgree;
    private Context mApplicationContext;
    private boolean mSWCodec;
    private VideoConsumer mVC, mRecordVC;
    private EasyMuxer mMuxer;
    private final HandlerThread mCameraThread;
    private final Handler mCameraHandler;
    public static CodecInfo info = new CodecInfo();
    private byte[] i420_buffer;
    private int frameWidth;
    private int frameHeight;
    private Camera.CameraInfo camInfo;
    private Camera.Parameters parameters;

    private boolean isPost = true;

    public void setPost(boolean post) {
        isPost = post;
    }

    public MediaStream(Context context, SurfaceTexture texture, boolean enableVideo) {
        mApplicationContext = context;
        audioStream = AudioStream.getInstance(mApplicationContext);
        mSurfaceHolderRef = new WeakReference(texture);
        mEasyPusher = new EasyRTMP();
        mCameraThread = new HandlerThread("CAMERA") {
            public void run() {
                try {
                    super.run();
                } catch (Throwable e) {
                    e.printStackTrace();
                    Intent intent = new Intent(mApplicationContext, BackgroundCameraService.class);
                    mApplicationContext.stopService(intent);
                } finally {
                    stopStream();
                    stopPreview();
                    destroyCamera();
                }
            }
        };
        mCameraThread.start();
        mCameraHandler = new Handler(mCameraThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

                if (msg.what == SWITCH_CAMERA) {
                    switchCameraTask.run();
                }
            }
        };
        this.enanleVideo = enableVideo;

        if (enableVideo)
            previewCallback = new Camera.PreviewCallback() {

                @Override
                public void onPreviewFrame(byte[] data, Camera camera) {
                    if (data == null) return;
                    //推流视频旋转角度
                    int cameraRotationOffset;//摄像头的挂载方向
                    if (camInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                        cameraRotationOffset = (camInfo.orientation + mDgree) % 360;
                    } else {  // back-facing
                        cameraRotationOffset = (camInfo.orientation - mDgree + 360) % 360;
                    }

                    if (i420_buffer == null || i420_buffer.length != data.length) {
                        i420_buffer = new byte[data.length];
                    }
                    JNIUtil.ConvertToI420(data, i420_buffer, width, height, 0, 0,
                            width, height, cameraRotationOffset % 360, 2);
                    System.arraycopy(i420_buffer, 0, data, 0, data.length);
                    if (mRecordVC != null) {
                        mRecordVC.onVideo(i420_buffer, 0);
                    }
                    mVC.onVideo(data, 0);
                    mCamera.addCallbackBuffer(data);
                }
            };

        audioStream.setAudiolistener(new AudioStream.AudioListener() {
            @Override
            public void callback(byte[] array, int i) {
                notifyAllObserver(array, i);
            }
        });
    }

    public void startStream(String url, InitCallback callback) {
        if (PreferenceManager.getDefaultSharedPreferences(
                EasyApplication.getEasyApplication()).getBoolean(
                EasyApplication.KEY_ENABLE_VIDEO, true))
            mEasyPusher.initPush(url, mApplicationContext, callback);
        else
            mEasyPusher.initPush(url, mApplicationContext, callback, ~0);
        pushStream = true;
    }

    public void startStream(String ip, String port, String id, InitCallback callback) {
        mEasyPusher.initPush(ip, port, String.format("%s.sdp", id), mApplicationContext, callback);
        pushStream = true;
    }

    public void setDgree(int dgree) {
        mDgree = dgree;
    }

    /**
     * 更新分辨率
     */
    public void updateResolution(final int w, final int h) {
        if (mCamera == null)
            return;

        stopPreview();
        destroyCamera();

        mCameraHandler.post(new Runnable() {
            @Override
            public void run() {
                width = w;
                height = h;
            }
        });

        setPost(false);
        createCamera();
        startPreview();
    }

    public static int[] determineMaximumSupportedFramerate(Camera.Parameters parameters) {
        int[] maxFps = new int[]{0, 0};
        List<int[]> supportedFpsRanges = parameters.getSupportedPreviewFpsRange();
        for (Iterator<int[]> it = supportedFpsRanges.iterator(); it.hasNext(); ) {
            int[] interval = it.next();
            if (interval[1] > maxFps[1] || (interval[0] > maxFps[0] && interval[1] == maxFps[1])) {
                maxFps = interval;
            }
        }
        return maxFps;
    }

    public void createCamera() {
        if (Thread.currentThread() != mCameraThread) {
            mCameraHandler.post(new Runnable() {
                @Override
                public void run() {
                    Process.setThreadPriority(Process.THREAD_PRIORITY_DEFAULT);
                    createCamera();
                }
            });
            return;
        }
        if (!enanleVideo) {
            return;
        }
        try {
            mSWCodec = PreferenceManager.getDefaultSharedPreferences(
                    mApplicationContext).getBoolean("key-sw-codec", false);
            mCamera = Camera.open(mCameraId);
            mCamera.setErrorCallback(new Camera.ErrorCallback() {
                @Override
                public void onError(int i, Camera camera) {
                    throw new IllegalStateException("Camera Error:" + i);
                }
            });
            Log.i(TAG, "open Camera");

            parameters = mCamera.getParameters();

            if (Util.getSupportResolution(mApplicationContext).size() == 0) {
                StringBuilder stringBuilder = new StringBuilder();
                List<Camera.Size> supportedPreviewSizes = mCamera.getParameters().getSupportedPreviewSizes();
                for (Camera.Size str : supportedPreviewSizes) {
                    if ((str.width == 1920 && str.height == 1080) || (str.width == 1280 && str.height == 720)) {
                        stringBuilder.append(str.width + "x" + str.height).append(";");
                    }
                }
                Util.saveSupportResolution(mApplicationContext, stringBuilder.toString());
            }
            if (isPost) {
                EventBus.getDefault().post(new SupportResolution());
                isPost = false;
            }

            camInfo = new Camera.CameraInfo();
            Camera.getCameraInfo(mCameraId, camInfo);

            int cameraRotationOffset = camInfo.orientation;
            if (mCameraId == Camera.CameraInfo.CAMERA_FACING_FRONT)
                cameraRotationOffset += 180;
            int rotate = (360 + cameraRotationOffset - mDgree) % 360;

            parameters.setRotation(rotate);

            ArrayList<CodecInfo> infos = listEncoders("video/avc");
            if (!infos.isEmpty()) {
                CodecInfo ci = infos.get(0);
                info.mName = ci.mName;
                info.mColorFormat = ci.mColorFormat;
            } else {
                mSWCodec = true;
            }

            parameters.setPreviewSize(width, height);
            int[] ints = determineMaximumSupportedFramerate(parameters);
            parameters.setPreviewFpsRange(ints[0], ints[1]);

            List<String> supportedFocusModes = parameters.getSupportedFocusModes();

            if (supportedFocusModes == null)
                supportedFocusModes = new ArrayList<>();

            if (supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            } else if (supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            }

            mCamera.setParameters(parameters);
            Log.i(TAG, "setParameters");
            int displayRotation;
            displayRotation = (cameraRotationOffset - mDgree + 360) % 360;
            mCamera.setDisplayOrientation(displayRotation);
            Log.i(TAG, "setDisplayOrientation");
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            destroyCamera();
            e.printStackTrace();
        }
    }

    public synchronized void startRecord() {
        if (Thread.currentThread() != mCameraThread) {
            mCameraHandler.post(new Runnable() {
                @Override
                public void run() {
                    startRecord();
                }
            });
            return;
        }
        if (mCamera == null) {
            return;
        }
        long millis = PreferenceManager.getDefaultSharedPreferences(mApplicationContext).getInt("record_interval", 300000);
        mMuxer = new EasyMuxer(new File(recordPath, new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(new Date())).toString(), millis);
        mRecordVC = new RecordVideoConsumer(mApplicationContext, mMuxer);
        mRecordVC.onVideoStart(frameWidth, frameHeight);
        if (audioStream != null) {
            audioStream.setMuxer(mMuxer);
        }
    }

    public synchronized void stopRecord() {
        if (Thread.currentThread() != mCameraThread) {
            mCameraHandler.post(new Runnable() {
                @Override
                public void run() {
                    stopRecord();
                }
            });
            return;
        }
        if (mRecordVC == null || audioStream == null) {
//            nothing
        } else {
            audioStream.setMuxer(null);
            mRecordVC.onVideoStop();
            mRecordVC = null;
        }
        if (mMuxer != null) mMuxer.release();
        mMuxer = null;
    }

    /**
     * 开启预览
     */
    public synchronized void startPreview() {
        if (Thread.currentThread() != mCameraThread) {
            mCameraHandler.post(new Runnable() {
                @Override
                public void run() {
                    startPreview();
                }
            });
            return;
        }
        if (mCamera != null) {
            int previewFormat = mCamera.getParameters().getPreviewFormat();
            Camera.Size previewSize = mCamera.getParameters().getPreviewSize();
            int size = previewSize.width * previewSize.height * ImageFormat.getBitsPerPixel(previewFormat) / 8;
            width = previewSize.width;
            height = previewSize.height;
            mCamera.addCallbackBuffer(new byte[size]);
            mCamera.addCallbackBuffer(new byte[size]);
            mCamera.setPreviewCallbackWithBuffer(previewCallback);
            Log.i(TAG, "setPreviewCallbackWithBuffer");

            try {
                SurfaceTexture holder = mSurfaceHolderRef.get();
                if (holder != null) {
                    mCamera.setPreviewTexture(holder);
                    Log.i(TAG, "setPreviewTexture");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            mCamera.startPreview();
            Log.i(TAG, "startPreview");
            try {
                mCamera.autoFocus(null);
                //添加自动对焦
                mCamera.cancelAutoFocus();
            } catch (Exception e) {
                //忽略异常
                Log.i(TAG, "auto foucus fail");
            }

            boolean frameRotate;
            int result;

            if (camInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                result = (camInfo.orientation + mDgree) % 360;
            } else {  // back-facing
                result = (camInfo.orientation - mDgree + 360) % 360;
            }

            frameRotate = result % 180 != 0;

            frameWidth = frameRotate ? height : width;
            frameHeight = frameRotate ? width : height;
            if (mSWCodec) {
                mVC = new ClippableVideoConsumer(mApplicationContext, new SWConsumer(
                        mApplicationContext, mEasyPusher), frameWidth, frameHeight);
            } else {
                mVC = new ClippableVideoConsumer(mApplicationContext, new HWConsumer(
                        mApplicationContext, mEasyPusher), frameWidth, frameHeight);
            }
            mVC.onVideoStart(frameWidth, frameHeight);
        }
        audioStream.addPusher(mEasyPusher);
    }

    Camera.PreviewCallback previewCallback;

    /**
     * 停止预览
     */
    public synchronized void stopPreview() {
        if (Thread.currentThread() != mCameraThread) {
            mCameraHandler.post(new Runnable() {
                @Override
                public void run() {
                    stopPreview();
                }
            });
            return;
        }
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.setPreviewCallbackWithBuffer(null);
            Log.i(TAG, "StopPreview");
        }
        if (audioStream != null) {
            audioStream.removePusher(mEasyPusher);
            Log.i(TAG, "Stop AudioStream");
            audioStream.setMuxer(null);
        }
        if (mVC != null) {
            mVC.onVideoStop();

            Log.i(TAG, "Stop VC");
        }
        if (mRecordVC != null) {
            mRecordVC.onVideoStop();
        }

        if (mMuxer != null) {
            mMuxer.release();
            mMuxer = null;
        }
    }

    public Camera getCamera() {
        return mCamera;
    }


    /**
     * 切换前后摄像头
     */
    public void switchCamera() {
        if (mCameraHandler.hasMessages(SWITCH_CAMERA)) return;
        mCameraHandler.sendEmptyMessage(SWITCH_CAMERA);
    }

    private Runnable switchCameraTask = new Runnable() {
        @Override
        public void run() {
            int cameraCount;
            if (isCameraBack) {
                isCameraBack = false;
            } else {
                isCameraBack = true;
            }
            if (!enanleVideo) return;

            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            cameraCount = Camera.getNumberOfCameras();//得到摄像头的个数
            for (int i = 0; i < cameraCount; i++) {
                Camera.getCameraInfo(i, cameraInfo);//得到每一个摄像头的信息
                stopPreview();
                destroyCamera();
                if (mCameraId == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    //现在是后置，变更为前置
                    if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {//代表摄像头的方位，CAMERA_FACING_FRONT前置      CAMERA_FACING_BACK后置
                        mCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
                        createCamera();
                        startPreview();
                        break;
                    }
                } else {
                    //现在是前置， 变更为后置
                    if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {//代表摄像头的方位，CAMERA_FACING_FRONT前置      CAMERA_FACING_BACK后置
                        mCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
                        createCamera();
                        startPreview();
                        break;
                    }
                }
            }
        }
    };

    private String recordPath = Environment.getExternalStorageDirectory().getPath();

    public void setRecordPath(String recordPath) {
        this.recordPath = recordPath;
    }

    /**
     * 销毁Camera
     */
    public synchronized void destroyCamera() {

        if (Thread.currentThread() != mCameraThread) {
            mCameraHandler.post(new Runnable() {
                @Override
                public void run() {
                    destroyCamera();
                }
            });
            return;
        }
        if (mCamera != null) {
            mCamera.stopPreview();
            try {
                mCamera.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
            Log.i(TAG, "release Camera");
            mCamera = null;
        }
        if (mMuxer != null) {
            mMuxer.release();
            mMuxer = null;
        }
    }

    public boolean isStreaming() {
        return pushStream;
    }


    public void stopStream() {
        mEasyPusher.stop();
        pushStream = false;
    }

    public void setSurfaceTexture(SurfaceTexture texture) {
        mSurfaceHolderRef = new WeakReference<SurfaceTexture>(texture);
    }

    public void release() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            mCameraThread.quitSafely();
        } else {
            if (!mCameraHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCameraThread.quit();
                }
            })) {
                mCameraThread.quit();
            }
        }
        try {
            mCameraThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public boolean isRecording() {
        return mMuxer != null;
    }

    public static class CodecInfo {
        public String mName;
        public int mColorFormat;
    }

    public static ArrayList<CodecInfo> listEncoders(String mime) {
        ArrayList<CodecInfo> codecInfos = new ArrayList<CodecInfo>();
        int numCodecs = MediaCodecList.getCodecCount();
        // int colorFormat = 0;
        // String name = null;
        for (int i1 = 0; i1 < numCodecs; i1++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i1);
            if (!codecInfo.isEncoder()) {
                continue;
            }
            if (codecMatch(mime, codecInfo)) {
                String name = codecInfo.getName();
                int colorFormat = getColorFormat(codecInfo, mime);
                if (colorFormat != 0) {
                    CodecInfo ci = new CodecInfo();
                    ci.mName = name;
                    ci.mColorFormat = colorFormat;
                    codecInfos.add(ci);
                }
            }
        }
        return codecInfos;
    }

    public static boolean codecMatch(String mimeType, MediaCodecInfo codecInfo) {
        String[] types = codecInfo.getSupportedTypes();
        for (String type : types) {
            if (type.equalsIgnoreCase(mimeType)) {
                return true;
            }
        }
        return false;
    }

    public static int getColorFormat(MediaCodecInfo codecInfo, String mimeType) {
        MediaCodecInfo.CodecCapabilities capabilities = codecInfo.getCapabilitiesForType(mimeType);
        int[] cf = new int[capabilities.colorFormats.length];
        System.arraycopy(capabilities.colorFormats, 0, cf, 0, cf.length);
        List<Integer> sets = new ArrayList<>();
        for (int i = 0; i < cf.length; i++) {
            sets.add(cf[i]);
        }
        if (sets.contains(COLOR_FormatYUV420SemiPlanar)) {
            return COLOR_FormatYUV420SemiPlanar;
        } else if (sets.contains(COLOR_FormatYUV420Planar)) {
            return COLOR_FormatYUV420Planar;
        } else if (sets.contains(COLOR_FormatYUV420PackedPlanar)) {
            return COLOR_FormatYUV420PackedPlanar;
        } else if (sets.contains(COLOR_TI_FormatYUV420PackedSemiPlanar)) {
            return COLOR_TI_FormatYUV420PackedSemiPlanar;
        }
        return 0;
    }

    List<AudioObserver> mList = new ArrayList<>();

    @Override
    public void addObserver(AudioObserver observer) {
        if (observer == null) {
            throw new NullPointerException("observer == null");
        }

        if (!mList.contains(observer)) {
            mList.add(observer);
        }
    }

    @Override
    public void removeObserver(AudioObserver observer) {
        mList.remove(observer);
    }

    @Override
    public void removeAll() {
        mList.clear();
    }

    @Override
    public void notifyAllObserver(byte[] array, int i) {
        for (AudioObserver observer : mList) {
            observer.audioCallBack(array, i);
        }
    }
}