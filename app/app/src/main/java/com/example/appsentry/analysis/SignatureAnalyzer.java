package com.example.appsentry.analysis;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.example.appsentry.model.AppReport;

import java.security.MessageDigest;

public class SignatureAnalyzer {

    private static final String TAG = "SignatureAnalyzer";

    // SHA256 of Android's default debug keystore cert
    private static final String DEBUG_CERT_SHA256 =
            "a40da80a59d170caa950cf15c18c454d47a39b26989d8b640ecd745ba71bf5dc";

    @SuppressWarnings("deprecation")
    public static void analyze(Context context, String packageName, AppReport report) {
        PackageManager pm = context.getPackageManager();
        report.isDebugSigned = false;

        try {
            Signature[] sigs;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // API 28+
                PackageInfo pi = pm.getPackageInfo(packageName,
                        PackageManager.GET_SIGNING_CERTIFICATES);
                if (pi.signingInfo == null) return;
                sigs = pi.signingInfo.getApkContentsSigners();
            } else {
                // API 26–27
                PackageInfo pi = pm.getPackageInfo(packageName,
                        PackageManager.GET_SIGNATURES);
                sigs = pi.signatures;
            }

            if (sigs == null || sigs.length == 0) return;

            String sha256 = getSha256(sigs[0].toByteArray());
            Log.d(TAG, packageName + " cert SHA256: " + sha256);

            if (DEBUG_CERT_SHA256.equalsIgnoreCase(sha256)) {
                report.isDebugSigned = true;
                Log.d(TAG, "DEBUG SIGNED detected!");
            }

        } catch (Exception e) {
            Log.e(TAG, "SignatureAnalyzer failed", e);
        }
    }

    private static String getSha256(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(data);
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }
}