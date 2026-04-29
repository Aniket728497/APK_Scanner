package com.example.appsentry.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.appsentry.R;
import com.example.appsentry.model.AppReport;

public class PermissionDetailFragment extends Fragment {

    private static final String KEY_PERMISSIONS = "permissions";
    private static final String KEY_COMBOS = "combos";

    public static PermissionDetailFragment newInstance(AppReport report) {
        PermissionDetailFragment f = new PermissionDetailFragment();
        Bundle args = new Bundle();
        args.putStringArrayList(KEY_PERMISSIONS,
                new java.util.ArrayList<>(report.dangerousPermissions));
        args.putStringArrayList(KEY_COMBOS,
                new java.util.ArrayList<>(report.flaggedCombos));
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_permission_detail, container, false);
        TextView tv = v.findViewById(R.id.tvPermissionList);

        Bundle args = getArguments();
        StringBuilder sb = new StringBuilder();

        sb.append("🔐 Dangerous Permissions:\n");
        if (args != null) {
            for (String p : args.getStringArrayList(KEY_PERMISSIONS)) {
                sb.append("  • ").append(p.replace("android.permission.", "")).append("\n");
            }
            sb.append("\n⚠️ Flagged Combos:\n");
            for (String c : args.getStringArrayList(KEY_COMBOS)) {
                sb.append("  • ").append(c).append("\n");
            }
        }

        tv.setText(sb.toString().trim());
        return v;
    }
}