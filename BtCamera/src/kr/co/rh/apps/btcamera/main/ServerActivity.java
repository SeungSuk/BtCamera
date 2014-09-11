package kr.co.rh.apps.btcamera.main;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import kr.co.rh.apps.btcamera.R;
import kr.co.rh.apps.btcamera.bluetooth.service.BluetoothChatService;
import kr.co.rh.apps.btcamera.camera.CameraPreview;
import kr.co.rh.apps.btcamera.comm.Constants;
import kr.co.rh.apps.btcamera.comm.Constants.BtProtocol;
import kr.co.rh.apps.btcamera.comm.SingleMediaScanner;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.Face;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

public class ServerActivity extends Activity implements CameraPreview.IFCameraPreviewTask {

    // camera
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

    DrawingView drawingView;

    Face[] detectedFaces;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_main);

        drawingView = new DrawingView(this);
        LayoutParams layoutParamsDrawing = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        this.addContentView(drawingView, layoutParamsDrawing);

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

        findViewById(R.id.btnFocus).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                mCameraPreview.autoFocusShow();
            }
        });

        btnChgResolution = (Button)findViewById(R.id.btnChgResolution);
        btnChgResolution.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                List<String> list = mCameraPreview.getStringPictureSizes();

                if (list == null) {
                    return;
                }

                String size = mCameraPreview.getNowPictureSize();
                int select = 0;

                for (int i = 0; i < list.size(); i++) {
                    String tmp = list.get(i);
                    if (tmp.equals(size)) {
                        select = i;
                        break;
                    }
                }

                final ArrayAdapter<String> adapter = new ArrayAdapter<String>(ServerActivity.this,
                                                                              android.R.layout.simple_list_item_single_choice,
                                                                              list);
                AlertDialog.Builder alert = new AlertDialog.Builder(ServerActivity.this);

                alert.setSingleChoiceItems(adapter, select, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String size = adapter.getItem(which);
                        mCameraPreview.chgPictureSize(size);
                        mChatService.writeCodeString(BtProtocol.CODE_REVOLUTION, size);
                        // 상태 저장
                        SharedPreferences prefs = getSharedPreferences("PrefName", MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString("PICTURE_SIZE", size);
                        editor.commit();

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
        // onResume() will be called when ACTION_REQUEST_ENABLE activity
        // returns.
        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't
            // started already
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
        // event break
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
        // screen size
        // Size size1 = params.getPreviewSize();

        // image size
        // Size size = params.getPictureSize();

        // int max = size.height > size.width ? size.height : size.width;
        // int per = 2;
        //
        // while (max >= 512) {
        // max = max / per;
        // per += 2;
        // }

        // BitmapFactory.Options opt = new BitmapFactory.Options();
        // opt.inSampleSize = per;

        YuvImage image = new YuvImage(data, format, w, h, null);
        image.compressToJpeg(area, 20, out);

        Log.e("ssryu", "area : " + area);

        if (mBitmap != null) {
            mBitmap.recycle();
        }

        // try {
        isStop = true;
        //
        // //thumbnail image change time delay
        // Thread.sleep(500);
        //
        // } catch (InterruptedException e) {
        // e.printStackTrace();
        // }

        isStop = false;
        // mBitmap = BitmapFactory.decodeByteArray(out.toByteArray(), 0,
        // out.size(), opt);
        // mNextView.setImageBitmap(mBitmap);

        if (mChatService != null) {
            // mNextView.buildDrawingCache();
            // Bitmap bmap = mNextView.getDrawingCache();
            // Log.e("ssryu", "before time : " +
            // System.currentTimeMillis()/1000);
            // Log.e("ssryu", "Bitmap size : " + mBitmap.getByteCount());
            // mChatService.write(mBitmap);
            mChatService.writes(out.toByteArray());
            Log.e("ssryu", "after time : " + System.currentTimeMillis() / 1000);
        }

    }

    private final static String PATH_NAME = "bt_camera";

    private final static String FILE_NAME = "%s_%s.jpg";

    /**
     * 파일저장
     */
    @Override
    public void saveImage(byte[] data) {
        String path = Environment.getExternalStorageDirectory() + File.separator + PATH_NAME;
        // String fileNm = File.separator + "1.jpg";

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        Date today = new Date();
        String format = sdf.format(today);

        String fileNm = File.separator + String.format(FILE_NAME, PATH_NAME, format);

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

        // media scan

        // kitkat
        File scanFile = new File(path + fileNm);
        SingleMediaScanner mediaScanner = new SingleMediaScanner(ServerActivity.this, scanFile);

        // ice

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
                            // mConversationArrayAdapter.clear();

                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                            break;
                        case BluetoothChatService.STATE_LISTEN:
                        case BluetoothChatService.STATE_NONE:
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    // byte[] writeBuf = (byte[]) msg.obj;
                    // // construct a string from the buffer
                    // String writeMessage = new String(writeBuf);
                    // // mConversationArrayAdapter.add("Me:  " + writeMessage);
                    break;
                case Constants.MESSAGE_READ:
                    if (msg.arg1 == BtProtocol.CODE_SHUTTER) {
                        btnSaveImg.performClick();
                    } else if (msg.arg1 == BtProtocol.CODE_REQUEST_INIT) {
                        List<String> pictureSizes = mCameraPreview.getStringPictureSizes();
                        String data = "";
                        for (int i = 0, size = pictureSizes.size(); i < size; i++) {
                            if (i == 0) {
                                data += pictureSizes.get(i);
                            } else {
                                data += "&" + pictureSizes.get(i);
                            }

                        }
                        mChatService.writeCodeString(BtProtocol.CODE_REVOLUTION_LIST, data);
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

    //
    //	@Override
    //	public boolean onCreateOptionsMenu(Menu menu) {
    //		MenuInflater inflater = getMenuInflater();
    //		inflater.inflate(R.menu.option_menu, menu);
    //		return true;
    //	}
    //
    //	@Override
    //	public boolean onOptionsItemSelected(MenuItem item) {
    //		Intent serverIntent = null;
    //		switch (item.getItemId()) {
    //		case R.id.secure_connect_scan:
    //			// Launch the DeviceListActivity to see devices and do scan
    //			serverIntent = new Intent(this, DeviceListActivity.class);
    //			startActivityForResult(serverIntent,
    //					Constants.REQUEST_CONNECT_DEVICE_SECURE);
    //			return true;
    //		case R.id.insecure_connect_scan:
    //			// Launch the DeviceListActivity to see devices and do scan
    //			serverIntent = new Intent(this, DeviceListActivity.class);
    //			startActivityForResult(serverIntent,
    //					Constants.REQUEST_CONNECT_DEVICE_INSECURE);
    //			return true;
    //		case R.id.discoverable:
    //			// Ensure this device is discoverable by others
    //			ensureDiscoverable();
    //			return true;
    //		}
    //		return false;
    //	}

    @Override
    public void touchFocus(final Rect tfocusRect) {

        // Convert from View's width and height to +/- 1000
        final Rect targetFocusRect = new Rect(tfocusRect.left * 2000 / drawingView.getWidth() - 1000,
                                              tfocusRect.top * 2000 / drawingView.getHeight() - 1000,
                                              tfocusRect.right * 2000 / drawingView.getWidth() - 1000,
                                              tfocusRect.bottom * 2000 / drawingView.getHeight() - 1000);

        mCameraPreview.touchFocus(targetFocusRect);

        drawingView.setHaveTouch(true, tfocusRect);
        drawingView.invalidate();
    }

    // FaceDetectionListener faceDetectionListener
    // = new FaceDetectionListener(){
    //
    // @Override
    // public void onFaceDetection(Face[] faces, Camera tcamera) {
    //
    // if (faces.length == 0){
    // //prompt.setText(" No Face Detected! ");
    // drawingView.setHaveFace(false);
    // }else{
    // //prompt.setText(String.valueOf(faces.length) + " Face Detected :) ");
    // drawingView.setHaveFace(true);
    // detectedFaces = faces;
    //
    // //Set the FocusAreas using the first detected face
    // List<Camera.Area> focusList = new ArrayList<Camera.Area>();
    // Camera.Area firstFace = new Camera.Area(faces[0].rect, 1000);
    // focusList.add(firstFace);
    //
    // Parameters para = camera.getParameters();
    //
    // if(para.getMaxNumFocusAreas()>0){
    // para.setFocusAreas(focusList);
    // }
    //
    // if(para.getMaxNumMeteringAreas()>0){
    // para.setMeteringAreas(focusList);
    // }
    //
    // camera.setParameters(para);
    //
    // buttonTakePicture.setEnabled(false);
    //
    // //Stop further Face Detection
    // camera.stopFaceDetection();
    //
    // buttonTakePicture.setEnabled(false);
    //
    // /*
    // * Allways throw java.lang.RuntimeException: autoFocus failed
    // * if I call autoFocus(myAutoFocusCallback) here!
    // *
    // camera.autoFocus(myAutoFocusCallback);
    // */
    //
    // //Delay call autoFocus(myAutoFocusCallback)
    // myScheduledExecutorService = Executors.newScheduledThreadPool(1);
    // myScheduledExecutorService.schedule(new Runnable(){
    // public void run() {
    // camera.autoFocus(myAutoFocusCallback);
    // }
    // }, 500, TimeUnit.MILLISECONDS);
    //
    // }
    //
    // drawingView.invalidate();
    //
    // }};

    private class DrawingView extends View {

        /*
         * 참고 http://android-er.blogspot.kr/2012/04/touch-to-select-focus-and-metering -area.html
         */

        boolean haveFace;

        Paint drawingPaint;

        boolean haveTouch;

        Rect touchArea;

        public DrawingView(Context context) {
            super(context);
            haveFace = false;
            drawingPaint = new Paint();
            drawingPaint.setColor(Color.GREEN);
            drawingPaint.setStyle(Paint.Style.STROKE);
            drawingPaint.setStrokeWidth(2);

            haveTouch = false;
        }

        public void setHaveFace(boolean h) {
            haveFace = h;
        }

        public void setHaveTouch(boolean t, Rect tArea) {
            haveTouch = t;
            touchArea = tArea;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if (haveFace) {

                // Camera driver coordinates range from (-1000, -1000) to (1000,
                // 1000).
                // UI coordinates range from (0, 0) to (width, height).

                int vWidth = getWidth();
                int vHeight = getHeight();

                for (int i = 0; i < detectedFaces.length; i++) {

                    if (i == 0) {
                        drawingPaint.setColor(Color.GREEN);
                    } else {
                        drawingPaint.setColor(Color.RED);
                    }

                    int l = detectedFaces[i].rect.left;
                    int t = detectedFaces[i].rect.top;
                    int r = detectedFaces[i].rect.right;
                    int b = detectedFaces[i].rect.bottom;
                    int left = (l + 1000) * vWidth / 2000;
                    int top = (t + 1000) * vHeight / 2000;
                    int right = (r + 1000) * vWidth / 2000;
                    int bottom = (b + 1000) * vHeight / 2000;
                    canvas.drawRect(left, top, right, bottom, drawingPaint);
                }
            } else {
                canvas.drawColor(Color.TRANSPARENT);
            }

            if (haveTouch) {
                drawingPaint.setColor(Color.BLUE);
                canvas.drawRect(touchArea.left - 100,
                                touchArea.top - 100,
                                touchArea.right + 100,
                                touchArea.bottom + 100,
                                drawingPaint);
            }
        }

    }
}
