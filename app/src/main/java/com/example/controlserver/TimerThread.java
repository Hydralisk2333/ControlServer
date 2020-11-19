package com.example.controlserver;

import android.net.SocketKeepalive;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.Socket;

public class TimerThread extends Thread {
    long lastTime;
    long curTime;
    TimerCallback timerCallback;
    Socket socket;

    TimerThread(TimerCallback timerCallback, Socket socket){
        this.timerCallback = timerCallback;
        lastTime = System.currentTimeMillis();
        curTime = System.currentTimeMillis();
        this.socket = socket;
    }

    public void setLastTime(long lastTime){
        this.lastTime = lastTime;
    }
    @Override
    public void run() {
        try {
            while (true){
                Thread.sleep(2500);
                long timeDiff = System.currentTimeMillis() - lastTime;
                Log.d("diffTime", timeDiff+"");
                if (Math.abs(timeDiff) > 4000){
                    socket.setSoTimeout(2000);
                    break;
                }
            }
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }
}
