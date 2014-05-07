package hgo.ipprint4;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

/**
 * Created by E841719 on 07.05.2014.
 */
public class ipListActivity extends Activity {
    // Debugging
    private static final String TAG = "HostListActivity";
    private static final boolean D = true;

    // Return Intent extra
    public static String EXTRA_HOST_ADDRESS = "host_address";

    // Member fields
    private PortScanner.ScanResult mScanResult;
    private ArrayAdapter<PortScanner.ScanResult> mRemotesArrayAdapter;

    Button scanButton;
    String scanButtonTextScan = "Scan";
    String scanButtonTextStop = "STOP";

    PortScanner portScanner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addLog("+++OnCreate+++");
        // Setup the window
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.ip_list);

        // Set result CANCELED incase the user backs out
        setResult(Activity.RESULT_CANCELED);

        // Initialize array adapters. One for hosts
        mRemotesArrayAdapter = new ArrayAdapter<PortScanner.ScanResult>(this, R.layout.iplistentry);

        // Find and set up the ListView for paired devices
        ListView hostsListView = (ListView) findViewById(R.id.hostsListView);
        hostsListView.setAdapter(mRemotesArrayAdapter);

        scanButton=(Button)findViewById(R.id.scanButton);
        scanButton.setText(scanButtonTextScan);
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        portScanner=new PortScanner(mHandler, "192.168.128.1");

        portScanner.startDiscovery();
    }

    // The on-click listener for all devices in the ListViews
    private AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            addLog("OnItemClickListener()");
            // Cancel discovery because it's costly and we're about to connect
            portScanner.cancelDiscovery();

            // Get the device MAC address, which is the last 17 chars in the View
            String address = ((TextView) v).getText().toString();

            // Create the result Intent and include the MAC address
            Intent intent = new Intent();
            intent.putExtra("host", address);

            addLog("setResult=OK");
            // Set result and finish this Activity
            setResult(Activity.RESULT_OK, intent);
            finish();
        }
    };

    // The Handler that gets information back from the ipPrintService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case msgTypes.addHost:
                    Bundle bundle = msg.getData();
                    String sHost = bundle.getString(msgTypes.HOST_NAME);
                    mRemotesArrayAdapter.add(new PortScanner.ScanResult(sHost, 9100, true));
                    addLog("msg got host_name: " + sHost);
                    break;
                case msgTypes.finished:
                    addLog("msg received: finished");
                    break;
                case msgTypes.started:
                    addLog("msg received: started");
                    break;
                case msgTypes.stopped:
                    addLog("msg received: stopped");
                    break;
            }
        }
    };

    public void addLog(String s) {
        Log.d(TAG, s);
    }
}