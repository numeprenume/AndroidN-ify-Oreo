package tk.wasdennnoch.androidn_ify.systemui.qs;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.view.View;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.systemui.qs.tiles.AOSPTile;
import tk.wasdennnoch.androidn_ify.systemui.qs.tiles.AndroidN_ifyTile;
import tk.wasdennnoch.androidn_ify.systemui.qs.tiles.BaseTile;
import tk.wasdennnoch.androidn_ify.systemui.qs.tiles.BatteryTile;
import tk.wasdennnoch.androidn_ify.systemui.qs.tiles.LiveDisplayTile;
import tk.wasdennnoch.androidn_ify.systemui.qs.tiles.NekoTile;
import tk.wasdennnoch.androidn_ify.systemui.qs.tiles.PartialScreenshotTile;
import tk.wasdennnoch.androidn_ify.systemui.qs.tiles.QSTile;
import tk.wasdennnoch.androidn_ify.systemui.qs.tiles.misc.BatteryMeterDrawable;
import tk.wasdennnoch.androidn_ify.utils.ColorUtils;
import tk.wasdennnoch.androidn_ify.utils.ConfigUtils;
import tk.wasdennnoch.androidn_ify.utils.ResourceUtils;
import tk.wasdennnoch.androidn_ify.utils.RomUtils;

public class TilesManager {

    private static final String TAG = "TilesManager";

    private final Object mQSTileHost;
    private final Context mContext;

    static final List<String> mCustomTileSpecs = new ArrayList<>();
    private final Map<String, BaseTile> mTiles = new HashMap<>();
    private String mCreateTileViewTileKey;
    private List<String> mSecureTiles;
    public boolean useVolumeTile = false;

    static boolean enableNeko = false;

    static {
        mCustomTileSpecs.add(AndroidN_ifyTile.TILE_SPEC);
        mCustomTileSpecs.add(BatteryTile.TILE_SPEC);
        mCustomTileSpecs.add(NekoTile.TILE_SPEC);
        mCustomTileSpecs.add(PartialScreenshotTile.TILE_SPEC);
        if (RomUtils.isCm() && ConfigUtils.M && !ConfigUtils.qs().alternative_qs_loading)
            mCustomTileSpecs.add(LiveDisplayTile.TILE_SPEC);
    }

    static public void setNekoEnabled(boolean enabled) {
        enableNeko = enabled;
    }

    static int getLabelResource(String spec) throws Exception {
        if (!mCustomTileSpecs.contains(spec))
            throw new Exception("No label for spec '" + spec + "'!");
        switch (spec) {
            case AndroidN_ifyTile.TILE_SPEC:
                return R.string.app_name;
            case BatteryTile.TILE_SPEC:
                return R.string.battery;
            case LiveDisplayTile.TILE_SPEC:
                return R.string.live_display;
            case NekoTile.TILE_SPEC:
                return R.string.default_tile_name;
            case PartialScreenshotTile.TILE_SPEC:
                return R.string.partial_screenshot;
        }
        return 0;
    }

    @SuppressWarnings("deprecation")
    static Drawable getIcon(Context context, String spec) throws Exception {
        Drawable icon;
        Resources.Theme theme = context.getTheme();
        switch (spec) {
            case AndroidN_ifyTile.TILE_SPEC:
                icon = ResourceUtils.getInstance(context).getResources().getDrawable(R.drawable.ic_stat_n, theme);
                break;
            case BatteryTile.TILE_SPEC:
                BatteryMeterDrawable batteryMeterDrawable = new BatteryMeterDrawable(context, new Handler(), ResourceUtils.getInstance(context).getColor(R.color.qs_batterymeter_frame_color));
                batteryMeterDrawable.setLevel(100);
                batteryMeterDrawable.onPowerSaveChanged(false);
                batteryMeterDrawable.setShowPercent(false);
                icon =  batteryMeterDrawable;
                break;
            case NekoTile.TILE_SPEC:
                Drawable nekoIcon =  ResourceUtils.getInstance(context).getResources().getDrawable(R.drawable.stat_icon, theme);
                nekoIcon.setTint(0x4DFFFFFF);
                icon =  nekoIcon;
                break;
            case PartialScreenshotTile.TILE_SPEC:
                icon = ResourceUtils.getInstance(context).getResources().getDrawable(R.drawable.ic_crop, theme);
                break;
            default:
                throw new Exception("No icon for spec '" + spec + "'!");
        }
        icon.setTint(ColorUtils.getColorAttr(context, android.R.attr.textColorPrimary));
        icon.setTintMode(android.graphics.PorterDuff.Mode.SRC_ATOP);//TODO theming see what we do with the battery icon
        return icon;
    }

    TilesManager(Object qsTileHost) {
        mQSTileHost = qsTileHost;
        mContext = (Context) XposedHelpers.callMethod(mQSTileHost, "getContext");
        hook();
    }

    List<String> getCustomTileSpecs() {
        return mCustomTileSpecs;
    }

    private QSTile createTileInternal(String key) {
        switch (key) {
            case AndroidN_ifyTile.TILE_SPEC:
                return new AndroidN_ifyTile(this, mQSTileHost, key);
            case BatteryTile.TILE_SPEC:
                return new BatteryTile(mContext, this, mQSTileHost, key);
            case LiveDisplayTile.TILE_SPEC:
                return new LiveDisplayTile(this, mQSTileHost, key);
            case NekoTile.TILE_SPEC:
                return new NekoTile(this, mQSTileHost, key);
            case PartialScreenshotTile.TILE_SPEC:
                return new PartialScreenshotTile(this, mQSTileHost, key);
        }
        return new QSTile(this, mQSTileHost, key);
    }

    BaseTile createTile(String key) {
        BaseTile tile = createTileInternal(key);
        tile.setSecure(mSecureTiles != null && mSecureTiles.contains(key));
        return tile;
    }

    AOSPTile createAospTile(Object tileHost, String tileSpec) {
        AOSPTile tile = new AOSPTile(this, tileHost, tileSpec);
        tile.setSecure(mSecureTiles != null && mSecureTiles.contains(tileSpec));
        return tile;
    }

    public synchronized void registerTile(BaseTile tile) {
        if (tile == null)
            return;

        String key = tile.getKey();
        if (!mTiles.containsKey(key))
            mTiles.put(key, tile);
    }

    public synchronized void unregisterTile(BaseTile tile) {
        if (tile == null)
            return;

        String key = tile.getKey();
        if (mTiles.containsKey(key))
            mTiles.remove(key);
    }

    private void hook() {
        try {
            ClassLoader classLoader = mContext.getClassLoader();

            Class hookClass;
            try {
                hookClass = XposedHelpers.findClass(QSTile.CLASS_INTENT_TILE, classLoader);
            } catch (Throwable ignore) {
                try {
                    hookClass = XposedHelpers.findClass(QSTile.CLASS_VOLUME_TILE, classLoader);
                    XposedHook.logI(TAG, "Using volume tile for custom tiles");
                    useVolumeTile = true;
                } catch (Throwable t) {
                    XposedHook.logE(TAG, "Couldn't find required tile class, aborting hook", null);
                    return;
                }
            }

            XposedHelpers.findAndHookMethod(hookClass, "handleUpdateState",
                    QSTile.CLASS_TILE_STATE, Object.class, new XC_MethodHook() {
                        @SuppressWarnings("SuspiciousMethodCalls")
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            final BaseTile tile = mTiles.get(XposedHelpers.getAdditionalInstanceField(param.thisObject, QSTile.TILE_KEY_NAME));
                            if (tile != null) {
                                tile.handleUpdateState(param.args[0], param.args[1]);
                                param.setResult(null);
                            }
                        }
                    });

            XposedHelpers.findAndHookMethod(QSTile.CLASS_QS_TILE, classLoader, "createTileView",
                    Context.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            mCreateTileViewTileKey = (String) XposedHelpers.getAdditionalInstanceField(param.thisObject, QSTile.TILE_KEY_NAME);
                        }

                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            if (mCreateTileViewTileKey == null) return;
                            final BaseTile tile = mTiles.get(mCreateTileViewTileKey);
                            if (tile != null)
                                tile.onCreateTileView((View) param.getResult());
                            mCreateTileViewTileKey = null;
                        }
                    });

            XposedHelpers.findAndHookMethod(QSTile.CLASS_TILE_VIEW, classLoader, "createIcon",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            if (mCreateTileViewTileKey == null) return;
                            final BaseTile tile = mTiles.get(mCreateTileViewTileKey);
                            if (tile != null) {
                                View icon = tile.onCreateIcon();
                                if (icon != null)
                                    param.setResult(icon);
                            }
                        }
                    });

            XposedHelpers.findAndHookMethod(QSTile.CLASS_QS_TILE, classLoader, "handleDestroy",
                    new XC_MethodHook() {
                        @SuppressWarnings("SuspiciousMethodCalls")
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            final BaseTile tile = mTiles.get(XposedHelpers.getAdditionalInstanceField(param.thisObject, QSTile.TILE_KEY_NAME));
                            if (tile != null)
                                tile.handleDestroy();
                        }
                    });

            XposedHelpers.findAndHookMethod(QSTile.CLASS_QS_TILE, classLoader, "getDetailAdapter",
                    new XC_MethodHook() {
                        @SuppressWarnings("SuspiciousMethodCalls")
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            final BaseTile tile = mTiles.get(XposedHelpers.getAdditionalInstanceField(param.thisObject, QSTile.TILE_KEY_NAME));
                            if (tile != null)
                                param.setResult(tile.getDetailAdapter());
                        }
                    });

            Method clickMethod;
            try {
                clickMethod = XposedHelpers.findMethodExact(hookClass, "handleClick");
            } catch (Throwable t) { // PA
                clickMethod = XposedHelpers.findMethodExact(hookClass, "handleToggleClick");
                XposedHelpers.findAndHookMethod(hookClass, "handleClick", boolean.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        param.args[0] = true; // So that handleToggleClick gets called
                    }
                });
            }
            XposedBridge.hookMethod(clickMethod, new XC_MethodHook() {
                        @SuppressWarnings("SuspiciousMethodCalls")
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            final BaseTile tile = mTiles.get(XposedHelpers.getAdditionalInstanceField(param.thisObject, QSTile.TILE_KEY_NAME));
                            if (tile != null) {
                                tile.handleClickInner();
                                param.setResult(null);
                            }
                        }
                    });

            try {
                Method longClickMethod;
                try {
                    longClickMethod = XposedHelpers.findMethodExact(hookClass, "handleLongClick");
                } catch (Throwable t) { // PA
                    longClickMethod = XposedHelpers.findMethodExact(hookClass, "handleDetailClick");
                }
                XposedBridge.hookMethod(longClickMethod, new XC_MethodHook() {
                    @SuppressWarnings("SuspiciousMethodCalls")
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        final BaseTile tile = mTiles.get(XposedHelpers.getAdditionalInstanceField(param.thisObject, QSTile.TILE_KEY_NAME));
                        if (tile != null) {
                            tile.handleLongClick();
                            param.setResult(null);
                        }
                    }
                });
            } catch (Throwable t) {
                XposedHook.logW(TAG, "No long click method found! Custom tiles won't recognize long clicks.");
            }

            XposedHelpers.findAndHookMethod(QSTile.CLASS_QS_TILE + "$H", classLoader, "handleMessage", Message.class, new XC_MethodHook() {
                @SuppressWarnings("SuspiciousMethodCalls")
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Message msg = (Message) param.args[0];
                    final BaseTile tile = mTiles.get(XposedHelpers.getAdditionalInstanceField(XposedHelpers.getSurroundingThis(param.thisObject), QSTile.TILE_KEY_NAME));
                    if (tile != null) {
                        if (msg.what == 3) {
                            tile.handleSecondaryClick();
                        }
                    }
                }
            });
            XC_MethodHook supportsDualTargets = new XC_MethodHook() {
                @SuppressWarnings("SuspiciousMethodCalls")
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    final BaseTile tile = mTiles.get(XposedHelpers.getAdditionalInstanceField(param.thisObject, QSTile.TILE_KEY_NAME));
                    if (tile != null){
                        param.setResult(tile.supportsDualTargets());
                    }
                }
            };
            try {
                XposedHelpers.findAndHookMethod(QSTile.CLASS_QS_TILE, classLoader, "supportsDualTargets", supportsDualTargets);
            } catch (NoSuchMethodError e) { //LOS
                XposedHelpers.findAndHookMethod(QSTile.CLASS_QS_TILE, classLoader, "hasDualTargetsDetails", supportsDualTargets);
            }

            XposedHelpers.findAndHookMethod(hookClass, "setListening",
                    boolean.class, new XC_MethodHook() {
                        @SuppressWarnings("SuspiciousMethodCalls")
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            final BaseTile tile = mTiles.get(XposedHelpers.getAdditionalInstanceField(param.thisObject, QSTile.TILE_KEY_NAME));
                            if (tile != null) {
                                tile.setListening((boolean) param.args[0]);
                                param.setResult(null);
                            }
                        }
                    });

            XposedHelpers.findAndHookMethod(QSTile.CLASS_RESOURCE_ICON, classLoader, "getDrawable",
                    Context.class, new XC_MethodHook() {
                        @SuppressWarnings("SuspiciousMethodCalls")
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            final BaseTile tile = mTiles.get(XposedHelpers.getAdditionalInstanceField(param.thisObject, QSTile.TILE_KEY_NAME));
                            if (tile != null && tile instanceof QSTile) {
                                param.setResult(((QSTile) tile).getResourceIconDrawable());
                            }
                        }/*

                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            Drawable icon = (Drawable) param.getResult();
                            icon.setTint(android.R.attr.textColorPrimary);
                        }*/
                    });

            try { // Fix a SystemUI crash caused by this tile
                XposedBridge.hookAllMethods(XposedHelpers.findClass(QSTile.CLASS_VISUALIZER_TILE, classLoader), "handleUpdateState", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (XposedHelpers.getObjectField(param.thisObject, "mVisualizer") == null)
                            param.setResult(null);
                    }
                });
            } catch (Throwable ignore) {
            }

        } catch (Throwable t) {
            XposedHook.logE(TAG, "Error in hook", t);
        }
    }

    void onSecureTilesChanged(List<String> secureTiles) {
        mSecureTiles = secureTiles;
        StringBuffer buffer = new StringBuffer();
        for (String sp : mSecureTiles) buffer.append(sp);
        String s = buffer.toString();
        XposedHook.logD(TAG, "onSecureTilesChanged called with specs: " + s);
        for (Map.Entry entry : mTiles.entrySet()) {
            BaseTile tile = (BaseTile) entry.getValue();
            if (tile != null) {
                tile.setSecure(mSecureTiles.contains(tile.getKey()));
            }
        }
    }
}
