package com.zkzj.rtmp_terminal.activity;

import android.support.v7.app.AppCompatActivity;

import com.zkzj.rtmp_terminal.widget.LoadingDialog;

public class BaseActivity extends AppCompatActivity {

    private LoadingDialog mLoadingDialog;

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    public void showLoading(String msg) {
        if (mLoadingDialog == null) {
            mLoadingDialog = new LoadingDialog(this, msg, false);
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
}