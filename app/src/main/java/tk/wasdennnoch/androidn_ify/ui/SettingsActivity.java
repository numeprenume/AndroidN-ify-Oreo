package tk.wasdennnoch.androidn_ify.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.annotation.Keep;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.io.IOException;

import tk.wasdennnoch.androidn_ify.BuildConfig;
import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.systemui.notifications.views.RemoteInputHelper;
import tk.wasdennnoch.androidn_ify.ui.emergency.PreferenceKeys;
import tk.wasdennnoch.androidn_ify.ui.preference.DropDownPreference;
import tk.wasdennnoch.androidn_ify.ui.preference.SubPreference;
import tk.wasdennnoch.androidn_ify.utils.ConfigUtils;
import tk.wasdennnoch.androidn_ify.utils.MiscUtils;
import tk.wasdennnoch.androidn_ify.utils.RomUtils;
import tk.wasdennnoch.androidn_ify.utils.UpdateUtils;
import tk.wasdennnoch.androidn_ify.utils.ViewUtils;

@Keep
public class SettingsActivity extends Activity implements View.OnClickListener,
        PreferenceFragment.OnPreferenceStartFragmentCallback {

    private static final String TAG = "SettingsActivity";

    private static SettingsActivity mInstance = null;

    public static final String ACTION_RECENTS_CHANGED = "tk.wasdennnoch.androidn_ify.action.ACTION_RECENTS_CHANGED";
    public static final String EXTRA_RECENTS_DOUBLE_TAP_SPEED = "extra.recents.DOUBLE_TAP_SPEED";
    public static final String ACTION_FIX_INVERSION = "tk.wasdennnoch.androidn_ify.action.ACTION_FIX_INVERSION";
    public static final String ACTION_GENERAL = "tk.wasdennnoch.androidn_ify.action.ACTION_GENERAL";
    public static final String EXTRA_GENERAL_DEBUG_LOG = "extra.general.DEBUG_LOG";
    public static final String ACTION_KILL_SYSTEMUI = "tk.wasdennnoch.androidn_ify.action.ACTION_KILL_SYSTEMUI";

    private boolean mExperimental;

    @SuppressLint("ApplySharedPref")
    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mInstance = this;
        final SharedPreferences prefs = ConfigUtils.getPreferences(this);
        ViewUtils.applyTheme(this, prefs);
        super.onCreate(savedInstanceState);
        if (!getPackageName().equals("tk.wasdennnoch.androidn_ify") && !BuildConfig.DEBUG && "com.android.vending".equals(getPackageManager().getInstallerPackageName(getPackageName()))) {
            prefs.edit().putBoolean("pro", true).commit();
            new AlertDialog.Builder(this)
                    .setTitle("\"Pro version\" warning")
                    .setMessage("The \"Pro version\" in the play store is not provided by the original developer (MrWasdennnoch). " +
                            "I (the original developer) do not get any penny from it. Please do yourself a favor and refund the purchase as " +
                            "fast as possible to not support people who just grab the work of others and want money for it.\n\n" +
                            "There are snapshot builds of Android N-ify available and linked in the XDA thread and on Github. " +
                            "These contain the same functionality as the play store version. The play version doesn't get any \"additional development\", " +
                            "it's exactly the same as one of these snapshots. Rating in the play store will be ignored by me as well as any bug reports.")
                    .setPositiveButton("Go to play store", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + getPackageName())));
                        }
                    })
                    .setCancelable(false)
                    .setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            finish();
                        }
                    })
                    .show();
        }
        RomUtils.init(this);
        setContentView(R.layout.activity_settings);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M)
            Toast.makeText(this, "API" + Build.VERSION.SDK_INT + "?", Toast.LENGTH_SHORT).show();
        if (!isActivated()) {
            getActionBar().setSubtitle(R.string.not_activated);
        } else if (!isPrefsFileReadable()) {
            TextView warning = findViewById(R.id.prefs_not_readable_warning);
            //noinspection deprecation
            warning.setText(Html.fromHtml(getString(R.string.prefs_not_readable)));
            warning.setVisibility(View.VISIBLE);
            warning.setOnClickListener(this);
        }
        mExperimental = ConfigUtils.isExperimental(prefs);
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction().replace(R.id.fragment, new SettingsFragment()).commit();
            if (BuildConfig.AUTOMATED_BUILD && !prefs.contains(getWarningPrefsKey())) {
                showDialog(R.string.warning, mExperimental ? R.string.experimental_build_warning : R.string.automated_build_warning, false, null, new Runnable() {
                    @Override
                    public void run() {
                        prefs.edit().putBoolean(getWarningPrefsKey(), true).apply();
                    }
                }, android.R.string.ok, R.string.dont_show_again);
            }
        }
    }

    private String getWarningPrefsKey() {
        return mExperimental ? "experimental_build_warning" : "automated_build_warning";
    }

    public static boolean isActivated() {
        return false;
    }

    private boolean isPrefsFileReadable() {
        return true;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.prefs_not_readable_warning:
                showDialog(0, R.string.prefs_not_readable_description, true, null);
                break;
        }
    }

    private static void showDialog(int titleRes, int contentRes, boolean onlyOk, final Runnable okAction) {
        showDialog(titleRes, contentRes, onlyOk, okAction, null, android.R.string.ok, android.R.string.cancel);
    }

    private static void showDialog(int titleRes, int contentRes, boolean onlyOk, final Runnable okAction, final Runnable cancelAction, int okText, int cancelText) {
        showDialog(titleRes, SettingsActivity.getInstance().getString(contentRes), onlyOk, okAction, cancelAction, okText, cancelText);
    }

    private static void showDialog(int titleRes, String content, boolean onlyOk, final Runnable okAction, final Runnable cancelAction, int okText, int cancelText) {
        //noinspection deprecation
        AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.getInstance())
                .setMessage(Html.fromHtml(content));
        if (titleRes > 0)
            builder.setTitle(titleRes);
        if (!onlyOk)
            builder.setNegativeButton(cancelText, cancelAction != null ? new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    cancelAction.run();
                }
            } : null);
        builder.setPositiveButton(okText, okAction != null ? new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                okAction.run();
            }
        } : null);
        builder.create();
        View v = builder.show().findViewById(android.R.id.message);
        if (v instanceof TextView)
            ((TextView) v).setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void showRestartSystemUIDialog() {
        showDialog(R.string.restart_systemui, R.string.restart_systemui_message, false, new Runnable() {
            @Override
            public void run() {
                sendBroadcast(new Intent(ACTION_KILL_SYSTEMUI).setPackage(XposedHook.PACKAGE_SYSTEMUI));
                Toast.makeText(SettingsActivity.this, R.string.restart_broadcast_sent, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragment caller, Preference pref) {
        if (pref instanceof SubPreference) {
            Fragment fragment = SubSettingsFragment.newInstance(((SubPreference) pref));
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            setTitle(pref.getTitle());
            transaction.setCustomAnimations(R.animator.fly_in, R.animator.fade_out, R.animator.fade_in, R.animator.fly_out);
            transaction.replace(R.id.fragment, fragment);
            transaction.addToBackStack("PreferenceFragment");
            transaction.commit();
            getActionBar().setDisplayHomeAsUpEnabled(true);
            return true;
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        getActionBar().setDisplayHomeAsUpEnabled(getFragmentManager().getBackStackEntryCount() != 0);
    }

    public static class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener, UpdateUtils.UpdateListener {

        private SharedPreferences prefs;
        private boolean mExperimental;
        private boolean mShowExperimental;

        @SuppressLint("ApplySharedPref")
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            //noinspection deprecation
            getPreferenceManager().setSharedPreferencesMode(Context.MODE_WORLD_READABLE);
            addPreferencesFromResource(R.xml.preferences);
            prefs = ConfigUtils.getPreferences(getActivity());
            findPreference("theme_colorPrimary").setEnabled(!prefs.getString("app_theme", "light").equals("device"));
            mExperimental = ConfigUtils.isExperimental(prefs);
            mShowExperimental = ConfigUtils.showExperimental(prefs);
            if (UpdateUtils.isEnabled()) {
                if (prefs.getBoolean("check_for_updates", true))
                    UpdateUtils.check(getActivity(), this);
            } else {
                PreferenceCategory appCategory = (PreferenceCategory) findPreference("settings_app");
                Preference updatePref = getPreferenceScreen().findPreference("check_for_updates");
                appCategory.removePreference(updatePref);
            }
            if (!mShowExperimental) {
                PreferenceCategory tweaksCategory = (PreferenceCategory) findPreference("settings_tweaks");
                Preference experimentalPref = getPreferenceScreen().findPreference("settings_experimental");
                tweaksCategory.removePreference(experimentalPref);
            }
            // SELinux test, see XposedHook
            prefs.edit().putBoolean("can_read_prefs", true).commit();
            setHasOptionsMenu(true);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            switch (key) {
                case "app_theme":
                case "theme_colorPrimary":
                case "force_english":
                    getActivity().recreate();
                    break;
                case "hide_launcher_icon":
                    int mode = prefs.getBoolean("hide_launcher_icon", false) ? PackageManager.COMPONENT_ENABLED_STATE_DISABLED : PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
                    getActivity().getPackageManager().setComponentEnabledSetting(new ComponentName(getActivity(), "tk.wasdennnoch.androidn_ify.SettingsAlias"), mode, PackageManager.DONT_KILL_APP);
                    break;
                default:
                    sendUpdateBroadcast(prefs, key);
                    break;
            }
        }

        private void lockPreference(Preference pref) {
            if (pref == null) return;
            pref.setEnabled(false);
            pref.setSummary(getString(R.string.requires_android_version, "Marshmallow"));
        }

        @Override
        public void onResume() {
            super.onResume();
            getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
            if (mShowExperimental != ConfigUtils.showExperimental(ConfigUtils.getPreferences(getActivity()))) {
                getActivity().recreate();
            } else {
                getActivity().setTitle(R.string.app_name);
            }
        }

        @SuppressLint("SetWorldReadable")
        @Override
        public void onPause() {
            super.onPause();
            getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
            File sharedPrefsDir = new File(getActivity().getFilesDir(), "../shared_prefs");
            File sharedPrefsFile = new File(sharedPrefsDir, getPreferenceManager().getSharedPreferencesName() + ".xml");
            if (sharedPrefsFile.exists()) {
                //noinspection ResultOfMethodCallIgnored
                sharedPrefsFile.setReadable(true, false);
                try {
                    Runtime.getRuntime().exec("chmod 664" + sharedPrefsFile.getAbsolutePath());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        @SuppressLint("ApplySharedPref")
        private void sendUpdateBroadcast(SharedPreferences prefs, String key) {
            Intent intent = new Intent();
            switch (key) {
                case "double_tap_speed":
                    intent.setAction(ACTION_RECENTS_CHANGED);
                    intent.putExtra(EXTRA_RECENTS_DOUBLE_TAP_SPEED, prefs.getInt(key, 400));
                    break;
                case "debug_log":
                    intent.setAction(ACTION_GENERAL);
                    intent.putExtra(EXTRA_GENERAL_DEBUG_LOG, prefs.getBoolean(key, false));
                    break;
            }
            if (intent.getAction() != null) {
                prefs.edit().commit();
                getActivity().sendBroadcast(intent);
            }
        }

        @Override
        public void onError(Exception e) {
            Log.e(TAG, "Error fetching updates", e);
        }

        @Override
        public void onFinish(UpdateUtils.UpdateData updateData) {
            Context mContext = getActivity();
            if (mContext == null) return;
            if (updateData.getNumber() > BuildConfig.BUILD_NUMBER && updateData.hasArtifact())
                UpdateUtils.showNotification(updateData, mContext, mExperimental);
        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            inflater.inflate(R.menu.menu_settings, menu);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.restart_systemui:
                    ((SettingsActivity) getActivity()).showRestartSystemUIDialog();
                    return true;
                case R.id.about:
                    startActivity(new Intent(getActivity(), AboutActivity.class));
                    return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    public static class SubSettingsFragment extends PreferenceFragment {

        private static final String TITLE = "title";
        private static final String CONTENT_RES_ID = "content_res_id";

        private SharedPreferences prefs;

        private boolean mShowedDialog;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(getContent());
            PreferenceScreen screen = getPreferenceScreen();
            prefs = screen.getSharedPreferences();
            if (getContent() == R.xml.qs_prefs) {
                if (!MiscUtils.isGBInstalled(getActivity())) {
                    Preference p = findPreference("inject_gb_tiles");
                    p.setEnabled(false);
                    p.setSummary(R.string.inject_gb_tiles_not_installed);
                }
                if (getResources().getBoolean(R.bool.quick_settings_show_full_alarm)) { // Already showing full alarm
                    final Preference forceOldDatePosPref = screen.findPreference("force_old_date_position");
                    forceOldDatePosPref.setEnabled(false);
                    forceOldDatePosPref.setSummary(R.string.force_old_date_position_disabled_summary);
                }
                findPreference("fix_stuck_inversion").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        getActivity().sendBroadcast(new Intent(ACTION_FIX_INVERSION).setPackage("com.android.systemui"));
                        return false;
                    }
                });
            } else if (getContent() == R.xml.recent_prefs) {
                DropDownPreference recentsBehaviorPref = (DropDownPreference) screen.findPreference("recents_button_behavior");
                if (!ConfigUtils.M) {
                    lockPreference(recentsBehaviorPref);
                } else {
                    final Preference delayPref = screen.findPreference("recents_navigation_delay");
                    if (recentsBehaviorPref.getValue().equals("2")) {
                        delayPref.setEnabled(true);
                    }
                    recentsBehaviorPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object newValue) {
                            delayPref.setEnabled(newValue.equals("2"));
                            return true;
                        }
                    });
                }
            } else if (getContent() == R.xml.notifications_prefs) {
                if (!ConfigUtils.M) {
                    lockPreference(screen.findPreference("notification_experimental"));
                    lockPreference(screen.findPreference("enable_notifications_background")); // For now
                }
                if (!RemoteInputHelper.DIRECT_REPLY_ENABLED) {
                    Preference directReplyOnKeyguard = findPreference("allow_direct_reply_on_keyguard");
                    if (directReplyOnKeyguard != null)
                        screen.removePreference(directReplyOnKeyguard);
                }
            } else if (getContent() == R.xml.experimental_prefs) {
                mShowedDialog = prefs.getBoolean("reconfigure_notification_panel_warning", false);

                /*if (UpdateUtils.isConnected(getActivity())) {
                    // Always update from cloud if connected
                    getLoaderManager().initLoader(0, null, updateLoaderCallbacks).startLoading();
                } else {*/
                // Else load config from assets

                Preference reconfigureNotifPanel = findPreference("reconfigure_notification_panel");
                reconfigureNotifPanel.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        boolean newVal = (boolean) newValue;
                        if(newVal && !mShowedDialog) {
                            showDialog(0, R.string.reconfigure_notification_panel_warning, false, new Runnable() {
                                @Override
                                public void run() {
                                    mShowedDialog = true;
                                    prefs.edit().putBoolean("reconfigure_notification_panel_warning", mShowedDialog).apply();
                                }
                            }, null, android.R.string.ok, android.R.string.cancel);
                            return false;
                        }
                        return true;

                    }
                });
                if (!ConfigUtils.M) {
                    lockPreference(reconfigureNotifPanel);
                }
            }
        }

        private void lockPreference(Preference pref) {
            if (pref == null) return;
            pref.setEnabled(false);
            pref.setSummary(getString(R.string.requires_android_version, "Marshmallow"));
        }

        private int getContent() {
            return getArguments().getInt(CONTENT_RES_ID);
        }

        @Override
        public void onResume() {
            super.onResume();
            getActivity().setTitle(getArguments().getString(TITLE));
        }

        public static SubSettingsFragment newInstance(SubPreference preference) {
            SubSettingsFragment fragment = new SubSettingsFragment();
            Bundle b = new Bundle(2);
            b.putString(TITLE, (String) preference.getTitle());
            b.putInt(CONTENT_RES_ID, preference.getContent());
            fragment.setArguments(b);
            return fragment;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static SettingsActivity getInstance() {
        return mInstance;
    }

}