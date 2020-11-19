package com.example.controlserver;

import android.content.Context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class Utils {

    public static String changeShow(String ori, String content){
        int index = ori.indexOf(':');
        String realStr = ori.substring(0, index+1) + content;
        return realStr;
    }

    public static ArrayList readFromFile(Context context, int fileId) throws IOException {

        ArrayList<String> strArray = new ArrayList<String>();
        InputStream fileInputStream = context.getResources().openRawResource(fileId);
        InputStreamReader reader = new InputStreamReader(fileInputStream);
        BufferedReader br = new BufferedReader(reader);
        String line = "";
        line = br.readLine();
        while(line != null && !line.equals("")) {
            strArray.add(line);
            line = br.readLine();
        }
        return strArray;
    }
}
