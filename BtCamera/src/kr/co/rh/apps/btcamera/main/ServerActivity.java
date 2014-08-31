package kr.co.rh.apps.btcamera.main;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import kr.co.rh.apps.btcamera.R;
import kr.co.rh.apps.btcamera.bluetooth.service.BluetoothChatService;
import kr.co.rh.apps.btcamera.camera.CameraPreview;
import kr.co.rh.apps.btcamera.comm.Constants;
import kr.co.rh.apps.btcamera.comm.SingleMediaScanner;
import kr.co.rh.apps.btcamera.comm.Constants.BtProtocol;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

public class ServerActivity extends Activity implements CameraPreview.IFChangeImage {

    //camera
    private CameraPreview mCameraPreview;

    private ImageView mNextView;

    private Bitmap mBitmap;

    private Button btnSaveImg, btnChgResolution;

    // Debugging
    private static final String TAG = "BluetoothChat";

    private static final boolean D = true;

    // Name of the connected device
    private String mConnectedDeviceName = null;

    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;

    // Member object for the chat services
    private BluetoothChatService mChatService = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_main);

        mCameraPreview = (CameraPreview)findViewById(R.id.preView);
        mCameraPreview.setChangeImage(this);
        mNextView = (ImageView)findViewById(R.id.nextView);
        btnSaveImg = (Button)findViewById(R.id.btnSaveImg);
        btnSaveImg.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                mCameraPreview.takePicture();
            }
        });

        btnChgResolution = (Button)findViewById(R.id.btnChgResolution);
        btnChgResolution.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                List<String> list = mCameraPreview.getStringPictureSizes();
                final ArrayAdapter<String> adapter = new ArrayAdapter<String>(ServerActivity.this,
                                                                              android.R.layout.simple_list_item_single_choice,
                                                                              list);
                AlertDialog.Builder alert = new AlertDialog.Builder(ServerActivity.this);
                alert.setAdapter(adapter, new OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        String size = adapter.getItem(which);
                        mCameraPreview.chgPictureSize(size);

                        dialog.dismiss();

                    }
                });                
                
                
                
                alert.show();
            }
        });

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

    }

    @Override
    protected void onStart() {
        super.onStart();

        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, Constants.REQUEST_ENABLE_BT);
            // Otherwise, setup the chat session
        } else {
            if (mChatService == null) {
                setupChat();
            }
        }
    }

    /**
     * activity stop 시 다른 task 정지를 위한 구분값
     */
    private boolean isStop = false;

    private boolean isPause = false;

    @Override
    protected void onResume() {
        super.onResume();
        isPause = false;

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
    }

    @Override
    protected void onPause() {
        super.onPause();
        isPause = true;
    }

    @Override
    public void chgImage(final byte[] data, final Camera camera) {
        Log.e("ssryu", "chgImage : " + System.currentTimeMillis() / 1000);
        //event break
        if (isStop || isPause) {
            return;
        }

        Camera.Parameters params = camera.getParameters();
        int w = params.getPreviewSize().width;
        int h = params.getPreviewSize().height;
        int format = params.getPreviewFormat();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Rect area = new Rect(0, 0, w, h);

        Log.e("ssryu", "width : " + w + ", height : " + h);
        //screen size
//            		Size size1 = params.getPreviewSize();

        //image size
//        Size size = params.getPictureSize();

//        int max = size.height > size.width ? size.height : size.width;
//        int per = 2;
//
//        while (max >= 512) {
//            max = max / per;
//            per += 2;
//        }

//        BitmapFactory.Options opt = new BitmapFactory.Options();
//        opt.inSampleSize = per;

        YuvImage image = new YuvImage(data, format, w, h, null);
        image.compressToJpeg(area, 20, out);

        Log.e("ssryu", "area : " + area);

        if (mBitmap != null) {
            mBitmap.recycle();
        }

        //    		try {
        isStop = true;
        //    			
        //    			//thumbnail image change time delay
        //    			Thread.sleep(500);
        //    			
        //    		} catch (InterruptedException e) {
        //    			e.printStackTrace();
        //    		}	

        isStop = false;
//        mBitmap = BitmapFactory.decodeByteArray(out.toByteArray(), 0, out.size(), opt);
        //    		mNextView.setImageBitmap(mBitmap);

        if (mChatService != null) {
            //				mNextView.buildDrawingCache();
            //				Bitmap bmap = mNextView.getDrawingCache();
            //    			Log.e("ssryu", "before time : " + System.currentTimeMillis()/1000);
            //            Log.e("ssryu", "Bitmap size : " + mBitmap.getByteCount());
//            mChatService.write(mBitmap);
                        mChatService.writes(out.toByteArray());
            Log.e("ssryu", "after time : " + System.currentTimeMillis() / 1000);
        }

    }

    /**
     * 파일저장
     */
    @Override
    public void saveImage(byte[] data) {
        String path = Environment.getExternalStorageDirectory() + File.separator + "test";
        String fileNm = File.separator + "1.jpg";
        FileOutputStream fo = null;

        try {
            File f = new File(path);
            if (!f.exists()) {
                f.mkdirs();
            }

            fo = new FileOutputStream(path + fileNm);
            fo.write(data);
            fo.close();
        } catch (FileNotFoundException fne) {
            fne.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fo != null) {
                try {
                    fo.close();
                    fo = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        //media scan 

        //kitkat
        File scanFile = new File(path + fileNm);
        SingleMediaScanner mediaScanner = new SingleMediaScanner(ServerActivity.this, scanFile);

        //ice

    }

    private void setupChat() {
        Log.d(TAG, "setupChat()");

        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothChatService(this, mHandler);

        ensureDiscoverable();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Stop the Bluetooth chat services
        if (mChatService != null) {
            mChatService.stop();
        }
    }

    private void ensureDiscoverable() {
        if (D) {
            Log.d(TAG, "ensure discoverable");
        }
        if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    if (D) {
                        Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                    }
                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:
                            //                        mConversationArrayAdapter.clear();
                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                            break;
                        case BluetoothChatService.STATE_LISTEN:
                        case BluetoothChatService.STATE_NONE:
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    //                    byte[] writeBuf = (byte[]) msg.obj;
                    //                    // construct a string from the buffer
                    //                    String writeMessage = new String(writeBuf);
                    ////                    mConversationArrayAdapter.add("Me:  " + writeMessage);
                    break;
                case Constants.MESSAGE_READ:
                	if(msg.arg1 == BtProtocol.CODE_SHUTTER){
                		btnSaveImg.performClick();
                	}
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    Toast.makeText(getApplicationContext(), "Connected to " + mConnectedDeviceName, Toast.LENGTH_SHORT)
                         .show();
                    break;
                case Constants.MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(),
                                   msg.getData().getString(Constants.TOAST),
                                   Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (D) {
            Log.d(TAG, "onActivityResult " + resultCode);
        }
        switch (requestCode) {
            case Constants.REQUEST_CONNECT_DEVICE_SECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, true);
                }
                break;
            case Constants.REQUEST_CONNECT_DEVICE_INSECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, false);
                }
                break;
            case Constants.REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    setupChat();
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                    finish();
                }
        }
    }

    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mChatService.connect(device, secure);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent serverIntent = null;
        switch (item.getItemId()) {
            case R.id.secure_connect_scan:
                // Launch the DeviceListActivity to see devices and do scan
                serverIntent = new Intent(this, DeviceListActivity.class);
                startActivityForResult(serverIntent, Constants.REQUEST_CONNECT_DEVICE_SECURE);
                return true;
            case R.id.insecure_connect_scan:
                // Launch the DeviceListActivity to see devices and do scan
                serverIntent = new Intent(this, DeviceListActivity.class);
                startActivityForResult(serverIntent, Constants.REQUEST_CONNECT_DEVICE_INSECURE);
                return true;
            case R.id.discoverable:
                // Ensure this device is discoverable by others
                ensureDiscoverable();
                return true;
        }
        return false;
    }
}
