package com.zkzj.rtmp_terminal.utils;

import android.app.Service;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import com.orhanobut.logger.Logger;
import com.zkzj.rtmp_terminal.event.LocationEvent;
import com.zkzj.rtmp_terminal.event.MqttArrivedMsgEvent;
import com.zkzj.rtmp_terminal.event.MqttToSendEvent;

import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.ref.WeakReference;

public class MqttService extends Service {

    private MqttBroker mqttBroker;
    private String[] TOPIC_VIDEO;
    private String[] TOPIC_AUDIO;//语音主题
    private static String video;
    private static String audio;

    private BeatHandler beatHandler;

    public static String getTopicAudio() {
        return audio;
    }

    public static String getTopcSelf() {
        return video;
    }

    private MqttBroker.MsgListener msgListener;

    public MqttService() {
        Logger.i("MqttService()...");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        EventBus.getDefault().register(this);
        initMqtt();
        initBeatHeard();
    }

    private void initBeatHeard() {
        beatHandler = new BeatHandler(MqttService.this);
        new BeatThread().start();
    }

    @Override
    public void unbindService(ServiceConnection conn) {
        Logger.d("unbindService" + conn.toString());
        super.unbindService(conn);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
        beatHandler.removeCallbacksAndMessages(null);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void Event(LocationEvent messageEvent) {
        String GPS_INFO = MqttUtil.getGpsInfo(messageEvent.getLat(), messageEvent.getLon(),
                SysUtils.getInstance().getDeviceId());
        if (mqttBroker != null) {
            mqttBroker.sendMessage(Constant.MQTT_VIDEO_S, GPS_INFO);
            Logger.i("Mqtt send gps " + GPS_INFO);
            Log.e("Tga","GPS:"+GPS_INFO);
        } else {
            Logger.e("broker is not ready");
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void Event(MqttToSendEvent messageEvent) {
        if (mqttBroker != null) {
            mqttBroker.sendMessage(messageEvent.getTopic(), messageEvent.getMsg());
        } else {
            Logger.e("broker is not ready");
        }
    }

    private class BeatThread extends Thread {
        @Override
        public void run() {
            while (true) {
                try {
                    Message msg = new Message();
                    msg.what = 1;
                    beatHandler.sendMessageDelayed(msg, 1000);
                    Thread.sleep(10 * 1000);//每隔10s执行一次
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static class BeatHandler extends Handler {

        private final WeakReference<Service> mServiceReference;

        BeatHandler(MqttService service) {
            this.mServiceReference = new WeakReference<Service>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            MqttService service = (MqttService) mServiceReference.get();
            if (msg.what == 1) {
                String beat = MqttUtil.getHeartBeatOnline(SysUtils.getInstance().getDeviceId());
                service.mqttBroker.sendMessage(Constant.MQTT_VIDEO_S, beat);
                Logger.v("send MQTT beat heard " + beat);
            }
        }
    }

    private void initMqtt() {
        video = "video/" + SysUtils.getInstance().getDeviceId();//初始化两个主题
        audio = "audio/" + SysUtils.getInstance().getDeviceId();
        TOPIC_VIDEO = new String[]{video};
        TOPIC_AUDIO = new String[]{audio};
        mqttBroker = MqttBroker.getInstance(this);
        mqttBroker.subscribe(TOPIC_VIDEO);//订阅两个主题
        mqttBroker.subscribe(TOPIC_AUDIO);
        msgListener = new MqttBroker.MsgListener() {
            @Override
            public void msgArrived(String topic, MqttMessage message) {
                Logger.i(topic + "->" + message);
                EventBus.getDefault().post(new MqttArrivedMsgEvent(topic, message.toString()));
            }
        };
        mqttBroker.setMsgListener(msgListener);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}