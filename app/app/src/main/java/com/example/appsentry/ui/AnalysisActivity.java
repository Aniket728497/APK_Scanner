package com.example.appsentry.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.appsentry.R;
import com.example.appsentry.analysis.InstallSourceAnalyzer;
import com.example.appsentry.analysis.PackageNameAnalyzer;
import com.example.appsentry.analysis.PermissionAnalyzer;
import com.example.appsentry.analysis.RiskMerger;
import com.example.appsentry.analysis.SignatureAnalyzer;
import com.example.appsentry.model.AppReport;
import com.example.appsentry.network.BackendClient;
import com.example.appsentry.service.AnalysisService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AnalysisActivity extends AppCompatActivity {

    private TextView tvAppName, tvPackageName, tvVerdict, tvScore;
    private TextView tvInstallInfo, tvPermissions, tvCombos;
    private TextView tvSignature, tvCommunity, tvPackageCheck;
    private Button btnUninstall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analysis);

        bindViews();
        requestNotificationPermission();
        startMonitoringService();
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        String packageName = null;

        // Safely extract package_name from intent
        if (intent != null && intent.getExtras() != null) {
            packageName = intent.getStringExtra("package_name");
        }

        if (packageName == null || packageName.isEmpty()) {
            showIdleScreen();
        } else {
            startAnalysis(packageName);
        }
    }

    private void startAnalysis(String packageName) {
        // Show loading state immediately
        tvAppName.setText("Analyzing...");
        tvPackageName.setText(packageName);
        tvVerdict.setText("⏳ Please wait");
        tvScore.setText("Collecting data...");
        tvInstallInfo.setText("Checking install source...");
        tvPermissions.setText("Scanning permissions...");
        tvCombos.setText("Checking combinations...");
        tvSignature.setText("Verifying signature...");
        tvCommunity.setText("Fetching community data...");
        tvPackageCheck.setText("Checking package name...");
        btnUninstall.setVisibility(View.GONE);

        // Run full analysis in background
        ExecutorService ex = Executors.newSingleThreadExecutor();
        ex.execute(() -> {
            AppReport report = buildReport(packageName);
            runOnUiThread(() -> displayReport(report));
        });
    }

    private AppReport buildReport(String packageName) {
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
        InstallSourceAnalyzer.analyze(this, packageName, report);
        PackageNameAnalyzer.analyze(packageName, appName, report);
        BackendClient.fetchCommunityData(packageName, appName, report);
        RiskMerger.merge(report);

        return report;
    }

    private void displayReport(AppReport report) {

        // Header
        tvAppName.setText(report.appName);
        tvPackageName.setText(report.packageName);

        // Verdict banner with color
        switch (report.verdict) {
            case TRUSTED:
                tvVerdict.setText("✅  TRUSTED");
                tvVerdict.setBackgroundColor(0xFF1B5E20);
                break;
            case SUSPICIOUS:
                tvVerdict.setText("⚠️  SUSPICIOUS");
                tvVerdict.setBackgroundColor(0xFF7F6000);
                break;
            case DANGEROUS:
                tvVerdict.setText("🔴  DANGEROUS");
                tvVerdict.setBackgroundColor(0xFF7F0000);
                break;
        }

        // Score
        tvScore.setText("Risk Score: " + report.riskScore + " / 100");

        // Install source
        String source = (report.installSource != null) ? report.installSource : "Unknown";
        tvInstallInfo.setText("Install source: " + source);

        // Dangerous permissions
        if (report.dangerousPermissions == null || report.dangerousPermissions.isEmpty()) {
            tvPermissions.setText("✅ No dangerous permissions requested");
        } else {
            StringBuilder sb = new StringBuilder();
            for (String p : report.dangerousPermissions) {
                sb.append("• ").append(p.replace("android.permission.", "")).append("\n");
            }
            tvPermissions.setText(sb.toString().trim());
        }

        // Flagged combos
        if (report.flaggedCombos == null || report.flaggedCombos.isEmpty()) {
            tvCombos.setText("✅ No suspicious combinations found");
        } else {
            StringBuilder sb = new StringBuilder();
            for (String c : report.flaggedCombos) {
                sb.append("⚠️ ").append(c).append("\n");
            }
            tvCombos.setText(sb.toString().trim());
        }

        // Signature
        tvSignature.setText(report.isDebugSigned
                ? "⚠️ Debug-signed APK — not a release build"
                : "✅ Properly signed release APK");

        // Community data
        if (!report.communityAvailable) {
            tvCommunity.setText("ℹ️ No community data available\nBackend may be offline or app not found online");
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("Trust Score: ").append((int) report.communityScore).append(" / 100\n");
            sb.append("Play Store Rating: ").append(
                    report.playStoreRating > 0 ? report.playStoreRating + " ⭐" : "Not found"
            ).append("\n");
            if (report.installCount > 0) {
                sb.append("Installs: ").append(formatInstalls(report.installCount)).append("\n");
            }
            if (report.communityFlags != null && !report.communityFlags.isEmpty()) {
                sb.append("\n🚩 Flags:\n");
                for (String f : report.communityFlags) {
                    sb.append("  • ").append(f).append("\n");
                }
            } else {
                sb.append("✅ No community flags raised");
            }
            tvCommunity.setText(sb.toString().trim());
        }

        // Package name check
        tvPackageCheck.setText(report.packageNameSuspicious
                ? "⚠️ " + report.suspicionReason
                : "✅ Package name looks legitimate");

        // Uninstall button
        btnUninstall.setVisibility(View.VISIBLE);
        btnUninstall.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DELETE);
            intent.setData(Uri.parse("package:" + report.packageName));
            startActivity(intent);
        });
    }

    private void showIdleScreen() {
        tvAppName.setText("AppSentry");
        tvPackageName.setText("Active protection enabled");
        tvVerdict.setText("🛡️  Monitoring Active");
        tvVerdict.setBackgroundColor(0xFF1A237E);
        tvScore.setText("Install any app to trigger analysis");
        tvInstallInfo.setText("AppSentry will automatically analyze\nany newly installed app.");
        tvPermissions.setText("—");
        tvCombos.setText("—");
        tvSignature.setText("—");
        tvCommunity.setText("—");
        tvPackageCheck.setText("—");
        btnUninstall.setVisibility(View.GONE);
    }

    private void bindViews() {
        tvAppName      = findViewById(R.id.tvAppName);
        tvPackageName  = findViewById(R.id.tvPackageName);
        tvVerdict      = findViewById(R.id.tvVerdict);
        tvScore        = findViewById(R.id.tvScore);
        tvInstallInfo  = findViewById(R.id.tvInstallInfo);
        tvPermissions  = findViewById(R.id.tvPermissions);
        tvCombos       = findViewById(R.id.tvCombos);
        tvSignature    = findViewById(R.id.tvSignature);
        tvCommunity    = findViewById(R.id.tvCommunity);
        tvPackageCheck = findViewById(R.id.tvPackageCheck);
        btnUninstall   = findViewById(R.id.btnUninstall);
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1001);
            }
        }
    }

    private void startMonitoringService() {
        Intent serviceIntent = new Intent(this, AnalysisService.class);
        startForegroundService(serviceIntent);
    }

    private String formatInstalls(long count) {
        if (count >= 1_000_000) return (count / 1_000_000) + "M+";
        if (count >= 1_000)     return (count / 1_000) + "K+";
        return count + "+";
    }
}