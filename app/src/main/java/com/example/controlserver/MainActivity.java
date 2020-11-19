package com.example.controlserver;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import static com.example.controlserver.GlobalConfig.*;

public class MainActivity extends AppCompatActivity {

    private TextView textConnectNum;
    private TextView textLeftNum;
    private TextView textShow;
    private Button buttonClick;
    private Button buttonTouch;
    private Button buttonBack;
    private Button buttonAhead;
    private TextView textIP;
    private EditText editId;
    private Button buttonId;

    private OnReceiveCallbackBlock receivedCallback;

    private String[] tagType = {"connected", "video", "ultra"};

    int port = 12345;
    int connectNum = 0;
    private int personId = -1;
    private int sentId = R.raw.sentence;
    private int condId = R.raw.condense;
    private ArrayList<String> sentArray = new ArrayList<String>();
    private ArrayList<String> condArray = new ArrayList<String>();

    private int curCount = 0;
    private int leftCount = MAX_COUNT;

    @SuppressLint("ClickableViewAccessibility")
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        //绑定元件
        textConnectNum = (TextView) findViewById(R.id.text_connect_num);
        textLeftNum = (TextView) findViewById(R.id.text_left_num);
        textShow = (TextView) findViewById(R.id.text_show);
        buttonClick = (Button) findViewById(R.id.button_click);
        buttonTouch = (Button) findViewById(R.id.button_touch);
        buttonAhead = (Button) findViewById(R.id.button_ahead);
        buttonBack = (Button) findViewById(R.id.button_back);
        textIP = (TextView) findViewById(R.id.text_selfip);
        editId = (EditText) findViewById(R.id.text_id);
        buttonId = (Button) findViewById(R.id.button_id);

        //获得ip并输出
        String ip = NetworkUtils.getLocalIpAddress(this);
        String ori = textIP.getText().toString();
        String content = Utils.changeShow(ori, ip);
        textIP.setText(content);

        //读取文件
        try {
            sentArray = Utils.readFromFile(this, sentId);
            condArray = Utils.readFromFile(this, condId);
        } catch (IOException e) {
            e.printStackTrace();
        }

        showContent();

        //实现callBack接口
        receivedCallback = new OnReceiveCallbackBlock() {
            @Override
            public void callback(String receicedMessage) {
                Log.d("receicedMessage", receicedMessage);
                int index = receicedMessage.indexOf(':');
                if (index < 0){
                    return;
                }
                String tag = receicedMessage.substring(0,index);
                int num = Integer.parseInt(receicedMessage.substring(index+1));
                if (tag.equals(CONNECT)){
                    connectNum = Math.max(0, connectNum+num);
                    String ori = textConnectNum.getText().toString();
                    String content = Utils.changeShow(ori, connectNum+"");
                    textConnectNum.setText(content);
                }
            }
        };

        //开启服务器线程
        final ServerThread serverThread = new ServerThread(port, receivedCallback);
        serverThread.start();

        buttonClick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String buttonStr = buttonClick.getText().toString();
                if (buttonStr.equals(getResources().getString(R.string.button_start))){
                    try {
                        serverThread.sentCommand("start");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    buttonClick.setText(getResources().getString(R.string.button_end));
                } else {
                    try {
                        serverThread.sentCommand("end");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    buttonClick.setText(getResources().getString(R.string.button_start));
                    changeNum(1);
                    showContent();
                }
            }
        });

        buttonTouch.setOnTouchListener(new View.OnTouchListener() {
//            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        try {
                            serverThread.sentCommand("start");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        try {
                            serverThread.sentCommand("end");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        changeNum(1);
                        showContent();
                        break;
                }
                return true;
            }
        });

        buttonAhead.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    serverThread.sentCommand("ahead");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                changeNum(1);
                showContent();
            }
        });

        buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    serverThread.sentCommand("back");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                changeNum(0);
                showContent();
            }
        });

        buttonId.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String sid = editId.getText().toString();
                boolean isNum = sid.matches("[0-9]+");
                if (isNum) {
                    personId = Integer.parseInt(sid);
                    try {
                        serverThread.sentCommand(personId+"");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "请输入一个正整数", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void changeNum(int flag){
        if (flag > 0){
            if (curCount >= sentArray.size()){
                return;
            }
            if (leftCount == 1){
                curCount++;
                leftCount=MAX_COUNT;
            }
            else {
                leftCount--;
            }
        } else {
            if (leftCount == MAX_COUNT) {
                if (curCount > 0){
                    curCount --;
                    leftCount = 1;
                }
            } else {
                leftCount++;
            }
        }
    }

    public void showContent(){
        String showSent;
        String showCount;
        if (curCount < sentArray.size()){
            showSent = sentArray.get(curCount);
            showCount = leftCount+"";
        } else {
            showSent = "数据收集结束！辛苦啦！";
            showCount = "null";
        }
        textLeftNum.setText(showCount);
        textShow.setText(showSent);
    }
}