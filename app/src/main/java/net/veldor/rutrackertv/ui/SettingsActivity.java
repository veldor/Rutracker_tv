package net.veldor.rutrackertv.ui;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.DropDownPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import net.veldor.rutrackertv.App;
import net.veldor.rutrackertv.R;

import java.util.Collection;
import java.util.HashMap;
import java.util.Set;

public class SettingsActivity extends FragmentActivity {
    public static final String KEY_HIDE_NO_SEED = "hide_no_seed";


    private static final HashMap<String, String> SORT_BY = new HashMap<String, String>() {{
        put("1", "дате регистрации");
        put("2", "названию темы");
        put("4", "количеству скачиваний");
        put("10", "количеству сидов");
        put("11", "количеству личей");
        put("7", "размеру");
    }};


    public static final String KEY_SORT_BY = "sort_by";
    public static final String KEY_LOW_TO_HIGH = "low to hight";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        if (savedInstanceState == null) {
            Fragment preferenceFragment = new PreferenceFragment();
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.add(R.id.pref_container, preferenceFragment);
            ft.commit();
        }
    }

    public static class PreferenceFragment extends PreferenceFragmentCompat {
        private SharedPreferences mPreferences;
        private SwitchPreference mHideNoSeed;
        private DropDownPreference mSortBy;
        private SwitchPreference mLowToHight;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            FragmentActivity activity = getActivity();
            if (activity != null) {
                mPreferences = App.getInstance().getSharedPreferences();
                PreferenceScreen rootScreen = getPreferenceManager().createPreferenceScreen(getActivity());
                setPreferenceScreen(rootScreen);

                boolean hideEmpty = mPreferences.getBoolean(SettingsActivity.KEY_HIDE_NO_SEED, true);
                mHideNoSeed = new SwitchPreference(activity);
                mHideNoSeed.setKey(SettingsActivity.KEY_HIDE_NO_SEED);
                mHideNoSeed.setTitle(R.string.hide_no_seed_message);
                mHideNoSeed.setChecked(hideEmpty);
                rootScreen.addPreference(mHideNoSeed);

                mSortBy = new DropDownPreference(activity);

                Set<String> keys = SORT_BY.keySet();
                Collection<String> values = SORT_BY.values();

                mSortBy.setEntries(values.toArray(new String[0]));
                mSortBy.setEntryValues(keys.toArray(new String[0]));
                mSortBy.setValueIndex(1);
                mSortBy.setKey(KEY_SORT_BY);
                mSortBy.setTitle(R.string.sort_by_message);
                mSortBy.setSummary("Результаты сортирутся по " + SORT_BY.get(mPreferences.getString(KEY_SORT_BY, "10")));
                rootScreen.addPreference(mSortBy);

                mSortBy.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        // обновлю информацию о выбранном способе сортировки
                        mSortBy.setSummary("Результаты сортируются по " + SORT_BY.get((String)newValue));
                        return true;
                    }
                });


                boolean lowToHigh = mPreferences.getBoolean(SettingsActivity.KEY_HIDE_NO_SEED, true);
                mLowToHight = new SwitchPreference(activity);
                mLowToHight.setKey(SettingsActivity.KEY_LOW_TO_HIGH);
                mLowToHight.setTitle(R.string.from_low_to_high_message);
                mLowToHight.setChecked(lowToHigh);
                rootScreen.addPreference(mLowToHight);

                setHasOptionsMenu(true);
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            mHideNoSeed.setChecked(mPreferences.getBoolean(SettingsActivity.KEY_HIDE_NO_SEED, true));
            mLowToHight.setChecked(mPreferences.getBoolean(SettingsActivity.KEY_LOW_TO_HIGH, false));
        }
    }
}
