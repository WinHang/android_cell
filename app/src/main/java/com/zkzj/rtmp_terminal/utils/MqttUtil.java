package com.zkzj.rtmp_terminal.utils;

/**
 * 类Util的实现描述
 */
public class MqttUtil {

    public static String getHeartBeatOnline(String deviceID) {
        return "{\"type\":\"heartbeat\",\"online\":true," + "\"deviceCode\":\"" + deviceID + "\"}";
    }

    public static String getStopAudio(String deviceID) {
        return "{\"type\":\"stop\"," + "\"user\":\"" + deviceID + "\"}";
    }

    public static String getGpsInfo(double lat, double lon, String deviceID) {
        return "{\"type\":\"gps\",\"miei\":\"" + deviceID + "\",\"lat\":\"" + lat + "\",\"lon\":\"" + lon + "\"}";
    }
}