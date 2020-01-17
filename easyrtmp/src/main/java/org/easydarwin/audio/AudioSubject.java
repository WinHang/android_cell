package org.easydarwin.audio;

/**
 * 定义抽象被观察者接口
 * Author:maxuesong
 * Created by Administrator on 2018-11-22.
 */
public interface AudioSubject {

    /**
     * 添加观察者
     * @param observer
     */
    void addObserver(AudioObserver observer);
    /**
     * 移除指定的观察者
     * @param observer
     */
    void removeObserver(AudioObserver observer);
    /**
     * 移除所有的观察者
     */
    void removeAll();

    /**
     * data 是要通知给观察者的数据
     * 因为Object是所有类的父类，可以使用多态，当然 你也可以使用 泛型
     * @param array
     * @param i
     */
    void notifyAllObserver(byte[] array, int i);

}
