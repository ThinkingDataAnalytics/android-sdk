package cn.thinkingdata.android.demo;

import android.annotation.SuppressLint;
import android.util.Log;

import com.alibaba.fastjson.JSON;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class UrlRequest {
    private static BufferedReader reader;
    private static StringBuffer response;

    public static HashMap<Object, Object> doPost(String httpMethodStr) throws Exception{
        String[] atrributesKeyArray = {};
        String[] atrributesValueArray = {};
        HashMap<Object, Object> responseDic = new HashMap<Object, Object>();
        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();
        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
        RequestBody body = RequestBody.create(mediaType, "");
        Request request = new Request.Builder()
                .url(httpMethodStr)
                .method("POST", body)
                .build();
        Response response = client.newCall(request).execute();
        String responseBodyString = response.body().string();
        if(response.isSuccessful()){
            String[] responseList = responseBodyString.split("\n");
            JSONObject atrributesKeyString = new JSONObject(responseList[0]);
            atrributesKeyArray = JSON.parseArray(String.valueOf(atrributesKeyString.getJSONObject("data").getJSONArray("headers"))).toArray(new String[0]);
            if(responseList.length > 1){
                String atrributesValueString = responseList[1].replaceAll("^\\[*","");
                atrributesValueString = atrributesValueString.replaceAll("^]*","").replace("\"", "");
                atrributesValueArray = atrributesValueString.split(",");
            }
        }
        if((atrributesValueArray.length > 0) && (atrributesKeyArray.length == atrributesValueArray.length)){
            for(int i = 0; i < atrributesKeyArray.length; i++){
                responseDic.put(atrributesKeyArray[i], atrributesValueArray[i]);
            }
        }
        return  responseDic;
    }
    public static String getTimeNowTogether(){
        @SuppressLint("SimpleDateFormat") String TimeNow = new SimpleDateFormat("yyyy-MM-dd").format(Calendar.getInstance().getTime());
        System.out.println(TimeNow);
        return TimeNow;
    }

}
