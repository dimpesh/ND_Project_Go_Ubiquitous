package com.example.android.sunshine.app;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import android.text.format.Time;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;

import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Created by DIMPESH : ${month}
 */
// s1
public class CustomWatchFaceService extends CanvasWatchFaceService {

    //s3 onc Engine
    @Override
    public Engine onCreateEngine() {
        return new WatchFaceEngine();

    }
    // s4 go to manifest for details...


    //s2 class extends Engine
    private class WatchFaceEngine extends Engine implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener
            , DataApi.DataListener, MessageApi.MessageListener{


        // GoogleApiClient Path...
        private  String DATA_PATH="/my_path";
        private GoogleApiClient googleApiClient;
        int id;
        double max;
        double min;
        String desc;

        private Typeface MY_TYPREFACE = Typeface.createFromAsset(getApplicationContext().getAssets(), "montserrat.ttf");

        //s8 defining Necessary variables
        private Typeface WATCH_TEXT_TYPEFACE = Typeface.create(Typeface.SERIF, Typeface.NORMAL);

        private static final int MSG_UPDATE_TIME_ID = 42;
        private static final long DEFAULT_UPDATE_RATE_MS = 1000;
        private long mUpdateRateMs = 1000;

        private Time mDisplayTime;

        private Paint mBackgroundColorPaint;
        private Paint mTextColorPaint;

        private boolean mHasTimeZoneReceiverBeenRegistered = false;
        private boolean mIsInMuteMode;
        private boolean mIsInLowBitAmbient;

        private float mXOffset;
        private float mYOffset;

        private int mBackgroundColor = Color.parseColor("black");
        private int mTextColor = Color.parseColor("red");

        // s9 define broadcast receiver when time zone changes...

        final BroadcastReceiver mTimeZoneBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mDisplayTime.clear(intent.getStringExtra("time-zone"));
                mDisplayTime.setToNow();
            }
        };


        // s10 makes handler that updates watchface every second
        // if it updates every minute we can ignore that...

        private final Handler mTimeHandler = new Handler() {

            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME_ID: {
                        invalidate();
                        if (isVisible() && !isInAmbientMode()) {
                            long currentTimeMillis = System.currentTimeMillis();
                            long delay = mUpdateRateMs - (currentTimeMillis % mUpdateRateMs);
                            mTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME_ID, delay);
                        }
                        break;
                    }
                }
            }
        };

        // s11 initialise the engine....


        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(CustomWatchFaceService.this)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_VARIABLE)
                    .setShowSystemUiTime(false)
                    .build()
            );

            mDisplayTime = new Time();


            // these methods definitions are to be written also
            initBackground();
            initDisplayText();

            googleApiClient = new GoogleApiClient.Builder(
                    CustomWatchFaceService.this).addApi(Wearable.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();

        }

        // custom methods
        private void initBackground() {
            mBackgroundColorPaint = new Paint();
            mBackgroundColorPaint.setColor(mBackgroundColor);
        }

        // custom methods
        private void initDisplayText() {

            mTextColorPaint = new Paint();
            mTextColorPaint.setColor(mTextColor);
            mTextColorPaint.setTypeface(MY_TYPREFACE);
            //mTextColorPaint.setTypeface(WATCH_TEXT_TYPEFACE);
            mTextColorPaint.setAntiAlias(true);
            mTextColorPaint.setTextSize(getResources().getDimension(R.dimen.text_size));
        }

        // s12 handling device state...
        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                if (!mHasTimeZoneReceiverBeenRegistered) {
                    IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
                    CustomWatchFaceService.this.registerReceiver(mTimeZoneBroadcastReceiver, filter);

                    mHasTimeZoneReceiverBeenRegistered = true;
                    googleApiClient.connect();

                }

                mDisplayTime.clear(TimeZone.getDefault().getID());
                mDisplayTime.setToNow();
            } else {
                if (mHasTimeZoneReceiverBeenRegistered) {
                    CustomWatchFaceService.this.unregisterReceiver(mTimeZoneBroadcastReceiver);
                    mHasTimeZoneReceiverBeenRegistered = false;

                    releaseGoogleApiClient();

                }
            }
            updateTimer();

        }

        private void releaseGoogleApiClient() {
            if (googleApiClient != null && googleApiClient.isConnected()) {
                Wearable.DataApi.removeListener(googleApiClient, this);
                googleApiClient.disconnect();
            }
        }

        // s13 define updateTimer method

        private void updateTimer() {
            mTimeHandler.removeMessages(MSG_UPDATE_TIME_ID);
            if (isVisible() && !isInAmbientMode()) {
                mTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME_ID);
            }
        }

        // s14 define onApplyWindowInsets
        // used to determine whetheer round or square basically sets x and y


        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);

            mYOffset = getResources().getDimension(R.dimen.y_offset);

            if (insets.isRound()) {
                mXOffset = getResources().getDimension(R.dimen.x_offset_round);

            } else {
                mXOffset = getResources().getDimension(R.dimen.x_offset_square);
            }
        }


        // s15 onPropertieChanged when h/w properties are determined...like low ambient or burn in protection


        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);

            if (properties.getBoolean(PROPERTY_BURN_IN_PROTECTION, false)) {
                mIsInLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
            }
        }


        // s16 conserve battery in ambient and muted mode


        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);

            if (inAmbientMode) {
                mTextColorPaint.setColor(Color.parseColor("white"));
            } else {
                mTextColorPaint.setColor(Color.parseColor("red"));
            }

            if (mIsInLowBitAmbient) {
                mTextColorPaint.setAntiAlias(!inAmbientMode);
            }

            invalidate();
            updateTimer();
        }


        // s17 onInterruptionChanged is called when interruption settings are manually changed...
        // iff muted or not is checked... (update in one minute or per second...)


        @Override
        public void onInterruptionFilterChanged(int interruptionFilter) {
            super.onInterruptionFilterChanged(interruptionFilter);

            boolean isDeviceMuted = (interruptionFilter == WatchFaceService.INTERRUPTION_FILTER_NONE);

            if (isDeviceMuted) {
                mUpdateRateMs = TimeUnit.MINUTES.toMillis(1);
            } else {
                mUpdateRateMs = DEFAULT_UPDATE_RATE_MS;
            }

            if (mIsInMuteMode != isDeviceMuted) {
                mIsInMuteMode = isDeviceMuted;
                int alpha = (isDeviceMuted) ? 100 : 255;
                mTextColorPaint.setAlpha(alpha);
                invalidate();
                updateTimer();
            }


        }

        //  s18 onTime tick used to update the time , when device in ambient Handler will be disabled but
        // watchface can be updated every minute using built in onTimeTick


        @Override
        public void onTimeTick() {
            super.onTimeTick();

            invalidate();
        }

        // s19 onDraw for drawing watchface


        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            super.onDraw(canvas, bounds);
            mDisplayTime.setToNow();
            // these methods are to be defined...
            drawBackgroundFill(canvas, bounds);
            drawTimeText(canvas);
            drawDayText(canvas);
            drawTemperatureText(canvas);

        }

        // for seting background....
        private void drawBackgroundFill(Canvas canvas, Rect bounds) {

            Paint mBackgroundPaint = new Paint();
            if (isInAmbientMode() || mIsInMuteMode) {
                mBackgroundPaint.setColor(Color.parseColor("#000000"));
            } else
                mBackgroundPaint.setColor(Color.parseColor("#42a5f5"));
//            canvas.drawRect(0,0,bounds.width(),bounds.height(),mBackgroundColorPaint);
            canvas.drawRect(0, 0, bounds.width(), bounds.height(), mBackgroundPaint);

        }

        // for setting time

        private void drawTimeText(Canvas canvas) {
            String timeText = getHourString() + " : " + String.format("%02d", mDisplayTime.minute);
            if (isInAmbientMode() || mIsInMuteMode) {
                timeText += (mDisplayTime.hour < 12) ? " AM" : " PM";
            } else {
                timeText += String.format(" : %02d ", mDisplayTime.second);
//                timeText += (mDisplayTime.hour < 12) ? " AM" : " PM";

            }

//            canvas.drawText(timeText,100,150,mTextColorPaint);
            Paint textpaint = new Paint();
            textpaint.setColor(Color.parseColor("#ffffff"));
            textpaint.setTextSize(getResources().getDimension(R.dimen.text_size));
            textpaint.setTypeface(MY_TYPREFACE);
            textpaint.setAntiAlias(true);
            if (isInAmbientMode() || isInAmbientMode()) {
                textpaint.setTypeface(Typeface.SERIF);
                canvas.drawText(timeText, 90, 150, mTextColorPaint);

            } else
                canvas.drawText(timeText, 90, 150, textpaint);

        }

        // Drawing day..
        private void drawDayText(Canvas canvas) {
            String result;
            String day = getDayName(mDisplayTime.weekDay)+" ";
            String month=getMonthName(mDisplayTime.month)+" ";
            String date=mDisplayTime.monthDay+" ";
            String year=mDisplayTime.year+"";
            result=day+" | "+month+" "+date+","+year;
            String result_ambient=date+month;
            Paint textpaint = new Paint();
            textpaint.setColor(Color.parseColor("#ffffff"));
            textpaint.setTextSize(getResources().getDimension(R.dimen.day_size));
            textpaint.setAntiAlias(true);
            if (isInAmbientMode() || isInAmbientMode())
            {
                canvas.drawText(result_ambient, 100, 180, textpaint);

            } else
                canvas.drawText(result, 90,180, textpaint);
        }

        private void drawTemperatureText(Canvas canvas)
        {
            if(id!=0 && desc!=null)
            {
                int max_value= (int) Math.round(max);
                int min_value=(int)Math.round(min);

                String temp_text=desc+" | "+max_value+"/"+min_value;
                String temp_text_ambient=max_value+"/"+min_value;
                Paint textpaint = new Paint();
                textpaint.setColor(Color.parseColor("#ffffff"));
                textpaint.setTextSize(getResources().getDimension(R.dimen.day_size));
                textpaint.setAntiAlias(true);
                if (isInAmbientMode())
                {
                    canvas.drawText(temp_text_ambient, 180,180, textpaint);
                } else
                    canvas.drawText(temp_text, 120, 210, textpaint);
            }


        }

        private String getDayName(int n) {
            switch (n) {
                case 1:
                    return "MON";
                case 2:
                    return "TUE";
                case 3:
                    return "WED";
                case 4:
                    return "THU";
                case 5:
                    return "FRI";
                case 6:
                    return "SAT";
                case 7:
                    return "SUN";
                default:
                    return "NA";
            }
        }
        public String getMonthName(int n)
        {
            switch (n)
            {
                case 0 :
                    return "JAN";
                case 1 :
                    return "FEB";
                case 2 :
                    return "MAR";
                case 3 :
                    return "APR";
                case 4 :
                    return "MAY";
                case 5 :
                    return "JUN";
                case 6 :
                    return "JUL";
                case 7 :
                    return "AUG";
                case 8 :
                    return "SEP";
                case 9 :
                    return "OCT";
                case 10 :
                    return "NOV";
                case 11 :
                    return "DEC";
                default : return "NA";
            }
        }

        private String getHourString() {
            if (mDisplayTime.hour % 12 == 0)
                return "12";
            else if (mDisplayTime.hour <= 12)
                return String.valueOf(mDisplayTime.hour);
            else
                return String.valueOf(mDisplayTime.hour - 12);

        }
        @Override
        public void onDestroy() {
            releaseGoogleApiClient();
            super.onDestroy();
        }

        // data 12 onConnected method


        @Override
        public void onConnected(Bundle bundle) {
            Log.v("onCOnnect", "connected GoogleAPI");
//            Wearable.DataApi.addListener(googleApiClient, onDataChangedListener);
            Wearable.DataApi.addListener(googleApiClient, this);
/*
            Wearable.DataApi.getDataItems(googleApiClient).setResultCallback(onConnectedResultCallback);
*/
        }

        @Override
        public void onConnectionSuspended(int i) {
            Log.e("onConnSuspend", "suspended GoogleAPI");
        }

        @Override
        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
            Log.e("onConnFailed", "onConnFailed Google Api");
        }


        @Override
        public void onDataChanged(DataEventBuffer dataEvents) {
            for (DataEvent event : dataEvents) {
                Log.v("Wearable :","data changed..");
                if (event.getType() == DataEvent.TYPE_CHANGED) {
                    DataItem item = event.getDataItem();
                    if (item.getUri().getPath().compareTo(DATA_PATH) == 0) {
                        DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
/*
                        String name = dataMap.getString("name");
                        int img = dataMap.getInt("img");
*/
                        id=dataMap.getInt("id");
                        max=dataMap.getDouble("high");
                        min=dataMap.getDouble("low");
                        desc=dataMap.getString("desc");

//                        Log.v("Wearable BASIC",name+img);
                        Log.v("Wearable DESC",desc);
                        Log.v("Wearable MAX",max+"");
                        Log.v("Wearable MIN",min+"");
                        Log.v("Wearable ID",id+"");

                    }
                }
            }
        }

        @Override
        public void onMessageReceived(MessageEvent messageEvent) {
            Log.v("Wearable Message", messageEvent.toString());
        }


    }


}

