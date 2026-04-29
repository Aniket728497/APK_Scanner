package com.example.appsentry.network;

import android.util.Log;

import com.example.appsentry.model.AppReport;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class BackendClient {

    private static final String TAG = "BackendClient";
    // Change this to your backend IP when running Flask locally
    private static final String BASE_URL = "http://172.17.62.78:5000/analyze";

    public static void fetchCommunityData(String packageName, String appName, AppReport report) {
        try {
            String url = BASE_URL
                    + "?package=" + URLEncoder.encode(packageName, "UTF-8")
                    + "&name=" + URLEncoder.encode(appName, "UTF-8");

            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(10000);
            conn.setRequestMethod("GET");

            int code = conn.getResponseCode();
            if (code != 200) {
                report.communityAvailable = false;
                return;
            }

            BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            br.close();

            JSONObject json = new JSONObject(sb.toString());
            report.communityScore = (float) json.optDouble("community_score", 50.0);
            report.playStoreRating = (float) json.optDouble("play_store_rating", 0);
            report.installCount = json.optLong("install_count", 0);

            List<String> flags = new ArrayList<>();
            JSONArray flagArr = json.optJSONArray("flags");
            if (flagArr != null) {
                for (int i = 0; i < flagArr.length(); i++) {
                    flags.add(flagArr.getString(i));
                }
            }
            report.communityFlags = flags;
            report.communityAvailable = true;

        } catch (Exception e) {
            Log.e(TAG, "Backend fetch failed", e);
            report.communityAvailable = false;
        }
    }
}