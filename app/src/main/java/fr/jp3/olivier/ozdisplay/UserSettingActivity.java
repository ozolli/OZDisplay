package fr.jp3.olivier.ozdisplay;

import android.app.ActionBar;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;

public class UserSettingActivity extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new MyPreferenceFragment()).commit();
    }

    public static class MyPreferenceFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences);
            EditTextPreference etp = (EditTextPreference) findPreference("udpPort");
            etp.setSummary("Numéro de port UDP du serveur wifi NMEA : "
                    + getPreferenceManager().getSharedPreferences().getString("udpPort", "10110"));
            setDefaultSummary("1", "BOAT SPEED (kn)");
            setDefaultSummary("2", "TARGET SPD (kn)");
            setDefaultSummary("3", "OPTIMUM VMG (%%)"); // %% pour échapper %
            setDefaultSummary("4", "OPTIMUM VMG (°)");
            setDefaultSummary("5", "TODAY LOCH (nm)");
            setDefaultSummary("6", "SOG (kn)");
            setDefaultSummary("7", "TWS (kn)");
            setDefaultSummary("8", "AWS (kn)");
            setDefaultSummary("9", "HEEL (°)");
            setDefaultSummary("10", "DEPTH (m)");
        }

        @Override
        public void onResume() {
            super.onResume();
            // Set up a listener whenever a key changes
            getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onPause() {
            super.onPause();
            // Set up a listener whenever a key changes
            getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            MainActivity.prefsChanged = true;
            if (key.equals("udpPort")) {
                EditTextPreference etp = (EditTextPreference) findPreference("udpPort");
                //etp.setSummary("dummy");
                etp.setSummary("Numéro de port UDP du serveur wifi NMEA : "
                        + getPreferenceManager().getSharedPreferences().getString("udpPort", "10110"));
            }
        }

        void setDefaultSummary(String key, String def) {
            ListPreference lp = (ListPreference) findPreference(key);
            if (lp.getSummary().equals("")) lp.setSummary(def);
        }
    }
}
