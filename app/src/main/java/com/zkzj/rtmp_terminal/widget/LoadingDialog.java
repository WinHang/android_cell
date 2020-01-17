package com.zkzj.rtmp_terminal.widget;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.widget.TextView;

import com.zkzj.rtmp_terminal.R;

/**
 * 进度框
 *
 * @author maxuesong
 * @date 2018/3/23 0023
 */

public class LoadingDialog extends ProgressDialog {

    private String mMessage;

    private TextView mTitleTv;


    public LoadingDialog(Context context, String message, boolean canceledOnTouchOutside) {
        super(context, R.style.Theme_Light_LoadingDialog);
        this.mMessage = message;
        // 如果触摸屏幕其它区域,可以选择让这个progressDialog消失或者无变化
        setCanceledOnTouchOutside(canceledOnTouchOutside);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_loading);
        mTitleTv = (TextView) findViewById(R.id.tv_loading_dialog);
        mTitleTv.setText(mMessage);
        setCancelable(false);//不可取消
    }

    public void setTitle(String message) {
        this.mMessage = message;
        mTitleTv.setText(mMessage);
    }


    /**
     * 显示在底部
     */
    public void showBottom() {
        // WindowManager windowManager = ((Activity)
        // mContext).getWindowManager();
        // Display display = windowManager.getDefaultDisplay();
        //
        // WindowManager.LayoutParams lp = getWindow().getAttributes();
        // lp.width = (int) (display.getWidth() * 0.8);
        // getWindow().setAttributes(lp);
        // super.show();
    }

}
