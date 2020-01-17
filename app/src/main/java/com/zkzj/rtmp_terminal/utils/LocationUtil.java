package com.zkzj.rtmp_terminal.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.TextView;

import com.orhanobut.logger.Logger;

public class LocationUtil {
    LocationManager locationManager;
    Context mContext;
    TextView mTextView;
    double mLat; // 纬度
    double mLon;// 经度

    LocationListener locationListener = new LocationListener() {
        // Provider的状态在可用、暂时不可用和无服务三个状态直接切换时触发此函数
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        // Provider被enable时触发此函数，比如GPS被打开
        @Override
        public void onProviderEnabled(String provider) {
            Logger.i(provider);
        }

        // Provider被disable时触发此函数，比如GPS被关闭
        @Override
        public void onProviderDisabled(String provider) {
            Logger.i(provider);
        }

        // 当坐标改变时触发此函数，如果Provider传进相同的坐标，它就不会被触发
        @Override
        public void onLocationChanged(Location location) {
            if (location != null) {
                mLat = location.getLatitude();
                mLon = location.getLongitude();
                Logger.i("locationListener the current location is " + location.toString());
            }
        }
    };

    public LocationUtil(Context context) {
        this(context, null);
    }

    @SuppressLint("MissingPermission")
    public LocationUtil(Context context, TextView tv) {
        mContext = context;
        mTextView = tv;
        locationManager = (LocationManager) mContext
                .getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000,
                0, locationListener);
    }

    @SuppressLint("MissingPermission")
    public void updateLocation() {
        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (location != null) {
            mLat = location.getLatitude();
            mLon = location.getLongitude();
        }
    }

    public boolean isGpsOpen() {
        if (null != locationManager && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            return true;
        }
        return false;
    }

    public void quit() {
        locationManager.removeUpdates(locationListener);
    }
}