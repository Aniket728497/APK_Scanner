package com.example.appsentry.model;

import java.util.List;

public class AppReport {
    public String packageName;
    public String appName;
    public int riskScore;           // 0–100
    public RiskVerdict verdict;
    public String explanation;

    // Local signals
    public List<String> dangerousPermissions;
    public List<String> flaggedCombos;
    public String installSource;
    public boolean isDebugSigned;
    public boolean packageNameSuspicious;
    public String suspicionReason;

    // Community signals
    public float communityScore;    // 0–100 from backend
    public List<String> communityFlags;
    public float playStoreRating;
    public long installCount;
    public boolean communityAvailable;
}