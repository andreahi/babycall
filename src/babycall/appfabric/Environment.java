
package babycall.appfabric;

import android.media.AudioTrack;

public class Environment {
	static final int COM_SIZE = 4;

	static final int SAMPLE_RATE = 44100;
	static final int SAMPLE_INTERVAL = 20; // milliseconds
	static final int SAMPLE_SIZE = 2; // bytes per sample
	static final int BUF_SIZE = SAMPLE_INTERVAL * SAMPLE_INTERVAL * SAMPLE_SIZE
			* 2 * 10;
	
	static final int PORT2 = 13270;
	static final int PORT1 = 13271;
	static final int PORT3 = 13272;
	static final byte[] VERSION = { 0, 2, 1 };

	static final int MESSAGE_TYPE_VERSION = 0;
	protected static final int STATUS_UPDATE = 1;
	static final int BATTERY_LEVEL = 2;
	static final int SEEKBAR_PROGRESS = 3;
	static final int VIDEO_SWITCH = 4;

	public static final byte USE_VIDEO = 5;
	public static final byte USE_HD_VIDEO = 6;

//	public static AudioTrack track;

//	public static WarningSoundPlayer warnSPlayer;

}