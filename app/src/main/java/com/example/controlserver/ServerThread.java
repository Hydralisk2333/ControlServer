package com.example.controlserver;

import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import static com.example.controlserver.GlobalConfig.*;

public class ServerThread extends Thread {
    private int port;
    private int connectNum = 0;
    private ServerSocket serverSock = null;
    private OnReceiveCallbackBlock receivedCallback;
    List<Socket> socketList = Collections.synchronizedList(new ArrayList<Socket>());
    List<BufferedWriter> bwList = Collections.synchronizedList(new ArrayList<BufferedWriter>());
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    ServerThread(int port, OnReceiveCallbackBlock receivedCallback){
        this.port = port;
        this.receivedCallback = receivedCallback;
        try {
            serverSock = new ServerSocket(port);
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    synchronized public void sentCommand(String cmd) throws IOException {
        for(BufferedWriter bw: bwList){
            bw.write(cmd);
            bw.newLine();
            bw.flush();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void run(){
        int idCount = 0;
        Log.d("server", "server thread is going");

        while(true){
            try {
                Socket s = serverSock.accept();
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
                        s.getOutputStream()));
                if (!s.getKeepAlive()) {
                    s.setKeepAlive(true);
                }
                socketList.add(s);
                bwList.add(bw);

                new Thread(new UserThread(s, idCount, socketList, bwList, receivedCallback)).start();

                String content = CONNECT + ":1";
                receivedCallback.callback(content);
                connectNum ++;
                idCount++;
            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }

}
