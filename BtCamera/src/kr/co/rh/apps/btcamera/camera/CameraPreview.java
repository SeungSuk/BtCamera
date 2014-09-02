/**
 * 
 */
package kr.co.rh.apps.btcamera.camera;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import kr.co.rh.apps.btcamera.main.ServerActivity;

import android.content.Context;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

/**
 * @author keirux
 * 
 */
public class CameraPreview extends SurfaceView implements
		SurfaceHolder.Callback {

	/**
	 * 이벤트 받기 위한 인터페이스
	 * 
	 * @author keirux
	 * 
	 */
	public interface IFCameraPreviewTask {
		/**
		 * 이미지가 변경됐을때 호출
		 * 
		 * @param data
		 * @param camera
		 */
		public void chgImage(byte[] data, Camera camera);

		/**
		 * 셔터 눌렀을때 호출
		 * 
		 * @param data
		 */
		public void saveImage(byte[] data);
		
		/**
		 * 터치시 포커스 이동
		 * 
		 * @param touchRect
		 */
		public void touchFocus(Rect touchRect);
	}

	private SurfaceHolder mHolder;
	private Camera mCamera;
	private IFCameraPreviewTask change;

	private Camera.Parameters mParameters;

	/**
	 * @param context
	 */
	public CameraPreview(Context context) {
		super(context);

		init();
	}

	/**
	 * @param context
	 * @param attrs
	 */
	public CameraPreview(Context context, AttributeSet attrs) {
		super(context, attrs);

		init();
	}

	/**
	 * @param context
	 * @param attrs
	 * @param defStyle
	 */
	public CameraPreview(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	private void init() {
		mHolder = getHolder();
		mHolder.addCallback(this);

	}

	public void setChangeImage(IFCameraPreviewTask change) {
		this.change = change;
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {

		//type 
		//1. 선택되어진 사진 사이즈에 맞춰 프리뷰 화면을 구상한다
		//2.  포커스 설정
		//
		
		mParameters = mCamera.getParameters();
		
		//사진사이즈와 프리뷰 화면을 맞춘다
		
		// mParameters.setPreviewSize(width, height);

		mParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);

		// refresh
		chgParameterSize();

//		Camera.Size cameraSize = getCameraPreviewSize(getPreviewSizes(), 0.05,
//				height);
//		mParameters.setPreviewSize(cameraSize.width, cameraSize.height);

		//카메라에 적용한다
		mCamera.setParameters(mParameters);

		mCamera.startPreview();

	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {

		try {
			mCamera = Camera.open();
			mCamera.setPreviewDisplay(holder);

			mCamera.setPreviewCallback(prev);
			
		} catch (IOException exception) {
			mCamera.release();
			mCamera = null;
		}
	}
	
	public void restartCamera(){
		try {
			mCamera.startPreview();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void autoFocusShow(){
		Rect rect = new Rect(100,100, 100, 100);
		mParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
		Camera.Area area = new Camera.Area(rect, 500);
		ArrayList<Camera.Area> list = new ArrayList<Camera.Area>();
		list.add(area);
		mParameters.setFocusAreas(list);
		
		mCamera.autoFocus(autoFocus);
	}

	public void takePicture() {
		mCamera.takePicture(shutter, raw, jpeg);
		
		Toast.makeText(getContext(), "save", 1000).show();
		
		try {
			mCamera.startPreview();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	Camera.PictureCallback jpeg = new Camera.PictureCallback() {

		@Override
		public void onPictureTaken(byte[] data, Camera camera) {
			// save file
			change.saveImage(data);
		}
	};

	Camera.PictureCallback raw = new Camera.PictureCallback() {

		@Override
		public void onPictureTaken(byte[] data, Camera camera) {
			// raw data
		}
	};

	Camera.ShutterCallback shutter = new Camera.ShutterCallback() {

		@Override
		public void onShutter() {
			// sutter sound

		}
	};

	Camera.PreviewCallback prev = new PreviewCallback() {

		@Override
		public void onPreviewFrame(byte[] data, Camera camera) {
			change.chgImage(data, camera);
		}
	};
	
	Camera.AutoFocusCallback autoFocus = new  Camera.AutoFocusCallback() {
		
		@Override
		public void onAutoFocus(boolean success, Camera camera) {
			
			if(success){
				//mCamera.takePicture(null, null, null);
			}
			
		}
	};

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {

		this.getHolder().removeCallback(this);

		// event delete
		mCamera.setPreviewCallback(null);

		mCamera.stopPreview();
		mCamera.release();
		mCamera = null;

	}
	
	/**
	 * 현재 사진 사이즈를 리턴한다
	 * @return
	 */
	public String getNowPictureSize(){
		Camera.Size size = mParameters.getPictureSize();
		String retSize = size.width + " / " + size.height;;
		
		return retSize;
	}

	/**
	 * 해상도에 지원되는 사진 사이즈를 구한다
	 * @return
	 */
	private List<Camera.Size> getPictureSizes() {

		List<Camera.Size> pictureSizes = mParameters.getSupportedPictureSizes();

		checkSupportedPictureSizeAtPreviewSize(pictureSizes);

		return pictureSizes;
	}

	private List<Camera.Size> getPreviewSizes() {
		List<Camera.Size> previewSizes = mParameters.getSupportedPreviewSizes();

		return previewSizes;
	}

	/**
	 * 해상도에 지원되는 사진 사이즈를 체크한다 
	 * @param pictureSizes
	 */
	private void checkSupportedPictureSizeAtPreviewSize(
			List<Camera.Size> pictureSizes) {
		List<Camera.Size> previewSizes = getPreviewSizes();
		Camera.Size pictureSize;
		Camera.Size previewSize;
		double pictureRatio = 0;
		double previewRatio = 0;
		final double aspectTolerance = 0.05;
		boolean isUsablePicture = false;

		for (int indexOfPicture = pictureSizes.size() - 1; indexOfPicture >= 0; --indexOfPicture) {
			pictureSize = pictureSizes.get(indexOfPicture);
			pictureRatio = (double) pictureSize.width
					/ (double) pictureSize.height;
			isUsablePicture = false;

			for (int indexOfPreview = previewSizes.size() - 1; indexOfPreview >= 0; --indexOfPreview) {
				previewSize = previewSizes.get(indexOfPreview);

				previewRatio = (double) previewSize.width
						/ (double) previewSize.height;

				if (Math.abs(pictureRatio - previewRatio) < aspectTolerance) {
					isUsablePicture = true;
					break;
				}
			}

			if (isUsablePicture == false) {
				pictureSizes.remove(indexOfPicture);

			}
		}
	}

	/**
	 * 지원되는 사진 사이즈를 구한다
	 * @return
	 */
	public List<String> getStringPictureSizes() {

		List<String> list = new ArrayList<String>();
		List<Camera.Size> pictureSizes = getPictureSizes();
		for (Camera.Size size : pictureSizes) {
			String strSize = size.width + " / " + size.height;
			list.add(strSize);
		}

		return list;
	}

	private void chgParameterSize() {
		List<Camera.Size> listPrev = getPreviewSizes();

		int tempW = mParameters.getPictureSize().width;
		int tempH = mParameters.getPictureSize().height;

		Log.e("ttttttttttt", "pic width : " + tempW + " / pic height : "
				+ tempH);

		int result = 0;
		int result2 = 0;
		int picSum = 0;
		int picSum2 = 0;
		int soin = 2;

		while (tempW >= soin && tempH >= soin) {
			result = tempW % soin;
			result2 = tempH % soin;
			if (result == 0 && result2 == 0) {
				picSum = tempW / soin;
				picSum2 = tempH / soin;

				tempW = picSum;
				tempH = picSum2;
			} else {
				soin++;
			}
		}

		for (Camera.Size size : listPrev) {
			tempW = size.width;
			tempH = size.height;

			result = 0;
			result2 = 0;

			int preSum = 0;
			int preSum2 = 0;

			soin = 2;

			while (tempW >= soin && tempH >= soin) {
				result = tempW % soin;
				result2 = tempH % soin;

				if (result == 0 && result2 == 0) {
					preSum = tempW / soin;
					preSum2 = tempH / soin;

					tempW = preSum;
					tempH = preSum2;
				} else {
					soin++;
				}
			}

			if (picSum == preSum && picSum2 == preSum2) {
				mParameters.setPreviewSize(size.width, size.height);
				mCamera.setParameters(mParameters);

				break;
			}
		}
	}

	public void chgPictureSize(String size) {
		if (size == null || size.indexOf("/") < 0) {
			return;
		}

		String[] arrSize = size.replace(" ", "").split("/");

		int w = Integer.parseInt(arrSize[0]);
		int h = Integer.parseInt(arrSize[1]);

		mParameters.setPictureSize(w, h);

		// refresh
		 chgParameterSize();

//		Camera.Size cameraSize = getCameraPreviewSize(getPreviewSizes(), 0.05,
//				h);
//		mParameters.setPreviewSize(cameraSize.width, cameraSize.height);
//		mCamera.setParameters(mParameters);

		// mCamera.stopPreview();
		// mCamera.startPreview();
	}
	
	private void setArea(Camera camera, List<Camera.Area> list) {
	    boolean     enableFocusModeMacro = true;
	     
	    Camera.Parameters parameters;
	    parameters = camera.getParameters();
	 
	    int         maxNumFocusAreas    = parameters.getMaxNumFocusAreas();
	    int         maxNumMeteringAreas = parameters.getMaxNumMeteringAreas();
	     
	    if (maxNumFocusAreas > 0) {
	        parameters.setFocusAreas(list);
	    } 
	 
	    if (maxNumMeteringAreas > 0) {
	        parameters.setMeteringAreas(list);
	    }
	     
	    if (list == null || maxNumFocusAreas < 1 || maxNumMeteringAreas < 1) {
	        enableFocusModeMacro = false;
	    }
	 
	    if (enableFocusModeMacro == true) {
	        /*
	         * FOCUS_MODE_MACRO을 사용하여 근접 촬영이 가능하도록 해야 
	         * 지정된 Focus 영역으로 초점이 좀더 선명하게 잡히는 것을 볼 수 있습니다. 
	         */
	        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_MACRO);
	       
	    } else {
	        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
	       
	    }
	    camera.setParameters(parameters);
	}
	
	public void touchFocus(Rect targetFocusRect){
		mCamera.stopFaceDetection();
	
		final List<Camera.Area> focusList = new ArrayList<Camera.Area>();
		Camera.Area focusArea = new Camera.Area(targetFocusRect, 1000);
		
		focusList.add(focusArea);

		Camera.Parameters para = mCamera.getParameters();
		para.setFocusAreas(focusList);
		para.setMeteringAreas(focusList);
		mCamera.setParameters(para);

		mCamera.autoFocus(autoFocus);
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if(event.getAction() == MotionEvent.ACTION_DOWN){
			float x = event.getX();
			float y = event.getY();
			float touchMajor = event.getTouchMajor();
			float touchMinor = event.getTouchMinor();
			
			Rect touchRect = new Rect(
					(int)(x - touchMajor / 2),
					(int)(y - touchMinor / 2 ),
					(int)(x + touchMajor / 2),
					(int)(y + touchMinor / 2 )
					);
			
			this.change.touchFocus(touchRect);
		}
		
		return true;
	}
}
