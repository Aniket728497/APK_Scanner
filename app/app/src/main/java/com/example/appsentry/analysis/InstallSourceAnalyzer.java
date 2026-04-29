package com.example.appsentry.analysis;

import android.content.Context;
import android.content.pm.InstallSourceInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import com.example.appsentry.model.AppReport;

public class InstallSourceAnalyzer {

    private static final String TAG = "InstallSourceAnalyzer";

    public static void analyze(Context context, String packageName, AppReport report) {
        PackageManager pm = context.getPackageManager();

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // API 30+ — precise install source
                InstallSourceInfo info = pm.getInstallSourceInfo(packageName);
                String installer = info.getInstallingPackageName();
                report.installSource = mapInstaller(installer);
            } else {
                // API 26–29 — deprecated but works
                @SuppressWarnings("deprecation")
                String installer = pm.getInstallerPackageName(packageName);
                report.installSource = mapInstaller(installer);
            }
        } catch (Exception e) {
            Log.e(TAG, "InstallSourceAnalyzer failed", e);
            report.installSource = "UNKNOWN";
        }

        Log.d(TAG, packageName + " install source: " + report.installSource);
    }

    private static String mapInstaller(String installer) {
        if (installer == null) return "SIDELOAD";
        switch (installer) {
            case "com.android.vending":
            case "com.google.android.feedback":
                return "PLAY_STORE";
            case "com.amazon.venezia":
                return "AMAZON_STORE";
            case "com.sec.android.app.samsungapps":
                return "SAMSUNG_STORE";
            case "com.android.packageinstaller":
            case "com.google.android.packageinstaller":
                return "SIDELOAD";
            case "com.android.adb":
                return "ADB";
            default:
                if (installer.contains("adb")) return "ADB";
                return "UNKNOWN (" + installer + ")";
        }
    }
}