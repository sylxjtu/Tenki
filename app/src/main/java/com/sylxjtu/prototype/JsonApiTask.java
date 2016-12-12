package com.sylxjtu.prototype;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by sylxjtu on 2016/12/11.
 */

public abstract class JsonApiTask {
    private String url;
    private Context context;
    private int bufferSize;
    private int readTimeout = 10000;
    private int connectTimeout = 15000;

    JsonApiTask(String url, Context context, int bufferSize) {
        this.url = url;
        this.context = context;
        this.bufferSize = bufferSize;
    }

    void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }


    void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    void execute() {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if(networkInfo != null && networkInfo.isConnected()){
            onPending();
            new AsyncTask<String, Void, String>() {
                @Override
                protected String doInBackground(String... urls) {
                    try {
                        URL url = new URL(urls[0]);
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setReadTimeout(readTimeout);
                        conn.setConnectTimeout(connectTimeout);
                        conn.setRequestMethod("GET");
                        conn.setDoInput(true);
                        conn.connect();

                        Reader reader = new InputStreamReader(conn.getInputStream());
                        char[] buffer = new char[bufferSize];
                        reader.read(buffer);
                        return new String(buffer);
                    } catch (Exception e) {
                        return null;
                    }
                }

                @Override
                public void onPostExecute(String result) {
                    if(result != null){
                        try {
                            onSuccess(new JSONObject(result), context);
                        } catch (JSONException e) {
                            onError();
                        }
                    }
                    else onError();
                }
            }.execute(url);
        } else {
            onError();
        }
    }

    abstract void onSuccess(JSONObject obj, Context context) throws JSONException;
    abstract void onPending();
    abstract void onError();
}
