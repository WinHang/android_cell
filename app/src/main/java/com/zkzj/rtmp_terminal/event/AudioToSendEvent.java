package com.zkzj.rtmp_terminal.event;

import java.util.Arrays;

/**
 * Author:maxuesong
 * Created by Administrator on 2018-11-26.
 */
public class AudioToSendEvent {

    private byte[] buffer;
    private int length;

    public AudioToSendEvent(byte[] buffer, int length) {
        this.buffer = buffer;
        this.length = length;
    }

    public void setBuffer(byte[] buffer) {
        this.buffer = buffer;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public byte[] getBuffer() {
        return buffer;
    }

    public int getLength() {
        return length;
    }

    @Override
    public String toString() {
        return "AudioToSendEvent{" +
                "buffer=" + Arrays.toString(buffer) +
                ", length=" + length +
                '}';
    }
}
