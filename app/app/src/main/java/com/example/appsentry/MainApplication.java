package com.example.appsentry;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;

public class MainApplication extends Application {

    public static final String CHANNEL_ID = "appsentry_channel";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "AppSentry Alerts",
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("App installation security alerts");
        channel.enableVibration(true);
        channel.setShowBadge(true);
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) nm.createNotificationChannel(channel);
    }
}