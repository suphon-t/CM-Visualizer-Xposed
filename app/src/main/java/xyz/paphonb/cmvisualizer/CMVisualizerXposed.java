package xyz.paphonb.cmvisualizer;

import android.view.View;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class CMVisualizerXposed implements IXposedHookLoadPackage {

    private static Object mVisualizerView;
    private static boolean mExpanded = false;

    private static XC_MethodReplacement updateViewVisibility = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            int mStatusBarState = XposedHelpers.getIntField(param.thisObject, "mStatusBarState");
            boolean showVisualizer = mStatusBarState != 0;
            boolean enabled = XposedHelpers.getBooleanField(param.thisObject, "mVisualizerEnabled");
            showVisualizer = mExpanded || showVisualizer;
            showVisualizer = enabled && showVisualizer;
            final int curVis = (int) XposedHelpers.callMethod(param.thisObject, "getVisibility");
            final int newVis = showVisualizer ? View.VISIBLE : View.GONE;
            if (curVis != newVis) {
                XposedHelpers.callMethod(param.thisObject, "setVisibility", newVis);
                XposedHelpers.callMethod(param.thisObject, "checkStateChanged");
            }
            return null;
        }
    };

    private static XC_MethodHook makeStatusBarView = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            mVisualizerView = XposedHelpers.getObjectField(param.thisObject, "mVisualizerView");
        }
    };

    private static XC_MethodHook updatePanelExpanded = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            boolean isExpanded = XposedHelpers.getBooleanField(param.thisObject, "mPanelExpanded");
            if (isExpanded != mExpanded) {
                mExpanded = isExpanded;
                XposedHelpers.callMethod(mVisualizerView, "updateViewVisibility");
            }
        }
    };

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (lpparam.packageName.equals("com.android.systemui")) {
            XposedHelpers.findAndHookMethod("com.android.systemui.statusbar.VisualizerView", lpparam.classLoader, "updateViewVisibility", updateViewVisibility);
            XposedHelpers.findAndHookMethod("com.android.systemui.statusbar.phone.PhoneStatusBar", lpparam.classLoader, "makeStatusBarView", makeStatusBarView);
            XposedHelpers.findAndHookMethod("com.android.systemui.statusbar.phone.NotificationPanelView", lpparam.classLoader, "updatePanelExpanded", updatePanelExpanded);
        }
    }
}
