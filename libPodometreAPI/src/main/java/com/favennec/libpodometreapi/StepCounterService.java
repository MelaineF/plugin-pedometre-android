package com.favennec.libpodometreapi;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

public class StepCounterService extends Service implements StepCounter.StepListener {

    private static final String CHANNEL_ID = "PedometerChannel";
    private static final int NOTIFICATION_ID = 1;

    private StepCounter stepCounter;
    private int lastNotifiedSteps = -1;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, buildNotification(StepCounter.getLastKnownStepsToday(this)));
        stepCounter = new StepCounter(this, this);
        stepCounter.start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (stepCounter != null) stepCounter.stop();
    }

    @Override
    public void onStepChanged(int steps) {
        if (steps == lastNotifiedSteps) return; // pas de changement, rien à faire
        lastNotifiedSteps = steps;
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.notify(NOTIFICATION_ID, buildNotification(steps));
        PedometerBridge.notifyUnity(steps);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Podomètre",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Comptage des pas en arrière-plan");
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    private Notification buildNotification(int steps) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return new Notification.Builder(this, CHANNEL_ID)
                    .setContentTitle("Podomètre actif")
                    .setContentText(steps + " pas aujourd'hui")
                    .setSmallIcon(android.R.drawable.ic_menu_compass)
                    .build();
        } else {
            return new Notification.Builder(this)
                    .setContentTitle("Podomètre actif")
                    .setContentText(steps + " pas aujourd'hui")
                    .setSmallIcon(android.R.drawable.ic_menu_compass)
                    .build();
        }
    }
}
