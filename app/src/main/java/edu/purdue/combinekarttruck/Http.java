package edu.purdue.combinekarttruck;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

import android.widget.Toast;

/**
 * Created by Zyglabs on 6/23/2016.
 *
 * Ref: http://www.codeproject.com/Tips/1034468/Android-Simply-Sending-HTTP-GET-POST-Requests-To-S
 */
public class Http {
    public static Object Get(String url) throws Exception {
        DefaultHttpClient hc = new DefaultHttpClient();
        ResponseHandler response = new BasicResponseHandler();
        HttpGet http = new HttpGet(url);
        final Object resp = hc.execute(http, response);
        return resp;
    }

    public static Object Post(String url, String postData, String contentType) throws Exception {
        DefaultHttpClient hc = new DefaultHttpClient();
        ResponseHandler response = new BasicResponseHandler();
        HttpPost http = new HttpPost(url);
        http.setHeader("Content-Type", contentType);
        http.setEntity(new StringEntity(postData));
        final Object resp = hc.execute(http, response);
        return resp;
    }
}
