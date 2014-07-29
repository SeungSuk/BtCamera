package kr.co.rh.apps.btcamera.main;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import kr.co.rh.apps.btcamera.R;
import kr.co.rh.apps.btcamera.camera.CameraPreview;
import kr.co.rh.apps.btcamera.comm.SingleMediaScanner;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;

public class MainActivity extends Activity implements CameraPreview.IFChangeImage {

    	private CameraPreview mCameraPreview;
    	private ImageView mNextView;
    	private Bitmap mBitmap;
    	private Button btnSaveImg;
    		
    	@Override
    	protected void onCreate(Bundle savedInstanceState) {
    		super.onCreate(savedInstanceState);
    		
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
    	}
    	
    	@Override
    	protected void onPause() {	
    		super.onPause();
    		isPause = true;		
    	}
    	
    	@Override
    	public void chgImage(final byte[] data, final Camera camera) {

    		//event break
    		if(isStop || isPause) return;
    		
    		Camera.Parameters params = camera.getParameters();
    		int w = params.getPreviewSize().width;
    		int h = params.getPreviewSize().height;
    		int format = params.getPreviewFormat();
    				
    		ByteArrayOutputStream out = new ByteArrayOutputStream();
    		Rect area = new Rect(0, 0, w, h);
    		
    		//screen size
//    		Size size1 = params.getPreviewSize();
    		
    		//image size
    		Size size = params.getPictureSize();
    		
    		int max = size.height > size.width ? size.height : size.width;
    		int per = 2;
    		
    		while(max >= 800){			
    				max = max / per;
    				per += 2;			
    		}
    		
    		BitmapFactory.Options opt = new BitmapFactory.Options();
    		opt.inSampleSize = per;		
    		
    		YuvImage image = new YuvImage(data, format, w, h, null);
    		image.compressToJpeg(area, 50, out);		
    		
    		if(mBitmap != null) mBitmap.recycle();
    		
    		try {
    			isStop = true;
    			
    			//thumbnail image change time delay
    			Thread.sleep(500);
    			
    		} catch (InterruptedException e) {
    			e.printStackTrace();
    		}	
    		
    		isStop = false;
    		mBitmap = BitmapFactory.decodeByteArray(out.toByteArray(), 0, out.size(), opt);
    		mNextView.setImageBitmap(mBitmap);
    	}

    	/**
    	 * 파일저장
    	 */
    	@Override
    	public void saveImage(byte[] data) {
    		String path = Environment.getExternalStorageDirectory() + File.separator + "test" ;
    		String fileNm = File.separator + "1.jpg";
    		FileOutputStream fo = null;
    		
    		try{
    			File f = new File(path);			
    			if(!f.exists()){				
    				f.mkdirs();
    			}
    			
    			fo = new FileOutputStream(path + fileNm);
    			fo.write(data);
    			fo.close();
    		}catch(FileNotFoundException fne){
    			fne.printStackTrace();			
    		}catch (IOException ioe) {
    			ioe.printStackTrace();			
    		}catch (Exception e) {
    			e.printStackTrace();
    		}finally {
    			if(fo != null){
    				try {
    					fo.close();
    					fo=null;
    				} catch (IOException e) {					
    					e.printStackTrace();
    				}
    			}
    		}
    		
    		//media scan 
    		
    		//kitkat
    		File scanFile = new File(path + fileNm);
    		SingleMediaScanner mediaScanner = new SingleMediaScanner(MainActivity.this, scanFile);
    		
    		//ice
    		
    	}
}
