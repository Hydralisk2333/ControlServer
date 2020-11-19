package com.example.controlserver;

import java.io.BufferedWriter;
import java.io.IOException;

public class SentThread implements Runnable {     //实现Runnable接口

    //成员变量定义私有
    private BufferedWriter bw;

    //带参构造，传入输出流对象
    public SentThread(BufferedWriter bw) {
        this.bw = bw;
    }

    //重写run()方法
    @Override
    synchronized public void run() {
        try {
            while (true) {
                String heartMsg = "heart";
                bw.write(heartMsg);
                bw.newLine();
                bw.flush();
                Thread.sleep(1500);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
