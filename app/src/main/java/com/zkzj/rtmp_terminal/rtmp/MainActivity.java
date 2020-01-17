package com.zkzj.rtmp_terminal.rtmp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.mengpeng.mphelper.ToastUtils;
import com.orhanobut.logger.Logger;
import com.zkzj.rtmp_terminal.BuildConfig;
import com.zkzj.rtmp_terminal.R;
import com.zkzj.rtmp_terminal.TerminalApp;
import com.zkzj.rtmp_terminal.activity.AboutActivity;
import com.zkzj.rtmp_terminal.activity.BaseActivity;
import com.zkzj.rtmp_terminal.event.AudioToSendEvent;
import com.zkzj.rtmp_terminal.event.LocationEvent;
import com.zkzj.rtmp_terminal.event.MqttArrivedMsgEvent;
import com.zkzj.rtmp_terminal.event.MqttToSendEvent;
import com.zkzj.rtmp_terminal.service.ActivityUtil;
import com.zkzj.rtmp_terminal.service.FloatingWindowService;
import com.zkzj.rtmp_terminal.socket.SocketService;
import com.zkzj.rtmp_terminal.utils.AndroidTools;
import com.zkzj.rtmp_terminal.utils.Constant;
import com.zkzj.rtmp_terminal.utils.GPSUtils;
import com.zkzj.rtmp_terminal.utils.LocationService;
import com.zkzj.rtmp_terminal.utils.MqttService;
import com.zkzj.rtmp_terminal.utils.MqttUtil;
import com.zkzj.rtmp_terminal.utils.SPUtils;
import com.zkzj.rtmp_terminal.utils.SysUtils;
import com.zkzj.rtmp_terminal.utils.TipHelp;


import org.easydarwin.audio.AudioObserver;
import org.easydarwin.bus.StreamStat;
import org.easydarwin.bus.SupportResolution;
import org.easydarwin.config.Config;
import org.easydarwin.easypusher.EasyApplication;
import org.easydarwin.easypusher.SettingActivity;
import org.easydarwin.easyrtmp.push.EasyRTMP;
import org.easydarwin.push.InitCallback;
import org.easydarwin.push.MediaStream;
import org.easydarwin.util.Util;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static com.zkzj.rtmp_terminal.utils.Constant.KEY_ENABLE_BACKGROUND_CAMERA;

public class MainActivity extends BaseActivity implements View.OnClickListener,
        TextureView.SurfaceTextureListener, AudioObserver {

    @BindView(R.id.tv_longitude)
    TextView tvLongitude;
    @BindView(R.id.tv_latitude)
    TextView tvLatitude;
    @BindView(R.id.tv_address)
    TextView tvAddress;
    @BindView(R.id.tv_push_status)
    TextView tvPushStatus;
    @BindView(R.id.tv_push_pause)
    TextView tvPushPause;
    @BindView(R.id.tv_text_command)
    TextView tvTextCommand;
    @BindView(R.id.txt_stream_address)
    TextView txtStreamAddress;
    @BindView(R.id.stream_stat)
    TextView streamStat;

    private static final String VALUE = "value";
    private static final int MSG_ARRIVED = 2;
    private static final int CLEAR_MSG = 3;
    private static final int MESSAGE_OVERLAY_PERMISSION = 4;
    public static final int REQUEST_OVERLAY_PERMISSION = 1004;
    private static int qiehuan = 0;

    @BindView(R.id.iv_send)
    ImageView mIvSend;
    TipHelp tipHelp = new TipHelp();

    Handler handler = new Handler();
    @BindView(R.id.about)
    TextView mAbout;

    private Handler updateStatusHandler;
    private Handler testOverlayPermHandler;

    private String video_url;
    public static final int REQUEST_CAMERA_PERMISSION = 1003;
    int width = Config.defaultWidth, height = Config.defaultHeight;

    Spinner spnResolution;
    List<String> listResolution = new ArrayList<String>();
    MediaStream mMediaStream;
    private boolean backgroundBinded;
    private NewBackgroundCameraService mService;
    private ServiceConnection backgroundConn;
    private boolean mNeedGrantedPermission;

    private boolean bgIsStop = false;
    private GPSUtils gpsUtils;
    private Location location;
    private String phones;

    @SuppressLint("NewApi")
    private void startAllService() {
        startService(new Intent(MainActivity.this, MqttService.class));
        startService(new Intent(MainActivity.this, LocationService.class));
        startService(new Intent(MainActivity.this, SocketService.class));
//        startFloatingService();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void startFloatingService() {
        if (ActivityUtil.isServiceWork(this, "com.demon.suspensionbox.FloatingService")) {//防止重复启动
            Toast.makeText(this, "已启动！", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "当前无权限，请授权", Toast.LENGTH_SHORT).show();
            startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())), 0);
        } else {
            startService(new Intent(MainActivity.this, FloatingWindowService.class));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setTheme(R.style.AppTheme);//切换正常主题
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setSystemUiVisibility(getSystemUiVisibility());
        setContentView(R.layout.activity_main);
        EventBus.getDefault().register(this);
        ButterKnife.bind(this);
        //开启GPS
        initLocationGPS();
        /*关于我们*/
        //initAbout();

        //初始化Toast
        ToastUtils.getInstance().initToast(this);
        int i = AndroidTools.ScreenOrient(MainActivity.this);

        if (i == 1) {
            FloatingWindowService.orientation = 1;
        } else {
            FloatingWindowService.orientation = 2;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) !=
                PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) != PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA,
                            Manifest.permission.RECORD_AUDIO}, REQUEST_CAMERA_PERMISSION);
            mNeedGrantedPermission = true;
        }

        String ip = (String) SPUtils.getParam(this, Constant.VIDEO_IP, Constant.VIDEO_IP_DEF);
        String port = (String) SPUtils.getParam(this, Constant.VIDEO_PORT, Constant.VIDEO_PORT_DEF);
        String deviceID = SysUtils.getInstance().getDeviceId();
        video_url = "rtmp://" + ip + ":" + port + "/live/" + deviceID + "_0001";
        Log.e("tag", video_url);
        //附加功能
        initHandler();
        startAllService();
        TerminalApp.setBackground(false);
    }


    private void initAbout() {
        mAbout.setOnClickListener(aboutClick);
    }

    //开启GPS定位
    private void initLocationGPS() {
        gpsUtils = new GPSUtils(MainActivity.this);//初始化GPS
        handler.postDelayed(runnable, 0);
    }

    @Override
    protected void onResume() {
        if (!mNeedGrantedPermission) {
            goonWithPermissionGranted();
        }
        super.onResume();
    }

    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            location = gpsUtils.getLocation();//获取位置信息
            if (location != null) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateView(location);
                    }
                });
                handler.removeCallbacks(runnable);
            } else {
                handler.postDelayed(this, 1000);
            }
        }
    };


    /**
     * 实时更新文本内容
     *
     * @param location
     */
    private void updateView(final Location location) {

        if (location != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.e("tag", "精度" + location.getLongitude());
                    Log.e("tag", "纬度" + location.getLatitude());
                    tvLongitude.setText(String.valueOf(location.getLongitude()));
                    tvLatitude.setText(String.valueOf(location.getLatitude()));
//                    tvAddress.setText(String.valueOf(gpsUtils.getAddressStr()));
                }
            });
        } else {
            Log.e("tag", "地址为null");
        }
    }

    //进行返回键的监听
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (mMediaStream.isStreaming()) {
                ToastUtils.onErrorShowToast("正在推送视频，请稍后再试");
            } else {
                finish();
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onPause() {
        if (!mNeedGrantedPermission) {
            unbindService(backgroundConn);
        }
        boolean isStreaming = mMediaStream != null && mMediaStream.isStreaming();
        if (mMediaStream != null) {
            mMediaStream.stopPreview();
            if (isStreaming && (boolean) SPUtils.getParam(MainActivity.this,
                    KEY_ENABLE_BACKGROUND_CAMERA, false)) {
                mService.activePreview();
                mMediaStream.removeObserver(this);
            } else {
                mMediaStream.stopStream();
                mMediaStream.release();
                mMediaStream = null;
                bgIsStop = stopService(new Intent(this, NewBackgroundCameraService.class));
            }
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }

        if (TerminalApp.isBackground()) {
            return;
        }

        if (updateStatusHandler != null) {
            updateStatusHandler.removeCallbacksAndMessages(null);
        }

        if (testOverlayPermHandler != null) {
            testOverlayPermHandler.removeCallbacksAndMessages(null);
        }

        if (mMediaStream != null && mMediaStream.isStreaming()) {
            mMediaStream.removeObserver(this);
            mMediaStream.stopPreview();
            mMediaStream.destroyCamera();
            mMediaStream.stopStream();
            mMediaStream = null;
        }
        stopService();

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void Event(LocationEvent messageEvent) {
        tvLongitude.setText(String.valueOf(messageEvent.getLon()));
        tvLatitude.setText(String.valueOf(messageEvent.getLat()));
        tvAddress.setText(messageEvent.getAddress());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void Event(MqttArrivedMsgEvent messageEvent) {
        if (messageEvent.getTopic().equals(MqttService.getTopcSelf())) {
            analyzeMqttMsg(messageEvent.getMsg());
        }
    }

    private void analyzeMqttMsg(String message) {
        try {
            JSONObject msgJson = JSONObject.parseObject(message);
            String type = msgJson.getString("type");
            if (type.equals("order")) {
                String tv_command = msgJson.getString("value");
                Message msg = Message.obtain();
                msg.what = MSG_ARRIVED;
                Bundle bundle = new Bundle();
                bundle.putString(VALUE, tv_command);
                msg.setData(bundle);
                updateStatusHandler.sendMessage(msg);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("HandlerLeak")
    private void initHandler() {
        updateStatusHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_STATE:
                        String state = msg.getData().getString(STATE);
                        tvPushStatus.setText(state);
                        break;
                    case MSG_ARRIVED:
                        updateStatusHandler.removeMessages(CLEAR_MSG);
                        String tv_command = msg.getData().getString(VALUE);
                        tvTextCommand.setText(tv_command);
                        updateStatusHandler.sendEmptyMessageDelayed(CLEAR_MSG, 10 * 1000);
                        break;
                    case CLEAR_MSG:
                        tvTextCommand.setText("");
                        break;
                    default:
                        break;
                }
            }
        };

        testOverlayPermHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == MESSAGE_OVERLAY_PERMISSION) {
                    hideLoading();
                    boolean canDraw;
                    int SDK_INT = Build.VERSION.SDK_INT;
                    if (SDK_INT >= Build.VERSION_CODES.M) {
                        canDraw = Settings.canDrawOverlays(MainActivity.this);
                    } else {
                        canDraw = true;
                    }
                    SPUtils.setParam(MainActivity.this, KEY_ENABLE_BACKGROUND_CAMERA, canDraw);
                    testOverlayPermHandler.removeMessages(MESSAGE_OVERLAY_PERMISSION);
                    onBackPressed();
                }
            }
        };
    }

    private void goonWithPermissionGranted() {
        spnResolution = findViewById(R.id.spn_resolution);
        streamStat.setText(null);
        txtStreamAddress.setVisibility(View.INVISIBLE);

        final TextureView surfaceView = findViewById(R.id.sv_surfaceview);

        surfaceView.setSurfaceTextureListener(this);
        surfaceView.setOnClickListener(this);

        Intent intent = new Intent(this, NewBackgroundCameraService.class);
        startService(intent);

        backgroundConn = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                mService = ((NewBackgroundCameraService.LocalBinder) iBinder).getService();
                if (surfaceView.isAvailable()) {
                    goonWithAvailableTexture(surfaceView.getSurfaceTexture());
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
            }
        };
        backgroundBinded = bindService(new Intent(this,
                NewBackgroundCameraService.class), backgroundConn, 0);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CAMERA_PERMISSION: {
                if (grantResults.length > 1
                        && grantResults[0] == PERMISSION_GRANTED &&
                        grantResults[1] == PERMISSION_GRANTED) {
                    mNeedGrantedPermission = false;
                    goonWithPermissionGranted();
                } else {
                    finish();
                }
                break;
            }
        }
    }

    private static final String STATE = "state";
    private static final int MSG_STATE = 1;

    private void sendMessage(String message) {
        Message msg = Message.obtain();
        msg.what = MSG_STATE;
        Bundle bundle = new Bundle();
        bundle.putString(STATE, message);
        msg.setData(bundle);
        updateStatusHandler.sendMessage(msg);
    }

    private void sendMessage(String message, boolean isPushing) {
        Message msg = Message.obtain();
        msg.what = MSG_STATE;
        Bundle bundle = new Bundle();
        bundle.putString(STATE, message);
        msg.setData(bundle);
        updateStatusHandler.sendMessage(msg);

        if (!isPushing) {
            String stopAudio = MqttUtil.getStopAudio(SysUtils.getInstance().getDeviceId());
            EventBus.getDefault().post(new MqttToSendEvent(Constant.MQTT_AUDIO_S, stopAudio));
            Logger.i("send stop Audio " + stopAudio);
        }
    }


    private void initSpninner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                org.easydarwin.easypusher.R.layout.spn_item, listResolution);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnResolution.setAdapter(adapter);

        int position = listResolution.indexOf(String.format("%dx%d", width, height));
        spnResolution.setSelection(position, false);

        spnResolution.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (mMediaStream != null && mMediaStream.isStreaming()) {
                    int pos = listResolution.indexOf(String.format("%dx%d", width, height));

                    if (pos == position)
                        return;

                    spnResolution.setSelection(pos, false);
                    ToastUtils.onErrorShowToast("正在推送中,无法切换分辨率");
                    return;
                }
                String r = listResolution.get(position);
                String[] splitR = r.split("x");

                int wh = Integer.parseInt(splitR[0]);
                int ht = Integer.parseInt(splitR[1]);
                if (width != wh || height != ht) {
                    width = wh;
                    height = ht;

                    if (mMediaStream != null) {
                        mMediaStream.updateResolution(width, height);
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void startCamera() {
        mMediaStream.setDgree(getDgree());
        mMediaStream.setPost(true);
        mMediaStream.createCamera();
        mMediaStream.startPreview();
        if (mMediaStream.isStreaming()) {
            sendMessage(getString(R.string.video_uploading), true);
        }
    }

    private int getDgree() {
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break; // Natural orientation
            case Surface.ROTATION_90:
                degrees = 90;
                break; // Landscape left
            case Surface.ROTATION_180:
                degrees = 180;
                break;// Upside down
            case Surface.ROTATION_270:
                degrees = 270;
                break;// Landscape right
        }
        return degrees;
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.sv_surfaceview) {
            try {
                mMediaStream.getCamera().autoFocus(new Camera.AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean success, Camera camera) {
                        Logger.i("onAutoFocus->success=" + success);
                        if (success) {
                            camera.cancelAutoFocus();
                        } else {
                            initCamera(camera);
                        }
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (i == R.id.btn_switchCamera) {
            mMediaStream.switchCamera();
            tipHelp.Vibrate(this, new long[]{400, 400}, false);
        }
    }

    private void initCamera(Camera camera) {
        Camera.Parameters parameters = camera.getParameters();
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        camera.setParameters(parameters);
        camera.startPreview();
        camera.cancelAutoFocus();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void Event(final StreamStat stat) {
        streamStat.post(new Runnable() {
            @Override
            public void run() {
                streamStat.setText(getString(R.string.stream_stat,
                        stat.bps / 1024));
            }
        });
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void Event(SupportResolution resolution) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                listResolution = Util.getSupportResolution(getApplicationContext());
                boolean supportdefault = listResolution.contains(String.format("%dx%d", width, height));
                if (!supportdefault) {
                    String r = listResolution.get(0);
                    String[] splitR = r.split("x");
                    width = Integer.parseInt(splitR[0]);
                    height = Integer.parseInt(splitR[1]);
                }
                initSpninner();
            }
        });
    }

    private void stopService() {
        stopService(new Intent(MainActivity.this, LocationService.class));
        stopService(new Intent(MainActivity.this, MqttService.class));
        stopService(new Intent(MainActivity.this, SocketService.class));
        stopService(new Intent(MainActivity.this, FloatingWindowService.class));
        if (backgroundBinded && !bgIsStop) {
            unbindService(backgroundConn);
        }
    }

    @Override
    public void onBackPressed() {
        boolean isStreaming = mMediaStream != null && mMediaStream.isStreaming();
        if (isStreaming && (boolean) SPUtils.getParam(MainActivity.this,
                KEY_ENABLE_BACKGROUND_CAMERA, false)) {
            new AlertDialog.Builder(this).setTitle("视频上传")
                    .setMessage("是否需要继续在后台采集并上传视频？")
                    .setNeutralButton("后台采集", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            TerminalApp.setBackground(true);
                            MainActivity.super.onBackPressed();
                        }
                    }).setPositiveButton("不需要", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    mMediaStream.stopStream();
                    MainActivity.super.onBackPressed();
                    Toast.makeText(MainActivity.this, "程序已退出", Toast.LENGTH_SHORT).show();
                }
            }).setNegativeButton(android.R.string.cancel, null).show();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onSurfaceTextureAvailable(final SurfaceTexture surface, int width, int height) {
        if (mService != null) {
            goonWithAvailableTexture(surface);
        }
    }

    private void goonWithAvailableTexture(SurfaceTexture surface) {
        final File easyPusher = new File(Environment.getExternalStorageDirectory() + "/TerminalApp");
        easyPusher.mkdir();
        MediaStream ms = mService.getMediaStream();
        if (ms != null) {// switch from background to front
            ms.stopPreview();
            mService.inActivePreview();
            ms.setSurfaceTexture(surface);
            ms.startPreview();
            mMediaStream = ms;
            if (ms.isStreaming()) {
                if (BuildConfig.DEBUG)
                    txtStreamAddress.setText(video_url);
                sendMessage(getString(R.string.video_uploading), true);
                tvPushPause.setText(getString(R.string.stop_video_uploading));
            }
        } else {
            ms = new MediaStream(getApplicationContext(), surface,
                    PreferenceManager.getDefaultSharedPreferences(this)
                            .getBoolean(EasyApplication.KEY_ENABLE_VIDEO, true));
            ms.setRecordPath(easyPusher.getPath());
            mMediaStream = ms;
            startCamera();
            mService.setMediaStream(ms);
        }
        mMediaStream.addObserver(this);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    public void onClickResolution(View view) {
        findViewById(R.id.spn_resolution).performClick();

    }

    private View.OnClickListener aboutClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            startActivity(new Intent(MainActivity.this, AboutActivity.class));
        }
    };

    public void onSwitchOrientation(View view) {
        if (mMediaStream != null) {
            if (mMediaStream.isStreaming()) {
                ToastUtils.onErrorShowToast("正在推送中,无法更改屏幕方向");
                return;
            }
        }
        int orientation = getRequestedOrientation();
        if (orientation == SCREEN_ORIENTATION_UNSPECIFIED || orientation ==
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    public void onStartOrStopPush(View view) {
        if (!mMediaStream.isStreaming()) {
            mMediaStream.startStream(video_url, new InitCallback() {
                @Override
                public void onCallback(int code) {
                    switch (code) {
                        case EasyRTMP.OnInitPusherCallback.CODE.EASY_ACTIVATE_INVALID_KEY:
                            sendMessage("无效Key");
                            break;
                        case EasyRTMP.OnInitPusherCallback.CODE.EASY_ACTIVATE_SUCCESS:
                            sendMessage("激活成功");
                            break;
                        case EasyRTMP.OnInitPusherCallback.CODE.EASY_RTMP_STATE_CONNECTING:
                            sendMessage("连接中");
                            break;
                        case EasyRTMP.OnInitPusherCallback.CODE.EASY_RTMP_STATE_CONNECTED:
                            sendMessage("连接成功");
                            break;
                        case EasyRTMP.OnInitPusherCallback.CODE.EASY_RTMP_STATE_CONNECT_FAILED:
                            sendMessage("连接失败");
                            break;
                        case EasyRTMP.OnInitPusherCallback.CODE.EASY_RTMP_STATE_CONNECT_ABORT:
                            sendMessage("连接异常中断");
                            break;
                        case EasyRTMP.OnInitPusherCallback.CODE.EASY_RTMP_STATE_PUSHING:
                            sendMessage(getString(R.string.video_uploading), true);
                            break;
                        case EasyRTMP.OnInitPusherCallback.CODE.EASY_RTMP_STATE_DISCONNECTED:
                            sendMessage(getString(R.string.no_video_upload), false);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mIvSend.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.send));
                                }
                            });
                            break;
                        case EasyRTMP.OnInitPusherCallback.CODE.EASY_ACTIVATE_PLATFORM_ERR:
                            sendMessage("平台不匹配");
                            break;
                        case EasyRTMP.OnInitPusherCallback.CODE.EASY_ACTIVATE_COMPANY_ID_LEN_ERR:
                            sendMessage("授权失败");
                            break;
                        case EasyRTMP.OnInitPusherCallback.CODE.EASY_ACTIVATE_PROCESS_NAME_LEN_ERR:
                            sendMessage("进程名称长度不匹配");
                            break;
                    }
                }
            });
            if (BuildConfig.DEBUG)
                txtStreamAddress.setText(video_url);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mIvSend.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.send_is));
                }
            });
            tvPushPause.setText(getString(R.string.stop_video_uploading));
        } else {
            //增加观察者声音回调
            mMediaStream.removeObserver(this);
            mMediaStream.stopStream();
            tvPushPause.setText(getString(R.string.start_video_uploading));
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mIvSend.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.send));
                }
            });
            sendMessage(getString(R.string.no_video_upload), false);
        }
    }

    public void onSetting(View view) {
        startActivity(new Intent(this,
                SettingActivity.class));
    }

    public void onExitOrBackground(View view) {
        //权限申请
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(MainActivity.this)) {
                new AlertDialog.Builder(MainActivity.this).setTitle("后台上传视频")
                        .setMessage("后台上传视频需要应用出现在顶部！是否确定？")
                        .setPositiveButton(android.R.string.ok,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        final Intent intent = new Intent(
                                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                Uri.parse("package:" + getApplicationContext().getPackageName()));
                                        startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION);
                                    }
                                }).setNegativeButton(android.R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                SPUtils.setParam(MainActivity.this, KEY_ENABLE_BACKGROUND_CAMERA,
                                        false);
                                onBackPressed();
                            }
                        }).setCancelable(false).show();
            } else {
                SPUtils.setParam(MainActivity.this, KEY_ENABLE_BACKGROUND_CAMERA, true);
                onBackPressed();
            }
        } else {
            SPUtils.setParam(MainActivity.this, KEY_ENABLE_BACKGROUND_CAMERA, true);
            onBackPressed();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_OVERLAY_PERMISSION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                showLoading("正在加载,请重新启动...");
                testOverlayPermHandler.sendEmptyMessageDelayed(MESSAGE_OVERLAY_PERMISSION, 1000);
            }
        }
        // 判断获取权限是否成功
        if (requestCode == 0) {
            if (!Settings.canDrawOverlays(this)) {
            } else {
                startService(new Intent(MainActivity.this, FloatingWindowService.class));
            }
        }
    }

    @TargetApi(19)
    private static int getSystemUiVisibility() {
        int flags = View.SYSTEM_UI_FLAG_FULLSCREEN;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            flags |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        }
        return flags;
    }

    @Override
    public void audioCallBack(byte[] array, int i) {
        if (!TerminalApp.isBackground()) {
            EventBus.getDefault().post(new AudioToSendEvent(array, i));
        }
    }
}