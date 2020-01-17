package com.zkzj.rtmp_terminal.utils;

/**
 * Author:maxuesong
 * Created by Administrator on 2018/4/3
 */

public class Constant {

//IP更改位置
        /*交通局*/
      public static final String SERVER_IP = "59.255.102.14";//bj

    //  public static final String SERVER_IP = "60.212.3.42";//yantai

        /*公司网络*/
//    public static final String SERVER_IP = "111.198.38.150";//Test

    public static final String DEFAULT_DEVICE_ID = "1234567890";
    public static final String SERVER = "server";
    public static final String MQTT_IP = "MQTT_IP";
    public static final String MQTT_PORT = "MQTT_PORT";
    public static final String VIDEO_IP = "VIDEO_IP";
    public static final String VIDEO_PORT = "VIDEO_PORT";
    public static final String AUDIO_IP = "AUDIO_IP";
    public static final String AUDIO_PORT = "AUDIO_PORT";
    public static final String MQTT_IP_DEFAULT = SERVER_IP;
    public static final String MQTT_PORT_DEFAULT = "1883";
    public static final String AUDIO_IP_DEF = SERVER_IP;
    public static final String AUDIO_PORT_DEF = "14343";
    public static final String VIDEO_IP_DEF = SERVER_IP;
    public static final String VIDEO_PORT_DEF = "10085";

    //订阅的主题
    public static final String MQTT_VIDEO_S = "video/server";
    public static final String MQTT_AUDIO_S = "audio/server";

    public static final boolean auto_login = true;//是否需要选择服务器
    public static final String auto_ip_port = SERVER_IP + ":8083";
//    public static final String auto_ip_port = SERVER_IP + ":80";//test

    public static final String KEY_ENABLE_BACKGROUND_CAMERA = "key_enable_background_camera";

    //播放器Key
    public static final String KEY = "59615A67427036526D3432416A7770656F665677512B4676636D63755A57467A65575268636E64706269356C59584E3563477868655756794C6E4A3062584170567778576F502F44346B566863336C4559584A33615735555A57467453584E55614756435A584E30497A49774D546B355A57467A65513D3D";

}