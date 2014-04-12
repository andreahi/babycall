package babycall.appfabric;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;


import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.google.ads.AdView;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnDragListener;
import android.view.animation.CycleInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class ParentActivity extends Activity implements OnSharedPreferenceChangeListener {

	//SeekBar seek;
	//Button btnConnect;
	//ProgressBar progressBarBattery;
	Drawable drawSeekYellow;
	//TextView connectionStatus;


	static final int COM_SIZE = 4;


	static final int SAMPLE_RATE = 44100;
	static final int SAMPLE_INTERVAL = 20; // milliseconds
	static final int SAMPLE_SIZE = 2; // bytes per sample
	static final int BUF_SIZE = SAMPLE_INTERVAL * SAMPLE_INTERVAL * SAMPLE_SIZE
	* 2 * 10;

	int amp_threshold = 0;

	boolean alarmPlayedbattery;


	static final byte[] VERSION = { 0, 2, 1 };

	// message types
	static final int MESSAGE_TYPE_VERSION = 0;
	protected static final int STATUS_UPDATE = 1;
	static final int BATTERY_LEVEL = 2;
	static final int SEEKBAR_PROGRESS = 3;

	long lastMessageReceived;
	LinkedList<String> messages = new LinkedList<String>();
	private AdView adView;

	static AudioTrack track;

	SeekBar seek;
	ProgressBar progressBarBattery;
	TextView connectionStatus;
	Button btnConnect;
	ParentCommunicator parentCommunicator;
	VideoPlayerSocket vps;
	ImageView previewMin;
	//ImageView previewMax;
	PowerManager.WakeLock wl;
	public void onCreate(Bundle savedInstanceState) {
		Log.d("debug message", "in create");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_babycall_parent);
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag");
		wl.acquire();

		btnConnect = (Button) findViewById(R.id.btnConnect);
		progressBarBattery = (ProgressBar) findViewById(R.id.progressBarBattery);
		connectionStatus = (TextView) findViewById(R.id.labelConnectionStatus);
		seek = (SeekBar) findViewById(R.id.seek);

		AdView ad = (AdView) findViewById(R.id.adView);
		AdRequest adr = new AdRequest();
		adr.addTestDevice("78E8E0913161C8B4FF6AD9750CCA6F42"); //my arc s
		//ad.loadAd(adr);

		PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);

		//receiveAndPlay();
		seek.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			public void onStopTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
			}
			public void onStartTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
			}
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				byte [] message = {SEEKBAR_PROGRESS, (byte)seekBar.getProgress()};
				if(parentCommunicator != null)
					parentCommunicator.communnicateSend(message);
				//Log.d("debug message", "I sent a message about th");				
			}
		});
		seek.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				byte [] message = {SEEKBAR_PROGRESS, (byte)((SeekBar)v).getProgress()};
				parentCommunicator.communnicateSend(message);
				Log.d("debug message", "I sent a message about th");
			}
		});
		amp_threshold = seek.getProgress();
		if (savedInstanceState != null){
			String btnText = savedInstanceState.getString("connectedBtn");
			if (btnText != null)
				btnConnect.setText(btnText);
			seek.setProgress(savedInstanceState.getInt("progress"));

			// returns false if it does'nt exist
			//connected = savedInstanceState.getBoolean("connected");
			connectionStatus.setText(savedInstanceState.getString("connectionStatus"));


			if(connectionStatus.getText().toString().equals("not connected"))
				connectionStatus.setBackgroundColor(Color.RED);
			else
				connectionStatus.setBackgroundColor(Color.GREEN);
		}
		previewMin = (ImageView) findViewById(R.id.imageViewPreviewMin);
		//	previewMax = (ImageView) findViewById(R.id.imageViewPreviewMax);


		//previewMax.setVisibility(View.INVISIBLE);

	}
	//end onCreate
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_babycall, menu);
		return true;
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		System.out.println("optionitemselected");
		switch (item.getItemId()) {

		case R.id.action_settings:
			System.out.println("actionsettings");
			Intent i = new Intent(this, SettingsActivity.class);
			startActivity(i);
			//startActivityForResult(i, RESULT_SETTINGS);
			break;

		}

		return true;
	}

	public void fullscreen(View view) {
		runOnUiThread(new Runnable() {
			public void run() {
				//	previewMax.setVisibility(View.VISIBLE);
			}
		});

	}

	public void minimize(View view) {
		runOnUiThread(new Runnable() {
			public void run() {
				//previewMax.setVisibility(View.INVISIBLE);

			}
		});
	}

	@Override
	public void onDestroy() {
		// Destroy the AdView.
		if (adView != null) {
			adView.destroy();
		}

		super.onDestroy();
	}
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		Log.d("debug message", "in onSaveInstanceState");
		//		outState.putBoolean("connected", connected);
		outState.putString("connectedBtn", btnConnect.getText().toString());
		outState.putInt("progress", seek.getProgress());
		outState.putString("connectionStatus", connectionStatus.getText().toString());


	}
	synchronized public void onClickBattery(View view){

		runOnUiThread(new Runnable() {
			public void run() {
				Context context = getApplicationContext();
				CharSequence text = "Indecate battery level on the phone with the child";
				int duration = Toast.LENGTH_LONG;

				Toast toast = Toast.makeText(context,
						text, duration);
				toast.show();
			}
		});
	}

	// called when someone press the connect/disconnect button
	synchronized public void connectButtonListener(View view) {
		if(parentCommunicator == null){
			parentCommunicator = new ParentCommunicator(this, connectionStatus, btnConnect, progressBarBattery, seek);
			btnConnect.setText("Connecting");
			parentCommunicator.start();

		}
		else{
			parentCommunicator.setDisconnectPressed();
			btnConnect.setText("Connect");
			parentCommunicator = null;
		}

		runOnUiThread(new Runnable() {
			public void run() {
			}
		});		
	}


	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		Log.d("debug message", "keykode KeyEvent.KEYCODE_BACK? " + keyCode + "," + KeyEvent.KEYCODE_BACK +(event.getKeyCode() == KeyEvent.KEYCODE_BACK));
		if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
			//setConnectionStatus(false, Color.RED, false);

			//connectButtonListener(null);
			if(parentCommunicator != null){
				parentCommunicator.setDisconnectPressed();
			}
		}
		return super.onKeyDown(keyCode, event);
	}

	public void seekBarCliked(View view) {
		SeekBar seek = (SeekBar) findViewById(R.id.seek);
		amp_threshold = seek.getProgress();
	}

	@Override
	protected void onPause() {
		wl.release();
		
		super.onPause();
	}


	void createDialog(final String message, final String title){
		runOnUiThread(new Runnable() {
			public void run() {

				AlertDialog.Builder builder = new AlertDialog.Builder(
						ParentActivity.this);


				builder.setMessage(
						message)
						.setTitle(title);

				builder.setNeutralButton("ok", null);
				AlertDialog dialog = builder.create();
				dialog.show();

			}
		});

	}


	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		if(key.equals("Usevideo")){
			boolean usevideo = sharedPreferences.getBoolean(key, true);
			byte [] message = {Environment.USE_VIDEO, (byte) (usevideo?1:0)};
			parentCommunicator.communnicateSend(message);
		}
		if(key.equals("UseHD")){
			boolean usehd = sharedPreferences.getBoolean(key, false);
			byte [] message = {Environment.USE_HD_VIDEO, (byte) (usehd?1:0)};
			parentCommunicator.communnicateSend(message);
		}

		// TODO Auto-generated method stub
	}

	/*	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		System.out.println("pref changed lawl");
		if(key.equals("HDSwitch")){
			boolean screenOn =PreferenceManager.getDefaultSharedPreferences(this).getBoolean("screenOn", false);
			if(screenOn)
				this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
			else
				this.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		}
		if(key.equals("searchdelay")){
			this.updateDelay = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(this).getString("searchdelay", "5000"));
			Criteria crit = new Criteria();
			crit.setAccuracy(Criteria.ACCURACY_FINE);

			locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE); 
			locationManager.requestLocationUpdates( locationManager.getBestProvider(crit, false), this.updateDelay, 0, this);

		}
	}
	 */




}


