package babycall.appfabric;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

/*
 *  this
 */
class Preview extends SurfaceView implements SurfaceHolder.Callback, PreviewCallback {  
	public static boolean usevideo = true;
	public static boolean usehd;
	SurfaceHolder mHolder;  
	private int[] pixels;  

	private Camera mCamera;  
	private boolean disconnectPressed;
	//This variable is responsible for getting and setting the camera settings  
	private Parameters parameters;  
	//this variable stores the camera preview size   
	Size previewSize;  
	//this array stores the pixels as hexadecimal pairs   
	ImageSender imageSender;
	FrameLayout frameLayout;

	Preview(Context context, FrameLayout frameLayout, ImageSender imageSender) {  
		super(context);  
		this.imageSender = imageSender;
		// Install a SurfaceHolder.Callback so we get notified when the  
		// underlying surface is created and destroyed.  
		mHolder = getHolder();  
		mHolder.addCallback(this);  
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);  
		this.frameLayout = frameLayout;
		Log.e("MYAPP", "creating new preview");

	}  

	public void surfaceCreated(SurfaceHolder holder) {  
		// The Surface has been created, acquire the camera and tell it where  
		// to draw.  
		mCamera = Camera.open();  
		try {  
			mCamera.setPreviewDisplay(holder);  

			//sets the camera callback to be the one defined in this class  
			mCamera.setPreviewCallback(this);  

			///initialize the variables  
			parameters = mCamera.getParameters();  
			previewSize = parameters.getPreviewSize();  
			pixels = new int[previewSize.width * previewSize.height];  
			RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(previewSize.width, previewSize.height);
			//FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(previewSize.width, previewSize.height, Gravity.CENTER_HORIZONTAL);
			frameLayout.setLayoutParams(lp);
		} catch (IOException exception) {  
			mCamera.release();  
			mCamera = null;  
			// TODO: add more exception handling logic here  
		}  
		mCamera.startPreview();

	}  

	public synchronized void surfaceDestroyed(SurfaceHolder holder) {  
		// Surface will be destroyed when we return, so stop the preview.  
		// Because the CameraDevice object is not a shared resource, it's very  
		// important to release it when the activity is paused. 
		if(mCamera != null){
			mCamera.stopPreview();  
			mCamera.release();  
			mCamera = null;  
		}
	}  

	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {  
		Log.e("debug message", "surface changed");
		// Now that the size is known, set up the camera parameters and begin  
		// the preview.  

		parameters.setPreviewSize(w, h);  
		//set the camera's settings  
		//		mCamera.setParameters(parameters); 

		//mCamera.stopPreview();

		mCamera.startPreview();
	}  
	boolean busy = false;
	int previewCount = 0;
	synchronized public void onPreviewFrame(byte[] data, Camera camera) { 

		Log.e("MYAPP", "onPreviewFrame called");
		synchronized (this) {
			if(busy)
				return;
			else
				busy = true;
		}
		//transforms NV21 pixel data into RGB pixels  
		//decodeYUV420SP(pixels, data, previewSize.width,  previewSize.height);  
		//Outuput the value of the top left pixel in the preview to LogCat  
		//Log.i("Pixels", "The top right pixel has the following RGB (hexadecimal) values:"  
		//       +Integer.toHexString(pixels[0]));
		if(disconnectPressed && mCamera != null){
			mCamera.stopPreview();
			mCamera.release();
			mCamera = null;
			notifyAll();
			busy=false;
			return;
		}
		if(!usevideo){
			busy = false;
			return;
		}
		int bSize = data.length*2/3;

		Log.e("debug message", "length: " + data.length);
		byte [] sData = Arrays.copyOf(data, bSize);

		Arrays.sort(sData);
		int [] cdf = new int[256];

		for (int i = 0; i < sData.length*2/3-1; i++) {
			cdf[0xff & data[i]]++;
		}
		for (int i = 1; i < cdf.length; i++) {
			cdf[i] += cdf[i-1];
		}
		for (int i = 0; i < bSize; i++) {
			data[i] = (byte) (((cdf[0xff & data[i]] - sData[0])*255/(bSize - sData[0])));
		}

		//decodeYUV420SP(pixels, data, previewSize.width, previewSize.height);
		//for (int i = 0; i < data.length; i++) {
		//		pixels[i] = data[i];
		//	}
		//final Bitmap bMap = Bitmap.createBitmap(pixels, previewSize.width, previewSize.height, Config.ARGB_8888);
		YuvImage im = new YuvImage(data, ImageFormat.NV21, previewSize.width,
				previewSize.height, null);
		Rect r = new Rect(0,0,previewSize.width,previewSize.height);
		parameters = mCamera.getParameters();  
		previewSize = parameters.getPreviewSize(); 
		pixels = new int[previewSize.width * previewSize.height];  

		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		//		bMap.compress(Bitmap.CompressFormat.JPEG, 30, baos);
		im.compressToJpeg(r, usehd?40:10, baos);

		imageSender.sendImage(baos.toByteArray(), previewSize.width, previewSize.height);
		try {
			Thread.sleep(usehd?50:200);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		busy = false;

		Log.e("debug message", "previewCount: " + previewCount++);
	}  

	void decodeYUV420SP(int[] rgb, byte[] yuv420sp, int width, int height) {  

		final int frameSize = width * height;  

		for (int j = 0, yp = 0; j < height; j++) {       
			int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;  
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

	 void setDiscnnectPressed(){
		disconnectPressed = true;
		Log.e("debug", "Preview disconnected");
	/*	try {
		if(mCamera != null)
				wait();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
	}
}  