package com.zkzj.rtmp_terminal.listeners;

import android.app.Activity;
import android.content.Context;
import android.view.View;

import com.zkzj.rtmp_terminal.beans.BuildBean;
import com.zkzj.rtmp_terminal.beans.TieBean;

import java.util.List;

public interface Assignable {

    /**
     * 日期选择器
     */
    BuildBean assignDatePick(Context context, int gravity, String dateTitle, long date, int dateType, int tag, DialogUIDateTimeSaveListener listener);

    /**
     * 加载框
     */
    BuildBean assignLoading(Context context, CharSequence msg, boolean isVertical, boolean cancleable, boolean outsideTouchable, boolean isWhiteBg);

    /**
     * Md加载框
     */
    BuildBean assignMdLoading(Context context, CharSequence msg, boolean isVertical, boolean cancleable, boolean outsideTouchable, boolean isWhiteBg);

    /**
     * md风格弹出框
     */
    BuildBean assignMdAlert(Activity activity, CharSequence title, CharSequence msg, boolean cancleable, boolean outsideTouchable, final DialogUIListener listener);

    /**
     * md风格多选框
     */
    BuildBean assignMdMultiChoose(Activity context, CharSequence title, final CharSequence[] words, final boolean[] checkedItems, boolean cancleable, boolean outsideTouchable,
                                  final DialogUIListener btnListener);

    /**
     * 单选框
     */
    BuildBean assignSingleChoose(Activity context, CharSequence title, final int defaultChosen, final CharSequence[] words, boolean cancleable, boolean outsideTouchable,
                                 final DialogUIItemListener listener);

    /**
     * 提示弹出框
     */
    BuildBean assignAlert(Context activity, CharSequence title, CharSequence msg, CharSequence hint1, CharSequence hint2,
                          CharSequence firstTxt, CharSequence secondTxt, boolean isVertical, boolean cancleable, boolean outsideTouchable, final DialogUIListener listener);

    /**
     * 提示弹出框
     */
    BuildBean assignResetPwdAlert(Activity activity, CharSequence title,
                                  CharSequence oldPwd, CharSequence newPwd1, CharSequence newPwd2,
                                  CharSequence firstTxt, CharSequence secondTxt,
                                  boolean cancleable, boolean outsideTouchable,
                                  DialogUIListener listener);

    /**
     * 中间弹出列表
     */
    BuildBean assignSheet(Context context, List<TieBean> datas, CharSequence bottomTxt, int gravity, boolean cancleable, boolean outsideTouchable, final DialogUIItemListener listener);

    /**
     * md风格弹出列表
     */
    BuildBean assignMdBottomSheet(Context context, boolean isVertical, CharSequence title, List<TieBean> datas, int columnsNum, boolean cancleable, boolean outsideTouchable, DialogUIItemListener listener);

    /**
     * 自定义弹出框
     */
    BuildBean assignCustomAlert(Context context, View contentView, int gravity, boolean cancleable, boolean outsideTouchable);

    /**
     * 自定义底部弹出框
     */
    BuildBean assignCustomBottomAlert(Context context, View contentView, boolean cancleable, boolean outsideTouchable);


}
