package com.example.samsung.smartpccsys;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.nfc.Tag;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

import bsh.EvalError;
import bsh.Interpreter;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;


public class MainActivity extends AppCompatActivity {

    Button btnOnOff, btnAdd, btnSub, btnMul, btnDiv, btnDiscover, btnFiles;
    ListView lv;
    TextView tv_readMsg, tv_connectionStatus, tv_size;
    private static final String TAG = MainActivity.class.getSimpleName();
    WifiManager wifiManger;
    WifiP2pManager wifiP2pManager;
    WifiP2pManager.Channel mChannel;
    BroadcastReceiver mReceiver;
    IntentFilter mIntentFilter;
    List<WifiP2pDevice> lPeers;
    String[] deviceNameArray;
    WifiP2pDevice[] deviceArray;
    int index;
    static final int MESSAGE_READ = 1;
    static final int MESSAGE_FILE = 2;
    ServerClass serverClass;
    ClientClass clientClass;
    SendReceive sendReceive;
    private Interpreter interpreter = new Interpreter();
    private String add, sub, mul, div;
    Intent intent;
    Uri fileUri;
    private File file;
    Utils utils;
    Receiver receiver;
    int nameIndex, sizeIndex;
    public static String fileName;
    private static final int REQUEST_CODE = 6384; // onActivityResult request code
    private static final int PERMISSION_REQUEST_CODE = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initialWork();
        exqListener();
    }

    Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what){
                case MESSAGE_READ:
                    Log.e(TAG,"Text/File Received!");
                    File desti = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Downloads/example.java");
                    if (!desti.exists()) {
                        try {
                            desti.createNewFile();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    Log.e(TAG,"FileName: " + desti.getName() + " File Path: " + desti.getAbsolutePath());
                    byte[] readBuff = (byte[]) msg.obj;
                    String tempMsg = new String(readBuff,0,msg.arg1);
                    Log.e(TAG, "Message: " + tempMsg);
                    try {
                        interpreter.set("Context", MainActivity.this);
                        interpreter.eval(tempMsg);

                        FileOutputStream fos = null;
                        try {
                            fos = new FileOutputStream(desti);
                            fos.write(readBuff);
                            fos.flush();
                            fos.close();

                            utils.writeBytesToFile(desti,readBuff);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }



                    } catch (EvalError evalError) {
                        evalError.printStackTrace();
                        Toast.makeText(MainActivity.this, "error: "+evalError.toString(), Toast.LENGTH_SHORT).show();
                    }

                   // tv_readMsg.setText(tempMsg);
                    break;
                case MESSAGE_FILE:
                    Log.e(TAG,"File Received!");
                    File destination = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Downloads/" + fileName);
                    utils = new Utils();
                    readBuff = (byte[]) msg.obj;
                    try {
                        utils.writeBytesToFile(destination,readBuff);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    tempMsg = new String(readBuff,0,msg.arg1);
                    try {
                        interpreter.set("Context", MainActivity.this);
                        interpreter.eval(tempMsg);

                    } catch (EvalError evalError) {
                        evalError.printStackTrace();
                        Toast.makeText(MainActivity.this, "error: "+evalError.toString(), Toast.LENGTH_SHORT).show();
                    }
                    break;



            }
            return true;
        }
    });

//    private void showChooser() {
//        // Use the GET_CONTENT intent from the utility class
//        Intent target = FileUtils.createGetContentIntent();
//        // Create the chooser Intent
//        Intent intent = Intent.createChooser(
//                target, "Select File");
//        try {
//            startActivityForResult(intent, REQUEST_CODE);
//        } catch (ActivityNotFoundException e) {
//            // The reason for the existence of aFileChooser
//        }
//    }
    private void exqListener() {

        btnOnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(wifiManger.isWifiEnabled()){
                    wifiManger.setWifiEnabled(false);
                    btnOnOff.setText("TURN ON WIFI");
                }else{
                    wifiManger.setWifiEnabled(true);
                    btnOnOff.setText("TURN OFF WIFI");
                }
            }
        });

        btnDiscover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                wifiP2pManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        tv_connectionStatus.setText("Discovery Started");
                    }

                    @Override
                    public void onFailure(int reason) {
                        tv_connectionStatus.setText("Discovery Starting Failed");
                    }
                });
            }
        });

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final WifiP2pDevice device = deviceArray[position];
                WifiP2pConfig config = new WifiP2pConfig();
                config.deviceAddress = device.deviceAddress;
                wifiP2pManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(getApplicationContext(),"Connected to: "+device.deviceName,Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onFailure(int reason) {
                        Toast.makeText(getApplicationContext(),"Not Connected",Toast.LENGTH_LONG).show();
                    }
                });
            }
        });

        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //String msg = et_writeMsg.getText().toString();
                sendReceive.write(add.getBytes());
            }
        });

        btnSub.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //String msg = et_writeMsg.getText().toString();
                sendReceive.write(sub.getBytes());
            }
        });

        btnMul.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //String msg = et_writeMsg.getText().toString();
                sendReceive.write(mul.getBytes());
            }
        });

        btnDiv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //String msg = et_writeMsg.getText().toString();
                sendReceive.write(div.getBytes());
            }
        });

        btnFiles.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!checkPermission()) {
                    requestPermission();
                    openChooser();
                }
                else{
                    Toast.makeText(getApplicationContext(), "Permissions already granted", Toast.LENGTH_LONG).show();
                    openChooser();
                }

            }
        });
    }
    private String getRealPathFromURIPath(Uri contentURI, Activity activity) {
        Cursor cursor = activity.getContentResolver().query(contentURI, null, null, null, null);
        String realPath = "";
        if (cursor == null) {
            realPath = Environment.getExternalStorageDirectory() + "/" + contentURI.getPath();
        } else {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME);
            File external[] = getApplicationContext().getExternalMediaDirs();
            if (external.length > 1) {
                realPath = external[1].getAbsolutePath();
                realPath = realPath.substring(0, realPath.indexOf("Android")) + cursor.getString(idx);

            }
          //  realPath = Environment.getExternalStorageDirectory() + "/" + cursor.getString(idx);
            nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
            tv_readMsg.setText(cursor.getString(nameIndex));
            tv_size.setText(cursor.getString(sizeIndex));
        }
        if (cursor != null) {
            cursor.close();
        }

        return realPath;
    }

    private boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(getApplicationContext(), WRITE_EXTERNAL_STORAGE );
        int result1 = ContextCompat.checkSelfPermission(getApplicationContext(), READ_EXTERNAL_STORAGE);

        return result1 == PackageManager.PERMISSION_GRANTED && result == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {

        ActivityCompat.requestPermissions(this, new String[]{WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0) {

                    boolean locationAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean cameraAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;

                    if (locationAccepted && cameraAccepted)
                        Toast.makeText(getApplicationContext(), "Permission Granted, Now you can access location data and camera.", Toast.LENGTH_LONG).show();
                    else {

                        Toast.makeText(getApplicationContext(), "Permission Denied, You cannot access location data and camera.", Toast.LENGTH_LONG).show();

                        if (shouldShowRequestPermissionRationale(READ_EXTERNAL_STORAGE) && shouldShowRequestPermissionRationale(WRITE_EXTERNAL_STORAGE)) {
                            showMessageOKCancel(
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            requestPermissions(new String[]{READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE},
                                                    PERMISSION_REQUEST_CODE);
                                        }
                                    });
                            return;
                        }

                    }
                }


                break;
        }
    }

    private void showMessageOKCancel(DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(MainActivity.this)
                .setMessage("You need to allow access to both the permissions")
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    @SuppressLint("ServiceCast")
    private void initialWork() {
        btnOnOff = (Button) findViewById(R.id.onOff);
        btnDiscover = (Button) findViewById(R.id.discover);
        btnAdd = (Button) findViewById(R.id.btnAdd);
        btnSub = (Button) findViewById(R.id.btnSub);
        btnMul = (Button) findViewById(R.id.btnMul);
        btnDiv = (Button) findViewById(R.id.btnDiv);
        btnFiles = (Button) findViewById(R.id.btnFiles);
        lv = (ListView) findViewById(R.id.peerListView);
        tv_readMsg = (TextView) findViewById(R.id.readMsg);
        tv_connectionStatus = (TextView) findViewById(R.id.connectionStatus);
        intent = new Intent();
        wifiManger = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiP2pManager = (WifiP2pManager) getApplicationContext().getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = wifiP2pManager.initialize(this,getMainLooper(),null);
        mReceiver = new WIFIDirectBroadcastReceiver(wifiP2pManager,mChannel,this);
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        lPeers = new ArrayList<WifiP2pDevice>();
        fileUri = intent.getData();
//        returnCursor = getContentResolver().query(fileUri, null, null, null, null);

        tv_size = (TextView) findViewById(R.id.fileSize);

        add = "import android.widget.Toast;\n" +
                "            import android.util.Log;" +
                "            int i=19568; " +
                "            int j=305; " +
                "            Toast.makeText(Context, \"Sum is: \"+(i+j), Toast.LENGTH_LONG).show();" +
                "            Log.e(\"MainActivity\", \"Printing...\");";

        sub = "import android.widget.Toast;\n" +
                "            import android.util.Log;" +
                "            int i=19568; " +
                "            int j=305; " +

                "Toast.makeText(Context, \"Difference is: \"+(i-j), Toast.LENGTH_LONG).show();"+
                "            Log.e(\"MainActivity\", \"Printing...\");";

        mul = "import android.widget.Toast;\n" +
                "            import android.util.Log;" +
                "            int i=19568; " +
                "            int j=305; " +

                "Toast.makeText(Context, \"Product is: \"+(i*j), Toast.LENGTH_LONG).show();"+
                "            Log.e(\"MainActivity\", \"Printing...\");";

        div = "import android.widget.Toast;\n" +
                "            import android.util.Log;" +
                "            int i=19568; " +
                "            int j=305; " +

                "Toast.makeText(Context, \"Quotient is: \"+(i/j), Toast.LENGTH_LONG).show();"+
                "            Log.e(\"MainActivity\", \"Printing...\");";
    }

    WifiP2pManager.PeerListListener peerListListener = new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peersList) {
            if(!peersList.getDeviceList().equals(lPeers)){
                lPeers.clear();
                lPeers.addAll(peersList.getDeviceList());
                deviceNameArray = new String[peersList.getDeviceList().size()];
                deviceArray = new WifiP2pDevice[peersList.getDeviceList().size()];
                index = 0;

                for (WifiP2pDevice device : peersList.getDeviceList()){
                    deviceNameArray[index] = device.deviceName;
                    deviceArray[index] = device;
                    index++;
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(),android.R.layout.simple_list_item_1, deviceNameArray);
                lv.setAdapter(adapter);
            }

            if(lPeers.size() == 0){
                Toast.makeText(getApplicationContext(), "No Device Found", Toast.LENGTH_LONG).show();
                return;
            }
        }
    };

    WifiP2pManager.ConnectionInfoListener connectionInfoListener = new WifiP2pManager.ConnectionInfoListener() {
        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo info) {
            final InetAddress groupOwnerAddress = info.groupOwnerAddress;
            if(info.groupFormed && info.isGroupOwner){
                tv_connectionStatus.setText("Group Owner");
                serverClass = new ServerClass();
                serverClass.start();
            }else if(info.groupFormed){
                tv_connectionStatus.setText("Group Client");
                clientClass = new ClientClass(groupOwnerAddress);
                clientClass.start();
            }
        }
    };



    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mReceiver,mIntentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        switch (requestCode) {
            case REQUEST_CODE:
                // If the file selection was successful
                if (resultCode == RESULT_OK) {
                    if (data != null) {
                        // Get the URI of the selected file
                        Uri documentUri = data.getData();
                        try {
                            Toast.makeText(MainActivity.this,
                                    "File Selected: " + documentUri, Toast.LENGTH_LONG).show();
                            previewFile(documentUri);
                            String filePath = getRealPathFromURIPath(documentUri, MainActivity.this);//utils.getUriRealPath(MainActivity.this,documentUri);

                            File source = new File(filePath);
                            //utils.readBytesFromFile(source);

                            Log.e(TAG,"FilePath: "+ filePath + " Filename: "+source.getName());
                            FileInputStream fis = new FileInputStream(source);

                            ByteArrayOutputStream bos = new ByteArrayOutputStream();
                            byte[] buf = new byte[1024];
                            try {
                                for (int readNum; (readNum = fis.read(buf)) != -1;) {
                                    bos.write(buf, 0, readNum); //no doubt here is 0
                                    //Writes len bytes from the specified byte array starting at offset off to this byte array output stream.
                                    Log.e(TAG,"read " + readNum + " bytes,");
                                }
                            } catch (IOException ex) {
                                Log.e(TAG,"IOException:" + ex);
                            }
                            byte[] bytes = bos.toByteArray();

                            sendReceive.write(bytes);
                        } catch (Exception e) {
                            Log.e("MainActivity", "File select error", e);
                        }
                    }
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }




    private void copy(File source, File destination) throws IOException {

        FileChannel in = new FileInputStream(source).getChannel();
        FileChannel out = new FileOutputStream(destination).getChannel();

        try {
            in.transferTo(0, in.size(), out);
        } catch(Exception e){
            Log.d("Exception", e.toString());
        } finally {
            if (in != null)
                in.close();
            if (out != null)
                out.close();
        }
    }

    private void DirectoryExist (File destination) {

        if (!destination.isDirectory()) {
            if (destination.mkdirs()) {
                Log.d("Carpeta creada", "....");
            } else {
                Log.d("Carpeta no creada", "....");
            }
        }
    }

    private void openChooser() {
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                 {
            // Permission is not granted
        }else {
            final Intent selectFile = new Intent(Intent.ACTION_GET_CONTENT);
            // The MIME data type filter
            selectFile.setType("*/*");
            // Only return URIs that can be opened with ContentResolver
            selectFile.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(selectFile, REQUEST_CODE);
        }
    }

    private void previewFile(Uri uri) {
        String filePath = getRealPathFromURIPath(uri, MainActivity.this);
        file = new File(filePath);
        Log.d(TAG, "Filename " + file.getName());
        tv_readMsg.setText(file.getName());
    }
        public class ServerClass extends Thread{
        Socket socket;
        ServerSocket serverSocket;

        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket(8888);
                socket = serverSocket.accept();
                sendReceive = new SendReceive(socket);
                sendReceive.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public class ClientClass extends Thread{
        Socket socket;
        String hostAdd;

        public ClientClass(InetAddress hostAddress){
            hostAdd = hostAddress.getHostAddress();
            socket = new Socket();
        }

        @Override
        public void run() {
            try {
                socket.connect(new InetSocketAddress(hostAdd,8888), 500);
                sendReceive = new SendReceive(socket);
                sendReceive.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class SendReceive extends Thread{
        private Socket socket;
        private InputStream inputStream;
        private OutputStream outputStream;

        public SendReceive(Socket skt){
            socket = skt;
            try {
                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        @Override
        public void run() {
            byte[] buffer = new byte[2048];
            int bytes;
            while (socket != null){
                try {
                    bytes = inputStream.read(buffer);
                    if(bytes>0){
                        handler.obtainMessage(MESSAGE_READ,bytes,-1,buffer).sendToTarget();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void write(final byte[] bytes) {

            if (bytes != null) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            outputStream.write(bytes);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }
                }).start();
            } else {
                Toast.makeText(MainActivity.this, "it is null", Toast.LENGTH_LONG).show();

            }
        }

    }

//public static class FileServerAsyncTask extends AsyncTask {
//
//    private Context context;
//    private TextView statusText;
//
//    public FileServerAsyncTask(Context context, View statusText) {
//        this.context = context;
//        this.statusText = (TextView) statusText;
//    }
//
//    @Override
//    protected Object doInBackground(Object[] objects) {
//        try {
//
//            /**
//             * Create a server socket and wait for client connections. This
//             * call blocks until a connection is accepted from a client
//             */
//            ServerSocket serverSocket = new ServerSocket(8888);
//            Socket client = serverSocket.accept();
//
//            /**
//             * If this code is reached, a client has connected and transferred data
//             * Save the input stream from the client as a JPEG file
//             */
//            final File f = new File(Environment.getExternalStorageDirectory() + "/"
//                    + context.getPackageName() + "/wifip2pshared-" + System.currentTimeMillis()
//                    + ".jpg");
//
//            File dirs = new File(f.getParent());
//            if (!dirs.exists())
//                dirs.mkdirs();
//            f.createNewFile();
//            InputStream inputstream = client.getInputStream();
//            copyFile(inputstream, new FileOutputStream(f));
//            serverSocket.close();
//            return f.getAbsolutePath();
//        } catch (IOException e) {
//            Log.e("MainActivity", e.getMessage());
//            return null;
//        }
//    }
//
//    @Override
//    protected void onPostExecute(Object o) {
//        if (o != null) {
//            statusText.setText("File copied - " + o);
//            Intent intent = new Intent();
//            intent.setAction(android.content.Intent.ACTION_VIEW);
//            intent.setDataAndType(Uri.parse("content://" + o), "image/*");
//            context.startActivity(intent);
//        }
//    }
//}
}
