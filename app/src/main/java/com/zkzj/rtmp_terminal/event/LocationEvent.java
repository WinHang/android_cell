package com.zkzj.rtmp_terminal.event;

/**
 * Author:maxuesong
 * Created by Administrator on 2018-11-25.
 */
public class LocationEvent {
    private double lat;
    private double lon;
    private String address;

    public LocationEvent(double lat, double lon, String address) {
        this.lat = lat;
        this.lon = lon;
        this.address = address;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
