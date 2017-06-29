package com.example.user1.sensorloggerwithandroid;


import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;


import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by user1 on 2017/06/29.
 */

public class FileTransferWithHttp extends AsyncTask<String, Integer, Integer> {
    ProgressDialog dialog;
    Context context;
    ///data/data/com.example.user1.sensorloggerwithandroid/filesからパスは指定しないとダメ
    public FileTransferWithHttp(Context context){ this.context = context; }
    @Override protected Integer doInBackground(String... params) {
        try {
            String fileName = params[0];
            HttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost("http://taiyakon.xyz/cgi-bin/filereceive.php");
            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            MultipartEntity multipartEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
            File file = new File(fileName);
            FileBody fileBody = new FileBody(file);
            multipartEntity.addPart("f1", fileBody);
            httpPost.setEntity(multipartEntity);
            httpClient.execute(httpPost, responseHandler);
        }
        catch (ClientProtocolException e) { e.printStackTrace(); }
        catch (IOException e) { e.printStackTrace(); } return 0; }

    @Override protected void onPostExecute(Integer result) {}
    @Override protected void onPreExecute() {
    }

}
