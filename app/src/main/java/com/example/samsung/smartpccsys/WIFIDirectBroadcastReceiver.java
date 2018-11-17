package com.example.samsung.smartpccsys;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.widget.Toast;

public class WIFIDirectBroadcastReceiver extends BroadcastReceiver {

    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private MainActivity mainActivity;

    public WIFIDirectBroadcastReceiver(WifiP2pManager mmanager, WifiP2pManager.Channel mchannel, MainActivity mActivity){
        this.mManager = mmanager;
        this.mChannel = mchannel;
        this.mainActivity = mActivity;
    }
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            // Check to see if Wi-Fi is enabled and notify appropriate activity
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                // Wifi P2P is enabled
                Toast.makeText(context,"Wifi is ON",Toast.LENGTH_LONG).show();
            } else {
                // Wi-Fi P2P is not enabled
                Toast.makeText(context,"Wifi is OFF",Toast.LENGTH_LONG).show();
            }
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            // Call WifiP2pManager.requestPeers() to get a list of current peers
            if(mManager != null){
                mManager.requestPeers(mChannel,mainActivity.peerListListener);
            }
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            // Respond to new connection or disconnections
            if (mManager == null){
                return;
            }
            NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
            if (networkInfo.isConnected()){
                mManager.requestConnectionInfo(mChannel,mainActivity.connectionInfoListener);
            } else {
                mainActivity.tv_connectionStatus.setText("Device Disconnected");
            }
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            // Respond to this device's wifi state changing
        }
    }


}
