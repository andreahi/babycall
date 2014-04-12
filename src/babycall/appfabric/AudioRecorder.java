package babycall.appfabric;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

public class AudioRecorder extends Thread {
	ServerSocket servsock;
	AudioRecord mRecorder;
	Socket socket;
	private boolean inAcceptState = false;
	boolean disconnectPressed = false;

	@Override
	public void run(){
		while(!disconnectPressed){
			if(initialize() && !disconnectPressed)
				recordeAndSend();
		}
	}

	private boolean initialize(){
		Log.d("debug message", "trying to setup the connection");
		try {
			servsock = new ServerSocket(Environment.PORT1);
			servsock.setReuseAddress(true);
			inAcceptState = true;
			socket = servsock.accept();
			inAcceptState = false;
			Log.d("debug message", "connection successfull " + socket.getInetAddress().getHostAddress());


			Log.d("debug message","initializing mRecorder");
			mRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
					44100, AudioFormat.CHANNEL_IN_MONO,
					AudioFormat.ENCODING_PCM_16BIT, 80000);
			Log.d("debug message", "state : " + mRecorder.getState());
			mRecorder.startRecording();
		} catch (IOException e2) {
			e2.printStackTrace();
			Log.d("debug message", "IOException in recordandsend");
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			return false;
		}
		return true;
	}
	void recordeAndSend(){

		byte[] buffer = new byte[3000];

		int timer = 0;
		while(true){
			try {
				int totalBytesRead = 0;

				while(totalBytesRead < buffer.length){
					if(disconnectPressed){
						Log.d("debug message", "closing sock");
						mRecorder.release();
						servsock.close();
						socket.close();
						//socket = null;
						Log.d("debug message", "sock closed");

						return;
					}
					/*
							if(timer++ > 10){
								byte data [] = {BATTERY_LEVEL,(byte) batteryLevel,0,0};

								communnicateSend(data);
								timer = 0;
							}
					 */
					int bytesRead = mRecorder.read(buffer, totalBytesRead, buffer.length);
					if(bytesRead > 0) //bytesRead can be negative when there is nothing to read
						totalBytesRead += bytesRead;
					else
						Thread.sleep(10);

				}


				//Log.e("debug message", "writing audio");
				socket.getOutputStream().write(buffer, 0, buffer.length);

			} catch (IOException e) {
				Log.d("debug message", "error while sending data");
				Log.d("debug message", e.getMessage());
				Log.d("debug message", e.getLocalizedMessage());
				try {
					mRecorder.release();
					servsock.close();
					socket.close();
					//socket = null;
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				return;

			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
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
