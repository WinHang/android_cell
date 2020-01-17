package com.zkzj.rtmp_terminal.activity;

import android.app.Dialog;
import android.arch.lifecycle.ProcessLifecycleOwner;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.gyf.barlibrary.ImmersionBar;
import com.orhanobut.logger.Logger;
import com.zkzj.rtmp_terminal.R;
import com.zkzj.rtmp_terminal.TerminalApp;
import com.zkzj.rtmp_terminal.listeners.DialogUIListener;
import com.zkzj.rtmp_terminal.rtmp.MainActivity;
import com.zkzj.rtmp_terminal.utils.Base64Utils;
import com.zkzj.rtmp_terminal.utils.DialogUIUtils;
import com.zkzj.rtmp_terminal.utils.HideIMEUtil;
import com.zkzj.rtmp_terminal.utils.SPUtils;
import com.zkzj.rtmp_terminal.utils.SysUtils;
import com.zkzj.rtmp_terminal.widget.LoadingDialog;

import java.util.Timer;
import java.util.TimerTask;

public class LoginActivity extends BaseActivity implements View.OnClickListener,
        CompoundButton.OnCheckedChangeListener {

    private EditText et_name;
    private EditText et_password;
    private Button mLoginBtn;
    private CheckBox checkBox_password;
    private ImageView iv_see_password;
    private ImageView user_delete;
    private TextView setPwd;

    private LoadingDialog mLoadingDialog;
    private int initfirst=0;
    private Dialog saveDialog;
    private ImageView mLogoImage;
    private ScrollView mScroll;
    private PopupWindow popupWindow3;
    private String phone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        initViews();
        SharedPreferences sp = getSharedPreferences("mima", 0);
        boolean flag = sp.getBoolean("flag", false);
        if (flag == true) {
            String phones = sp.getString("phone", "");
            SysUtils.getInstance().phone_sys=phones;
            if (phones.length() == 11) {
                initfirst=1;
            } else {
                initFirst();
                initfirst=0;
            }
        }

        ImmersionBar.with(this)
                .statusBarDarkFont(true)   //状态栏字体是深色，不写默认为亮色
                .fullScreen(true)
                .titleBar(mScroll)
                .init();

        HideIMEUtil.wrap(this);//键盘管理，点击除editText外区域收起键盘

        if (TerminalApp.isBackground()) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
        setupEvents();
        initData();
    }

    private void initFirst() {
        Window window = getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.alpha = 0.5f;

        window.setAttributes(lp);
        View inflate3 = LayoutInflater.from(LoginActivity.this).inflate(R.layout.pop_phone, null);
        Button btn_dl = inflate3.findViewById(R.id.btn_dl);
        EditText et_phone = inflate3.findViewById(R.id.et_phone);
        openKeyboard();

        btn_dl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popupWindow3.dismiss();
                phone = et_phone.getText().toString();
                if (phone.length() == 11) {
                    login();
                    SysUtils.getInstance().phone_sys=phone;
                    initfirst=1;
                    SharedPreferences sp = getSharedPreferences ("mima", 0);
                    SharedPreferences.Editor edit = sp.edit ();
                    boolean clickable = btn_dl.isClickable ();
                    if (clickable == true) {
                        edit.putString ("phone", phone);
                        edit.putBoolean ("flag", true);
                        edit.commit ();
                    }
                } else {
                    Toast.makeText(LoginActivity.this, "请输入正确的手机号", Toast.LENGTH_SHORT).show();
                    /**隐藏软键盘**/
                    View view = getWindow().peekDecorView();
                    if (view != null) {
                        InputMethodManager inputmanger = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        inputmanger.hideSoftInputFromWindow(view.getWindowToken(), 0);
                    }
                }
            }
        });


        popupWindow3 = new PopupWindow(inflate3, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        popupWindow3.setBackgroundDrawable(new BitmapDrawable());
        popupWindow3.setOutsideTouchable(false);
        popupWindow3.setFocusable(true);

         /*
             2.先创建动画的style 样式,去使用进出场动画

          */
        popupWindow3.setAnimationStyle(R.style.popAnimation);

        popupWindow3.showAtLocation(et_password, Gravity.CENTER, 0, 0);
        popupWindow3.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                Window window = getWindow();
                WindowManager.LayoutParams lp = window.getAttributes();
                lp.alpha = 1.0f;
                window.setAttributes(lp);
            }
        });
    }

    private void openKeyboard() {

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                InputMethodManager imm = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);

            }
        }, 500);
    }

    private void initData() {
        if (firstLogin()) {
            checkBox_password.setChecked(false);
        }

        if (remenberPassword()) {
            checkBox_password.setChecked(true);
            setTextNameAndPassword();
        } else {
            setTextName();
        }
        //Logo的进场动画
        Animation anim = AnimationUtils.loadAnimation(this, R.anim.alpha_img);
        mLogoImage.startAnimation(anim);
    }

    public void setTextNameAndPassword() {
        et_name.setText("" + getLocalName());
        et_password.setText("" + getLocalPassword());
    }

    public void setTextName() {
        et_name.setText("" + getLocalName());
    }

    public String getLocalName() {
        String name = (String) SPUtils.getParam(this, "name", "zkzj");
        return name;
    }

    public String getLocalPassword() {
        String password = (String) SPUtils.getParam(this, "password", "");
        return Base64Utils.decode(password);
    }

    private boolean remenberPassword() {
        boolean remenberPassword = (boolean) SPUtils.getParam(this, "remenberPassword",
                false);
        return remenberPassword;
    }

    private void initViews() {
        mLoginBtn = (Button) findViewById(R.id.btn_login);
        et_name = (EditText) findViewById(R.id.et_account);
        et_password = (EditText) findViewById(R.id.et_password);
        checkBox_password = (CheckBox) findViewById(R.id.checkBox_password);
        setPwd = findViewById(R.id.tv_set_pwd);
        iv_see_password = (ImageView) findViewById(R.id.iv_see_password);
        mLogoImage = (ImageView) findViewById(R.id.logo_image);
        mScroll = (ScrollView) findViewById(R.id.scroll);
        user_delete = (ImageView) findViewById(R.id.user_delete);
    }

    private void setupEvents() {
        mLoginBtn.setOnClickListener(this);
        checkBox_password.setOnCheckedChangeListener(this);
        setPwd.setOnClickListener(this);
        iv_see_password.setOnClickListener(this);
        user_delete.setOnClickListener(this);
        /*一键删除功能实现*/
        et_name.addTextChangedListener(textWatcher);
        et_name.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

                //回车时返回true拦截事件，不让换行
                if (event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                    if (TextUtils.isEmpty(et_name.getText().toString().trim())) {
                        Animation shake = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.shake);
                        et_name.startAnimation(shake);
                        showToast("请先输入内容！");

                    } else {//不为空时才添加标签
                        if (TextUtils.isEmpty(et_password.getText().toString().trim())) {
                            Animation shake = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.shake);
                            et_password.startAnimation(shake);
                            showToast("请先输入密码！");
                        } else {
                            login();
                            finish();
                        }
                    }
                    return true;
                }
                return false;
            }
        });
    }

    private boolean firstLogin() {
        boolean first = (boolean) SPUtils.getParam(this, "first", true);
        if (first) {
            SPUtils.setParam(this, "remenberPassword", false);
            SPUtils.setParam(this, "autoLogin", false);
            SPUtils.setParam(this, "name", "zkzj");
            SPUtils.setParam(this, "password", Base64Utils.encode("123456"));
            SPUtils.setParam(this, "first", false);
            return true;
        }
        return false;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_login:
                loadUserName();
                if (initfirst==0){
                    initFirst();
                }else {
                    login();
                }
                break;
            case R.id.iv_see_password:
                setPasswordVisibility();
                break;
            case R.id.user_delete:
                deleteuser();
                break;
            case R.id.tv_set_pwd:
                DialogUIUtils.showAlertResetPwd(LoginActivity.this, "密码重置",
                        "请输入旧密码", "请输入新密码",
                        "再次输入新密码", "重置", "取消",
                        false, false, new DialogUIListener() {
                            @Override
                            public void onPositive() {
                                saveDialog = DialogUIUtils.showLoading(LoginActivity.this,
                                        "密码重置中...",
                                        false, true, true,
                                        true).show();
                            }

                            @Override
                            public void onNegative() {
                                Logger.i("onNegative");
                            }

                            @Override
                            public void onGetPWDInput(CharSequence inputOld, CharSequence input1,
                                                      CharSequence input2) {
                                super.onGetPWDInput(inputOld, input1, input2);
                                String oldPwd = String.valueOf(inputOld).trim();
                                String newPwd1 = String.valueOf(input1).trim();
                                String newPwd2 = String.valueOf(input2).trim();

                                if (!TextUtils.isEmpty(oldPwd) &&
                                        Base64Utils.encode(oldPwd).equals(SPUtils.getParam(
                                                LoginActivity.this, "password", ""))) {
                                    if (!TextUtils.isEmpty(newPwd1) && !TextUtils.isEmpty(newPwd2)
                                            && newPwd1.equals(newPwd2)) {
                                        SPUtils.setParam(LoginActivity.this, "password",
                                                Base64Utils.encode(newPwd1));
                                        DialogUIUtils.showToast("重置成功");
                                    } else {
                                        DialogUIUtils.showToast("两次新密码输入不一致,请重新设置");
                                    }
                                } else {
                                    DialogUIUtils.showToast("原密码输入不正确");
                                }
                                if (saveDialog != null && saveDialog.isShowing()) {
                                    saveDialog.dismiss();
                                }
                            }
                        }).show();
                break;
            default:
                break;
        }
    }

    private TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            if (et_name.getEditableText().length() >= 1) {
                user_delete.setVisibility(View.VISIBLE);
            } else {
                user_delete.setVisibility(View.GONE);
            }
        }
    };

    private void deleteuser() {
        et_name.setText("");
    }

    private void login() {
        if (getAccount().isEmpty()) {
            Animation shake = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.shake);
            et_name.startAnimation(shake);
            showToast("你输入的账号为空！");
            return;
        }

        if (getPassword().isEmpty()) {
            Animation shake = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.shake);
            et_password.startAnimation(shake);
            showToast("你输入的密码为空！");
            return;
        }


        showLoading();
        Thread loginRunnable = new Thread() {
            @Override
            public void run() {
                super.run();
                setLoginBtnClickable(false);

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                String name = getAccount();
                String oldName = (String) SPUtils.getParam(LoginActivity.this, "name", "");
                String pwd = Base64Utils.encode(getPassword());
                String oldPwd = (String) SPUtils.getParam(LoginActivity.this, "password", "");
                if (pwd.equals(oldPwd)) {
                    loadCheckBoxState();
                    startActivity(new Intent(LoginActivity.this, SelectServiceActivity.class));
                    finish();
                } else {
                    showToast("输入的登录账号或密码不正确");
                }
                setLoginBtnClickable(true);
                hideLoading();
            }
        };
        loginRunnable.start();
    }


    public void loadUserName() {
        if (!getAccount().equals("") || !getAccount().equals("请输入登录账号")) {
            SPUtils.setParam(this, "name", getAccount());
        }
    }

    private void setPasswordVisibility() {
        if (iv_see_password.isSelected()) {
            iv_see_password.setSelected(false);
            et_password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        } else {
            iv_see_password.setSelected(true);
            et_password.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        }
    }

    public String getAccount() {
        return et_name.getText().toString().trim();//去掉空格
    }

    public String getPassword() {
        return et_password.getText().toString().trim();//去掉空格
    }

    private void loadCheckBoxState() {
        loadCheckBoxState(checkBox_password, null);
    }

    public void loadCheckBoxState(CheckBox checkBox_password, CheckBox checkBox_login) {
        if (!checkBox_password.isChecked()) {
            SPUtils.setParam(this, "remenberPassword", false);
            SPUtils.setParam(this, "autoLogin", false);
        } else if (checkBox_password.isChecked()) {
            SPUtils.setParam(this, "remenberPassword", true);
            SPUtils.setParam(this, "autoLogin", false);
            SPUtils.setParam(this, "password", Base64Utils.encode(getPassword()));
        }
    }

    public void setLoginBtnClickable(boolean clickable) {
        mLoginBtn.setClickable(clickable);
    }

    public void showLoading() {
        if (mLoadingDialog == null) {
            mLoadingDialog = new LoadingDialog(this, getString(R.string.loading), false);
        }
        mLoadingDialog.show();
    }

    public void hideLoading() {
        if (mLoadingDialog != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mLoadingDialog.hide();
                }
            });
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
    }

    @Override
    public void onBackPressed() {
        if (mLoadingDialog != null) {
            if (mLoadingDialog.isShowing()) {
                mLoadingDialog.cancel();
            } else {
                finish();
            }
        } else {
            finish();
        }
    }

    protected void onDestroy() {
        if (mLoadingDialog != null) {
            mLoadingDialog.cancel();
            mLoadingDialog = null;
        }
        super.onDestroy();
    }

    public void showToast(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(LoginActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initView() {
        mLogoImage = (ImageView) findViewById(R.id.logo_image);
        mScroll = (ScrollView) findViewById(R.id.scroll);
    }
}