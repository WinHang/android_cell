package com.zkzj.rtmp_terminal.utils;

/**
 * Author:maxuesong
 * Created by Administrator on 2018/7/27 0027.
 */
public class ByteUtil {

    //System.arraycopy()方法
    public static byte[] byteMerger(byte[] bt1, byte[] bt2) {
        byte[] bt3 = new byte[bt1.length + bt2.length];
        System.arraycopy(bt1, 0, bt3, 0, bt1.length);
        System.arraycopy(bt2, 0, bt3, bt1.length, bt2.length);
        return bt3;
    }

    public static byte[] byteMergerAudio(byte[] bt1, byte[] bt2, byte[] bt3, int audio_length) {
        byte[] bt4 = new byte[bt1.length + bt2.length + audio_length];
        System.arraycopy(bt1, 0, bt4, 0, bt1.length);
        System.arraycopy(bt2, 0, bt4, bt1.length, bt2.length);
        System.arraycopy(bt3, 0, bt4, bt1.length + bt2.length, audio_length);
        return bt4;
    }

    public static byte[] byteArrayCut(byte[] array, int length, int start) {
        byte[] result = new byte[length - start];
        System.arraycopy(array, start, result, 0, length - start);
        return result;
    }
}