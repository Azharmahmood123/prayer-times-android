/*
 * Copyright (c) 2016 Metin Kale
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

package com.metinkale.prayerapp;

import android.Manifest;
import android.app.Activity;
import android.app.IntentService;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.CalendarContract;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.text.format.DateUtils;
import android.text.format.Time;
import com.crashlytics.android.Crashlytics;
import com.metinkale.prayer.R;
import com.metinkale.prayerapp.settings.Prefs;
import com.metinkale.prayerapp.vakit.sounds.Sounds;
import com.metinkale.prayerapp.vakit.times.Times;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

public class MainIntentService extends IntentService {

    private static final String ACTION_DOWNLOAD_HADIS = "com.metinkale.prayer.action.DOWNLOAD_HADIS";
    private static final String ACTION_DOWNLOAD_SOUND = "com.metinkale.prayer.action.DOWNLOAD_SOUND";
    private static final String ACTION_SET_ALARMS = "com.metinkale.prayer.action.SET_ALARMS";
    private static final String ACTION_RESCHEDULE_ALARMS = "com.metinkale.prayer.action.RESCHEDULE_ALARMS";
    private static final String ACTION_CALENDAR_INTEGRATION = "com.metinkale.prayer.action.CALENDAR_INTEGRATION";
    private static final String EXTRA_SOUND = "com.metinkale.prayer.extra.SOUND";


    private static Runnable mCallback;

    public MainIntentService() {
        super("MainIntentService");
    }


    public static void startCalendarIntegration(Context context) {
        Intent intent = new Intent(context, MainIntentService.class);
        intent.setAction(ACTION_CALENDAR_INTEGRATION);
        context.startService(intent);
    }


    public static void downloadHadis(Context context, Runnable callback) {
        mCallback = callback;
        Intent intent = new Intent(context, MainIntentService.class);
        intent.setAction(ACTION_DOWNLOAD_HADIS);
        context.startService(intent);
    }

    public static void downloadSound(Context context, Serializable sound, Runnable callback) {
        mCallback = callback;
        Intent intent = new Intent(context, MainIntentService.class);
        intent.setAction(ACTION_DOWNLOAD_SOUND);
        intent.putExtra(EXTRA_SOUND, sound);
        context.startService(intent);
    }

    public static void setAlarms(Context context) {
        Intent intent = new Intent(context, MainIntentService.class);
        intent.setAction(ACTION_SET_ALARMS);
        context.startService(intent);
    }

    public static void rescheduleAlarms(Context context) {
        if (!"samsung".equalsIgnoreCase(Build.MANUFACTURER)) return;
        Intent intent = new Intent(context, MainIntentService.class);
        intent.setAction(ACTION_RESCHEDULE_ALARMS);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            try {
                String action = intent.getAction();
                Runnable callback = mCallback;
                switch (action) {
                    case ACTION_DOWNLOAD_HADIS:
                        mCallback = null;
                        handleDownloadHadis(callback);
                        break;
                    case ACTION_DOWNLOAD_SOUND:
                        Sounds.Sound sound = (Sounds.Sound) intent.getSerializableExtra(EXTRA_SOUND);
                        mCallback = null;
                        handleDownloadSound(sound, callback);
                        break;
                    case ACTION_RESCHEDULE_ALARMS:
                        if (!PreferenceManager.getDefaultSharedPreferences(this).getBoolean("alarmsNeedReschedule", false))
                            break;
                    case ACTION_SET_ALARMS:
                        if ("samsung".equalsIgnoreCase(Build.MANUFACTURER)) {
                            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                            boolean isScreenOn;
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
                                isScreenOn = pm.isInteractive();
                            } else {
                                isScreenOn = pm.isScreenOn();
                            }

                            PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("alarmsNeedReschedule", !isScreenOn).apply();
                        }

                        Times.setAlarms();
                        break;

                    case ACTION_CALENDAR_INTEGRATION:
                        handleCalendarIntegration();
                        break;
                }

            } catch (Exception e) {
                Crashlytics.logException(e);
            }

        }
    }


    private void downloadFile(String Url, File to, final CharSequence notificationText) {
        final Activity act = BaseActivity.CurrectAct;


        NotificationManager nm = (NotificationManager) App.getContext().getSystemService(Context.NOTIFICATION_SERVICE);
        File f = to;

        if (f.exists()) {
            f.delete();
        }
        nm.notify(1111, new NotificationCompat.Builder(App.getContext()).setSmallIcon(R.drawable.ic_abicon).setContentTitle("Downloading").setContentText(notificationText).build());

        class Holder {
            ProgressDialog dlg;
        }
        final Holder holder = new Holder();
        if (act != null) act.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                holder.dlg = new ProgressDialog(act);
                holder.dlg.setTitle("Downloading");
                holder.dlg.setMessage(notificationText);
                holder.dlg.setIndeterminate(true);
                holder.dlg.setCancelable(false);
                holder.dlg.setCanceledOnTouchOutside(false);
                holder.dlg.show();
            }
        });
        try {
            URL url = new URL(Url);
            f.getParentFile().mkdirs();
            URLConnection ucon = url.openConnection();

            InputStream is = ucon.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(is);

            FileOutputStream fos = new FileOutputStream(f.getAbsolutePath());

            byte[] data = new byte[4096];
            long total = 0;
            int count;
            while ((count = bis.read(data)) != -1) {
                total += count;
                fos.write(data, 0, count);
            }


            fos.close();
        } catch (Exception e) {
            f.delete();
        }

        if (act != null) act.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                holder.dlg.dismiss();
            }
        });
        nm.cancel(1111);

    }


    private void handleDownloadHadis(Runnable callback) {
        if (Prefs.getLanguage() == null) return;


        String file = Prefs.getLanguage() + "/hadis.db";
        File f = new File(App.getContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), file);

        String url = App.API_URL + "/hadis." + Prefs.getLanguage() + ".db";
        downloadFile(url, f, getString(R.string.hadis));
        if (callback != null) callback.run();
    }


    private void handleDownloadSound(Sounds.Sound sound, Runnable callback) {
        downloadFile(sound.url, sound.getFile(), sound.name);
        if (callback != null) callback.run();
    }


    private void handleCalendarIntegration() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            Prefs.setCalendar("-1");
            return;
        }
        Context context = App.getContext();
        try {
            ContentResolver cr = context.getContentResolver();


            cr.delete(CalendarContract.Events.CONTENT_URI, CalendarContract.Events.DESCRIPTION + "=\"com.metinkale.prayer\"", null);


            String id = Prefs.getCalendar();

            if ("-1".equals(id) || Prefs.getLanguage() == null) return;
            int year = LocalDate.now().getYear();
            List<int[]> days = new ArrayList<>();
            days.addAll(HicriDate.getHolydays(year));
            days.addAll(HicriDate.getHolydays(year + 1));

            int i = 0;
            ContentValues[] events = new ContentValues[days.size()];
            for (int[] date : days) {
                ContentValues event = new ContentValues();

                event.put(CalendarContract.Events.CALENDAR_ID, id);
                event.put(CalendarContract.Events.TITLE, Utils.getHolyday(date[HicriDate.DAY] - 1));
                event.put(CalendarContract.Events.DESCRIPTION, "com.metinkale.prayer");

                DateTime cal = new DateTime(date[HicriDate.GY], date[HicriDate.GM], date[HicriDate.GD], 0, 0, 0);

                long dtstart = cal.getMillis();
                long dtend = dtstart + DateUtils.DAY_IN_MILLIS;

                event.put(CalendarContract.Events.DTSTART, dtstart);
                event.put(CalendarContract.Events.DTEND, dtend);
                event.put(CalendarContract.Events.EVENT_TIMEZONE, Time.TIMEZONE_UTC);
                event.put(CalendarContract.Events.STATUS, CalendarContract.Events.STATUS_CONFIRMED);
                event.put(CalendarContract.Events.ALL_DAY, 1);

                events[i] = event;
                i++;
            }
            cr.bulkInsert(CalendarContract.Events.CONTENT_URI, events);
            Prefs.setLastCalIntegration(year);
        } catch (Exception e) {
            Prefs.setCalendar("-1");
        }
    }
}
