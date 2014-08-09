package kr.co.rh.apps.btcamera.bluetooth.data;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;

public class SendData implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String title;
	private Bitmap img;
	
	public SendData(String title, Bitmap img){
		this.title = title;
		this.img = img;
	}

	public Bitmap getImg(){
		return this.img;
	}
	
	public String getTitle(){
		return this.title;
	}

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
    	out.writeObject(this.title);
    	
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        img.compress(Bitmap.CompressFormat.PNG, 0, byteStream);
        byte bitmapBytes[] = byteStream.toByteArray();
//        int length = bitmapBytes.length;
//        out.writeInt(length);
        out.write(bitmapBytes);
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
    	this.title = (String) in.readObject();
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        int b;
        while((b = in.read()) != -1)
            byteStream.write(b);
        byte bitmapBytes[] = byteStream.toByteArray();
        Options option = new Options();
        option.inPurgeable = true;       // 메모리를 줄여주는 옵션
        option.inDither = true; 
        img = BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.length, option);
    }

}
