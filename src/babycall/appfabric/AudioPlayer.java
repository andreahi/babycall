package babycall.appfabric;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.media.AudioTrack;
import android.util.Log;
import android.widget.SeekBar;
import android.widget.Toast;

public class AudioPlayer extends Thread {
	Socket sock2;
	boolean disconnectPressed;
	SeekBar seek;
	Activity parentActivity;
	AudioTrack track;
	InetAddress inetAddress;
	int port;
	public AudioPlayer(InetAddress inetAddress, int port2, SeekBar seek, Activity parentActivity, AudioTrack track) {
		this.seek = seek;
		this.parentActivity =  parentActivity;
		this.track = track;
		this.inetAddress = inetAddress;
		this.port = port2;
		// TODO Auto-generated constructor stub
	}

	@Override
	public void run(){
		while(!disconnectPressed){
			initialize();
			receiveAndPlay();
		}
		Log.e("debug message", "stoping track");
		track.stop();
		track.release();
	}
	
	void initialize(){
		try {
			sock2 = new Socket(inetAddress, port);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	void receiveAndPlay() {
		
				try {
					byte[] buffer = new byte[3000];
					double amp = 0;
					int writeCounts = 0;

					while (true) {
						int totalRead = 0;
						while (totalRead < 3000) {
							int number = sock2.getInputStream().read(buffer,
									totalRead, 3000 - totalRead);

							if (number > 0)
								totalRead += number;
							else
								Thread.sleep(10);


							if(disconnectPressed){
								sock2.close();
								//sock = null;
								return;
							}
							
						}

						// calculate the value for the sound ProgressBar
						for (int i = 0; i < buffer.length - 1; i += 2){
							//Log.d("debuge message", buffer[i +1] + ", " + buffer[i]);
							amp = amp
							* 0.99999
							+ Math.abs(((short) ((buffer[i + 1] << 8) & 0xff00) | buffer[i] & 0xff))
							* 0.00001;
						}
						final double localAmp = amp;
						//Log.d("debug message", "" +amp);
						//Log.d("debug message", "amp: " + (int) (((Math.log(localAmp) / Math.log(2)) * 10) - 35));
						parentActivity.runOnUiThread(new Runnable() {
							public void run() {

								seek
								.setSecondaryProgress((int) (((Math
										.log(localAmp) / Math.log(2)) * 10) - 35));
							}
						});

						if ((int) (((Math.log(localAmp) / Math.log(2)) * 10) - 35) > seek.getProgress()){
							if(writeCounts++ > 100){
								track.flush();
								writeCounts = 0;
								Log.d("debug message", "flushing track");
							}
							track.write(buffer, 0, 3000);
						}


						// if not connected, then close the connection
						if (disconnectPressed) {
							sock2.close();
							//sock = null;
							return;
						}
						track.play();

						try {
							Thread.sleep(10); // nothing to read, then wait and
							// try again
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				} catch (IOException e) {
					Log.d("debug message", "I cought a IOexception");
					e.printStackTrace();
					Log.d("debug mesage", e.getMessage());

				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	}
	
	void setDisconnectPressed(){
		disconnectPressed = true;
		
	}
}
