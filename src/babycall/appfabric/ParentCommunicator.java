package babycall.appfabric;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class ParentCommunicator extends Thread {
	Socket sock2;
	ParentActivity parentActivity;
	TextView connectionStatus;
	AudioPlayer audioPlayer;
	Button btnConnect;
	AudioTrack track;
	WarningSoundPlayer warnSPlayer;
	SeekBar seek;

	boolean connected = false;
	private boolean disconnectPressed;
	private boolean alarmPlayedbattery;
	ProgressBar progressBarBattery;
	VideoPlayerSocket videoPlayerSocket;

	public ParentCommunicator(ParentActivity parentActivity, TextView connectionStatus, 
			Button btnConnect, ProgressBar progressBarBattery, SeekBar seek) {
		// TODO Auto-generated constructor stub
		this.parentActivity = parentActivity;
		this.connectionStatus = connectionStatus;
		this.btnConnect = btnConnect;
		this.progressBarBattery = progressBarBattery;
		this.seek = seek;
	}

	@Override
	public void run(){
		while(!disconnectPressed){
			track = new AudioTrack(AudioManager.STREAM_MUSIC,
					Environment.SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO,
					AudioFormat.ENCODING_PCM_16BIT, Environment.BUF_SIZE,
					AudioTrack.MODE_STREAM);
			warnSPlayer = new WarningSoundPlayer(track);
			
			if(initialize()){ //if the initialization went well
				if(PreferenceManager.getDefaultSharedPreferences(parentActivity).getBoolean("Usevideo", true))
					startVideo();
				else
					Log.e("debug message", "video was false");
				communicateReceive();
			}
			else{
				setConnectionStatus(false, Color.RED, false);
				return;
			}

			Log.e("debug mssage", "in run loop");
			setConnectionStatus(false, Color.RED, false);
		}
		warnSPlayer.exit();
	}

	private boolean initialize(){
		// try to find a host
		sock2 = searchAfterHostQuick(getIP(), Environment.PORT2);
		//still null?
		if (sock2 == null) {
			Log.d("debug message", "sock == null");
			parentActivity.runOnUiThread(new Runnable() {
				public void run() {

					Context context = parentActivity.getApplicationContext();
					CharSequence text = "Host not found, make sure host is in Child mode";
					int duration = Toast.LENGTH_LONG;

					Toast toast = Toast.makeText(context, text,
							duration);
					toast.show();

					connectionStatus.setText("not connected");
				}
			});
			return false;
		}

		else{
			Log.d("debug message", "sock was not null");
			audioPlayer =  new AudioPlayer(sock2.getInetAddress(), Environment.PORT1, seek, parentActivity, track);
			audioPlayer.start();

			Log.d("debug message", "connection sucessfull");
			setConnectionStatus(true, Color.GREEN, false);
			communnicateSend(Environment.VERSION);
			setConnectionStatus(true, Color.GREEN, false);
			return true;
		}
	}
	void startVideo(){
		if(sock2 != null){
			videoPlayerSocket = new VideoPlayerSocket(
					sock2.getInetAddress(),
					parentActivity.previewMin,
					parentActivity);
			videoPlayerSocket.start();
		}

	}
	synchronized void setConnectionStatus(final boolean connectedStatus, final int color, final boolean playAlarm) {
		//Log.d("debug message", "setting connection status");
		parentActivity.runOnUiThread(new Runnable() {
			public void run() {
				if(connectedStatus == false){
					connectionStatus.setText("not connected");
					connected = false;
					btnConnect.setText("Connect");
				}
				else{
					connected = true;
					connectionStatus.setText("Connected");
					btnConnect.setText("Disconnect");
				}
				connectionStatus.setBackgroundColor(color);
				if(playAlarm && warnSPlayer != null){
					Log.d("debug message", "warn palyer ready");
					warnSPlayer.startPlaying();
				}
				if(warnSPlayer == null)
					Log.d("debug message", "warnSPlayer == null");
			}
		});		
	}


	Socket searchAfterHostQuick(final byte[] ip, int port) {
		final LinkedList<Socket> sockets = new LinkedList<Socket>();
		final Object o = new Object();
		final Semaphore sema = new Semaphore(0);
		final AtomicInteger count = new AtomicInteger(0);
		parentActivity.runOnUiThread(new Runnable() {
			public void run() {
				connectionStatus.setText("Connecting..");
				btnConnect.setText("Stop connecting");

			}
		});
		SharedPreferences sharedPref = parentActivity.getPreferences(0);
		String lastIp = sharedPref.getString("last ip", null);
		Socket lastSock = null;
		if(lastIp != null){
			Log.d("debug message", "last sock was not null");
			//String [] splited = lastIp.split("\\.");
			//String tmp = {Integer.parseInt(splited[0]),Integer.parseInt(splited[1]), Integer.parseInt(splited[2]), Integer.parseInt(splited[3])};
			try {
				lastSock = new Socket(lastIp, port);
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			if(lastSock != null && lastSock.isBound()){
				Log.d("debug message", "using last sock");
				return lastSock;
			}
		}
		else
			Log.d("debug message", "last sock was null");
		for (int i = 0; i < 256; i++) {
			final int localI = i;
			new Thread() {
				@Override
				public void run() {

					byte[] tmp = { ip[0], ip[1], ip[2], (byte) localI };

					try {
						new IpSearcher(InetAddress.getByAddress(tmp), sema,	sockets, count);
					} catch (UnknownHostException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}.start();

		}

		try {
			sema.acquire();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


		if (sockets.size() > 1)
			Log.d("debug message", "multiple hosts found");
		// TODO: let the user pick one?
		if (sockets.size() > 0){

			//add the found ip
			SharedPreferences.Editor editor = sharedPref.edit(); 
			editor.putString("last ip", sockets.getFirst().getInetAddress().getHostAddress());
			editor.commit(); //save
			return sockets.getFirst();
		}
		// no host found
		return null;
	}

	void communicateReceive() {

		try {

			sock2.setSoTimeout(8000); // throws java.net.SocketException subclass

			while (true) {
				int timeOutCounter = 0;

				byte[] buffer = new byte[Environment.COM_SIZE];
				int totalBytes = 0;

				while (totalBytes < Environment.COM_SIZE) {
					//Log.d("debug message", "in ze loop");

					if(disconnectPressed){
						try {
							sock2.close();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

						return;
					}

					try {
						int bytesRead = sock2.getInputStream().read(
								buffer, totalBytes,
								Environment.COM_SIZE - totalBytes);

						//	Log.d("debug message", "bytesRead " + bytesRead
						//			+ " timeoutcounter " + timeOutCounter);

						if (bytesRead == -1) {
							Log.d("debug message",
							"I didnt read anything");
							timeOutCounter++;
							try {
								Thread.sleep(10);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						} else {
							timeOutCounter = 0;

							totalBytes += bytesRead;

						}


						//catches timeouts from socket read

					} 
					catch(SocketTimeoutException e){
						Log.d("debug message", "i cought one of those SocketTimeoutExceptions");
						e.printStackTrace();
						timeOutCounter++;
					}
					catch (IOException e) {
						e.printStackTrace();


					}

					if (timeOutCounter == 3) {
						Log.d("debug message", "timeout");
						setConnectionStatus(false,
								Color.RED,true);
						try {
							sock2.close();

						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}


						Log.d("debug message",
								"conecntionStatus is "
								+ connectionStatus.getText().toString());

						return;

					}

				} // end while totalBytes < COM_SIZE

				Log.d("debug message", "message type: " + buffer[0]);
				Log.d("debug message", "data: " + buffer[1] + ", "
						+ buffer[2] + ", " + buffer[3]);
				switch (buffer[0]) {

				case Environment.MESSAGE_TYPE_VERSION:

					if (!(Environment.VERSION[0] == buffer[1]&& Environment.VERSION[1] == buffer[2] && Environment.VERSION[2] == buffer[3])) {
						String message = "There is a mismatch between the version on this phone and" +
						" the other phone. Please update both phones to the latest version";
						setConnectionStatus(false, Color.RED, true); 
						createDialog(message, "Version mismatch");
					}
					Log.e("debug message", "received version data:" + buffer[1] + buffer[2] + buffer[3]);

					break;


				case Environment.STATUS_UPDATE:
					if (buffer[1] == 0) {
						parentActivity.runOnUiThread(new Runnable() {
							public void run() {

								Context context = parentActivity.getApplicationContext();
								CharSequence text = "Host disconnected";
								int duration = Toast.LENGTH_LONG;

								Toast toast = Toast.makeText(context,
										text, duration);
								toast.show();
								setConnectionStatus(false,
										Color.RED, true);

							}
						});
					}
					break;

				case Environment.BATTERY_LEVEL:
					Log.d("debug message", "battery level: "
							+ buffer[1]);
					final byte bLevel = buffer[1];

					//makes sure the alarm only play once
					if(bLevel < 15 && !alarmPlayedbattery){
						alarmPlayedbattery = true;
						warnSPlayer.startPlaying();
						createDialog("Battery level on the other phone is below 15%", "Low battery level");
					}
					//if the battery have been charged the alarm is allowed to be played again
					else if(bLevel > 20 && alarmPlayedbattery)
						alarmPlayedbattery = false;

					progressBarBattery.setProgress(bLevel);
				default:
					break;
				}
				totalBytes = 0;
				// message.setText(new String(buffer));

			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	void communnicateSend(byte[] message) {
		if (message.length > Environment.COM_SIZE) {
			Log.d("debug message", "message was larger than COM_SIZE");
			return;
		}
		try {
			//ignore to message if the os is not initialized
			if(sock2 != null && !sock2.isClosed()){
				sock2.getOutputStream().write(message);
				sock2.getOutputStream().write(new byte[Environment.COM_SIZE - message.length]);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			if (sock2 == null)
				Log.d("debug message", "sock2 was null");
			e.printStackTrace();
		}
	}
	void createDialog(final String message, final String title){
		parentActivity.runOnUiThread(new Runnable() {
			public void run() {

				AlertDialog.Builder builder = new AlertDialog.Builder(
						parentActivity);


				builder.setMessage(
						message)
						.setTitle(title);

				builder.setNeutralButton("ok", null);
				AlertDialog dialog = builder.create();
				dialog.show();

			}
		});

	}
	void setDisconnectPressed(){
		Log.e("debug message", "setDiconnectPressed");
		this.disconnectPressed = true;
		if(audioPlayer != null){
			audioPlayer.setDisconnectPressed();

		}
		if(videoPlayerSocket != null)
			videoPlayerSocket.setDisconnectPressed();
	}

	byte[] getIP() {
		WifiManager wifiManager = (WifiManager) parentActivity.getSystemService(parentActivity.WIFI_SERVICE);
		WifiInfo wifiInfo = wifiManager.getConnectionInfo();
		int ipAddress = wifiInfo.getIpAddress();

		byte[] ip = { (byte) (ipAddress & 0xff),
				(byte) (ipAddress >> 8 & 0xff),
				(byte) (ipAddress >> 16 & 0xff),
				(byte) (ipAddress >> 24 & 0xff) };

		return ip;
	}
}
