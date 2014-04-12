package babycall.appfabric;

import android.os.Bundle;
import android.os.PowerManager;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.support.v4.app.NavUtils;

public class Babycall extends Activity {
	Intent intObjC;
	Intent intObjP;
    protected PowerManager.WakeLock mWakeLock;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_babycall);
	    //this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        /*Button btn = (Button)findViewById(R.id.button1);
        btn.setBackgroundResource(R.drawable.cloud);
        
        Button btn2 = (Button)findViewById(R.id.button2);
        btn2.setBackgroundResource(R.drawable.cloud);
    */
    /*    
        final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        this.mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag");
        this.mWakeLock.acquire();
    */
        AppRater.app_launched(this);
        
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //getMenuInflater().inflate(R.menu.activity_babycall, menu);
        
        return true;
    }

    public void btnParent(View view){
		 intObjP = new Intent(this,
				ParentActivity.class);
		//intObj.putExtra("USERNAME", name);
		startActivity(intObjP);
		
	}

	//method called when "child mode" is chosen
	public void btnChild(View view){
		intObjC = new Intent(this,
				ChildActivity.class);
		//intObj.putExtra("USERNAME", name);
		startActivity(intObjC);
		Log.d("debug message", "after started child");
    	
	}
	
	  @Override
	    public void onPause() {
		  super.onPause();
		  //      this.mWakeLock.release();
	   //     super.onDestroy();
	    }
    
}
 class MyApplicationClass extends Application {
	  @Override
	  public void onCreate() {
		  Log.d("debug message", "THIS IS APPLICATION");
	    super.onCreate();
	    // TODO Put your application initialization code here.
	  }
	}
