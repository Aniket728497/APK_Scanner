package com.example.appsentry.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.example.appsentry.service.AnalysisService;

public class AppInstallReceiver extends BroadcastReceiver {

    private static final String TAG = "AppInstallReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_PACKAGE_ADDED.equals(intent.getAction())) return;

        Uri data = intent.getData();
        if (data == null) return;

        String packageName = data.getSchemeSpecificPart();
        boolean isReplacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false);

        // Skip updates — only new installs
        if (isReplacing) {
            Log.d(TAG, "Skipping update for: " + packageName);
            return;
        }

        Log.d(TAG, "New app installed: " + packageName);

        Intent serviceIntent = new Intent(context, AnalysisService.class);
        serviceIntent.putExtra("package_name", packageName);
        context.startForegroundService(serviceIntent);
    }
}