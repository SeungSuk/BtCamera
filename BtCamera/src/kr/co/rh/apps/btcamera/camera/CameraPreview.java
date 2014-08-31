/**
 * 
 */
package kr.co.rh.apps.btcamera.camera;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.util.AttributeSet;
import android.util.Log;
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
	public interface IFChangeImage {
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
	}

	private SurfaceHolder mHolder;
	private Camera mCamera;
	private IFChangeImage change;

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

	public void setChangeImage(IFChangeImage change) {
		this.change = change;
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {

		Log.e("ttttttttttt", "surfaceChanged width : " + width
				+ " /  height : " + height);

		mParameters = mCamera.getParameters();
		// mParameters.setPreviewSize(width, height);

		mParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);

		// refresh
		// chgParameterSize();

		Camera.Size cameraSize = getCameraPreviewSize(getPreviewSizes(), 0.05,
				height);
		mParameters.setPreviewSize(cameraSize.width, cameraSize.height);
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

	public void takePicture() {
		mCamera.takePicture(shutter, raw, jpeg);
		Toast.makeText(getContext(), "save", 1000).show();
		try {
			mCamera.startPreview();
		} catch (Exception e) {

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

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {

		this.getHolder().removeCallback(this);

		// event delete
		mCamera.setPreviewCallback(null);

		mCamera.stopPreview();
		mCamera.release();
		mCamera = null;

	}

	private List<Camera.Size> getPictureSizes() {

		List<Camera.Size> pictureSizes = mParameters.getSupportedPictureSizes();

		checkSupportedPictureSizeAtPreviewSize(pictureSizes);

		return pictureSizes;
	}

	private List<Camera.Size> getPreviewSizes() {
		List<Camera.Size> previewSizes = mParameters.getSupportedPreviewSizes();

		return previewSizes;
	}

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

				Log.e("ttttttttttt", "pre width : " + size.width
						+ " / pre height : " + size.height);
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
		// chgParameterSize();

		Camera.Size cameraSize = getCameraPreviewSize(getPreviewSizes(), 0.05,
				h);
		mParameters.setPreviewSize(cameraSize.width, cameraSize.height);
		mCamera.setParameters(mParameters);

		// mCamera.stopPreview();
		// mCamera.startPreview();
	}

	private Camera.Size getCameraPreviewSize(List<Camera.Size> previewList,
			double targetRatio, int targetHeight) {
		final double aspectTolerance = 0.05;
		double minDiff = Double.MAX_VALUE;
		Size optimalSize = null;

		for (Size size : previewList) {
			double ratio = (double) size.width / size.height;

			if (Math.abs(ratio - targetRatio) > aspectTolerance) {
				continue;
			}

			if (Math.abs(size.height - targetHeight) < minDiff) {
				optimalSize = size;
				minDiff = Math.abs(size.height - targetHeight);
			}
		}

		return optimalSize;
	}
}
