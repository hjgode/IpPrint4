package hgo.ipprint4;

/**
 * Created by hgode on 04.04.2014.
 */
public class msgTypes {

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";
    public static final String INFO = "info";
    public static final String STATE = "state";
    public static final String READ = "read";
    public static final String WRITE = "write";

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    public static final int MESSAGE_INFO = 6;

    //msgTypes used by ipListActivity
    public static final int addHost= 10;
    public static final int finished=11;
    public static final int started= 12;
    public static final int stopped= 13;
    public static final int error= 14;

    public static final String HOST_NAME = "device_name";

}
