package com.zkzj.rtmp_terminal.aac;

public class audioProcess
{

    public static native int create();

    public static native int set_1config(int sample_rate, int delayms);


    public static native int init();


    public static native boolean processStream10msData(byte[] speaker, int len1, byte[] mic, int len2, byte[] out);

    public static native int destroy();
}
