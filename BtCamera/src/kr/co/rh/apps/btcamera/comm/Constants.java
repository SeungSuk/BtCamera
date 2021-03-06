package kr.co.rh.apps.btcamera.comm;

public class Constants {

    public static final int MESSAGE_STATE_CHANGE = 1;

    public static final int MESSAGE_READ = 2;

    public static final int MESSAGE_WRITE = 3;

    public static final int MESSAGE_DEVICE_NAME = 4;

    public static final int MESSAGE_TOAST = 5;

    public static final String DEVICE_NAME = "device_name";

    public static final String TOAST = "toast";

    public static final int REQUEST_CONNECT_DEVICE_SECURE = 1;

    public static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;

    public static final int REQUEST_ENABLE_BT = 3;

    public class BtProtocol {

        public static final int CODE_IMAGE = 1;

        public static final int CODE_SHUTTER = 2;

        public static final int CODE_REVOLUTION = 3;

        public static final int CODE_REVOLUTION_LIST = 4;

        public static final int CODE_REQUEST_INIT = 5;

        public static final int OPTION_NULL = 0;
    }

}
