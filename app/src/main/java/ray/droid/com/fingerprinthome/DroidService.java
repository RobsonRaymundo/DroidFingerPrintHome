package ray.droid.com.fingerprinthome;

import android.app.ActivityManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

import com.samsung.android.sdk.SsdkUnsupportedException;
import com.samsung.android.sdk.pass.Spass;
import com.samsung.android.sdk.pass.SpassFingerprint;

public class DroidService extends Service implements Handler.Callback {
    private static final String LOG_TAG = "DroidFingerPrintHome";

    private PowerManager pm;
    private SpassFingerprint mSpassFingerprint;
    private Spass mSpass;
    private Context mContext;

    private boolean onReadyIdentify = false;
    private boolean isServiceEnabled = false;


    private boolean isFeatureEnabled_fingerprint = false;

    private Handler mHandler;
    private static final int MSG_AUTH = 1000;
    private static final int MSG_CANCEL = 1003;

    private SpassFingerprint.IdentifyListener mIdentifyListener = new SpassFingerprint.IdentifyListener() {
        @Override
        public void onFinished(int eventStatus) {
            Log.i(LOG_TAG, "onFinished");
            onReadyIdentify = false;
            mHandler.sendEmptyMessageDelayed(MSG_AUTH, 100);
        }

        @Override
        public void onReady() {
            Log.i(LOG_TAG, "identify state is ready");
        }

        @Override
        public void onStarted() {
            Intent intent = new Intent("android.intent.action.MAIN");
            intent.addCategory("android.intent.category.HOME");
            //intent.setFlags(268435456);
            mContext.startActivity(intent);

            try {
                mSpassFingerprint.cancelIdentify();
            } catch (IllegalStateException ise) {
                Log.i(LOG_TAG, ise.getMessage());
            }
            Log.i(LOG_TAG, "User touched fingerprint sensor");
        }

        @Override
        public void onCompleted() {
            Log.i(LOG_TAG, "the identify is completed");
        }
    };

    private void startIdentify() {
        if (isServiceEnabled && !onReadyIdentify && pm.isInteractive()) {
            try {
                onReadyIdentify = true;
                if (mSpassFingerprint != null) {
                    mSpassFingerprint.startIdentify(mIdentifyListener);
                }
            } catch (IllegalStateException e) {
                onReadyIdentify = false;
                Log.i(LOG_TAG, "Exception: " + e);
                mHandler.sendEmptyMessageDelayed(MSG_AUTH, 1000);
            }
        }
    }

    private void cancelIdentify() {
        if (onReadyIdentify) {
            try {
                if (mSpassFingerprint != null) {
                    mSpassFingerprint.cancelIdentify();
                }
                Log.i(LOG_TAG, "cancelIdentify is called");
            } catch (IllegalStateException ise) {
                Log.i(LOG_TAG, ise.getMessage());
            }
            onReadyIdentify = false;
        }
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_AUTH:
                startIdentify();
                break;
            case MSG_CANCEL:
                cancelIdentify();
                break;
        }
        return true;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mContext = this;
        mHandler = new Handler(this);
        mSpass = new Spass();

        try {
            mSpass.initialize(DroidService.this);
        } catch (SsdkUnsupportedException e) {
            Log.i(LOG_TAG, "Exception: " + e);
            Toast.makeText(mContext, "Biblioteca usada no Fingerprint Home não é suportado para o seu dispositivo.",
                    Toast.LENGTH_LONG).show();
            stopSelf();
            return START_NOT_STICKY;
        } catch (UnsupportedOperationException e) {
            Log.i(LOG_TAG, "Fingerprint Home não é suportado para o seu dispositivo.");
            Toast.makeText(mContext, "Fingerprint Home não é suportado para o seu dispositivo.",
                    Toast.LENGTH_SHORT).show();
            stopSelf();
            return START_NOT_STICKY;
        }
        isFeatureEnabled_fingerprint = mSpass.isFeatureEnabled(Spass.DEVICE_FINGERPRINT);

        if (isFeatureEnabled_fingerprint) {
            mSpassFingerprint = new SpassFingerprint(DroidService.this);
            Log.i(LOG_TAG, "SDK version : " + mSpass.getVersionName());
        } else {
            Log.i(LOG_TAG, "Fingerprint Home não é suportado para o seu dispositivo.");
            Toast.makeText(mContext, "Fingerprint Home não é suportado para o seu dispositivo.",
                    Toast.LENGTH_SHORT).show();
            stopSelf();
            return START_NOT_STICKY;
        }

        registerBroadcastReceiver();

        isServiceEnabled = true;

        mHandler.sendEmptyMessage(MSG_AUTH);


        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(LOG_TAG, "In onDestroy");
        unregisterBroadcastReceiver();
        isServiceEnabled = false;


    }

    @Override
    public IBinder onBind(Intent intent) {
        // Used only in case of bound services.
        return null;
    }

    private BroadcastReceiver mPassReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (Intent.ACTION_USER_PRESENT.equals(action)) {
                mHandler.sendEmptyMessageDelayed(MSG_AUTH, 100);
            } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                mHandler.sendEmptyMessageDelayed(MSG_CANCEL, 100);
            }
        }
    };

    private void registerBroadcastReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_USER_PRESENT);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        mContext.registerReceiver(mPassReceiver, filter);
    }

    private void unregisterBroadcastReceiver() {
        try {
            if (mContext != null) {
                mContext.unregisterReceiver(mPassReceiver);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
