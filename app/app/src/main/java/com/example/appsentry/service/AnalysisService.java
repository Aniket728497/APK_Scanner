package com.example.appsentry.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.appsentry.MainApplication;
import com.example.appsentry.analysis.PackageNameAnalyzer;
import com.example.appsentry.analysis.PermissionAnalyzer;
import com.example.appsentry.analysis.RiskMerger;
import com.example.appsentry.analysis.SignatureAnalyzer;
import com.example.appsentry.model.AppReport;
import com.example.appsentry.network.BackendClient;
import com.example.appsentry.ui.AnalysisActivity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.example.appsentry.analysis.InstallSourceAnalyzer;
public class AnalysisService extends Service {

    private static final String TAG = "AnalysisService";
    private static final int PERSISTENT_NOTIF_ID = 1000;
    private ExecutorService executor;
    private BroadcastReceiver installReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        executor = Executors.newCachedThreadPool();

        // Start as foreground immediately with a persistent notification
        startForeground(PERSISTENT_NOTIF_ID, buildPersistentNotification());

        // Register receiver INSIDE the foreground service — bypasses OEM restrictions
        installReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (!Intent.ACTION_PACKAGE_ADDED.equals(intent.getAction())) return;
                if (intent.getData() == null) return;

                String packageName = intent.getData().getSchemeSpecificPart();
                boolean isReplacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false);

                Log.d(TAG, "Broadcast received for: " + packageName + " replacing=" + isReplacing);

                if (!isReplacing) {
                    executor.execute(() -> runAnalysis(packageName));
                }
            }
        };

        IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
        filter.addDataScheme("package");
        registerReceiver(installReceiver, filter);

        Log.d(TAG, "AnalysisService started, receiver registered");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Handle direct triggers (e.g. from ADB test broadcast via old receiver)
        if (intent != null && intent.hasExtra("package_name")) {
            String packageName = intent.getStringExtra("package_name");
            if (packageName != null) {
                executor.execute(() -> runAnalysis(packageName));
            }
        }
        // Keep service alive
        return START_STICKY;
    }

    private void runAnalysis(String packageName) {
        Log.d(TAG, "Running analysis for: " + packageName);

        PackageManager pm = getPackageManager();
        String appName;
        try {
            ApplicationInfo ai = pm.getApplicationInfo(packageName, 0);
            appName = pm.getApplicationLabel(ai).toString();
        } catch (PackageManager.NameNotFoundException e) {
            appName = packageName;
        }

        AppReport report = new AppReport();
        report.packageName = packageName;
        report.appName = appName;

        PermissionAnalyzer.analyze(this, packageName, report);
        SignatureAnalyzer.analyze(this, packageName, report);
        PackageNameAnalyzer.analyze(packageName, appName, report);
        PermissionAnalyzer.analyze(this, packageName, report);
        SignatureAnalyzer.analyze(this, packageName, report);
        InstallSourceAnalyzer.analyze(this, packageName, report);   // ← ADD THIS
        PackageNameAnalyzer.analyze(packageName, appName, report);
        BackendClient.fetchCommunityData(packageName, appName, report);
        RiskMerger.merge(report);

        Log.d(TAG, "Analysis done. Score=" + report.riskScore + " Verdict=" + report.verdict);
        showResultNotification(report);
    }

    private Notification buildPersistentNotification() {
        return new NotificationCompat.Builder(this, MainApplication.CHANNEL_ID)
                .setContentTitle("AppSentry is active")
                .setContentText("Monitoring for new app installs...")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setOngoing(true)
                .build();
    }

    private void showResultNotification(AppReport report) {
        String emoji = report.verdict.name().equals("TRUSTED") ? "✅"
                : report.verdict.name().equals("SUSPICIOUS") ? "⚠️" : "🔴";

        Intent tapIntent = new Intent(this, AnalysisActivity.class);
        tapIntent.putExtra("package_name", report.packageName);
        tapIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pi = PendingIntent.getActivity(
                this, report.packageName.hashCode(), tapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notif = new NotificationCompat.Builder(this, MainApplication.CHANNEL_ID)
                .setContentTitle(emoji + " " + report.appName + " — " + report.verdict.name())
                .setContentText("Risk Score: " + report.riskScore + " | Tap for details")
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();

        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) nm.notify(report.packageName.hashCode(), notif);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (installReceiver != null) {
            unregisterReceiver(installReceiver);
        }
        if (executor != null) executor.shutdownNow();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}