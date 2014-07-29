package kr.co.rh.apps.btcamera.comm;

import java.io.File;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;

/**
 * media scan
 * 
 * @author keirux
 *
 */
public class SingleMediaScanner implements MediaScannerConnectionClient {

	private MediaScannerConnection mMs;
	private File mFile;
	
	public SingleMediaScanner(Context context, File f){
		mFile = f;
		mMs = new MediaScannerConnection(context, this);
		mMs.connect();
	}
	
	@Override
	public void onMediaScannerConnected() {
		mMs.scanFile(mFile.getAbsolutePath(),  null);
	}

	@Override
	public void onScanCompleted(String arg0, Uri arg1) {
		mMs.disconnect();
	}

}
