package babycall.appfabric;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import android.app.AlertDialog;
import android.util.Log;
import android.widget.FrameLayout;

class ImageSender extends Thread{
	Socket socket;
	byte [] picture;
	int width = 0;
	int heigth = 0;
	boolean newPicture = false;
	FrameLayout previewFrameLayout;
	Preview preview;
	boolean disconnectPressed;
	private boolean acceptingConnection;
	ServerSocket servsock;
	ChildActivity childActivity;
	ImageSender(FrameLayout previewFrameLayout, ChildActivity childActivity){
		this.previewFrameLayout = previewFrameLayout;
		this.childActivity = childActivity;
	}

	synchronized void sendImage(byte[] picture, int width, int heigth){
		this.picture = picture;
		this.width = width;
		this.heigth = heigth;
		notifyAll();
	}
	boolean initialize(){
		try {
			servsock = new ServerSocket(Environment.PORT3);
			servsock.setReuseAddress(true);
			acceptingConnection = true;
			socket = servsock.accept();
			acceptingConnection = false;
			
			childActivity.runOnUiThread(new Runnable() {
				public void run() {

					preview = new Preview(childActivity,  previewFrameLayout, ImageSender.this);
					previewFrameLayout.addView(preview);	

				}
			});

			Log.e("MYAPP", "connection accepted");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		return true;
	}
	synchronized void streamImages(){
		while(!disconnectPressed){
			try {
				wait();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			try {
				socket.getOutputStream().write(intToByteArray(width));
				socket.getOutputStream().write(intToByteArray(heigth));
				socket.getOutputStream().write(intToByteArray(picture.length));
				socket.getOutputStream().write(picture);
				Log.e("MYAPP", "image sent with size " + picture.length);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				try {
					servsock.close();
					socket.close();
					Log.e("debug", "dcing prew");
					preview.setDiscnnectPressed();

				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				return;
			}
		}
	}
	@Override
	synchronized public void run(){
		while(!disconnectPressed){
			if(initialize() && !disconnectPressed)
				streamImages();
			if(preview != null)
				preview.setDiscnnectPressed();

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
		return (array[0] << 24) & 0xff000000 | (array[1] << 16)& 0x00ff0000 | (array[2] << 8)& 0x0000ff00 | array[3] & 0x000000ff;
	}
	
	void setDisconnectPressed(){

		disconnectPressed = true;
		
		try {
			servsock.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
