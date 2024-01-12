package ca.cozycode.cordova.ads;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaWebView;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.util.Log;
import android.provider.Settings;
import android.app.Activity;
import android.net.Uri;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.view.Gravity;
import android.graphics.Rect;

import com.yandex.mobile.ads.common.AdRequestConfiguration;
import com.yandex.mobile.ads.common.AdRequestError;
import com.yandex.mobile.ads.common.ImpressionData;
import com.yandex.mobile.ads.interstitial.InterstitialAd;
import com.yandex.mobile.ads.interstitial.InterstitialAdEventListener;
import com.yandex.mobile.ads.interstitial.InterstitialAdLoadListener;
import com.yandex.mobile.ads.interstitial.InterstitialAdLoader;

import ca.cozycode.cordova.ads.NextAsync;

public class AdMobPlugin extends CordovaPlugin {

    private static final String TAG = "AdMobPlugin";

    //Debug logging
    boolean mExtraDebugLoggingEnabled = true; //SET TO FALSE for app store, asks for more permissions set these in your androidManfiest.xml too

    //cordova plugin
    public final String PLUGIN_API_CALLS_CREATE_BANNER = "banner";
    public final String PLUGIN_API_CALLS_REMOVE_BANNER = "removeBanner";
    public final String PLUGIN_API_CALLS_CREATE_INTERSTITIAL = "interstitial";
    public final String PLUGIN_API_CALLS_READY_INTERSTITIAL = "isReadyInterstitial";
    public final String PLUGIN_API_CALLS_SHOW_INTERSTITIAL = "showInterstitial";
    public final String PLUGIN_API_CALLS_CREATE_REWARDED = "rewarded";
    public final String PLUGIN_API_CALLS_READY_REWARDED = "isReadyRewarded";
    public final String PLUGIN_API_CALLS_SHOW_REWARDED = "showRewarded";
    public final String PLUGIN_API_CALLS_CREATE_REWARDEDINTERSTITIAL = "rewardedInterstitial";
    public final String PLUGIN_API_CALLS_READY_REWARDEDINTERSTITIAL = "isReadyRewardedInterstitial";
    public final String PLUGIN_API_CALLS_SHOW_REWARDEDINTERSTITIAL = "showRewardedInterstitial";
    protected HashMap<String, String> PLUGIN_API_CALLS = new HashMap<String, String>();
    public final String PLUGIN_ERROR_CODES_INVALID_ARGUMENTS = "INVALID_ARGUMENTS";
    public final String PLUGIN_ERROR_CODES_DEVELOPER_ERROR = "PLUGIN_DEVELOPER_ERROR";
    public final String PLUGIN_ERROR_CODES_UNKNOWN_ERROR = "UNKNOWN_ERROR";
    public final String PLUGIN_ERROR_CODES_LOAD_AD_ERROR = "LOAD_AD_ERROR";
    public final String PLUGIN_ERROR_CODES_SHOW_AD_ERROR = "SHOW_AD_ERROR";
    protected HashMap<String, String> PLUGIN_ERROR_CODES = new HashMap<String, String>();
    protected boolean mInitialized = false;
    protected CallbackContext mCurrentCallbackContext;
    protected CordovaWebView mCordovaWebView;
    protected Activity mActivity;
    protected Context mContext;
    private Object mLock = new Object();
    protected boolean mAdsInitialized = false;

    //ads
    //AdView mBannerAdView;
    InterstitialAd mInterstitialAd;
    InterstitialAdLoader mInterstitialAdLoader;
    //RewardedAd mRewardedAd;
    //RewardedInterstitialAd mRewardedInterstitialAd;

    RelativeLayout mBannerLayout;
    FrameLayout mBannerContainerLayout;
    RelativeLayout mInterstitialLayout;
    FrameLayout mInterstitialContainerLayout;

    NextAsync mBannerNext;
    NextAsync mIntersitialNext;
    NextAsync mRewarededNext;
    NextAsync mRewardedInterstitialNext;

    Boolean mRewardedAdRewarded = false;
    boolean mBannerTopActive = false;
    int mRewardedAdRewardedAmount = 0;
    String mRewardedAdRewardedType = "";
    Boolean mRewardedInterstitialAdRewarded = false;
    int mRewardedInterstitialAdRewardedAmount = 0;
    String mRewardedInterstitialAdRewardedType = "";

    /**
     * Cordova plugin
     */

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        PLUGIN_API_CALLS.put(PLUGIN_API_CALLS_CREATE_BANNER, "create or update a banner ad");
        PLUGIN_API_CALLS.put(PLUGIN_API_CALLS_REMOVE_BANNER, "remove the banner ad");
        PLUGIN_API_CALLS.put(PLUGIN_API_CALLS_CREATE_INTERSTITIAL, "prepare an interstitial ad");
        PLUGIN_API_CALLS.put(PLUGIN_API_CALLS_READY_INTERSTITIAL, "check if interstitial as is ready");
        PLUGIN_API_CALLS.put(PLUGIN_API_CALLS_SHOW_INTERSTITIAL, "show a previously prepared interstitial ad");
        PLUGIN_API_CALLS.put(PLUGIN_API_CALLS_CREATE_REWARDED, "prepare an rewarded ad");
        PLUGIN_API_CALLS.put(PLUGIN_API_CALLS_READY_REWARDED, "check if rewarded ad is ready");
        PLUGIN_API_CALLS.put(PLUGIN_API_CALLS_SHOW_REWARDED, "show a previously prepared rewarded ad");
        PLUGIN_API_CALLS.put(PLUGIN_API_CALLS_CREATE_REWARDEDINTERSTITIAL, "prepare an interstitial rewarded ad");
        PLUGIN_API_CALLS.put(PLUGIN_API_CALLS_READY_REWARDEDINTERSTITIAL, "check if interstitial rewarded ad is ready");
        PLUGIN_API_CALLS.put(PLUGIN_API_CALLS_SHOW_REWARDEDINTERSTITIAL, "show a previously prepared interstitial rewarded ad");
        PLUGIN_ERROR_CODES.put(PLUGIN_ERROR_CODES_INVALID_ARGUMENTS, "invalid arguments sent to the plugin, view the documentation");
        PLUGIN_ERROR_CODES.put(PLUGIN_ERROR_CODES_DEVELOPER_ERROR, "something went wrong with the plugin, contact github issues");
        PLUGIN_ERROR_CODES.put(PLUGIN_ERROR_CODES_UNKNOWN_ERROR, "an unexpected error occurred");
        PLUGIN_ERROR_CODES.put(PLUGIN_ERROR_CODES_LOAD_AD_ERROR, "a request to load an ad failed");
        PLUGIN_ERROR_CODES.put(PLUGIN_ERROR_CODES_SHOW_AD_ERROR, "tried to show an ad that has not yet been loaded");
        if (mInitialized)
            throw new RuntimeException("PLUGIN ADS INITIALIZATION_ERROR: too many instances");
        super.initialize(cordova, webView);
        mInitialized = true;
        mActivity = this.cordova.getActivity();
        mContext = mActivity.getApplicationContext(); //this.cordova.getContext(); //mActivity.getApplicationContext();
        mCordovaWebView = webView;
    }

    @Override
    public boolean execute(String action, final JSONArray args, final CallbackContext callbackContext) {
        try {
            mCurrentCallbackContext = callbackContext;

            logInfo(TAG + " " + "executing " + action + " with " + Integer.toString(args.length()) + " arguments");

            NextAsync next = new NextAsync(this, mActivity, callbackContext, args, action);
            if (!PLUGIN_API_CALLS.containsKey(action)) {
                callbackContext.error(makeError(PLUGIN_ERROR_CODES_DEVELOPER_ERROR, "Invalid API Request: " + action));
                return false;
            }
            if (PLUGIN_API_CALLS.get(action).equals(PLUGIN_API_CALLS.get(PLUGIN_API_CALLS_CREATE_BANNER))) {
                // return banner(next);
            }
//            else if (PLUGIN_API_CALLS.get(action).equals(PLUGIN_API_CALLS.get(PLUGIN_API_CALLS_REMOVE_BANNER))) {
//                return removeBanner(next);
//            }
            else if (PLUGIN_API_CALLS.get(action).equals(PLUGIN_API_CALLS.get(PLUGIN_API_CALLS_CREATE_INTERSTITIAL))) {
                return interstitial(next);
            } else if (PLUGIN_API_CALLS.get(action).equals(PLUGIN_API_CALLS.get(PLUGIN_API_CALLS_READY_INTERSTITIAL))) {
                return isReadyInterstitial(next);
            } else if (PLUGIN_API_CALLS.get(action).equals(PLUGIN_API_CALLS.get(PLUGIN_API_CALLS_SHOW_INTERSTITIAL))) {
                return showInterstitial(next);
            }
//            else if (PLUGIN_API_CALLS.get(action).equals(PLUGIN_API_CALLS.get(PLUGIN_API_CALLS_CREATE_REWARDED))) {
//                return rewarded(next);
//            } else if (PLUGIN_API_CALLS.get(action).equals(PLUGIN_API_CALLS.get(PLUGIN_API_CALLS_READY_REWARDED))) {
//                return isReadyRewarded(next);
//            } else if (PLUGIN_API_CALLS.get(action).equals(PLUGIN_API_CALLS.get(PLUGIN_API_CALLS_SHOW_REWARDED))) {
//                return showRewarded(next);
//            } else if (PLUGIN_API_CALLS.get(action).equals(PLUGIN_API_CALLS.get(PLUGIN_API_CALLS_CREATE_REWARDEDINTERSTITIAL))) {
//                return rewardedInterstitial(next);
//            } else if (PLUGIN_API_CALLS.get(action).equals(PLUGIN_API_CALLS.get(PLUGIN_API_CALLS_READY_REWARDEDINTERSTITIAL))) {
//                return isReadyRewardedInterstitial(next);
//            } else if (PLUGIN_API_CALLS.get(action).equals(PLUGIN_API_CALLS.get(PLUGIN_API_CALLS_SHOW_REWARDEDINTERSTITIAL))) {
//                return showRewardedInterstitial(next);
//            }

            return false;
        } catch (Exception ex) {
            callbackContext.error(makeError(PLUGIN_ERROR_CODES_UNKNOWN_ERROR, ex.toString()));
            return false;
        }
    }

    @Override
    public void onDestroy() {
        mAdsInitialized = false;
        mInitialized = false;
    }

    /* Ads Plugin API */

    private View getView() {
        if (View.class.isAssignableFrom(CordovaWebView.class)) {
            return (View) mCordovaWebView;
        }
        return mActivity.getWindow().getDecorView().findViewById(android.R.id.content);
    }


    private boolean interstitial(NextAsync next) {
        cordova.getActivity().runOnUiThread(new Runnable() {
            public void run() {
                try {
                    mIntersitialNext = next;

                    mInterstitialAdLoader = new InterstitialAdLoader(mContext);
                    mInterstitialAdLoader.setAdLoadListener(new InterstitialAdLoadListener() {

                        @Override
                        public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                            mInterstitialAd = interstitialAd;
                            if (mIntersitialNext != null) {
                                mIntersitialNext.callbackContext.success();
                            }
                            logInfo("ad was loaded");
                        }

                        @Override
                        public void onAdFailedToLoad(@NonNull final AdRequestError adRequestError) {
                            logError("ad wasn't loaded");
                            if (mIntersitialNext != null) {
                                mIntersitialNext.callbackContext.error(adRequestError.toString());
                            }
                        }
                    });

                    if (mInterstitialAdLoader != null) {
                        final AdRequestConfiguration adRequestConfiguration =
                                new AdRequestConfiguration.Builder("demo-interstitial-yandex").build();
                        mInterstitialAdLoader.loadAd(adRequestConfiguration);
                    }

                    logInfo("interstitial ad loaded");

                } catch (Exception ex) {
                    next.callbackContext.error(makeError(PLUGIN_ERROR_CODES_UNKNOWN_ERROR, ex.toString()));
                }
                //next.callbackContext.success(); // Thread-safe.
            }
        });
        return true;
    }

    private boolean isReadyInterstitial(NextAsync next) {
        next.callbackContext.success(mInterstitialAd != null ? 1 : 0);
        return true;
    }

    private boolean showInterstitial(NextAsync next) {
        cordova.getActivity().runOnUiThread(new Runnable() {
            public void run() {
                try {
                    if (mInterstitialAd != null) {
                        mInterstitialAd.setAdEventListener(new InterstitialAdEventListener() {
                            @Override
                            public void onAdShown() {
                                logInfo("Interstitial ad has shown, hehe");
                            }

                            @Override
                            public void onAdFailedToShow(@NonNull com.yandex.mobile.ads.common.AdError adError) {
                                logError(adError.toString());
                                if (mIntersitialNext != null) {
                                    mIntersitialNext.callbackContext.error(adError.toString());
                                }
                            }

                            @Override
                            public void onAdDismissed() {
                                // Called when ad is dismissed.
                                // Clean resources after Ad dismissed


                                logInfo("Interstitial ad has been dismissed");


                                if (mIntersitialNext != null){
                                    logInfo(mIntersitialNext.toString());
                                    mIntersitialNext.callbackContext.success();
                                    mIntersitialNext = null;
                                }

                                logInfo("Interstitial ad has been dismissed and callback success");

                                if (mInterstitialAd != null) {
                                    mInterstitialAd.setAdEventListener(null);

                                }

                                // Now you can preload the next interstitial ad.
                                if (mInterstitialAdLoader != null) {
                                    final AdRequestConfiguration adRequestConfiguration =
                                            new AdRequestConfiguration.Builder("demo-interstitial-yandex").build();
                                    mInterstitialAdLoader.loadAd(adRequestConfiguration);
                                }

                                mInterstitialAd = null; //do not show again
                                View mainView = getView();
                                if (mainView != null) {
                                    mainView.requestFocus();
                                }
                            }

                            @Override
                            public void onAdClicked() {
                                // Called when a click is recorded for an ad.
                            }

                            @Override
                            public void onAdImpression(@Nullable ImpressionData impressionData) {

                            }
                        });

                        if (mIntersitialNext != null) next.setArgs(mIntersitialNext);
                        mIntersitialNext = next;

                        mInterstitialAd.show(mActivity);
                    } else {
                        next.OnError(PLUGIN_ERROR_CODES_SHOW_AD_ERROR, mIntersitialNext.getArgsAdMobId());
                    }
                } catch (Exception ex) {
                    next.callbackContext.error(makeError(PLUGIN_ERROR_CODES_UNKNOWN_ERROR, ex.toString()));
                }
            }
        });
        return true;
    }

    private JSONObject rewardObject(boolean rewarded, int amount, String type) {
        JSONObject ret = new JSONObject();
        try {
            ret.put("rewarded", rewarded);
            ret.put("type", type);
            ret.put("amount", amount);
        } catch (JSONException e) {
            logError("ERROR: error creating response object " + e.toString());
        }
        return ret;
    }

    /* Errors */
    protected JSONObject makeError(String name) {
        return makeError(name, null, null, null, null);
    }

    protected JSONObject makeError(String name, String message) {
        return makeError(name, null, null, null, message);
    }

    protected JSONObject makeError(String name, Integer responseCode, String responseMessage, String appendStr) {
        return makeError(name, null, responseCode, responseMessage, appendStr);
    }

    protected JSONObject makeError(String name, String message, Integer responseCode, String responseMessage, String appendStr) {
        String details;
        if (!PLUGIN_ERROR_CODES.containsKey(name)) {
            message = name;
            name = PLUGIN_ERROR_CODES_UNKNOWN_ERROR;
        } else if (message == null) {
            message = PLUGIN_ERROR_CODES.get(name);
        }
        if (appendStr != null) {
            message += " - " + appendStr;
        }
        JSONObject error = new JSONObject();
        try {
            if (name != null) error.put("name", name);
            if (message != null) error.put("message", message);
            if (responseCode != null) error.put("responseCode", (int) responseCode);
            if (responseMessage != null) error.put("responseMessage", responseMessage);
        } catch (JSONException e) {
            logError("ERROR: error creating error object " + e.toString());
        }
        logError(error.toString());
        return error;
    }

    /**
     * Logging
     **/

    public void logInfo(String msg) {
        //Log.d(TAG, "Ad info: " + msg);
        if (!mExtraDebugLoggingEnabled) return;
        System.out.println("Ad info: " + msg);
    }

    public void logError(String msg) {
        //Log.e(TAG, "Ad error: " + msg);
        if (!mExtraDebugLoggingEnabled) return;
        logInfo(TAG + "Ad error: " + msg);
    }

    public void logWarning(String msg) {
        //Log.w(TAG, "Ad warning: " + msg);
        if (!mExtraDebugLoggingEnabled) return;
        logInfo(TAG + "Ad warning: " + msg);
    }

}
