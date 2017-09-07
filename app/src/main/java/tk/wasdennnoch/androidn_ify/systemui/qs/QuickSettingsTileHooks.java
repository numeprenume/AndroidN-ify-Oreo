package tk.wasdennnoch.androidn_ify.systemui.qs;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Space;
import android.widget.TextView;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.extracted.systemui.qs.ButtonRelativeLayout;
import tk.wasdennnoch.androidn_ify.systemui.qs.tiles.QSTile;
import tk.wasdennnoch.androidn_ify.utils.ColorUtils;
import tk.wasdennnoch.androidn_ify.utils.ResourceUtils;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class QuickSettingsTileHooks {

    private static final String TAG = "QuickSettingsTileHooks";

    private static final String CLASS_QS_TILE = "com.android.systemui.qs.QSTile";
    private static final String CLASS_QS_TILE_VIEW = "com.android.systemui.qs.QSTileView";
    public static final ArrayList<String> QS_CLASSES = new ArrayList<>(Arrays.asList(QSTile.CLASS_CELLULAR_TILE, QSTile.CLASS_DND_TILE));

    private static Class mQsTileClass;
    private static Class mQsTileViewClass;
    private static Method mNewTileBackground;
    private static Method mSetRipple;
    private static Method mUpdateRippleSize;

    public static void hook(ClassLoader classLoader) {
        mQsTileViewClass = XposedHelpers.findClass(CLASS_QS_TILE_VIEW, classLoader);
        mQsTileClass = XposedHelpers.findClass(CLASS_QS_TILE, classLoader);
        mUpdateRippleSize = XposedHelpers.findMethodExact(mQsTileViewClass, "updateRippleSize", int.class, int.class);
        mNewTileBackground = XposedHelpers.findMethodExact(mQsTileViewClass, "newTileBackground");
        mSetRipple = XposedHelpers.findMethodExact(mQsTileViewClass, "setRipple", RippleDrawable.class);
        hookQsTileView();
        hookQsTile();
    }

    public static Class getQsTileClass() {
        return mQsTileClass;
    }

    private static void hookQsTileView() {
        XposedHelpers.findAndHookMethod(mQsTileViewClass, "updateAccessibilityOrder", View.class, XC_MethodReplacement.DO_NOTHING);
        XposedHelpers.findAndHookConstructor(mQsTileViewClass, Context.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                ViewGroup qsTileView = (ViewGroup) param.thisObject;
                Context context = (Context) XposedHelpers.getObjectField(qsTileView, "mContext");
                ResourceUtils res = ResourceUtils.getInstance(context);
                View mIcon = (View) XposedHelpers.getObjectField(qsTileView, "mIcon");
                ((ViewGroup) mIcon.getParent()).removeView(mIcon);
                int padding = res.getDimensionPixelSize(R.dimen.qs_quick_tile_padding);
                XposedHelpers.setIntField(qsTileView, "mTilePaddingBelowIconPx", 0);
                XposedHelpers.setIntField(qsTileView, "mTileSpacingPx", 0);
                XposedHelpers.setIntField(qsTileView, "mTilePaddingTopPx", 0);
                FrameLayout mIconFrame = new FrameLayout(context);
                mIconFrame.setForegroundGravity(Gravity.CENTER);
                int size = res.getDimensionPixelSize(R.dimen.qs_quick_tile_size);
                qsTileView.addView(mIconFrame, new FrameLayout.LayoutParams(size, size));
                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(-2, -2);
                params.gravity = Gravity.CENTER;
                params.setMargins(0, padding, 0, padding);
                mIconFrame.addView(mIcon, params);
                XposedHelpers.setAdditionalInstanceField(qsTileView, "mIconFrame", mIconFrame);
                XposedHelpers.setIntField(qsTileView, "mIconSizePx", size);
                Drawable mTileBackground = (Drawable) mNewTileBackground.invoke(qsTileView);
                if (mTileBackground instanceof RippleDrawable) {
                    mSetRipple.invoke(qsTileView, mTileBackground);
                }
                qsTileView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
                qsTileView.setBackground(mTileBackground);
                qsTileView.setPadding(0, 0, 0, 0);
                qsTileView.setClipChildren(false);
                qsTileView.setClipToPadding(false);
                qsTileView.setFocusable(true);
            }
        });
        XposedHelpers.findAndHookMethod(mQsTileViewClass, "recreateLabel", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                ViewGroup qsTileView = (ViewGroup) param.thisObject;
                Context context = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                qsTileView.removeView((TextView) XposedHelpers.getObjectField(param.thisObject, "mLabel"));
                ButtonRelativeLayout labelContainer = createLabel(context);
                XposedHelpers.setAdditionalInstanceField(qsTileView, "mLabelContainer", labelContainer);
                XposedHelpers.setObjectField(qsTileView, "mLabel", labelContainer.findViewById(R.id.tile_label));
                XposedHelpers.setAdditionalInstanceField(qsTileView, "mExpandIndicator", labelContainer.findViewById(R.id.expand_indicator));
                XposedHelpers.setAdditionalInstanceField(qsTileView, "mExpandSpace", labelContainer.findViewById(R.id.expand_space));
                qsTileView.addView(labelContainer);
                return null;
            }
        });
        XposedHelpers.findAndHookMethod(mQsTileViewClass, "updateTopPadding", XC_MethodReplacement.DO_NOTHING);
        XposedHelpers.findAndHookMethod(mQsTileViewClass, "onLayout", boolean.class, int.class, int.class, int.class, int.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                ViewGroup qsTileView = (ViewGroup) param.thisObject;
                FrameLayout mIconFrame = (FrameLayout) XposedHelpers.getAdditionalInstanceField(qsTileView, "mIconFrame");
                Object mRipple = XposedHelpers.getObjectField(qsTileView, "mRipple");
                RelativeLayout labelContainer = (RelativeLayout) XposedHelpers.getAdditionalInstanceField(qsTileView, "mLabelContainer");
                int w = qsTileView.getMeasuredWidth();
                int h = qsTileView.getMeasuredHeight();
                final int iconLeft = (w - mIconFrame.getMeasuredWidth()) / 2;
                final int iconTop = ((qsTileView.getMeasuredHeight() - mIconFrame.getMeasuredHeight()) - labelContainer.getMeasuredHeight()) / 2; //to center the view
                layout(mIconFrame, iconLeft, iconTop);
                if (mRipple != null) {
                    mUpdateRippleSize.invoke(qsTileView, w, h);
                }
                layout(labelContainer, 0, mIconFrame.getBottom());
                return null;
            }
        });
        XposedHelpers.findAndHookMethod(mQsTileViewClass, "onMeasure", int.class, int.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                ViewGroup qsTileView = (ViewGroup) param.thisObject;
                FrameLayout mIconFrame = (FrameLayout) XposedHelpers.getAdditionalInstanceField(qsTileView, "mIconFrame");
                RelativeLayout labelContainer = (RelativeLayout) XposedHelpers.getAdditionalInstanceField(qsTileView, "mLabelContainer");
                int mIconSizePx = XposedHelpers.getIntField(qsTileView, "mIconSizePx");
                int widthMeasureSpec = (int) param.args[0];
                int heightMeasureSpec = (int) param.args[1];
                int w = View.MeasureSpec.getSize(widthMeasureSpec);
                int h = View.MeasureSpec.getSize(heightMeasureSpec);
                mIconFrame.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.AT_MOST), View.MeasureSpec.makeMeasureSpec(mIconSizePx, View.MeasureSpec.EXACTLY));
                labelContainer.measure(widthMeasureSpec, View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.AT_MOST));
                XposedHelpers.callMethod(qsTileView, "setMeasuredDimension", w, h);
                return null;
            }
        });
        XposedBridge.hookAllMethods(mQsTileViewClass, "handleStateChanged", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                Object state = param.args[0];
                ViewGroup qsTileView = (ViewGroup) param.thisObject;
                boolean dualTarget = XposedHelpers.getBooleanField(qsTileView, "mDual");
                String dualLabelContentDescription = (String) XposedHelpers.getObjectField(state, "dualLabelContentDescription");
                ButtonRelativeLayout mLabelContainer = (ButtonRelativeLayout) XposedHelpers.getAdditionalInstanceField(qsTileView, "mLabelContainer");
                ImageView mExpandIndicator = (ImageView) XposedHelpers.getAdditionalInstanceField(qsTileView, "mExpandIndicator");
                Space mExpandSpace = (Space) XposedHelpers.getAdditionalInstanceField(qsTileView, "mExpandSpace");
                TextView mLabel = (TextView) XposedHelpers.getObjectField(qsTileView, "mLabel");
                View mIcon = (View) XposedHelpers.getObjectField(qsTileView, "mIcon");
                qsTileView.setClickable(true);
                if (mIcon instanceof ImageView) {
                    XposedHelpers.callMethod(qsTileView, "setIcon", mIcon, state);
                }
                qsTileView.setContentDescription((String) XposedHelpers.getObjectField(state, "contentDescription"));
                if (!equal(mLabel.getText().toString(), XposedHelpers.getObjectField(state, "label"))) {
                    mLabel.setText((String) XposedHelpers.getObjectField(state, "label"));
                }
                mExpandIndicator.setVisibility(dualTarget ? VISIBLE : GONE);
                mExpandSpace.setVisibility(dualTarget ? VISIBLE : GONE);
                if (!dualTarget) {
                    dualLabelContentDescription = null;
                }
                mLabelContainer.setContentDescription(dualLabelContentDescription);
                if (dualTarget != mLabelContainer.isClickable()) {
                    mLabelContainer.setClickable(dualTarget);
                    mLabelContainer.setLongClickable(dualTarget);
                    mLabelContainer.setBackground(dualTarget ? (Drawable) mNewTileBackground.invoke(qsTileView) : null);
                }
                return null;
            }
        });
        XposedHelpers.findAndHookMethod(mQsTileViewClass, "updateRippleSize", int.class, int.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                int width = (int) param.args[0];
                int height = (int) param.args[1];
                int cx = width / 2;
                int cy = ((ViewGroup) XposedHelpers.getAdditionalInstanceField(param.thisObject, "mIconFrame")).getMeasuredHeight() / 2;
                int rad = (int) (((float) ((View) XposedHelpers.getObjectField(param.thisObject, "mIcon")).getHeight()) * 0.85f);
                ((RippleDrawable) XposedHelpers.getObjectField(param.thisObject, "mRipple")).setHotspotBounds(cx - rad, cy - rad, cx + rad, cy + rad);
                return null;
            }
        });
        XposedHelpers.findAndHookMethod(mQsTileViewClass, "setDual", boolean.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                boolean changed = false;
                boolean dual = (boolean) param.args[0];
                if (dual != XposedHelpers.getBooleanField(param.thisObject, "mDual")) {
                    changed = true;
                }
                XposedHelpers.setBooleanField(param.thisObject, "mDual", dual);
                return changed;
            }
        });
        XposedHelpers.findAndHookMethod(mQsTileViewClass, "init", View.OnClickListener.class, View.OnClickListener.class, View.OnLongClickListener.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                ViewGroup qsTileView = (ViewGroup) param.thisObject;
                View.OnClickListener clickPrimary = (View.OnClickListener) param.args[0];
                View.OnClickListener clickSecondary = (View.OnClickListener) param.args[1];
                View.OnLongClickListener longClick = (View.OnLongClickListener) param.args[2];
                ButtonRelativeLayout mLabelContainer = (ButtonRelativeLayout) XposedHelpers.getAdditionalInstanceField(param.thisObject, "mLabelContainer");

                qsTileView.setOnClickListener(clickPrimary);
                qsTileView.setOnLongClickListener(longClick);
                mLabelContainer.setOnClickListener(clickSecondary);
                mLabelContainer.setOnLongClickListener(longClick);
                mLabelContainer.setClickable(false);
                mLabelContainer.setLongClickable(false);
            }
        });
    }

    private static void hookQsTile() {
        XposedHelpers.findAndHookMethod(mQsTileClass, "supportsDualTargets", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (QS_CLASSES.contains(param.thisObject.getClass().getSimpleName())) {
                    param.setResult(true);
                }
            }
        });
    }

    private static void layout(View child, int left, int top) {
        child.layout(left, top, child.getMeasuredWidth() + left, child.getMeasuredHeight() + top);
    }

    private static ButtonRelativeLayout createLabel(Context context) {
        ResourceUtils res = ResourceUtils.getInstance(context);
        ButtonRelativeLayout labelContainer = new ButtonRelativeLayout(context);
        labelContainer.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        labelContainer.setClipChildren(false);
        labelContainer.setClipToPadding(false);
        labelContainer.setMinimumHeight(res.getDimensionPixelSize(R.dimen.qs_label_min_height));
        labelContainer.setPadding(0, res.getDimensionPixelSize(R.dimen.qs_label_padding), 0, 0);
        LinearLayout labelGroup = new LinearLayout(context);
        RelativeLayout.LayoutParams labelGroupLp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        labelGroupLp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        labelGroup.setGravity(Gravity.CENTER);
        labelGroup.setClipChildren(false);
        labelGroup.setClipToPadding(false);
        labelGroup.setOrientation(LinearLayout.HORIZONTAL);
        Space expandSpace = new Space(context);
        expandSpace.setId(R.id.expand_space);
        TextView label = new TextView(context);
        label.setClickable(false);
        label.setMaxLines(2);
        label.setPadding(0, 0, 0, 0);
        label.setGravity(Gravity.CENTER);
        label.setEllipsize(TextUtils.TruncateAt.END);
        label.setTextSize(TypedValue.COMPLEX_UNIT_PX, res.getDimension(R.dimen.qs_tile_text_size));
        label.setTypeface(Typeface.create("sans-serif-condensed", Typeface.NORMAL));
        label.setTextColor(ColorUtils.getColorAttr(context, android.R.attr.textColorPrimary));
        label.setId(R.id.tile_label);
        ImageView expandIndicator = new ImageView(context);
        LinearLayout.LayoutParams expandIndicatorLp = new LinearLayout.LayoutParams(res.getDimensionPixelSize(R.dimen.qs_expand_indicator_width), -1);
        expandIndicatorLp.setMarginStart(res.getDimensionPixelSize(R.dimen.qs_expand_indicator_margin));
        expandIndicator.setColorFilter(ColorUtils.getColorAttr(context, android.R.attr.textColorPrimary));
        expandIndicator.setImageDrawable(res.getResources().getDrawable(R.drawable.qs_dual_tile_caret));
        expandIndicator.setId(R.id.expand_indicator);
        labelGroup.addView(expandSpace, new LinearLayout.LayoutParams(res.getDimensionPixelSize(R.dimen.qs_expand_space_width), 0));
        labelGroup.addView(label, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        labelGroup.addView(expandIndicator, expandIndicatorLp);
        labelContainer.addView(labelGroup, labelGroupLp);
        return labelContainer;
    }

    public static boolean equal(Object a, Object b) {
        return a == b || (a != null && a.equals(b));
    }
}
