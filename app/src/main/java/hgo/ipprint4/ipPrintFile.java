package hgo.ipprint4;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import android.annotation.TargetApi;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * Created by hgode on 04.04.2014.
 */
public class ipPrintFile {

    private int socketPort=9100;
    private int iTimeOut=5000;

    public ipPrintFile(Context context, Handler handler)
    {
        log("ipPrintFile()");
        _context=context;
        mHandler=handler;
        //_btMAC=sBTmac;
        //_sFile=sFileName;
        mState=STATE_IDLE;

        //mAdapter=BluetoothAdapter.getDefaultAdapter();

        addText("ipPrintFile initialized 1");
    }

    public static boolean isNetworkOnline(Context context) {
        boolean status=false;
        try{
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = cm.getNetworkInfo(0);
            if (netInfo != null && netInfo.getState()==NetworkInfo.State.CONNECTED) {
                status= true;
            }else {
                netInfo = cm.getNetworkInfo(1);
                if(netInfo!=null && netInfo.getState()==NetworkInfo.State.CONNECTED)
                    status= true;
            }
        }catch(Exception e){
            e.printStackTrace();
            return false;
        }
        return status;
    }

    // init a new btPrint with a conext for callbacks
    // the BT MAC as string and the file to be printed
    public ipPrintFile(Handler handler, String sIPaddr, String sFileName)
    {
        log("ipPrintFile()");
        //_context=context;
        mHandler=handler;
        _IPaddr=sIPaddr;
        _sFile=sFileName;
        mState=STATE_IDLE;

        //mAdapter=BluetoothAdapter.getDefaultAdapter();

        addText("ipPrintFile initialized 2");
    }
    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume() */
    public synchronized void start() {
        log("start");
        addText("start()");

        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        setState(STATE_IDLE);   //idle
        addText("start done.");
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {
        log("stop");
        addText("stop()");
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}
        setState(STATE_DISCONNECTED);
        addText("stop() done.");
    }

    //vars
    // Debugging
    private static final String TAG = "ipPrintFile";
    private static final boolean D = true;

    private Context _context=null;
    private String	_IPaddr="";
    private String _sFile="";

    private String mDevice=null;

    private Handler mHandler=null;
    private int mState;

    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;

    private static final UUID UUID_SPP = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // Constants that indicate the current connection state
    public static final int STATE_IDLE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device
    public static final int STATE_DISCONNECTED = 4;  // now connected to a remote device

    // Message types sent from the BluetoothChatService Handler
//    public static final int MESSAGE_STATE_CHANGE = 1;
//    public static final int MESSAGE_READ = 2;
//    public static final int MESSAGE_WRITE = 3;
//    public static final int MESSAGE_DEVICE_NAME = 4;
//    public static final int MESSAGE_TOAST = 5;
//
//    // Key names received from the BluetoothChatService Handler
//    public static final String DEVICE_NAME = "device_name";
//    public static final String TOAST = "toast";

//    private final String msgState="STATE";


    /**
     * Return the current connection state. */
    public synchronized int getState() {
        return mState;
    }
    private synchronized void setState(int state) {
        if (D) Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;

        addText(msgTypes.STATE, state);
        // Give the new state to the Handler so the UI Activity can update
//        Message msg = new Message();// mHandler.obtainMessage(_Activity.MESSAGE_DEVICE_NAME);
//        Bundle bundle = new Bundle();
//        bundle.putInt("STATE", state);
//        msg.setData(bundle);
//        mHandler.sendMessage(msg);
        //mHandler.obtainMessage(BluetoothChat.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    public String printESCP()
    {
        String message = "w(          FUEL CITY\r\n" +
                "       8511 WHITESBURG DR\r\n" +
                "      HUNTSVILLE, AL 35802\r\n" +
                "         (256)585-6389\r\n\r\n" +
                " Merchant ID: 1312\r\n" +
                " Ref #: 0092\r\n\r\n" +
                "w)      Sale\r\n" +
                "w( XXXXXXXXXXX4003\r\n" +
                " AMEX       Entry Method: Swiped\r\n\r\n\r\n" +
                " Total:               $    53.22\r\n\r\n\r\n" +
                " 12/21/12               13:41:23\r\n" +
                " Inv #: 000092 Appr Code: 565815\r\n" +
                " Transaction ID: 001194600911275\r\n" +
                " Apprvd: Online   Batch#: 000035\r\n\r\n\r\n" +
                "          Cutomer Copy\r\n" +
                "           Thank You!\r\n\r\n\r\n\r\n";
        return message;
    }
    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     * @param device  The BluetoothDevice to connect
     */
    public synchronized void connect(String device) {
        if (D) Log.d(TAG, "connect to: " + device);
        addText("connecting to "+device);
        mDevice=device;
        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
            addText("already connected. Disconnecting first");
            if (mConnectThread != null)
            {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        addText("new connect thread started");
        setState(STATE_CONNECTING);
    }

    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private Socket mmSocket=null;
        private InetAddress serverIP;
        private SocketAddress socketAddress=null;

        //private final BluetoothDevice mmDevice;

        @TargetApi(Build.VERSION_CODES.GINGERBREAD_MR1)
        public ConnectThread(String sIPremote) {
            _IPaddr=sIPremote;
            try {
                addText("get host IP");
                serverIP=InetAddress.getByName(_IPaddr);
                socketAddress=new InetSocketAddress(serverIP, socketPort);
                //tmp = device.createRfcommSocketToServiceRecord(UUID_SPP);
            }catch (UnknownHostException e){
                Log.e(TAG, "ConnectThread create() failed", e);
            }
            catch (IOException e) {
                Log.e(TAG, "ConnectThread create() failed", e);
            }
        }
        @Override
        public void run() {
            Log.i(TAG, "ConnectThread::run()");
            setName("ConnectThread");
            Socket tmp = null;

            // Make a connection to the Socket
            try {

                addText("new Socket()...");
                // This is a blocking call and will only return on a
                // successful connection or an exception
                //tmp=new Socket(serverIP, socketPort);
                tmp=new Socket();
                tmp.connect(socketAddress, iTimeOut);
                addText("new socket() done");
                mmSocket=tmp;
            }
            catch(IllegalArgumentException e){
                addText("IllegalArgumentException: " + e.getMessage());
                //if new Socket() failed
                connectionFailed();
                addText("Connect failed");
                if(mmSocket!=null) {
                    // Close the socket
                    try {
                        mmSocket.close();
                        tmp = null;
                    } catch (IOException e2) {
                        Log.e(TAG, "unable to close() socket during connection failure", e2);
                    }
                }
                // Start the service over to restart listening mode
//                ipPrintFile.this.start();
                return;
            }
            catch (IOException e){
                addText("IOException: " + e.getMessage());
                //if new Socket() failed
                connectionFailed();
                addText("Connect failed");
                if(mmSocket!=null) {
                    // Close the socket
                    try {
                        mmSocket.close();
                        tmp = null;
                    } catch (IOException e2) {
                        Log.e(TAG, "unable to close() socket during connection failure", e2);
                    }
                }
                // Start the service over to restart listening mode
//                ipPrintFile.this.start();
                return;
            }
            catch (Exception e) {
                //if new Socket() failed
                connectionFailed();
                addText("Connect failed");
                if(mmSocket!=null) {
                    // Close the socket
                    try {
                        mmSocket.close();
                        tmp = null;
                    } catch (IOException e2) {
                        Log.e(TAG, "unable to close() socket during connection failure", e2);
                    }
                }
                // Start the service over to restart listening mode
                /*
                ipPrintFile.this.start();
                */
                return;
            }//catch

            // Reset the ConnectThread because we're done
            synchronized (ipPrintFile.this) {
                mConnectThread = null;
            }
            // start listening mode
            ipPrintFile.this.start();

            // Start the connected thread
            connected(mmSocket);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final Socket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(Socket socket) {
            Log.d(TAG, "create ConnectedThread");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[1024];
            int bytes;

            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);

                    // Send the obtained bytes to the UI Activity
                    mHandler.obtainMessage(msgTypes.MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                } catch (SocketException e) {
                    Log.e(TAG, "socket exception", e);
                    connectionLost();
                    break;
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    break;
                }
            }
        }

        /**
         * Write to the connected OutStream.
         * @param buffer  The bytes to write
         */
        public void write(byte[] buffer) {
            addText("write...");
            try {
                mmOutStream.write(buffer);

                // Share the sent message back to the UI Activity
                mHandler.obtainMessage(msgTypes.MESSAGE_WRITE, -1, -1, buffer)
                        .sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
            addText("write done");
        }

        public void cancel() {
            addText("cancel");
            try {
                if(mmSocket!=null)
                    mmSocket.close();
            }catch (NullPointerException e){
                Log.e(TAG, "close() of connect socket failed", e);
            }catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     * @param socket  The BluetoothSocket on which the connection was made
     */
    public synchronized void connected(Socket socket) {
        if (D) Log.d(TAG, "connected");

        // Cancel the thread that completed the connection
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

        //
        String iAddress ="unknown host";
        try {
            iAddress = socket.getInetAddress().getHostName();
        }catch(Exception e){

        }
        // Send the name of the connected device back to the UI Activity
        Message msg = mHandler.obtainMessage(msgTypes.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(msgTypes.DEVICE_NAME, iAddress);
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        setState(STATE_CONNECTED);
    }
    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
        addText("connectionLost()");
        setState(STATE_DISCONNECTED);

        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(msgTypes.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(msgTypes.TOAST, "Device connection was lost");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     * @param out The bytes to write
     * @see hgo.ipprint4.ipPrintFile.ConnectedThread#write(byte[])
     */
    public void write(byte[] out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }
    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {
        addText("connectionFailed()");
        setState(STATE_DISCONNECTED);

        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(msgTypes.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(msgTypes.TOAST, "Toast: connectionFailed");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }
    //helpers
    void addText(String s){
        try{
            Message msg = mHandler.obtainMessage(msgTypes.MESSAGE_INFO);
            Bundle bundle = new Bundle();
            bundle.putString(msgTypes.INFO , "INFO: " + s);
            msg.setData(bundle);
            mHandler.sendMessage(msg);
        }catch(NullPointerException e){
            ;
        }
        catch (Exception e){
            ;
        }
    }
    void addText(String msgType, int state){
        // Give the new state to the Handler so the UI Activity can update
        msgTypes type;
        Message msg;
        Bundle bundle = new Bundle();
        if(msgType.equals(msgTypes.STATE)){
            msg = mHandler.obtainMessage(msgTypes.MESSAGE_STATE_CHANGE);// mHandler.obtainMessage(_Activity.MESSAGE_DEVICE_NAME);
        }
        else if(msgType.equals(msgTypes.DEVICE_NAME)){
            msg = mHandler.obtainMessage(msgTypes.MESSAGE_DEVICE_NAME);
        }
        else if(msgType.equals(msgTypes.INFO)){
            msg = mHandler.obtainMessage(msgTypes.MESSAGE_INFO);
        }
        else if(msgType.equals(msgTypes.TOAST)){
            msg = mHandler.obtainMessage(msgTypes.MESSAGE_TOAST);
        }
        else if(msgType.equals(msgTypes.READ)){
            msg = mHandler.obtainMessage(msgTypes.MESSAGE_READ);
        }
        else if(msgType.equals(msgTypes.WRITE)){
            msg = mHandler.obtainMessage(msgTypes.MESSAGE_WRITE);
        }
        else {
            msg = new Message();
        }
        //msg = mHandler.obtainMessage(msgTypes.MESSAGE_STATE_CHANGE);// mHandler.obtainMessage(_Activity.MESSAGE_DEVICE_NAME);
        //mHandler.obtainMessage(BluetoothChat.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
        bundle.putInt(msgType, state);
        msg.setData(bundle);
        msg.arg1=state;             //we can use arg1 or the bundle to provide additional information to the message handler
        mHandler.sendMessage(msg);
        Log.i(TAG, "addText: "+msgType+", state="+state);
    }
    void log(String msg){
        if(D) Log.d(TAG, msg);
    }

}
