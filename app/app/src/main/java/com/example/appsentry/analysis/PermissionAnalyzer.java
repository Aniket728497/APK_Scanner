package com.example.appsentry.analysis;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.util.Log;

import com.example.appsentry.model.AppReport;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PermissionAnalyzer {

    private static final String TAG = "PermissionAnalyzer";

    private static final List<Set<String>> DANGEROUS_COMBOS = Arrays.asList(
            new HashSet<>(Arrays.asList(
                    "android.permission.READ_CONTACTS",
                    "android.permission.READ_SMS",
                    "android.permission.RECORD_AUDIO"
            )),
            new HashSet<>(Arrays.asList(
                    "android.permission.ACCESS_FINE_LOCATION",
                    "android.permission.READ_CONTACTS",
                    "android.permission.CAMERA"
            )),
            new HashSet<>(Arrays.asList(
                    "android.permission.READ_CALL_LOG",
                    "android.permission.PROCESS_OUTGOING_CALLS",
                    "android.permission.READ_SMS"
            ))
    );

    public static void analyze(Context context, String packageName, AppReport report) {
        PackageManager pm = context.getPackageManager();
        List<String> dangerous = new ArrayList<>();
        List<String> combosFound = new ArrayList<>();

        try {
            // Use GET_PERMISSIONS flag — works on all API 26+
            PackageInfo pi = pm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS);

            if (pi.requestedPermissions == null || pi.requestedPermissions.length == 0) {
                Log.d(TAG, packageName + " requests no permissions");
                report.dangerousPermissions = dangerous;
                report.flaggedCombos = combosFound;
                return;
            }

            Set<String> allRequested = new HashSet<>(Arrays.asList(pi.requestedPermissions));
            Log.d(TAG, "Total permissions requested: " + allRequested.size());

            for (String perm : pi.requestedPermissions) {
                try {
                    PermissionInfo permInfo = pm.getPermissionInfo(perm, 0);
                    int protection = permInfo.protectionLevel & PermissionInfo.PROTECTION_MASK_BASE;
                    if (protection == PermissionInfo.PROTECTION_DANGEROUS) {
                        dangerous.add(perm);
                        Log.d(TAG, "Dangerous permission: " + perm);
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    // Custom permission — skip
                }
            }

            // Check for dangerous combos
            for (Set<String> combo : DANGEROUS_COMBOS) {
                if (allRequested.containsAll(combo)) {
                    combosFound.add("Spyware pattern: " + formatCombo(combo));
                    Log.d(TAG, "Dangerous combo found: " + combo);
                }
            }

        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Package not found: " + packageName);
        } catch (Exception e) {
            Log.e(TAG, "PermissionAnalyzer failed", e);
        }

        report.dangerousPermissions = dangerous;
        report.flaggedCombos = combosFound;
    }

    private static String formatCombo(Set<String> combo) {
        StringBuilder sb = new StringBuilder();
        for (String p : combo) {
            sb.append(p.replace("android.permission.", "")).append(" + ");
        }
        return sb.substring(0, sb.length() - 3);
    }
}