package org.easydarwin.audio;

/**
 * 定义抽象观察者接口
 * Author:maxuesong
 * Created by Administrator on 2018-11-22.
 */
public interface AudioObserver {
    void audioCallBack(byte[] array, int i);
}
