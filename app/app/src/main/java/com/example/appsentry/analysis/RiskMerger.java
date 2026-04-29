package com.example.appsentry.analysis;

import com.example.appsentry.model.AppReport;
import com.example.appsentry.model.RiskVerdict;

public class RiskMerger {

    public static void merge(AppReport report) {
        float score = 0;

        // 40% — Dangerous permission combos
        float permScore = 0;
        if (report.flaggedCombos != null && !report.flaggedCombos.isEmpty()) {
            permScore = Math.min(100, report.flaggedCombos.size() * 50f);
        } else if (report.dangerousPermissions != null) {
            permScore = Math.min(100, report.dangerousPermissions.size() * 8f);
        }
        score += permScore * 0.40f;

        // 20% — Install source
        float sourceScore = 50f; // default unknown
        if (report.installSource != null) {
            if (report.installSource.equals("PLAY_STORE"))         sourceScore = 5f;
            else if (report.installSource.equals("AMAZON_STORE"))  sourceScore = 15f;
            else if (report.installSource.equals("SAMSUNG_STORE")) sourceScore = 15f;
            else if (report.installSource.equals("ADB"))           sourceScore = 60f;
            else if (report.installSource.equals("SIDELOAD"))      sourceScore = 70f;
            else                                                    sourceScore = 80f;
        }
        score += sourceScore * 0.20f;

        // 25% — Community sentiment
        float commScore = 50f;
        if (report.communityAvailable) {
            commScore = 100f - report.communityScore;
        }
        score += commScore * 0.25f;

        // 10% — Package name suspicion
        float nameScore = report.packageNameSuspicious ? 80f : 0f;
        score += nameScore * 0.10f;

        // 5% — Signature anomaly
        float sigScore = report.isDebugSigned ? 90f : 0f;
        score += sigScore * 0.05f;

        int finalScore = Math.round(score);

        // Override rules
        if (report.flaggedCombos != null && !report.flaggedCombos.isEmpty()) {
            finalScore = Math.max(finalScore, 61);
        }
        if (report.packageNameSuspicious && report.suspicionReason != null
                && report.suspicionReason.toLowerCase().contains("impersonation")) {
            finalScore = Math.max(finalScore, 50);
        }
        if (report.communityFlags != null) {
            for (String flag : report.communityFlags) {
                if (flag.toLowerCase().contains("malware")) {
                    finalScore = Math.max(finalScore, 75);
                    break;
                }
            }
        }

        finalScore = Math.min(100, Math.max(0, finalScore));
        report.riskScore = finalScore;

        if (finalScore <= 30)       report.verdict = RiskVerdict.TRUSTED;
        else if (finalScore <= 60)  report.verdict = RiskVerdict.SUSPICIOUS;
        else                        report.verdict = RiskVerdict.DANGEROUS;
    }
}