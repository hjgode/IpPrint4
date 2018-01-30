package hgo.ipprint4;

import java.io.FileInputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Enumeration;

public class IpPrint4 extends Activity {
    ipPrintFile ipPrintService = null;
    // Layout Views
//    private TextView mTitle;
    private EditText mRemoteDevice;
    Button mConnectButton;
    // Debugging
    private static final String TAG = "ipprint";
    private static final boolean D = true;

    TextView mLog = null;
    Button mBtnExit = null;
    Button mBtnScan = null;
    Button mBtnBrowseForFile = null;

    Button mBtnSelectDemoFile;
    TextView mTxtFilename;
    Button mBtnPrint;
    PrintFileXML printFileXML = null;
    ArrayList<PrintFileDetails> printFileDetailses;

    // Name of the connected device
    private String mConnectedDeviceName = null;
    // Array adapter for the conversation thread
    private ArrayAdapter<String> mConversationArrayAdapter;

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    // Intent request codes for demo files list
    private static final int REQUEST_SELECT_DEMO_FILE = 3;
    //BluetoothAdapter mBluetoothAdapter = null;
    // Intent request codes for files list
    private static final int REQUEST_SELECT_FILE = 4;

    View _view;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        //show
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ipprint_main);

        // CRASHES!
//        // Set up the window layout
//        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
//        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);
//        setContentView(R.layout.ipprint_main);
//
//        // Set up the custom title
//        mTitle = (TextView) findViewById(R.id.title_left_text);
//        mTitle.setText(R.string.app_name);
//        mTitle = (TextView) findViewById(R.id.title_right_text);

        mLog = (TextView) findViewById(R.id.log);
        mLog.setMovementMethod(ScrollingMovementMethod.getInstance());

        /* does not work to hide keypad
        mRemoteDevice.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                //hide keyboard on lost focus
                if(!hasFocus) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(mRemoteDevice.getWindowToken(), 0);
                }
            }
        });
        */

        //connect button
        mConnectButton = (Button) findViewById(R.id.buttonConnect);
        mConnectButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if(ipPrintService.getState() == ipPrintFile.STATE_CONNECTED)
                    disconnectDevice();
                else
                    connectToDevice(mRemoteDevice.getText().toString());
            }
        });

        addLog("ipprint2 started");

        //exit button
        mBtnExit = (Button) findViewById(R.id.button1);
        mBtnExit.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                disconnectDevice();
                finish();
                return;
            }
        });

        //scan button
        mBtnScan = (Button) findViewById(R.id.button_scan);
        mBtnScan.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                disconnectDevice();
                startDiscovery();
            }
        });

        mBtnSelectDemoFile = (Button) findViewById(R.id.btnSelectDemo);
        mBtnSelectDemoFile.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                startFileList();
            }
        });

        mBtnBrowseForFile=(Button)findViewById(R.id.idFileBtn);
        mBtnBrowseForFile.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                startFileBrowser();
            }
        });

        mTxtFilename = (TextView) findViewById(R.id.txtFileName);
        mTxtFilename.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                startFileList();
            }
        });

        mBtnPrint = (Button) findViewById(R.id.btnPrintFile);
        mBtnPrint.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                printFile();
            }
        });
        //setupComm();
        //list files
        AssetFiles assetFiles = new AssetFiles(this);

        //read file descriptions
        readPrintFileDescriptions();

        //is wifi enabled, changed to use AlertDialog
/*        if (!ipPrintFile.isNetworkOnline(this))
            toggleWiFi(true);
*/
        String sIPLocalList=getString(R.string.ip_default_address);
        mRemoteDevice = (EditText) findViewById(R.id.remote_device);
        //if Wifi
        if(ipPrintFile.isNetworkOnline(this)){
            sIPLocalList=getLocalIpAddress();
        }
        else{
            myToast("ipPrint4: Can not continue without TCP/IP. Please enable WLAN and get a working connection.");
            showAlertNoIP();
//            return;
        }
        mRemoteDevice.setText(sIPLocalList);
    }

    void showAlertNoIP(){
        AlertDialog alertDialog = new AlertDialog.Builder(IpPrint4.this).create();
        alertDialog.setTitle("No IP available");
        alertDialog.setMessage("Can not continue without a valid local IP stack!");
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "EXIT",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        finish();
                    }
                });
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "WiFi ON",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        toggleWiFi(true);
                    }
                });

        alertDialog.show();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (D) Log.e(TAG, "++ ON START ++");
/*
        if (mBluetoothAdapter != null) {
            // If BT is not on, request that it be enabled.
            // setupChat() will then be called during onActivityResult
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
                // Otherwise, setup the comm session
            } else {
*/
                if (ipPrintService == null)
                    setupComm();
                addLog("starting print service...");//if (mChatService == null) setupChat();
/*
            }
        }
*/
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        if (D) Log.e(TAG, "+ ON RESUME +");
        addLog("onResume");
        /*
        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
              // Start the Bluetooth chat services
              mChatService.start();
            }
        }
        */
    }

    @Override
    public synchronized void onPause() {
        super.onPause();
        if (D) Log.e(TAG, "- ON PAUSE -");
    }

    @Override
    public void onStop() {
        super.onStop();
        if (D) Log.e(TAG, "-- ON STOP --");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth chat services
        if (ipPrintService != null) ipPrintService.stop();
        if (D) Log.e(TAG, "--- ON DESTROY ---");
    }

    public void toggleWiFi(boolean status) {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (status && !wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        } else if (status && wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(false);
        }
    }


    public String getLocalIpAddress()
    {
        String firstIP="192.168.0.1";
        ArrayList<String> ipList=new ArrayList<String>();
        try
        {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();)
            {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();)
                {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress())
                    {
                        Log.d(TAG, inetAddress.getHostAddress().toString());
                        ipList.add(inetAddress.getHostAddress().toString());
                        //return inetAddress.getHostAddress().toString();
                    }
                }
            }
        }
        catch (SocketException ex)
        {
            Log.e(TAG, ex.toString());
        }

        int firstIP4=-1;
        for (int i=0; i<ipList.size(); i++){// String s : ipList){
            if (ipList.get(i).indexOf("%") != -1) {
                //IP6 address
                ;
            } else {
                firstIP4=i;
                addLog("Local IP: " + ipList.get(i));
            }
        }
        if(firstIP4!=-1){
            firstIP=ipList.get(firstIP4);
        }
        return firstIP;
    }

    void readPrintFileDescriptions() {
        InputStream inputStream = null;
        try {
            inputStream = this.getAssets().open("demofiles.xml");
            printFileXML = new PrintFileXML(inputStream);
            //now assign the array of known print files and there details
            printFileDetailses = printFileXML.printFileDetails;
        } catch (IOException e) {
            Log.e(TAG, "Exception in readPrintFileDescriptions: " + e.getMessage());
        }
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException e) {
            }
        }
    }

    byte[] escpQuery() {
        byte[] buf;
        String sBuf = "?{QST:HW}";
        ByteBuffer buf2;
        Charset charset = Charset.forName("UTF-8");
        buf2 = charset.encode(sBuf);
        buf2.put(0, (byte) 0x1B);
        return buf2.array();
    }

    /**
     * this will print a file to the printer
     */
    void printFile() {
        String fileName = mTxtFilename.getText().toString();
        if (!fileName.endsWith("prn")) {
            myToast("Not a prn file!", "Error");
            return; //does not match file pattern for a print file
        }
        if (ipPrintService.getState() != ipPrintFile.STATE_CONNECTED) {
            myToast("Please connect first!", "Error");
            //PROBLEM: this Toast does not work!
            //Toast.makeText(this, "please connect first",Toast.LENGTH_LONG);
            return; //does not match file pattern for a print file
        }
        //do a query if escp
        if (fileName.startsWith("escp")) {
            byte[] bufQuery = escpQuery();
            ipPrintService.write(bufQuery);
        }
        if (mTxtFilename.length() > 0) {
            //TODO: add code
            InputStream inputStream = null;
            ByteArrayInputStream byteArrayInputStream;
            Integer totalWrite = 0;
            StringBuffer sb = new StringBuffer();
            try {
                //TODO: test if this is a storage file or a Assets resource file?
                if(fileName.startsWith("/")){
                    inputStream = new FileInputStream(fileName);
                    addLog("Using regular file: '"+fileName+"'");
                }
                else {
                    inputStream = this.getAssets().open(fileName);
                    addLog("Using demo file: '"+fileName+"'");
                }

                byte[] buf = new byte[2048];
                int readCount = 0;
                do {
                    readCount = inputStream.read(buf);
                    if (readCount > 0) {
                        totalWrite += readCount;
                        byte[] bufOut = new byte[readCount];
                        System.arraycopy(buf, 0, bufOut, 0, readCount);
                        ipPrintService.write(bufOut);
                    }
                } while (readCount > 0);
                inputStream.close();
                addLog(String.format("printed " + totalWrite.toString() + " bytes"));
            } catch (IOException e) {
                Log.e(TAG, "Exception in printFile: " + e.getMessage());
                addLog("printing failed!");
                //Toast.makeText(this, "printing failed!", Toast.LENGTH_LONG);
                myToast("Printing failed","Error");
            }
        } else {
            addLog("no demo file");
            //Toast.makeText(this, "no demo file", Toast.LENGTH_LONG);
            myToast("No demo file selected!","Error");
        }
    }

    void myToast(String sInfo, String sTitle){
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.toast_layout, (ViewGroup) findViewById(R.id.toast_layout_root));

        //assign a better icon
        try {
            ImageView imageView = (ImageView) layout.findViewById(R.id.toast_image);
            Resources resources = getResources();

            if (sTitle.startsWith("Error"))
                imageView.setImageDrawable(resources.getDrawable(R.drawable.exclamation));
            else
                imageView.setImageDrawable(resources.getDrawable(R.drawable.information));

        }catch(Exception e){
            Log.e(TAG, "can not assign toast image: "+ e.getMessage());
        }

        ((TextView) layout.findViewById(R.id.toast_text)).setText(sInfo);
        TextView txt = (TextView)layout.findViewById(R.id.toast_text);
        if(sInfo.length()>10) {
            float textSize=txt.getTextSize(); //size in pixels
            textSize=textSize/2;
            txt.setTextSize(textSize);
        }
        ((TextView) layout.findViewById(R.id.toast_title)).setText(sTitle);
        Toast toast = new Toast(getBaseContext());
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.setGravity(Gravity.CENTER,0,0);
        toast.show();
    }
    void myToast(String sInfo){
//        Message msg;
//        Bundle bundle=new Bundle();
//        msg = mHandler.obtainMessage(msgTypes.MESSAGE_TOAST);
//        bundle.putString(msgTypes.TOAST, sInfo);
//        msg.setData(bundle);
//        mHandler.sendMessage(msg);
        myToast(sInfo, "Information");
    }

    private void ensureDiscoverable() {
        if (D) Log.d(TAG, "ensure discoverable");
/*
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
*/
    }

    public void addLog(String s) {
        Log.d(TAG, s);
        mLog.append(s + "\r\n");
        // [http://stackoverflow.com/questions/3506696/auto-scrolling-textview-in-android-to-bring-text-into-view]
        // find the amount we need to scroll.  This works by
        // asking the TextView's internal layout for the position
        // of the final line and then subtracting the TextView's height
        int scrollAmount=0;
        try {
            scrollAmount = mLog.getLayout().getLineTop(mLog.getLineCount()) - mLog.getHeight();
        }catch(NullPointerException e){
            scrollAmount=0;
        }
        // if there is no need to scroll, scrollAmount will be <=0
        if (scrollAmount > 0)
            mLog.scrollTo(0, scrollAmount);
        else
            mLog.scrollTo(0, 0);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);
        return true;
    }

    boolean bDiscoveryStarted = false;

    void startDiscovery() {
        if (bDiscoveryStarted)
            return;
        bDiscoveryStarted = true;
        // Launch the DeviceListActivity to see devices and do scan

        //Intent serverIntent = new Intent(this, DeviceListActivity.class);
        //startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);

        Intent serverIntent = new Intent(this, ipListActivity.class);
        Bundle bundle=new Bundle();
        String sIP= String.valueOf(mRemoteDevice.getText());
        bundle.putString("startip", sIP);
        serverIntent.putExtras(bundle);
        startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
    }

    boolean bDemoFileListStarted = false;
    void startFileList() {
        if (bDemoFileListStarted)
            return;
        bDemoFileListStarted = true;
        Intent fileListerIntent = new Intent(this, FileListActivity.class);
        startActivityForResult(fileListerIntent, REQUEST_SELECT_DEMO_FILE);
    }

    boolean bFileBrowserStarted=false;
    void startFileBrowser(){
        if(bFileBrowserStarted)
            return;
        bFileBrowserStarted=true;
//        Intent fileBrowserIntent = new Intent(this, FileexplorerActivity.class);
//        //Intent fileBrowserIntent = new Intent(this, FileBrowserActivity.class);
//        startActivityForResult(fileBrowserIntent, REQUEST_SELECT_DEMO_FILE);
        Intent intent1 = new Intent(this, hgo.ipprint4.FileChooser.class);
        startActivityForResult(intent1, REQUEST_SELECT_FILE);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.mnuScan:
                startDiscovery();
                return true;
            case R.id.mnuDiscoverable:
                // Ensure this device is discoverable by others
                ensureDiscoverable();
                return true;
            case R.id.mnuFilelist:
                startFileList();
                return true;
            case R.id.mnuAbout:
                showAboutScreen();
                return true;
        }
        return false;
    }

    void showAboutScreen(){
        AlertDialog alertDialog = new AlertDialog.Builder(IpPrint4.this).create();
        alertDialog.setTitle("About");
        String sVersion = getResources().getString(R.string.versionstring);;
        alertDialog.setMessage(sVersion);
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alertDialog.show();
    }
    void printESCP() {
        if (ipPrintService != null) {
            if (ipPrintService.getState() == ipPrintFile.STATE_CONNECTED) {
                String message = ipPrintService.printESCP();
                byte[] buf = message.getBytes();
                ipPrintService.write(buf);
                addLog("ESCP printed");
            }
        }
    }

    private void setupComm() {
        // Initialize the array adapter for the conversation thread
        mConversationArrayAdapter = new ArrayAdapter<String>(this, R.id.remote_device);
        Log.d(TAG, "setupComm()");
        ipPrintService = new ipPrintFile(this, mHandler);
        if (ipPrintService == null)
            Log.e(TAG, "ipPrintService init() failed");
/*
        // Initialize the array adapter for the conversation thread
        mConversationArrayAdapter = new ArrayAdapter<String>(this, R.layout.message);
        mConversationView = (ListView) findViewById(R.id.in);
        mConversationView.setAdapter(mConversationArrayAdapter);

        // Initialize the compose field with a listener for the return key
        mOutEditText = (EditText) findViewById(R.id.edit_text_out);
        mOutEditText.setOnEditorActionListener(mWriteListener);

        // Initialize the send button with a listener that for click events
        mSendButton = (Button) findViewById(R.id.button_send);
        mSendButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                TextView view = (TextView) findViewById(R.id.edit_text_out);
                String message = view.getText().toString();
                sendMessage(message);
            }
        });

        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothChatService(this, mHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");
        */
    }

    // The Handler that gets information back from the ipPrintService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case msgTypes.MESSAGE_STATE_CHANGE:
                    Bundle bundle = msg.getData();
                    int status = bundle.getInt("state");
                    if (D)
                        Log.i(TAG, "handleMessage: MESSAGE_STATE_CHANGE: " + msg.arg1);  //arg1 was not used! by ipprintFile
                    setConnectState(msg.arg1);
                    switch (msg.arg1) {
                        case ipPrintFile.STATE_CONNECTED:
                            addLog("connected to: " + mConnectedDeviceName);
                            mConversationArrayAdapter.clear();
                            Log.i(TAG, "handleMessage: STATE_CONNECTED: " + mConnectedDeviceName);
                            break;
                        case ipPrintFile.STATE_CONNECTING:
                            addLog("connecting...");
                            Log.i(TAG, "handleMessage: STATE_CONNECTING: " + mConnectedDeviceName);
                            break;
                        case ipPrintFile.STATE_LISTEN:
                            addLog("connection ready");
                            Log.i(TAG, "handleMessage: STATE_LISTEN");
                            break;
                        case ipPrintFile.STATE_IDLE:
                            addLog("STATE_NONE");
                            Log.i(TAG, "handleMessage: STATE_NONE: not connected");
                            break;
                        case ipPrintFile.STATE_DISCONNECTED:
                            addLog("disconnected");
                            Log.i(TAG, "handleMessage: STATE_DISCONNECTED");
                            break;
                    }
                    break;
                case msgTypes.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    mConversationArrayAdapter.add("Me:  " + writeMessage);
                    break;
                case msgTypes.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    mConversationArrayAdapter.add(mConnectedDeviceName + ":  " + readMessage);
                    addLog("recv>>>" + readMessage);
                    break;
                case msgTypes.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(msgTypes.DEVICE_NAME);
                    //Toast.makeText(getApplicationContext(), "Connected to " + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    myToast(mConnectedDeviceName, "Connected");
                    Log.i(TAG, "handleMessage: CONNECTED TO: " + msg.getData().getString(msgTypes.DEVICE_NAME));
                    //printESCP();
                    updateConnectButton(false);

                    break;
                case msgTypes.MESSAGE_TOAST:
//                    Toast toast = Toast.makeText(getApplicationContext(), msg.getData().getString(msgTypes.TOAST), Toast.LENGTH_SHORT);//.show();
//                    toast.setGravity(Gravity.CENTER,0,0);
//                    toast.show();
                    myToast(msg.getData().getString(msgTypes.TOAST));
                    Log.i(TAG, "handleMessage: TOAST: " + msg.getData().getString(msgTypes.TOAST));
                    addLog(msg.getData().getString(msgTypes.TOAST));
                    break;
                case msgTypes.MESSAGE_INFO:
                    addLog(msg.getData().getString(msgTypes.INFO));
                    //mLog.append(msg.getData().getString(msgTypes.INFO));
                    //mLog.refreshDrawableState();
                    String s = msg.getData().getString(msgTypes.INFO);
                    if (s.length() == 0)
                        s = String.format("int: %i" + msg.getData().getInt(msgTypes.INFO));
                    Log.i(TAG, "handleMessage: INFO: " + s);
                    break;
            }
        }
    };

    void disconnectDevice(){
        if(ipPrintService==null)
            return;
        if (ipPrintService.getState() == ipPrintFile.STATE_CONNECTED) {
            ipPrintService.stop();
            setConnectState(ipPrintFile.STATE_DISCONNECTED);
            //return;
        }
    }
    void connectToDevice(String remote) {
        //String remote = mRemoteDevice.getText().toString();
        if (remote.length() == 0)
            return;
        //disconnect first
        if (ipPrintService.getState() == ipPrintFile.STATE_CONNECTED) {
            ipPrintService.stop();
            setConnectState(ipPrintFile.STATE_DISCONNECTED);
            //return;
        }

        InetAddress iAddress;
        try {
            iAddress = InetAddress.getByName(remote);
        }catch (UnknownHostException e){
            myToast("Invalid IP host");
            remote=null;
        }

        if (remote != null) {
            addLog("connecting to " + remote);
            ipPrintService.connect(remote);
        } else {
            addLog("unknown remote device!");
        }
    }

    void connectToDevice1(String _device) {
        if (_device != null) {
            addLog("connecting to " + _device);
            ipPrintService.connect(_device);
        } else {
            addLog("unknown remote device!");
        }
    }


    //handles the scan devices and file list activity (dialog)
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (D) Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
            case REQUEST_SELECT_FILE:
                if (resultCode == RESULT_OK) {
                    String curFileName = data.getStringExtra("GetFileName");
                    String curPath = data.getStringExtra("GetPath");
                    if(!curPath.endsWith("/")) {
                        curPath += "/";
                    }
                    String fullName=curPath+curFileName;
                    mTxtFilename.setText(fullName);
                    addLog("Filebrowser Result_OK: '"+fullName+"'");
                }
                bFileBrowserStarted=false;
                break;

            case REQUEST_SELECT_DEMO_FILE:
                addLog("onActivityResult: requestCode==REQUEST_SELECT_DEMO_FILE");
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    addLog("resultCode==OK");
                    // Get the device MAC address
                    String file = data.getExtras().getString(FileListActivity.EXTRA_FILE_NAME);
                    addLog("onActivityResult: got file=" + file);
                    if (printFileXML != null) {
                        PrintFileDetails details = printFileXML.getPrintFileDetails(file);
                        addLog("printfile is:\n" + details.toString());
                        //Toast.makeText(this, "You selected:\n" + details.toString(), Toast.LENGTH_LONG);
                        myToast("Demo File:\n"+details.toString());
                    }

                    mTxtFilename.setText(file);
                    //mRemoteDevice.setText(device.getAddress());
                    // Attempt to connect to the device
                }
                bDemoFileListStarted = false;
                break;
            case REQUEST_CONNECT_DEVICE:    //called from List activity with new device/host address
                addLog("onActivityResult: requestCode==REQUEST_CONNECT_DEVICE");
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    addLog("resultCode==OK");
                    // Get the device address

                    String address = data.getExtras().getString("host");// mRemoteDevice.getText().toString();
                    addLog("iplist OK with '"+address+"'");
                    mRemoteDevice.setText(address);
                    // Attempt to connect to the device
                    addLog("onActivityResult: connecting device...");
                    //ipPrintService.connect(device);
                    //disconnect existing connection?
                    connectToDevice(address);
                }
                bDiscoveryStarted = false;
                break;
/*
            case REQUEST_ENABLE_BT:
                addLog("requestCode==REQUEST_ENABLE_BT");
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    Log.i(TAG, "onActivityResult: resultCode==OK");
                    // Bluetooth is now enabled, so set up a chat session
                    Log.i(TAG, "onActivityResult: starting setupComm()...");
                    setupComm();
                } else {
                    // User did not enable Bluetooth or an error occured
                    Log.d(TAG, "onActivityResult: BT not enabled");
                    Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                    finish();
                }
*/
        }
    }

    void setConnectState(Integer iState) {
        switch (iState) {
            case ipPrintFile.STATE_CONNECTED:
                updateConnectButton(true);
                break;
            case ipPrintFile.STATE_DISCONNECTED:
                updateConnectButton(false);
                break;
            case ipPrintFile.STATE_CONNECTING:
                addLog("connecting...");
                break;
            case ipPrintFile.STATE_LISTEN:
                addLog("listening...");
                break;
            case ipPrintFile.STATE_IDLE:
                addLog("state none");
                break;
            default:
                addLog("unknown state var " + iState.toString());
        }
    }

    void updateConnectButton(boolean bConnected){
        if(bConnected) {
            mConnectButton.setText(R.string.button_disconnect_text);
            mConnectButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.disconnectme, 0, 0, 0);
        }
        else{
            mConnectButton.setText(R.string.button_connect_text);
            mConnectButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.connectme, 0,0,0);
        }
    }


}