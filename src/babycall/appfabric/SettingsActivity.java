package babycall.appfabric;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.PreferenceActivity;
import android.text.InputType;

public class SettingsActivity extends PreferenceActivity{
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		//EditTextPreference pref = (EditTextPreference)findPreference("searchdelay");
		//pref.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);
	}
}