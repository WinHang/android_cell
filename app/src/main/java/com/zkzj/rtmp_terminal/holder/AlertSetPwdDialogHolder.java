package com.zkzj.rtmp_terminal.holder;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.zkzj.rtmp_terminal.R;
import com.zkzj.rtmp_terminal.beans.BuildBean;
import com.zkzj.rtmp_terminal.utils.DialogUIUtils;
import com.zkzj.rtmp_terminal.utils.ToolUtils;

public class AlertSetPwdDialogHolder extends SuperHolder {
    protected TextView tvTitle;
    public EditText etOld;
    public EditText et1;
    public EditText et2;
    protected View line;
    protected Button btn1;
    protected View lineBtn2;
    protected Button btn2;

    public AlertSetPwdDialogHolder(Context context) {
        super(context);
    }

    @Override
    protected void findViews() {
        tvTitle = (TextView) rootView.findViewById(R.id.dialogui_tv_title);
        etOld = rootView.findViewById(R.id.et_old);
        et1 = (EditText) rootView.findViewById(R.id.et_1);
        et2 = (EditText) rootView.findViewById(R.id.et_2);
        line = (View) rootView.findViewById(R.id.line);
        btn1 = (Button) rootView.findViewById(R.id.btn_1);
        lineBtn2 = (View) rootView.findViewById(R.id.line_btn2);
        btn2 = (Button) rootView.findViewById(R.id.btn_2);
    }

    @Override
    protected int setLayoutRes() {
        return R.layout.dialogui_holder_alert_set_pwd;
    }

    @Override
    public void assingDatasAndEvents(Context context, final BuildBean bean) {

        //style
        tvTitle.setTextColor(ToolUtils.getColor(tvTitle.getContext(), bean.titleTxtColor));
        tvTitle.setTextSize(bean.titleTxtSize);

        btn2.setTextSize(bean.btnTxtSize);
        btn1.setTextSize(bean.btnTxtSize);

        btn1.setTextColor(ToolUtils.getColor(btn1.getContext(), bean.btn1Color));
        btn2.setTextColor(ToolUtils.getColor(btn1.getContext(), bean.btn2Color));


        //隐藏view
        if (TextUtils.isEmpty(bean.title)) {
            tvTitle.setVisibility(View.GONE);
        } else {
            tvTitle.setVisibility(View.VISIBLE);
            tvTitle.setText(bean.title);
        }

        if (TextUtils.isEmpty(bean.oldPwd)) {
            etOld.setVisibility(View.GONE);
        } else {
            etOld.setVisibility(View.VISIBLE);
            etOld.setHint(bean.oldPwd);

            etOld.setTextColor(ToolUtils.getColor(etOld.getContext(), bean.inputTxtColor));
            etOld.setTextSize(bean.inputTxtSize);
        }

        if (TextUtils.isEmpty(bean.newPwd1)) {
            et1.setVisibility(View.GONE);
        } else {
            et1.setVisibility(View.VISIBLE);
            et1.setHint(bean.newPwd1);

            et1.setTextColor(ToolUtils.getColor(et1.getContext(), bean.inputTxtColor));
            et1.setTextSize(bean.inputTxtSize);
        }

        if (TextUtils.isEmpty(bean.newPwd2)) {
            et2.setVisibility(View.GONE);
        } else {
            et2.setVisibility(View.VISIBLE);
            et2.setHint(bean.newPwd2);
            et2.setTextColor(ToolUtils.getColor(et2.getContext(), bean.inputTxtColor));
            et2.setTextSize(bean.inputTxtSize);
        }

        //按钮数量
        if (TextUtils.isEmpty(bean.text2)) {
            btn2.setVisibility(View.GONE);
            lineBtn2.setVisibility(View.GONE);
            btn1.setBackgroundResource(R.drawable.dialogui_selector_right_bottom);
        } else {
            btn2.setVisibility(View.VISIBLE);
            lineBtn2.setVisibility(View.VISIBLE);
            btn2.setText(bean.text2);
        }
        if (TextUtils.isEmpty(bean.text1)) {
            line.setVisibility(View.GONE);
        } else {
            btn1.setText(bean.text1);
        }

        //事件
        btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DialogUIUtils.dismiss(bean.dialog, bean.alertDialog);
                bean.listener.onPositive();
                bean.listener.onGetPWDInput(etOld.getText().toString().trim(),
                        et1.getText().toString().trim(), et2.getText().toString().trim());
            }
        });

        btn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DialogUIUtils.dismiss(bean.dialog, bean.alertDialog);
                bean.listener.onNegative();
            }
        });
    }
}