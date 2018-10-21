/*
 * Copyright (c) 2013-2017 Metin Kale
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.metinkale.prayer.times.alarm;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;

import com.crashlytics.android.Crashlytics;
import com.metinkale.prayer.App;
import com.metinkale.prayer.Prefs;
import com.metinkale.prayer.base.BuildConfig;
import com.metinkale.prayer.times.NotificationUtils;
import com.metinkale.prayer.times.fragments.NotificationPopup;
import com.metinkale.prayer.times.sounds.MyPlayer;
import com.metinkale.prayer.times.times.Times;

import org.joda.time.LocalDateTime;

import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;


public class AlarmService extends IntentService {
    private static final String EXTRA_ALARMID = "alarmId";
    private static final String EXTRA_TIME = "time";
    private static AtomicBoolean sInterrupt = new AtomicBoolean(false);

    private static Pair<Alarm, LocalDateTime> sLastSchedule;

    public AlarmService() {
        super("AlarmService");
    }


    public static void setAlarm(@NonNull Context c, @Nullable Pair<Alarm, LocalDateTime> alarm) {

        MyAlarmManager am = MyAlarmManager.with(c);

        Intent i = new Intent(c, AlarmReceiver.class);
        if (alarm != null) {
            if (alarm.equals(sLastSchedule) && !BuildConfig.DEBUG) return;

            if (!Build.MANUFACTURER.equals("samsung") || Build.VERSION.SDK_INT < 20) {
                sLastSchedule = alarm;
            } else {
                PowerManager pm = (PowerManager) c.getSystemService(Context.POWER_SERVICE);
                if (pm.isInteractive()) {
                    sLastSchedule = alarm;
                }
            }

            long time = alarm.second.toDateTime().getMillis();

            i.putExtra(EXTRA_ALARMID, alarm.first.getId());
            i.putExtra(EXTRA_TIME, time);
            PendingIntent service = PendingIntent.getBroadcast(c, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);

            am.cancel(service);

            am.setExact(AlarmManager.RTC_WAKEUP, time, service);


        }

    }


    @Override
    protected void onHandleIntent(Intent intent) {
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "AlarmService");
        wakeLock.acquire();
        try {
            fireAlarm(intent);
        } catch (Exception e) {
            Crashlytics.logException(e);
        }

        Times.setAlarms();

        wakeLock.release();

    }

    public void fireAlarm(@Nullable Intent intent) throws InterruptedException {


        Context c = App.get();

        if ((intent == null) || !intent.hasExtra(EXTRA_ALARMID)) {
            return;
        }


        int alarmId = intent.getIntExtra(EXTRA_ALARMID, 0);


        long time = intent.getLongExtra(EXTRA_TIME, 0);


        Alarm alarm = Alarm.fromId(alarmId);

        if (alarm == null) return;

        intent.removeExtra(EXTRA_ALARMID);

        Times t = alarm.getCity();
        if (t == null) return;

        String notId = t.getID() + "";

        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);

        nm.cancel(notId, NotificationUtils.ALARM);


        alarm.vibrate(c);


        final MyPlayer player = MyPlayer.from(alarm);
        if (player == null) {
            Notification not = AlarmUtils.buildAlarmNotification(c, alarm, time);
            nm.notify(notId, NotificationUtils.ALARM, not);
            return;//no audio, nothing else to do here
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground(NotificationUtils.PLAYING, AlarmUtils.buildPlayingNotification(c, alarm, time));
        } else {
            nm.notify(notId, NotificationUtils.PLAYING, AlarmUtils.buildPlayingNotification(c, alarm, time));
        }

        if (Prefs.showNotificationScreen()) {
            NotificationPopup.start(c, alarm);
            Thread.sleep(1000);
        }


        try {
            player.play();

            if (Prefs.stopByFacedown()) {
                StopByFacedownMgr.start(this, player);
            }

            sInterrupt.set(false);
            while (!sInterrupt.get() && player.isPlaying()) {
                Thread.sleep(500);
            }

            if (player.isPlaying()) {
                player.stop();
            }
        } catch (Exception e) {
            Crashlytics.logException(e);
        }

        nm.cancel(notId, NotificationUtils.PLAYING);

        if (!alarm.isRemoveNotification()) {
            stopForeground(true);
            Notification not = AlarmUtils.buildAlarmNotification(c, alarm, time);
            nm.notify(notId, NotificationUtils.ALARM, not);
        }

        if (alarm.getSilenter() != 0) {
            SilenterReceiver.silent(c, alarm.getSilenter());
        }

        if (NotificationPopup.instance != null && Prefs.showNotificationScreen()) {
            NotificationPopup.instance.finish();
        }


    }


    public static class StopAlarmPlayerReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            sInterrupt.set(true);
        }
    }

    public static class AlarmReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(@NonNull Context context, @NonNull Intent intent) {
            int alarmId = intent.getIntExtra(EXTRA_ALARMID, -1);
            long time = intent.getLongExtra(EXTRA_TIME, -1);
            if (alarmId > 0 && time > 0) {
                Intent service = new Intent(context, AlarmService.class);
                service.putExtra(EXTRA_ALARMID, alarmId);
                service.putExtra(EXTRA_TIME, time);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !Alarm.fromId(alarmId).getSounds().isEmpty()) {
                    context.startForegroundService(service);
                } else {
                    context.startService(service);
                }
            } else {
                Times.setAlarms();
            }
        }
    }

}