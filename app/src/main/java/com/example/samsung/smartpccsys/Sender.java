package com.example.samsung.smartpccsys;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.ListView;

import java.net.Socket;
import java.util.ArrayList;

public class Sender extends AsyncTask<Void, Void, Void> {

    private ArrayList<String> a;
    private ListView listView;
    private Context context;
    private Activity activity;
    private String destinationAddress="-1";
    private String filePath;
    private String wholePath;
    private boolean xceptionFlag = false;
    private Socket socket;
    private String hostName,canonicalHostname;
    private String givenName;

    Sender(Context cont, Activity act, String path, String fullPath){
        this.context = cont;
        this.activity = act;
        this.filePath = path;
        this.wholePath = fullPath;
    }

    @Override
    protected void onProgressUpdate(Void... values) {
        super.onProgressUpdate(values);
    }

    @Override
    protected Void doInBackground(Void... voids) {
        return null;
    }
}
