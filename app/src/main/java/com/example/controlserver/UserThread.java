package com.example.controlserver;


import android.content.Context;
import android.util.Log;

import java.io.*;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import static com.example.controlserver.GlobalConfig.*;

public class UserThread implements Runnable {
    private int id;
    private Socket s;
    private List<Socket> socketList;
    private List<BufferedWriter> bwList;
    private OnReceiveCallbackBlock receivedCallback;
    private boolean isDelay = false;

    public UserThread(Socket s, int id, List<Socket> socketList, List<BufferedWriter> bwList, OnReceiveCallbackBlock receivedCallback) {
        this.s = s;
        this.id = id;
        this.socketList = socketList;
        this.bwList = bwList;
        this.receivedCallback = receivedCallback;
    }

    @Override
    synchronized public void run() {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    s.getInputStream()));

            TimerCallback tc = new TimerCallback() {
                @Override
                public void callback(boolean judge) {
                    isDelay = judge;
                }
            };
            TimerThread tt = new TimerThread(tc, s);
            tt.start();

            String line = null;
            while ((line = br.readLine()) != null) {
                long curTime = System.currentTimeMillis();
                tt.setLastTime(curTime);
                if (!line.equals("heart")){
                    receivedCallback.callback(line);
                }
                else {
                    for (int i=0; i<socketList.size(); i++){
                        if (socketList.get(i).equals(s)){
                            BufferedWriter bw = bwList.get(i);
                            bw.write("heart");
                            bw.newLine();
                            bw.flush();
                        }
                    }
                }
                Log.d("client msg", line);
            }
            if (isDelay){
                for (int i=0; i<socketList.size(); i++){
                    Socket _s = socketList.get(i);
                    if (_s.equals(s)){
                        socketList.remove(i);
                        bwList.remove(i);
                        String content = CONNECT + ":-1";
                        receivedCallback.callback(content);
                        Log.d("remove client", id+"");
                        break;
                    }
                }
            }

        } catch (IOException e) {
            for (int i=0; i<socketList.size(); i++){
                Socket _s = socketList.get(i);
                if (_s.equals(s)){
                    socketList.remove(i);
                    bwList.remove(i);
                    String content = CONNECT + ":-1";
                    receivedCallback.callback(content);
                    Log.d("remove client", id+"");
                    break;
                }
            }
            e.printStackTrace();
        }
    }

}




