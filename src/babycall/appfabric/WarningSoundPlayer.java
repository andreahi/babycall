package babycall.appfabric;

import android.media.AudioTrack;
import android.util.Log;


class WarningSoundPlayer{
	static final int SAMPLE_RATE = 44100;
	private boolean exit = false;
	private AudioTrack track;
	static Boolean playing = false;
	private byte tone[];
	public WarningSoundPlayer(AudioTrack track) {
		this.track = track;
		this.tone = genTone();
		new Thread(){
			public void run(){
				play();
			}
		}.start();

	}

	private void play(){
		while(true){
			while(!playing){
				if(exit)
					return;
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			Log.d("debug message", "playing alarm");
			track.write(tone, 0, tone.length);
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			playing = false;


		}
	}
	
	void exit(){
		exit = true;
	}

	void startPlaying() {
		playing = true;

	}

	private byte [] genTone(){


		int numSamples = 100000;
		double sample[] = new double[numSamples];
		byte generatedSnd[] = new byte[2 * numSamples];
		final double freqOfTone = 840; // hz
		// fill out the array
		for (int i = 0; i < numSamples; ++i) {
			sample[i] = Math.sin(2 * Math.PI * i / (SAMPLE_RATE/freqOfTone));
		}

		// convert to 16 bit pcm sound array
		// assumes the sample buffer is normalised.
		int idx = 0;
		for (final double dVal : sample) {
			// scale to maximum amplitude
			final short val = (short) ((dVal * 32767));

			// in 16 bit wav PCM, first byte is the low order byte
			generatedSnd[idx++] = (byte) (val & 0x00ff);
			generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);

		}
		return generatedSnd;
	}

}