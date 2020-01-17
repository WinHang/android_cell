package com.zkzj.rtmp_terminal.utils;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.baidu.location.Address;
import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.orhanobut.logger.Logger;
import com.zkzj.rtmp_terminal.encrypt.AESUtils;
import com.zkzj.rtmp_terminal.event.LocationEvent;

import org.greenrobot.eventbus.EventBus;

import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Response;

import static com.zkzj.rtmp_terminal.encrypt.AESUtils.AES_KEY;

public class LocationService extends Service {

    private LocationClient locationClient = null;
    public BDAbstractLocationListener myListener = new MyBdlocationListener();

    private LocationUtil mLocationUtil;

    @Override
    public IBinder onBind(Intent intent) {
        return new Binder();
    }

    public class Binder extends android.os.Binder {
        public LocationService getService() {
            return LocationService.this;
        }
    }

    @Override
    public void onCreate() {
        mLocationUtil = new LocationUtil(this);
        boolean isGpsOpen = mLocationUtil.isGpsOpen();
        if (!isGpsOpen) {
            Toast.makeText(this, "请开启GPS", Toast.LENGTH_LONG).show();
        }
        initLocation();
    }

    private void initLocation() {
        locationClient = new LocationClient(getApplicationContext());
        locationClient.registerLocationListener(myListener);
        LocationClientOption option = new LocationClientOption();
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);//可选，默认高精度，设置定位模式，高精度，低功耗，仅设备
        option.setCoorType("bd09ll");//设置返回的定位结果坐标系
        int span = 1000 * 10;  // get gps info per 10 second.
        option.setScanSpan(span);//可选，默认0，即仅定位一次，设置发起定位请求的间隔需要大于等于1000ms才是有效的
        option.setIsNeedAddress(true);//可选，设置是否需要地址信息，默认不需要
        option.setOpenGps(true);//可选，默认false,设置是否使用gps
        option.setLocationNotify(true);//可选，默认false，设置是否当gps有效时按照1S1次频率输出GPS结果
        option.setIsNeedLocationDescribe(true);//可选，默认false，设置是否需要位置语义化结果，可以在BDLocation.getLocationDescribe里得到，结果类似于“在北京天安门附近”
        option.setIsNeedLocationPoiList(true);//可选，默认false，设置是否需要POI结果，可以在BDLocation.getPoiList里得到
        option.setIgnoreKillProcess(false);//可选，默认true，定位SDK内部是一个SERVICE，并放到了独立进程，设置是否在stop的时候杀死这个进程，默认不杀死
        option.SetIgnoreCacheException(false);//可选，默认false，设置是否收集CRASH信息，默认收集
        option.setEnableSimulateGps(false);//可选，默认false，设置是否需要过滤gps仿真结果，默认需要
        locationClient.setLocOption(option);
        if (locationClient.isStarted()) {
            locationClient.stop();
        }
        locationClient.start();
        locationClient.requestLocation();
    }

    @Override
    public void onDestroy() {
        locationClient.unRegisterLocationListener(myListener);
        locationClient.stop();
        mLocationUtil.quit();
        this.stopSelf();
    }

    public class MyBdlocationListener extends BDAbstractLocationListener {
        @Override
        public void onReceiveLocation(BDLocation location) {
            if (null == location) {
                Logger.e("location is null");
                return;
            }
            //将坐标转换为GPS坐标系
            BDLocation bdLocation = GlobalTool.BAIDU_to_WGS84(location);
            updateLocation(bdLocation);
        }
    }

    private void updateLocation(BDLocation bdLocation) {
        mLocationUtil.updateLocation();
        double lat = bdLocation.getLatitude();
        double lon = bdLocation.getLongitude();
        Address address = bdLocation.getAddress();
        String info;
        String province = address.province;
        String city = address.city;
        String des = bdLocation.getLocationDescribe().substring(1);
        if (province.equals(city)) {
            info = city + address.district + address.street + des;
        } else {
            info = province + city + address.district + address.street + des;
        }

        EventBus.getDefault().post(new LocationEvent(lat, lon, info));
        sendLoc(lat, lon);
    }

    //调用接口发送经纬度坐标
    private void sendLoc(double lat, double lon) {
        Map<String, String> map = new HashMap<>();

        Map<String, String> dataMap = new HashMap<>();
        dataMap.put("vin", SysUtils.getInstance().getDeviceId());
        dataMap.put("lat", String.valueOf(lat));
        dataMap.put("lng", String.valueOf(lon));
        String request = JSON.toJSONString(dataMap);
        Logger.i("Api send GPS "+request);

        try {
            map.put("request", AESUtils.encrypt(AES_KEY, request));
        } catch (Exception e) {
            e.printStackTrace();
        }
        String URL = "http://" + Constant.auto_ip_port + "/video/mobile/rdss.do";

        OkHttpManager.getInstance().postAsyn(URL, new OkHttpManager.ResultCallback() {
            @Override
            public void onError(Call call, Exception e) {
                Logger.e(call.request().toString());
            }

            @Override
            public void onResponse(Call call, Response response) {
                String responseResult;
                try {
                    String body = response.body().string();
                    responseResult = AESUtils.decrypt(AES_KEY, body);
                    Logger.i("AES.decrypt--" + responseResult);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, map);
    }
}