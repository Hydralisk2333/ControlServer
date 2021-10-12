package com.example.controlserver;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static com.example.controlserver.GlobalConfig.*;

public class MainActivity extends AppCompatActivity {

    private TextView textConnectStatus;
    private TextView textLeftNum;
    private TextView textShow;
    private Button buttonClick;
    private Button buttonTouch;
    private Button buttonBack;
    private Button buttonAhead;
    private EditText editId;
    private Button buttonSetId;
    private EditText editSkipText;
    private Button buttonSkipText;

    private String[] tagType = {"connected", "video", "ultra"};

    int connectNum = 0;
    private int personId = -1;
    private int sentId = R.raw.mod50_sent;
    private int condId = R.raw.mod50_cond;
    private ArrayList<String> sentArray = new ArrayList<String>();
    private ArrayList<String> condArray = new ArrayList<String>();

    int[] syOneIndex = new int[]{0, 24};
    int[] syTwoIndex = new int[]{25, 39};
    int[] syThreeIndex = new int[]{40, 49};

    private int curCount = 0;
    private int leftCount = MAX_COUNT;

    private BluetoothAdapter bluetoothAdapter = null;
    private BluetoothServer bluetoothServer = null;
    private StringBuffer outStringBuffer;

    private static final int REQUEST_ENABLE_BT = 3;

    private List<String> deviceNames = new ArrayList<>();

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loadDataFromFiles();
        initialAllViews();
        showContent();
        initialIdButton();

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            this.finish();
        }
        if (!bluetoothAdapter.isEnabled()){
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } else if (bluetoothServer == null) {
            setupServer();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bluetoothServer != null) {
            if (bluetoothServer.getState() == BluetoothServer.STATE_NONE) {
                bluetoothServer.start();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bluetoothServer != null) {
            bluetoothServer.stop();
        }
    }

    private void setupServer() {
        bluetoothServer = new BluetoothServer(this, mHandler);
        outStringBuffer = new StringBuffer();
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
//        sendPackedMessage(SHOW_WORD);
    }

    public void initialAllViews() {
        //绑定元件
        textConnectStatus = (TextView) findViewById(R.id.text_connect_status);
        textLeftNum = (TextView) findViewById(R.id.text_left_num);
        textShow = (TextView) findViewById(R.id.text_show);
        buttonClick = (Button) findViewById(R.id.button_click);
        buttonTouch = (Button) findViewById(R.id.button_touch);
        buttonAhead = (Button) findViewById(R.id.button_ahead);
        buttonBack = (Button) findViewById(R.id.button_back);
        editId = (EditText) findViewById(R.id.text_id);
        buttonSetId = (Button) findViewById(R.id.button_id);
        editSkipText = (EditText) findViewById(R.id.skip_text_id);
        buttonSkipText = (Button) findViewById(R.id.skip_button_id);
        //设置editSkipText的hint
        String hintContent = 1 + "-" + sentArray.size();
        editSkipText.setHint(hintContent);
    }

    public void loadDataFromFiles(){
        //读取文件
        try {
            sentArray = Utils.readFromFile(this, sentId);
            condArray = Utils.readFromFile(this, condId);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String packSendMessage(String command){
        String condenseName = null;
        if (curCount >= sentArray.size()) {
            condenseName = "null";
        } else {
            condenseName = condArray.get(curCount);
        }
        String message = command + SPILIT_CHAR + condenseName + SPILIT_CHAR + leftCount + SPILIT_CHAR + personId;
        return message;
    }

    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (bluetoothServer.getState() != BluetoothServer.STATE_ALL_CONNECTED) {
            Toast.makeText(getApplicationContext(), "You are not connected to a device", Toast.LENGTH_SHORT).show();
            return;
        }
        if (message.length() > 0) {
            byte[] send = message.getBytes();
            bluetoothServer.write(send);
            // Reset out string buffer to zero and clear the edit text field
            outStringBuffer.setLength(0);
        }
    }

    private void sendPackedMessage(String command) {
        String message = packSendMessage(command);
        sendMessage(message);
    }

    public void initialIdButton() {
        setButtonSetIdListener();
        setButtonSkipLisener();
    }

    public void initialOtherButton() {
        setButtonClickListener();
        setButtonTouchListener();
        setButtonAheadListener();
        setButtonBackListener();
    }

    public void initialBluetoothServer(){
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            this.finish();
        }
        if (!bluetoothAdapter.isEnabled()){
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } else if (bluetoothServer == null) {
            setupServer();
        }
    }

    Handler handler = new Handler();
    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            //要执行的事件
            sendPackedMessage(END);
            changeNum(1);
            showContent();
        }
    };

    public void setButtonClickListener(){
        buttonClick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String buttonStr = buttonClick.getText().toString();
//                if (buttonStr.equals(getResources().getString(R.string.button_start))){
//                    sendPackedMessage(START);
//                    buttonClick.setText(getResources().getString(R.string.button_end));
//                } else {
//                    sendPackedMessage(END);
//                    buttonClick.setText(getResources().getString(R.string.button_start));
//                    changeNum(1);
//                    showContent();
//                }
                int delayTime = 3000;
//                if (curCount>=syOneIndex[0] && curCount<=syOneIndex[1]){
//                    delayTime = 2000;
//                }
//                if (curCount>=syTwoIndex[0] && curCount<=syTwoIndex[1]){
//                    delayTime = 2400;
//                }
//                if (curCount>=syThreeIndex[0] && curCount<=syThreeIndex[1]){
//                    delayTime = 2800;
//                }
                buttonClick.setEnabled(false);
                sendPackedMessage(START);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        sendPackedMessage(END);
                        changeNum(1);
                        showContent();
                        buttonClick.setEnabled(true);
                    }
                }, delayTime);

            }
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    public void setButtonTouchListener(){
        buttonTouch.setOnTouchListener(new View.OnTouchListener() {
            //            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        sendPackedMessage(START);
                        break;
                    case MotionEvent.ACTION_UP:
                        sendPackedMessage(END);
                        changeNum(1);
                        showContent();
                        break;
                }
                return true;
            }
        });
    }

    public void setButtonAheadListener(){
        buttonAhead.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changeNum(1);
                showContent();
            }
        });
    }

    public void setButtonBackListener(){
        buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changeNum(0);
                sendPackedMessage(BACK);
                showContent();
            }
        });
    }

    public void setButtonSetIdListener(){
        buttonSetId.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String sid = editId.getText().toString();
                boolean isNum = sid.matches("[0-9]+");
                if (isNum) {
                    personId = Integer.parseInt(sid);
                    initialOtherButton();
                    Toast.makeText(MainActivity.this, "现在可以开始正常控制了", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "请输入一个正整数", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void setButtonSkipLisener(){
        buttonSkipText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String sid = editSkipText.getText().toString();
                boolean isNum = sid.matches("[0-9]+");
                if (isNum) {
                    int tempIndex = Integer.parseInt(sid);
                    if (tempIndex <1 || tempIndex > sentArray.size()){
                        Toast.makeText(MainActivity.this, "输入索引不在单词索引范围内，请重新输入", Toast.LENGTH_SHORT).show();
                    }
                    else {
                        curCount = tempIndex - 1;
                        leftCount = MAX_COUNT;
                        showContent();
                        Toast.makeText(MainActivity.this, "跳转成功", Toast.LENGTH_SHORT).show();
                    }
                    editSkipText.setText("");
                } else {
                    Toast.makeText(MainActivity.this, "跳转索引不正确", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void setStatus(CharSequence subTitle) {
        textConnectStatus.setText(subTitle);
    }

    @SuppressLint("HandlerLeak")
    private final Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothServer.STATE_ALL_CONNECTED:
                            setStatus("connect finish!");
                            showContent();
//                            mConversationArrayAdapter.clear();
                            break;
                        case BluetoothServer.STATE_CONNECTED:
                        case BluetoothServer.STATE_LISTEN:
                            setStatus("waiting for " + msg.arg2 + " device(s)");
                            break;
                        case BluetoothServer.STATE_NONE:
                            setStatus("not connect");
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);

                    break;
                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    String connectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    Log.d("checkpoint", "come here");
                    Toast.makeText(getApplicationContext(), "Connected to "
                            + connectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;
                case Constants.MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(Constants.TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

}