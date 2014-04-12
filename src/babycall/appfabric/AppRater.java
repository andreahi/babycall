package babycall.appfabric;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
public class AppRater {
	private final static String APP_TITLE = "Babycall";
	private final static String APP_PNAME = "babycall.appfabric";

	private final static int DAYS_UNTIL_PROMPT = 3;
	private final static int LAUNCHES_UNTIL_PROMPT = 6;

	public static void app_launched(Context mContext) {
		SharedPreferences prefs = mContext.getSharedPreferences("apprater", 0);
		if (prefs.getBoolean("dontshowagain", false)) { return ; }

		SharedPreferences.Editor editor = prefs.edit();
		// Increment launch counter
		long launch_count = prefs.getLong("launch_count", 0) + 1;
		editor.putLong("launch_count", launch_count);
		Log.d(Log.DEBUG+"", "launch count " + launch_count);
		// Get date of first launch
		Long date_firstLaunch = prefs.getLong("date_firstlaunch", 0);
		if (date_firstLaunch == 0) {
			date_firstLaunch = System.currentTimeMillis();
			editor.putLong("date_firstlaunch", date_firstLaunch);
		}

		// Wait at least n days before opening
		if (launch_count >= LAUNCHES_UNTIL_PROMPT) {
			if (System.currentTimeMillis() >= date_firstLaunch + 
					(DAYS_UNTIL_PROMPT * 24 * 60 * 60 * 1000)) {
				showRateDialog(mContext, editor);
			}
		}

		editor.commit();
	}   

	public static void showRateDialog(final Context mContext, final SharedPreferences.Editor editor) {


		String message = "If you enjoy using " + APP_TITLE + ", please take a moment to rate it." +
		" If you have problems related to the app, please consider mailing us about your problem before giving us a bad review." +
		" Thanks for your support!";

		AlertDialog.Builder builder = new AlertDialog.Builder(
				mContext);
		builder.setMessage(message).setTitle("Rate App");


		builder.setPositiveButton("Rate now", new OnClickListener() {

			public void onClick(DialogInterface dialog, int which) {
				mContext.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + APP_PNAME)));
				if (editor != null) {
					editor.putBoolean("dontshowagain", true);
					editor.commit();
				}		

			}
		});

		builder.setNegativeButton("No, thanks", new  OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				if (editor != null) {
					editor.putBoolean("dontshowagain", true);
					editor.commit();
				}				
			}
		});

		builder.setNeutralButton("Remind me later", new OnClickListener() {

			public void onClick(DialogInterface dialog, int which) {
				SharedPreferences prefs = mContext.getSharedPreferences("apprater", 0);
				SharedPreferences.Editor editor = prefs.edit();
				//reset the the timer and launch_count
				editor.putLong("launch_count", 0);
				editor.putLong("date_firstlaunch", System.currentTimeMillis());
				editor.commit();

			}
		});





		AlertDialog dialog = builder.create();
		dialog.show();
	}
}