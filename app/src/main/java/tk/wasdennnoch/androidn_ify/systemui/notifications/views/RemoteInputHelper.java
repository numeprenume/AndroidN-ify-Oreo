package tk.wasdennnoch.androidn_ify.systemui.notifications.views;

import android.app.PendingIntent;
import android.app.RemoteInput;
import android.view.View;
import android.view.ViewParent;
import android.widget.TextView;

import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.extracted.systemui.RemoteInputView;
import tk.wasdennnoch.androidn_ify.systemui.notifications.NotificationHooks;
import tk.wasdennnoch.androidn_ify.utils.Classes;
import tk.wasdennnoch.androidn_ify.utils.Fields;
import tk.wasdennnoch.androidn_ify.utils.Methods;
import static tk.wasdennnoch.androidn_ify.utils.ReflectionUtils.*;


public class RemoteInputHelper {

    @SuppressWarnings({"PointlessBooleanExpression", "ConstantConditions"})
    public static final String TAG = "RemoteInputHelper";
    public static final boolean DIRECT_REPLY_ENABLED = true;

    public static boolean handleRemoteInput(View view, RemoteInput[] inputs, PendingIntent pendingIntent) {

        //RemoteInput[] inputs = null;
        /*if (tag instanceof RemoteInput[]) {
            inputs = (RemoteInput[]) tag;
        }*/

        if (inputs == null) {
            return false;
        }

        RemoteInput input = null;

        for (RemoteInput i : inputs) {
            if (i.getAllowFreeFormInput()) {
                input = i;
            }
        }

        if (input == null) {
            return false;
        }

        ViewParent p = view.getParent();
        RemoteInputView riv = null;
        while (p != null) {
            if (p instanceof View) {
                View pv = (View) p;
                if ((boolean) XposedHelpers.callMethod(pv, "isRootNamespace")) {
                    riv = findRemoteInputView(pv);
                    break;
                }
            }
            p = p.getParent();
        }
        Object row = null;
        while (p != null) {
            if (Classes.SystemUI.ExpandableNotificationRow.isInstance(p)) {
                row = p;
                break;
            }
            p = p.getParent();
        }

        if (row == null) {
            return false;
        }

        invoke(Methods.SystemUI.ExpandableNotificationRow.setUserExpanded, row, true);

        if (riv == null) {
            View expandedChild = invoke(Methods.SystemUI.NotificationContentView.getExpandedChild, get(Fields.SystemUI.ExpandableNotificationRow.mPrivateLayout, row));
            riv = findRemoteInputView(expandedChild);
            if (riv == null) {
                return false;
            }
//            if (!expandedChild.isShown()) {
//                onMakeExpandedVisibleForRemoteInput(row, view);
//                return true;
//            }
        }

        int width = view.getWidth();
        if (view instanceof TextView) {
            // Center the reveal on the text which might be off-center from the TextView
            TextView tv = (TextView) view;
            if (tv.getLayout() != null) {
                int innerWidth = (int) tv.getLayout().getLineWidth(0);
                innerWidth += tv.getCompoundPaddingLeft() + tv.getCompoundPaddingRight();
                width = Math.min(width, innerWidth);
            }
        }
        int cx = view.getLeft() + width / 2;
        int cy = view.getTop() + view.getHeight() / 2;
        int w = riv.getWidth();
        int h = riv.getHeight();
        int r = Math.max(
                Math.max(cx + cy, cx + (h - cy)),
                Math.max((w - cx) + cy, (w - cx) + (h - cy)));

        riv.setRevealParameters(cx, cy, r);
        riv.setPendingIntent(pendingIntent);
        riv.setRemoteInput(inputs, input);
        riv.focusAnimated();

        return true;
    }

    private static RemoteInputView findRemoteInputView(View v) {
        if (v == null) {
            return null;
        }
        return (RemoteInputView) v.findViewWithTag(RemoteInputView.VIEW_TAG);
    }

    public static void setWindowManagerFocus(boolean focus) {
        NotificationHooks.remoteInputActive = focus;
        if (NotificationHooks.mStatusBarWindowManager != null)
            XposedHelpers.callMethod(NotificationHooks.mStatusBarWindowManager, "apply", XposedHelpers.getObjectField(NotificationHooks.mStatusBarWindowManager, "mCurrentState"));
    }
}