package tk.wasdennnoch.androidn_ify.systemui.qs.tiles.hooks;

import android.content.Intent;
import android.provider.Settings;

import com.android.internal.logging.MetricsLogger;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

import tk.wasdennnoch.androidn_ify.utils.ConfigUtils;

public class BluetoothTileHook extends QSTileHook {

    private static final String CLASS_BLUETOOTH_TILE = "com.android.systemui.qs.tiles.BluetoothTile";

    private Object mController;

    public BluetoothTileHook(ClassLoader classLoader) {
        super(classLoader, CLASS_BLUETOOTH_TILE);
    }

    @Override
    protected void afterConstructor(XC_MethodHook.MethodHookParam param) {
        mController = getObjectField("mController");
    }

    @Override
    public void handleClick() {
        Object mState = getObjectField("mState");
        boolean enabled = XposedHelpers.getBooleanField(mState, "value");

        if (ConfigUtils.M) {
            MetricsLogger.action(mContext, MetricsLogger.QS_BLUETOOTH, !enabled);
        }
        XposedHelpers.callMethod(mController, "setBluetoothEnabled", !enabled);
    }

    @Override
    protected Intent getSettingsIntent() {
        return new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
    }

}
