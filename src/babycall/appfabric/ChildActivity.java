package babycall.appfabric;


import java.io.File;
import java.io.FileOutputStream;
import  java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;






import com.google.ads.AdRequest;
import com.google.ads.AdView;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.Camera;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.hardware.Camera.PictureCallback;
public class ChildActivity extends Activity {

	static final int COM_SIZE = Environment.COM_SIZE;


	static final int SAMPLE_RATE =  Environment.SAMPLE_RATE;
	static final int SAMPLE_INTERVAL = Environment.SAMPLE_INTERVAL; // milliseconds
	static final int SAMPLE_SIZE = Environment.SAMPLE_SIZE; // bytes per sample
	static final int BUF_SIZE = SAMPLE_INTERVAL * SAMPLE_INTERVAL * SAMPLE_SIZE * 2 * 10;

	static final byte[] VERSION = Environment.VERSION;

	static final int MESSAGE_TYPE_VERSION = Environment.MESSAGE_TYPE_VERSION;
	protected static final int STATUS_UPDATE = Environment.STATUS_UPDATE;
	static final int BATTERY_LEVEL = Environment.BATTERY_LEVEL;


	static AudioRecord mRecorder;
	AudioRecorder audiorecorder;
	static Communicator communicator;
	static Socket socket;

	ServerSocket servsock;
	String clientIp; 

	ServerSocket servsock2;
	static Socket sock2;


	LinkedList<String> messages = new LinkedList<String>();
	LinkedList<Byte> inData = new LinkedList<Byte>();


	static TextView connectionSatus;
	static WifiManager wifiManager;

	int volumeThreshold;

	static int batteryLevel;

	static boolean backpressed;
	PowerManager.WakeLock wl;
	int initalRingerMode;
	AudioManager am;
	ImageSender is;
	static BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver(){
		@Override
		public void onReceive(Context arg0, Intent intent) {
			final int level = intent.getIntExtra("level", 0);
			Log.d("debug message", "battery level " + String.valueOf(level) + "%");
			new Thread(){
				public void run(){
					communicator.setBatteyLevel(level);
					byte data [] = {BATTERY_LEVEL,(byte) level,0,0};
					batteryLevel = level;
					communicator.communnicateSend(data);

				}
			}.start();
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		// this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		setContentView(R.layout.activity_babycall_child);
		backpressed = false;
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		  wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag");
		 wl.acquire();
		if(savedInstanceState != null){
			String savedPortNr = savedInstanceState.getString("port");
			Log.d("debug message", "savedPortNr: " + savedPortNr);
		}
		else
			Log.d("debug message", "bundle is null");


		AdView ad = (AdView) findViewById(R.id.adView1);
		AdRequest adr = new AdRequest();
		adr.addTestDevice("78E8E0913161C8B4FF6AD9750CCA6F42"); //my arc s
		//ad.loadAd(adr);

		AdView ad2 = (AdView) findViewById(R.id.adView2);
		AdRequest adr2 = new AdRequest();
		adr2.addTestDevice("78E8E0913161C8B4FF6AD9750CCA6F42"); //my arc s
		//ad2.loadAd(adr2);
		connectionSatus = (TextView)findViewById(R.id.connectionStatus);

		wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);


		am = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
		initalRingerMode = am.getRingerMode();
		updataConnectionStatus();

		if(socket == null || (socket != null && socket.isClosed())){			
			backpressed = false;

			audiorecorder = new AudioRecorder();
			audiorecorder.start();
			communicator = new Communicator();
			communicator.start();
			updataConnectionStatus();
			this.registerReceiver(this.mBatInfoReceiver, 
					new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
		}
		/*image preview */
		FrameLayout previewFrameLayout = (FrameLayout) findViewById(R.id.camera_preview);
		this.is = new ImageSender(previewFrameLayout, this);
		this.is.start();

	//	Log.d("debug message", "is checked: " + ((CheckBox)findViewById(R.id.silentmodecheck)).isChecked());
	//	Log.d("debug message", "intent is available " + isIntentAvailable(this, MediaStore.ACTION_IMAGE_CAPTURE));
	}
	public void useCallClicked(View view){

		/* 
		 * start stop stuff
		 */
		Log.d("debug message", "usecallclicked");
		backpressed = true;
		audiorecorder.setDisconnectPressed();
		communicator.setDisconnectPressed();
		is.setDisconnectPressed();
		/*
		 * end stop stuff
		 */

		
		Intent intObjC = new Intent(this,
				PhoneModeActivity.class);
		//intObj.putExtra("USERNAME", name);
		startActivity(intObjC);
		((CheckBox)view).setChecked(false);

	}
	public void silentmodeclicked(View view){
		Log.d("debug message", "is checked2: " + ((CheckBox)findViewById(R.id.silentmodecheck)).isChecked());

		if(((CheckBox)view).isChecked()){
			am.setRingerMode(AudioManager.RINGER_MODE_SILENT);
		}
		else
			am.setRingerMode(initalRingerMode);

	}

	public static boolean isIntentAvailable(Context context, String action) {

		final PackageManager packageManager = context.getPackageManager();
		final Intent intent = new Intent(action);
		List<ResolveInfo> list =
			packageManager.queryIntentActivities(intent,
					PackageManager.MATCH_DEFAULT_ONLY);
		return list.size() > 0;
	}
	@Override
	protected void onStop(){
		super.onStop();
		Log.d("debug message", "in onStop()");

		//disconnectPressed = true;
		/*
		try {
			if(os != null)
				os.close();
			if(mRecorder != null)
				mRecorder.release();
			if(servsock != null)
				servsock.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		 */
	}

	@Override
	protected void onPause(){
		super.onPause();
		wl.release();

		Log.d("debug message", "in onPause()");
		//disconnectPressed = true;

	}

	@Override
	protected void onDestroy(){
		super.onDestroy();
		Log.d("debug message", "in onDestroy()");
	}

	@Override
	protected void onSaveInstanceState (Bundle outState){


		super.onSaveInstanceState(outState);

	}

	/*	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState){
		super.onRestoreInstanceState(savedInstanceState);

		String savedPortNr = savedInstanceState.getString("port");
		TextView portNr = (TextView) findViewById(R.id.portNr);
		portNr.setText(savedPortNr);
		Log.d("debug message", "retrieved port was : " + portNr.getText().toString());

	}
	 */

	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)  {
		Log.d("debug message", "on keydown in child");
		if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
			Log.d("debug message", "key is keyback");
			audiorecorder.setDisconnectPressed();
			Log.e("debug message", "audio dc");
			communicator.setDisconnectPressed();
			Log.e("debug message", "communicator dc");
			is.setDisconnectPressed();
			Log.e("debug message", "imageSender dc");
			backpressed = true;
			try{
				unregisterReceiver(mBatInfoReceiver);
			}
			catch(IllegalArgumentException e){
				//handle it, or not?
				e.printStackTrace();

			}
			Log.d("debug message", "unregistered battery event");
		} 

		Log.d("debug message", "in key back, going to return");

		return super.onKeyDown(keyCode, event);
	}



	void updataConnectionStatus(){
		new Thread(){
			public void run(){
				while(true){
					final WifiInfo wifiInfo = wifiManager.getConnectionInfo();

					runOnUiThread(new Runnable() {
						public void run() {
							if(wifiInfo.getBSSID() != null){
								connectionSatus.setText("Online");
								connectionSatus.setBackgroundColor(Color.GREEN);
							}
							else{
								connectionSatus.setText("Offline");
								connectionSatus.setBackgroundColor(Color.RED);
							}
						}
					});
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					if(backpressed)
						return;
				}

			}
		}.start();
	}






	//sets disconnectPressed = true and when the connection is closed
	// recordAndSend is started again
	/*	private void restartConnection(){
		Log.d("debug message", "restarting connection");
		disconnectPressed = true;
		if(sock2 != null && socket != null){
			while(!servsock2.isClosed() || !servsock.isClosed()){
				try {
					Thread.sleep(100);
					Log.d("debug message", "in restart connection: sock2.isClosed(): " + sock2.isClosed() + " socket.isClosed(): " + socket.isClosed());
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		disconnectPressed = false;
		recordeAndSend();
	}
	 */
	private static byte[] codeData(final byte[] buffer) {

		byte [] codeData = new byte [buffer.length*2];
		byte count = 0;
		short prevE = buffer[0];
		int i = 0;
		for (int j = 0; j < buffer.length-2; j+=2) {
			//short e = (short) (((buffer[j]<<8)&0xff00) | buffer[j+1]&0xff);

			if(buffer[j] == buffer[j+2] && buffer[j+1] == buffer[j+3]&& count < 127){
				count++;
			}
			else{

				codeData[i++] = count;
				codeData[i++] = buffer[j];
				codeData[i++] = buffer[j+1];
				count = 0;
				//codeData[i++] = (byte) (prevE>> 8);
				//codeData[i++] = (byte) (prevE & 0xFF);

			}
		}
		codeData[i++] = count;
		codeData[i++] = buffer[buffer.length-2];
		codeData[i++] = buffer[buffer.length-1];
		//return Arrays.copyOf(codeData, i);
		return buffer;
	}

}
