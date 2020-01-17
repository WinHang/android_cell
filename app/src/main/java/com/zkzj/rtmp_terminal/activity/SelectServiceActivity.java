package com.zkzj.rtmp_terminal.activity;

import android.Manifest;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import com.alibaba.fastjson.JSONObject;
import com.orhanobut.logger.Logger;
import com.zkzj.rtmp_terminal.R;
import com.zkzj.rtmp_terminal.beans.ServerBean;
import com.zkzj.rtmp_terminal.encrypt.AESUtils;
import com.zkzj.rtmp_terminal.listeners.DialogUIListener;
import com.zkzj.rtmp_terminal.rtmp.MainActivity;
import com.zkzj.rtmp_terminal.utils.Constant;
import com.zkzj.rtmp_terminal.utils.DialogUIUtils;
import com.zkzj.rtmp_terminal.utils.OkHttpManager;
import com.zkzj.rtmp_terminal.utils.SPUtils;
import com.zkzj.rtmp_terminal.utils.SysUtils;
import com.zkzj.rtmp_terminal.utils.ToolUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import okhttp3.Call;
import okhttp3.Response;
import pub.devrel.easypermissions.EasyPermissions;

import static com.zkzj.rtmp_terminal.encrypt.AESUtils.AES_KEY;
import static com.zkzj.rtmp_terminal.utils.Constant.KEY_ENABLE_BACKGROUND_CAMERA;

public class SelectServiceActivity extends BaseActivity implements EasyPermissions.PermissionCallbacks {

    @BindView(R.id.sp_services)
    Spinner spServices;
    @BindView(R.id.btn_add)
    Button btnAdd;
    @BindView(R.id.btn_delete)
    Button btnDelete;
    @BindView(R.id.btn_login)
    Button btnLogin;

    private Map<String, Object> serverMap;
    private Dialog saveDialog;
    private ArrayList<String> nameList;
    private ArrayAdapter<String> mAdapter;
    private String currentName;
    public static final int REQUEST_OVERLAY_PERMISSION = 1004;

    private static final String[] read_state_perm = {
            Manifest.permission.READ_PHONE_STATE
    };

    private static final int STATE_REQ_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_service);
        ButterKnife.bind(this);

        boolean netWorking = ToolUtils.isNetworkAvailable(this);
        if (netWorking) {
            initPerm();
        } else {
            DialogUIUtils.showToast("请检查网络是否可用");
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }



    private void initPerm() {
        if (EasyPermissions.hasPermissions(this, read_state_perm)) {
            if (Constant.auto_login) {
                login(true);
            } else {
                initEvent();
                initData();
            }
        } else {
            EasyPermissions.requestPermissions(this, "为验证终端是否合法需要获取设备信息的权限",
                    STATE_REQ_CODE, read_state_perm);
        }
    }

    private void initData() {
        serverMap = SPUtils.getServersFromShare(SelectServiceActivity.this);
        if (serverMap == null) {
            serverMap = new HashMap<>();
        }

        if (nameList == null) {
            nameList = new ArrayList<>();
        } else {
            nameList.clear();
        }

        for (Map.Entry<String, Object> entry : serverMap.entrySet()) {
            nameList.add(entry.getKey());
        }
        mAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, nameList);
        spServices.setAdapter(mAdapter);
    }

    private void updateSp() {
        serverMap = SPUtils.getServersFromShare(SelectServiceActivity.this);
        if (serverMap == null) {
            serverMap = new HashMap<>();
        }

        if (nameList == null) {
            nameList = new ArrayList<>();
        } else {
            nameList.clear();
        }

        for (Map.Entry<String, Object> entry : serverMap.entrySet()) {
            nameList.add(entry.getKey());
        }
        mAdapter.notifyDataSetChanged();
    }

    private void initEvent() {
        spServices.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Logger.i(nameList.get(position) + " position=" + position + " id=" + id);
                Logger.i(serverMap.get(nameList.get(position)).toString());
                currentName = nameList.get(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                Logger.i("onNothingSelected");
            }
        });
    }

    @OnClick({R.id.btn_add, R.id.btn_delete, R.id.btn_login})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.btn_add:
                DialogUIUtils.showAlert(this, "新增服务器", "", "服务器名称",
                        "IP地址:端口", "保存", "取消", false,
                        true, true, new DialogUIListener() {
                            @Override
                            public void onPositive() {
                                saveDialog = DialogUIUtils.showLoading(SelectServiceActivity.this,
                                        "保存中...",
                                        false, true, true,
                                        true).show();
                            }

                            @Override
                            public void onNegative() {
                                Logger.i("onNegative");
                            }

                            @Override
                            public void onGetInput(CharSequence input1, CharSequence input2) {
                                super.onGetInput(input1, input2);
                                // TODO: 2018/3/30  保存并验证信息时候正确
                                String name = String.valueOf(input1);
                                String address = String.valueOf(input2);
                                try {
                                    if (!TextUtils.isEmpty(name) || !TextUtils.isEmpty(address)) {
                                        String[] array = address.split(":|：");
                                        ServerBean bean = new ServerBean(name, array[0], array[1]);
                                        if (serverMap == null || serverMap.size() == 0) {
                                            serverMap = new HashMap<String, Object>();
                                        } else {
                                            if (serverMap.containsKey(name)) {
                                                serverMap.remove(name);
                                            }
                                        }
                                        serverMap.put(name, bean);
                                        SPUtils.setServersToShare(SelectServiceActivity.this, serverMap);
                                        DialogUIUtils.showToast("保存成功");
                                        updateSp();
                                    } else {
                                        DialogUIUtils.showToast("服务器信息不正确请重新录入");
                                    }
                                    if (saveDialog != null && saveDialog.isShowing()) {
                                        saveDialog.dismiss();
                                    }
                                } catch (Exception e) {
                                    if (saveDialog != null && saveDialog.isShowing()) {
                                        saveDialog.dismiss();
                                    }
                                    DialogUIUtils.showToast("请检查IP:端口号的格式是否正确");
                                }
                            }
                        }).show();
                break;
            case R.id.btn_delete:
                if (TextUtils.isEmpty(currentName)) {
                    DialogUIUtils.showToastCenter("服务器信息为空");
                } else {
                    DialogUIUtils.showMdAlert(SelectServiceActivity.this, "删除",
                            "是否删除" + currentName + "服务器的信息？", new DialogUIListener() {
                                @Override
                                public void onPositive() {
                                    serverMap.remove(currentName);
                                    SPUtils.setServersToShare(SelectServiceActivity.this, serverMap);
                                    updateSp();
                                }

                                @Override
                                public void onNegative() {

                                }

                            }).show();
                }
                break;
            case R.id.btn_login:
                login(false);
                break;
        }
    }

    private void login(boolean isAuto) {
        Map<String, String> map = new HashMap<>();
        String request = "{\"IMEI\":\"" + SysUtils.getInstance().getDeviceId() + "\"}";
        Logger.i("login api request:" + request);
        try {
            map.put("request", AESUtils.encrypt(AES_KEY, request));//登录只需要IMEI
        } catch (Exception e) {
            e.printStackTrace();
        }
        String URL;
        if (isAuto) {
            URL = "http://" + Constant.auto_ip_port + "/video/mobile/login.do";
        } else {
            JSONObject server = JSONObject.parseObject(String.valueOf(serverMap.get(currentName)));
            URL = "http://" + server.getString("ip") + ":" + server.getString("port")
                    + "/video/mobile/login.do";
        }

        OkHttpManager.getInstance().postAsyn(URL, new OkHttpManager.ResultCallback() {
            @Override
            public void onError(Call call, Exception e) {
                Logger.e(call.request().toString());
                DialogUIUtils.showToastCenter("登录失败1" + call.request().toString());
                startActivity(new Intent(SelectServiceActivity.this, LoginActivity.class));
                finish();
            }

            @Override
            public void onResponse(Call call, Response response) {
                String responseResult;
                try {
                    responseResult = AESUtils.decrypt(AES_KEY, (response.body().string()));
                    Logger.i("login api result" + responseResult);
                    JSONObject json = JSONObject.parseObject(responseResult);
                    int rc = json.getInteger("rc");
                    if (rc == 0) {
                        JSONObject jsonContent = JSONObject.parseObject(json.getString("content"));
                        String audio_ip = jsonContent.getString("audio_ip");
                        String audio_port = jsonContent.getString("audio_port");
                        String mqtt_ip = jsonContent.getString("mqtt_ip");
                        String mqtt_port = jsonContent.getString("mqtt_port");
                        String video_ip = jsonContent.getString("video_ip");
                        String video_port = jsonContent.getString("video_port");

                        SPUtils.setParam(SelectServiceActivity.this, Constant.AUDIO_IP, audio_ip);
                        SPUtils.setParam(SelectServiceActivity.this, Constant.AUDIO_PORT, audio_port);
                        SPUtils.setParam(SelectServiceActivity.this, Constant.MQTT_IP, mqtt_ip);
                        SPUtils.setParam(SelectServiceActivity.this, Constant.MQTT_PORT, mqtt_port);
                        SPUtils.setParam(SelectServiceActivity.this, Constant.VIDEO_IP, video_ip);
                        SPUtils.setParam(SelectServiceActivity.this, Constant.VIDEO_PORT, video_port);

                        startActivity(new Intent(SelectServiceActivity.this,
                                NewSplashActivity.class));
                        finish();
                    } else {
                        String error_msg = "登录失败2";
                        if (json.containsKey("msg")) {
                            error_msg = json.getString("msg");
                        }
                        DialogUIUtils.showToastCenter(error_msg);
                        startActivity(new Intent(SelectServiceActivity.this, LoginActivity.class));
                        finish();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    String error_msg = "登录失败3";
                    DialogUIUtils.showToastCenter(error_msg + e.getMessage());
                    startActivity(new Intent(SelectServiceActivity.this, LoginActivity.class));
                    finish();
                }
            }
        }, map);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {
        if (Constant.auto_login) {
            login(true);
        } else {
            initEvent();
            initData();
        }
    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        EasyPermissions.requestPermissions(this, "没有获取设备信息权限应用将无法继续使用",
                STATE_REQ_CODE, read_state_perm);
    }
}