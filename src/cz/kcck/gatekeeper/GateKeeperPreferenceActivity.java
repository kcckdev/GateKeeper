package cz.kcck.gatekeeper;

import android.app.Activity;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.widget.Toast;

public class GateKeeperPreferenceActivity extends Activity {
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

		 // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new PrefsFragment())
                .commit();

	}

	
	
	 public static class PrefsFragment extends PreferenceFragment {
	        @Override
	        public void onCreate(Bundle savedInstanceState) {
	            super.onCreate(savedInstanceState);
	            
	            addPreferencesFromResource(R.xml.preferences);
	    		setValidationToPreference("editText_imageURL"); 

	        }
	        
	        private void setValidationToPreference(String preferenceName) {
	    		findPreference(preferenceName).setOnPreferenceChangeListener(
	    				new OnPreferenceChangeListener() {

	    					@Override
	    					public boolean onPreferenceChange(Preference pref,
	    							Object newValue) {
	    						return checkNotEmpty(pref, (String) newValue);
	    					}
	    				});
	    	}
	        
	        private boolean checkNotEmpty(Preference pref, String value) {

	    		if (value == null || value.equals("")) {
	    			Toast.makeText(this.getActivity(),
	    					"Preference " + pref.getTitle() + " cannot be empty.",
	    					Toast.LENGTH_SHORT).show();
	    			return false;
	    		}	    				
	    	
	    		return true;
	    	}

	    }



}
