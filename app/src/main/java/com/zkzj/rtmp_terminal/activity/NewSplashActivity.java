package com.zkzj.rtmp_terminal.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.RelativeLayout;

import com.gyf.barlibrary.ImmersionBar;
import com.zkzj.rtmp_terminal.R;
import com.zkzj.rtmp_terminal.listeners.DialogUIListener;
import com.zkzj.rtmp_terminal.rtmp.MainActivity;
import com.zkzj.rtmp_terminal.utils.DialogUIUtils;
import com.zkzj.rtmp_terminal.utils.PermissionTools;
import com.zkzj.rtmp_terminal.utils.SysUtils;
import com.zkzj.rtmp_terminal.utils.view.ChangeTextViewSpace;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import pub.devrel.easypermissions.EasyPermissions;

public class NewSplashActivity extends Activity implements EasyPermissions.PermissionCallbacks {

    private static final int STATE_REQ_CODE = 101;

    private static final String[] require_perm = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS,
            Manifest.permission.ACCESS_WIFI_STATE
    };
//    @BindView(R.id.text_tv)
//    ChangeTextViewSpace mTextTv;
    @BindView(R.id.relt)
    RelativeLayout mRelt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);//切换正常主题
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_newsplash);
        ButterKnife.bind(this);
        ImmersionBar.with(this)
                .titleBar(mRelt)
                .init();
//        mTextTv.setSpacing(10);
//        mTextTv.setText("机要交通局");

        int code = SysUtils.getInstance().getSDKCode();
        if (code < Build.VERSION_CODES.M) {
            if (cameraIsAvilable()) {
                initPerms();
            }
        } else {
            initPerms();
        }
    }

    private boolean cameraIsAvilable() {
        if (PermissionTools.isCameraCanUse()) {
            return true;
        } else {
            DialogUIUtils.showMdAlert(NewSplashActivity.this, "设备检测",
                    "请安装摄像头,完成请按确定", new DialogUIListener() {
                        @Override
                        public void onPositive() {
                            if (PermissionTools.isCameraCanUse()) {
                                initPerms();
                            } else {
                                cameraIsAvilable();
                            }
                        }

                        @Override
                        public void onNegative() {
                            cameraIsAvilable();
                        }
                    }).show();

            return false;
        }
    }

    private void initPerms() {
        if (EasyPermissions.hasPermissions(this, require_perm)) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    startActivity(new Intent(NewSplashActivity.this, MainActivity.class));
                    NewSplashActivity.this.finish();
                }
            }, 2000);
        } else {
            EasyPermissions.requestPermissions(this, "为保证功能正常使用我们需要以下权限",
                    STATE_REQ_CODE, require_perm);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {
        startActivity(new Intent(NewSplashActivity.this, MainActivity.class));

        NewSplashActivity.this.finish();
    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        EasyPermissions.requestPermissions(this, "没有获取以上权限应用将无法继续使用",
                STATE_REQ_CODE, require_perm);
    }

//    @OnClick(R.id.text_tv)
//    public void onClick(View v) {
//        switch (v.getId()) {
//            default:
//                break;
//            case R.id.text_tv:
//                break;
//        }
//    }
}