package com.zkzj.rtmp_terminal.aac;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;

import com.zkzj.rtmp_terminal.TerminalApp;

public class MyAudioTrack {
    private int mFrequency;
    private int mChannel;
    private int mSampBit;
    private AudioTrack mAudioTrack;

    public MyAudioTrack(int frequency, int channel, int sampbit) {
        mFrequency = frequency;
        mChannel = channel;
        mSampBit = sampbit;
    }

    public void init() {
        if (mAudioTrack != null) {
            release();
        }
        int minBufSize = getMinBufferSize();
        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                mFrequency,
                mChannel,
                mSampBit,
                minBufSize,
                AudioTrack.MODE_STREAM);
        mAudioTrack.play();

    }

    /**
     * 释放资源
     */
    public void release() {
        if (mAudioTrack != null) {
            mAudioTrack.stop();
            mAudioTrack.release();
        }
    }

    /**
     * 将解码后的pcm数据写入audioTrack播放
     *
     * @param data   数据
     * @param offset 偏移
     * @param length 需要播放的长度
     */
    public void playAudioTrack(byte[] data, int offset, int length) {
        if (data == null || data.length == 0) {
            return;
        }
        try {
            mAudioTrack.write(data, offset, length);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getMinBufferSize() {
        return AudioTrack.getMinBufferSize(mFrequency,
                mChannel, mSampBit);
    }
}