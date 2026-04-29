package com.example.appsentry.analysis;

import com.example.appsentry.model.AppReport;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class PackageNameAnalyzer {

    // Known legitimate packages that are commonly impersonated
    private static final List<String> KNOWN_LEGIT = Arrays.asList(
            "com.whatsapp", "com.google.android.gm", "com.facebook.katana",
            "com.instagram.android", "com.snapchat.android", "com.twitter.android",
            "com.netflix.mediaclient", "com.spotify.music", "com.amazon.mShop.android.shopping"
    );

    private static final Pattern GIBBERISH = Pattern.compile(
            ".*[a-z]{8,}[0-9]{3,}.*|.*[0-9]{4,}.*"
    );

    public static void analyze(String packageName, String appName, AppReport report) {
        report.packageNameSuspicious = false;
        report.suspicionReason = null;

        // Check impersonation
        for (String legit : KNOWN_LEGIT) {
            if (!packageName.equals(legit) && isSimilar(packageName, legit)) {
                report.packageNameSuspicious = true;
                report.suspicionReason = "Possible impersonation of " + legit;
                return;
            }
        }

        // Gibberish/random-looking name
        String[] parts = packageName.split("\\.");
        for (String part : parts) {
            if (GIBBERISH.matcher(part).matches()) {
                report.packageNameSuspicious = true;
                report.suspicionReason = "Package name looks randomly generated";
                return;
            }
        }

        // Very short segments
        if (parts.length < 2) {
            report.packageNameSuspicious = true;
            report.suspicionReason = "Unusual package name structure";
        }
    }

    // Simple edit-distance based similarity
    private static boolean isSimilar(String a, String b) {
        if (Math.abs(a.length() - b.length()) > 5) return false;
        int dist = levenshtein(a, b);
        return dist > 0 && dist <= 3;
    }

    private static int levenshtein(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= b.length(); j++) dp[0][j] = j;
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                dp[i][j] = a.charAt(i-1) == b.charAt(j-1) ? dp[i-1][j-1] :
                        1 + Math.min(dp[i-1][j-1], Math.min(dp[i-1][j], dp[i][j-1]));
            }
        }
        return dp[a.length()][b.length()];
    }
}