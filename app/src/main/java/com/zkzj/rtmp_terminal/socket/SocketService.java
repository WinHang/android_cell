package com.zkzj.rtmp_terminal.socket;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;

import com.alibaba.fastjson.JSONObject;
import com.zkzj.rtmp_terminal.aac.AACDecoderUtil;
import com.zkzj.rtmp_terminal.event.AudioToSendEvent;
import com.zkzj.rtmp_terminal.event.MqttArrivedMsgEvent;
import com.zkzj.rtmp_terminal.utils.ByteUtil;
import com.zkzj.rtmp_terminal.utils.Constant;
import com.zkzj.rtmp_terminal.utils.MqttService;
import com.zkzj.rtmp_terminal.utils.NumberUtil;
import com.zkzj.rtmp_terminal.utils.SPUtils;
import com.zkzj.rtmp_terminal.utils.SysUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class SocketService extends Service {

    private SocketBinder mBinder = new SocketBinder();
    public volatile boolean isCommunication = false;

    private String imei;
    private Handler heartBeatHandler;
    private DatagramSocket datagramSocket;
    private Runnable heartBeatRunnable;
    private byte[] received_bytes;
    private byte[] id_bytes;
    private byte[] roomID_bytes;

    private String ip;
    private String port;

    private volatile boolean isWorking = true;

    private AACDecoderUtil audioUtil;

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class SocketBinder extends Binder {
        public SocketService getService() {
            return SocketService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        EventBus.getDefault().register(this);

        ip = (String) SPUtils.getParam(this, Constant.AUDIO_IP, Constant.AUDIO_IP_DEF);
        port = (String) SPUtils.getParam(this, Constant.AUDIO_PORT, Constant.AUDIO_PORT_DEF);

        try {
            datagramSocket = new DatagramSocket(Integer.parseInt(port));
        } catch (SocketException e) {
            e.printStackTrace();
        }

        heartBeatHandler = new Handler();
        heartBeatRunnable = new Runnable() {
            @Override
            public void run() {
                sendHeartBeat = true;//发送心跳的控制信号
                heartBeatHandler.postDelayed(this, 5 * 1000);
            }
        };
        heartBeatHandler.postDelayed(heartBeatRunnable, 2 * 1000);

        imei = SysUtils.getInstance().getDeviceId();
        new HeartBeatThread().start();
        new ResetHeartBeat().start();
        new UdpReceiveThread().start();
        new UdpSendThread().start();

        audioUtil = new AACDecoderUtil();
        audioUtil.start();
    }

    private void playAudioBuffer(byte[] buffer, int length) {
        if (audioUtil != null) {
            audioUtil.decode(ByteUtil.byteArrayCut(buffer, length, 4), 0, length - 4);
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void Event(AudioToSendEvent messageEvent) {
        Message msg = Message.obtain();
        msg.setTarget(mHandler);
        msg.what = MSG_AUDIO;
        Bundle bundle = new Bundle();
        bundle.putByteArray(AUDIO, messageEvent.getBuffer());
        bundle.putInt(LENGTH, messageEvent.getLength());
        //是否有语音消息发送出去
//        Log.i("Event", "Event: "+messageEvent.toString());
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void Event(MqttArrivedMsgEvent messageEvent) {
        if (messageEvent.getTopic().equals(MqttService.getTopicAudio())) {
            if (datagramSocket == null || datagramSocket.isClosed()) {
                return;
            }
            JSONObject jsonAudio = JSONObject.parseObject(messageEvent.getMsg());
            String type = jsonAudio.getString("type");
            if (type.equals("heartbeat")) {
                int value = jsonAudio.getInteger("value");
                setCommStatus(value);
            }
            if (type.equals("stop")) {
                setCommStatus(-1);
            }
        }
    }

    public void setCommStatus(int roomID) {
        if (roomID == -1) {
            isCommunication = false;
        } else {
            isCommunication = true;
            roomID_bytes = NumberUtil.int2byte(roomID);
            lastTimes = System.currentTimeMillis();
        }
    }

    public void sendBuffer(byte[] buffer, int length) {
        if (datagramSocket == null) {
            return;
        }
        if (!isCommunication) {
            return;
        }
        try {
            if (roomID_bytes != null && id_bytes != null && id_bytes.length > 0) {
                byte[] packet = ByteUtil.byteMergerAudio(roomID_bytes, id_bytes, buffer, length);
                InetAddress serverAddr = InetAddress.getByName(ip);
                DatagramPacket outPacket = new DatagramPacket(
                        packet,
                        packet.length,
                        serverAddr,
                        Integer.parseInt(port));
                datagramSocket.send(outPacket);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private final int MSG_AUDIO = 10086;
    private static final String AUDIO = "audio";
    private static final String LENGTH = "length";
    private Handler mHandler;

    class UdpSendThread extends Thread {
        @Override
        public void run() {
            Looper.prepare();
            mHandler = new Handler() {//2、绑定handler到CustomThread实例的Looper对象
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                        case MSG_AUDIO:
                            Bundle bundle = msg.getData();
                            sendBuffer(bundle.getByteArray(AUDIO), bundle.getInt(LENGTH));
                        default:
                            break;
                    }
                }
            };
            Looper.loop();
        }
    }

    class UdpReceiveThread extends Thread {
        @Override
        public void run() {
            try {
                while (isWorking) {
                    byte[] inBuf = new byte[10 * 1024];
                    DatagramPacket inPacket = new DatagramPacket(inBuf, inBuf.length);
                    datagramSocket.receive(inPacket);
                    received_bytes = inPacket.getData();
                    if (received_bytes != null && received_bytes.length > 0) {
                        if (inPacket.getLength() > 10) {
                            byte[] readBuffer = new byte[inPacket.getLength()];
                            System.arraycopy(received_bytes, 0, readBuffer, 0, inPacket.getLength());
                            if (isCommunication) {
                                playAudioBuffer(readBuffer, inPacket.getLength());
                            }
                        } else {
                            id_bytes = new byte[inPacket.getLength()];
                            System.arraycopy(received_bytes, 0, id_bytes, 0, inPacket.getLength());
                            hasID = true;
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private volatile boolean sendHeartBeat = false;//控制发送频率
    private volatile boolean hasID = false;

    private static byte[] LoginHead = {73, 77, 69, 73, 76};
    private static byte[] HeartHead = {73, 77, 69, 73, 72};

    class HeartBeatThread extends Thread {
        @Override
        public void run() {
            while (isWorking) {
                try {
                    if (!sendHeartBeat) {
                        continue;
                    }
                    if (imei != null) {
                        if (datagramSocket == null) {
                            return;
                        }

                        byte[] data;
                        if (hasID) {
                            data = ByteUtil.byteMerger(HeartHead, imei.getBytes());
                        } else {
                            data = ByteUtil.byteMerger(LoginHead, imei.getBytes());
                        }

                        InetAddress serverAddr = InetAddress.getByName(ip);
                        DatagramPacket outPacket = new DatagramPacket(
                                data,
                                data.length,
                                serverAddr,
                                Integer.parseInt(port));
                        datagramSocket.send(outPacket);
                        sendHeartBeat = false;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private volatile long lastTimes = 0;

    class ResetHeartBeat extends Thread {
        @Override
        public void run() {
            do {
                long time = System.currentTimeMillis();
                if (time - lastTimes >= 30 * 1000) {
                    isCommunication = false;
                }
                try {
                    Thread.sleep(30 * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } while (isWorking);
        }
    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().unregister(this);
        isWorking = false;
        if (!datagramSocket.isClosed()) {
            datagramSocket.close();
        }
        if (audioUtil != null) {
            audioUtil.stop();
        }
        super.onDestroy();
    }
}