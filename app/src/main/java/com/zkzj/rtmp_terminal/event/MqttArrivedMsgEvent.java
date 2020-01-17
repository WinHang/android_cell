package com.zkzj.rtmp_terminal.event;

/**
 * Author:maxuesong
 * Created by Administrator on 2018-11-25.
 */
public class MqttArrivedMsgEvent {

    private String topic;
    private String msg;

    public MqttArrivedMsgEvent(String topic, String msg) {
        this.topic = topic;
        this.msg = msg;
    }

    public String getMsg() {
        return msg;
    }

    public String getTopic() {
        return topic;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }
}
