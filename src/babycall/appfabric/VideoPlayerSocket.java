package babycall.appfabric;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera.PreviewCallback;
import android.media.MediaPlayer;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

public class VideoPlayerSocket extends Thread {
	MediaPlayer mMediaPlayer;

	ImageView previewMin;
	//ImageView previewMax;
	boolean imageready = false;
	boolean disconnectPressed;
	Activity mainAct;
	InetAddress inetAddress;
	public VideoPlayerSocket(InetAddress inetAddress, ImageView previewMin, Activity mainAct) {
		Log.e("debug message", "preview == " + previewMin);
		this.previewMin = previewMin;
		//this.previewMax = previewMax;
		this.mainAct = mainAct;
		this.inetAddress = inetAddress;
	}


	@Override
	public void run(){
		// this is your network socket, connected to the server
		Socket socket = null;
		byte[] buffer = new byte[3000000]; //282996
		byte[] sizeBuffer = new byte[4];
		byte [] widthBuffer = new byte[4];
		byte []  heigthBuffer = new byte[4];
		try {
			socket = new Socket(inetAddress, Environment.PORT3);
			Log.e("MYAPP", "I'm connected");
			while(!disconnectPressed){
				Log.e("MYAPP", "going to read");
				readFromStream(socket, widthBuffer, 4);
				int width = byteArrayToInt(widthBuffer);
				readFromStream(socket, heigthBuffer, 4);
				int heigth = byteArrayToInt(heigthBuffer);
				readFromStream(socket, sizeBuffer, 4);
				int imageSize = byteArrayToInt(sizeBuffer);
				Log.e("MYAPP", "image size is: " + imageSize);
				readFromStream(socket, buffer, imageSize);
				//decodeYUV420SP(pixels, buffer, width, heigth);
				displayImage(buffer, width, heigth);

			}
			socket.close();

		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			Log.e("MYAPP"," "+e.getMessage());
			Log.e("MYAPP"," "+ e.getLocalizedMessage());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			Log.e("MYAPP"," "+ e.getMessage());
			Log.e("MYAPP"," "+ e.getLocalizedMessage());
			try {
				if(socket != null)
					socket.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	}

	private void displayImage(byte [] buffer2, int width, int height) {
		//final Bitmap bMap = Bitmap.createBitmap(buffer2,width, height, Config.ARGB_8888);
		//final Bitmap bMap = BitmapFactory.decodeByteArray(buffer2, 0, imageSize);
		//ByteArrayOutputStream baos = new ByteArrayOutputStream();
		//bMap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
		Matrix matrix = new Matrix();
		matrix.postRotate(90);

		final Bitmap aMap = BitmapFactory.decodeByteArray(buffer2, 0, buffer2.length);
		final Bitmap bMap = Bitmap.createBitmap(aMap, 0, 0, 
				aMap.getWidth(), aMap.getHeight(), matrix, true);
		mainAct.runOnUiThread(new Runnable() {
			public void run() {
				//preview.setImageBitmap(bMap);			
				previewMin.setImageBitmap(bMap);			
	//			previewMax.setImageBitmap(bMap);
			}
		});

		Log.e("MYAPP", "image changed");

	}


	// read the given number of bytes, no more, no less
	void readFromStream(Socket sock, byte [] buffer, int length ) throws IOException{
		int bytesRead = 0;
		int tmp = 0;
		while(bytesRead < length){
			if((tmp = sock.getInputStream().read(buffer, bytesRead, length - bytesRead)) > 0)
				bytesRead += tmp;
			else
				throw new IOException();
		}
	}
	void decodeYUV420SP(int[] rgb, byte[] yuv420sp, int width, int height) {  

		final int frameSize = width * height;  

		for (int j = 0, yp = 0; j < height; j++) {       int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;  
		for (int i = 0; i < width; i++, yp++) {  
			int y = (0xff & ((int) yuv420sp[yp])) - 16;  
			if (y < 0)  
				y = 0;  
			if ((i & 1) == 0) {  
				v = (0xff & yuv420sp[uvp++]) - 128;  
				u = (0xff & yuv420sp[uvp++]) - 128;  
			}  

			int y1192 = 1192 * y;  
			int r = (y1192 + 1634 * v);  
			int g = (y1192 - 833 * v - 400 * u);  
			int b = (y1192 + 2066 * u);  

			if (r < 0)                  r = 0;               else if (r > 262143)  
				r = 262143;  
			if (g < 0)                  g = 0;               else if (g > 262143)  
				g = 262143;  
			if (b < 0)                  b = 0;               else if (b > 262143)  
				b = 262143;  

			rgb[yp] = 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);  
		}  
		}  
	}  
	public static final byte[] intToByteArray(int value) {
		return new byte[] {
				(byte)(value >>> 24),
				(byte)(value >>> 16),
				(byte)(value >>> 8),
				(byte)value};
	}
	public static final int byteArrayToInt(byte[] array) {

		return (array[0] << 24) & 0xff000000 
		| (array[1] << 16)& 0x00ff0000 
		| (array[2] << 8)& 0x0000ff00 
		| array[3] & 0x000000ff;
	}

	void setDisconnectPressed(){
		disconnectPressed = true;
	}
}

