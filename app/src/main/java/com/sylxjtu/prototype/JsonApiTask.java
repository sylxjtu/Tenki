package com.sylxjtu.prototype;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;

abstract class JsonApiTask {
    private String url;
    private Context context;
    private int bufferSize = 2048;
    private int readTimeout = 10000;
    private int connectTimeout = 15000;

    JsonApiTask(String url, Context context) {
        this.url = url;
        this.context = context;
    }

    private static String readStream(InputStream is, int bufferSize) throws IOException {
        Reader reader = new InputStreamReader(is);
        char[] buffer = new char[bufferSize];
        String res = "";
        while(reader.read(buffer) == bufferSize) {
            res += new String(buffer);
        }
        return res + new String(buffer);
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

                        return readStream(conn.getInputStream(), bufferSize);
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
