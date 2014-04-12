package babycall.appfabric;

import java.io.IOException;


import com.google.ads.AdRequest;
import com.google.ads.AdView;



import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

public class PhoneModeActivity extends Activity {
	static SeekBar seekbar;
	static Button btn;
	static EditText phoneText;
	static boolean running = false;
	static boolean backpressed = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
	    this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		backpressed = false;
		super.onCreate(savedInstanceState);
		setContentView(R.layout.call_mode);
		seekbar = (SeekBar)findViewById(R.id.seekbar);
		btn = (Button)findViewById(R.id.activateButton);
		phoneText = (EditText)findViewById(R.id.phoneText);
		AdView ad = (AdView) findViewById(R.id.adView1);
		AdRequest adr = new AdRequest();
		adr.addTestDevice("78E8E0913161C8B4FF6AD9750CCA6F42"); //my arc s
		//ad.loadAd(adr);
		if(!running){
			updateSeekbar();
			EndCallListener callListener = new EndCallListener();
			TelephonyManager mTM = (TelephonyManager)this.getSystemService(Context.TELEPHONY_SERVICE);
			mTM.listen(callListener, PhoneStateListener.LISTEN_CALL_STATE);
		}
		SharedPreferences prefs = getSharedPreferences("phoneMode", 0);
        phoneText.setText(prefs.getString("phonenumber", ""));

	}
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		Log.d("debug message", "keykode KeyEvent.KEYCODE_BACK? " + keyCode + "," + KeyEvent.KEYCODE_BACK +(event.getKeyCode() == KeyEvent.KEYCODE_BACK));
		if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
			backpressed = true;
			running = false;
			SharedPreferences prefs = getSharedPreferences("phoneMode", 0);
	        SharedPreferences.Editor editor = prefs.edit();
	        editor.putString("phonenumber", phoneText.getText().toString());
	        editor.commit();

		}
		return super.onKeyDown(keyCode, event);

	}
	public void helpClicked(View view){
		Log.d("debug message", "you clicked something in the menu");
		
	
		String message = "To setup your phone do the 3 following steps \r\r" +
				"Step1: Type the phone number in the top text field that you want this phone to call if the baby is crying" +
        		"\r\r" +
        		"Step2: Adjust the threshold by pulling the seekbar hand up and down, you want the threshold above the bar in the background but not to high so that it's still trigger if the baby starts crying" +
        		"\r\r" +
        		"Step3: press the Activate button, you then have 20 seconds before it's goes active, after the 20 seconds have passed it will call the phone number when the sound in the room goes above the threshold you sat" +
        		"\r\r note: Make sure the phone have a good signal and enough battery";
		AlertDialog.Builder builder = new AlertDialog.Builder(
				this);


		builder.setMessage(
				message)
				.setTitle("Help");

		builder.setNeutralButton("Got it", null);
		AlertDialog dialog = builder.create();
		dialog.show();
		
	}

	 
	public void OnActivateClicked(View view){
		listenAndCall();
	}
	
	private void listenAndCall() {
		btn.setClickable(false);
		new Thread() {
			public void run(){
				final int WAIT_TIME = 20;
				for (int i = 0; i < WAIT_TIME; i++) {

					final int localI = i;
					runOnUiThread(new Runnable() {
						public void run() {
							btn.setText("Activating in: " + (WAIT_TIME - localI));
						}
					});
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				runOnUiThread(new Runnable() {
					public void run() {
						btn.setText("Activated");
					}
				});

				while(true){
					if(backpressed)
						return;
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					if (seekbar.getSecondaryProgress() > seekbar.getProgress()){
						try {
							Thread.sleep(1500);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						//check if the sound level still is above the threshold
						if (seekbar.getSecondaryProgress() > seekbar.getProgress()){
							makeTheCall();
							runOnUiThread(new Runnable() {
								public void run() {
									btn.setText("Activate");
								}
							});
							return;
						}
					}

				}

			}
		}.start();
	}
	private void makeTheCall() {
		try {
			Intent callIntent = new Intent(Intent.ACTION_CALL);
			callIntent.setData(Uri.parse("tel:"+phoneText.getText()));
			startActivity(callIntent);
			
		} catch (ActivityNotFoundException e) {
			Log.d("debug message", "Call failed", e);
		}		
	}
	private void updateSeekbar() {
		running = true;
		new Thread() {
			public void run(){
				byte[] buffer = new byte[3000];

				AudioRecord mRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
						44100, AudioFormat.CHANNEL_IN_MONO,
						AudioFormat.ENCODING_PCM_16BIT, 80000);
				mRecorder.startRecording();
				double amp = 0;

				while(true){

					if(backpressed){
						mRecorder.stop();
						mRecorder.release();
						return;
					}
					int totalBytesRead = 0;

					while(totalBytesRead < buffer.length){

						int bytesRead = mRecorder.read(buffer, totalBytesRead, buffer.length);
						if(bytesRead > 0) //bytesRead can be negative when there is nothing to read
							totalBytesRead += bytesRead;
						else
							try {
								Thread.sleep(10);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}

					}
					for (int i = 0; i < buffer.length - 1; i += 2){
						//Log.d("debuge message", buffer[i +1] + ", " + buffer[i]);
						amp = amp
								* 0.99999
								+ Math.abs(((short) ((buffer[i + 1] << 8) & 0xff00) | buffer[i] & 0xff))
								* 0.00001;

					}
					//Log.d("debug message", "amp: " + amp);
					final double localAmp = amp;
					runOnUiThread(new Runnable() {
						public void run() {
							seekbar
							.setSecondaryProgress((int) (((Math
									.log(localAmp) / Math.log(2)) * 10) -35));
						}
					});
				}

			}
		}.start();
	}
	private class EndCallListener extends PhoneStateListener {
		boolean haveBeenOFFHOOK = false;
	    @Override
	    public void onCallStateChanged(int state, String incomingNumber) {
	        if(TelephonyManager.CALL_STATE_RINGING == state) {
	            Log.i("message", "RINGING, number: " + incomingNumber);
	        }
	        if(TelephonyManager.CALL_STATE_OFFHOOK == state) {
	            //wait for phone to go offhook (probably set a boolean flag) so you know your app initiated the call.
	            Log.i("message", "OFFHOOK");
	            haveBeenOFFHOOK = true;
	        }
	        if(TelephonyManager.CALL_STATE_IDLE == state) {
	            //when this state occurs, and your flag is set, restart your app
	            Log.i("message", "IDLE");
	            if(haveBeenOFFHOOK)
	            	listenAndCall();
	            
	            haveBeenOFFHOOK = false;
	        }
	        
	    }
	}
}


