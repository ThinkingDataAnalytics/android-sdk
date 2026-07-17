/*
 * Copyright (C) 2022 ThinkingData
 */
package cn.thinkingdata.analytics;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Collects latency / resource samples from instrumented tests and writes
 * JSON + HTML reports to public Downloads (survives test APK uninstall).
 *
 * <p>Device path: {@code /sdcard/Download/thinkinganalytics-perf/}
 *
 * <p>Optional pull to host:
 * <pre>
 *   adb pull /sdcard/Download/thinkinganalytics-perf/ ./thinkingsdk/build/reports/perf/
 * </pre>
 */
public final class PerfReportStore {

    private static final String TAG = "TDApiPerfReport";
    private static final String PUBLIC_DIR_NAME = "thinkinganalytics-perf";
    private static final String JSON_NAME = "td-api-perf.json";
    private static final String HTML_NAME = "td-api-perf.html";

    private static final List<JSONObject> LATENCY = new ArrayList<>();
    private static final List<JSONObject> RESOURCE = new ArrayList<>();
    private static final Object LOCK = new Object();

    private PerfReportStore() {
    }

    public static void recordLatency(String name, double avgMs, double p50Ms, double maxMs,
                                     long limitMs, double[] samples, boolean passed) {
        try {
            JSONObject row = new JSONObject();
            row.put("type", "latency");
            row.put("name", name);
            row.put("avgMs", round3(avgMs));
            row.put("p50Ms", round3(p50Ms));
            row.put("maxMs", round3(maxMs));
            row.put("limitMs", limitMs);
            row.put("passed", passed);
            JSONArray arr = new JSONArray();
            if (samples != null) {
                for (double s : samples) {
                    arr.put(round3(s));
                }
            }
            row.put("samplesMs", arr);
            synchronized (LOCK) {
                LATENCY.add(row);
            }
        } catch (Exception e) {
            Log.w(TAG, "recordLatency failed: " + e.getMessage());
        }
    }

    public static void recordLatencyOnce(String name, double actualMs, long limitMs, boolean passed) {
        recordLatency(name, actualMs, actualMs, actualMs, limitMs, new double[]{actualMs}, passed);
    }

    public static void recordResource(String name, String metric, long value, long limit,
                                      String unit, boolean passed, JSONObject extras) {
        try {
            JSONObject row = new JSONObject();
            row.put("type", "resource");
            row.put("name", name);
            row.put("metric", metric);
            row.put("value", value);
            row.put("limit", limit);
            row.put("unit", unit);
            row.put("passed", passed);
            if (extras != null) {
                row.put("extras", extras);
            }
            synchronized (LOCK) {
                RESOURCE.add(row);
            }
        } catch (Exception e) {
            Log.w(TAG, "recordResource failed: " + e.getMessage());
        }
    }

    /**
     * Write accumulated rows to device storage. Safe to call from multiple {@code @AfterClass}.
     * Prefers public Downloads so files remain after the test APK is uninstalled.
     *
     * @return primary directory containing the report files, or null on failure
     */
    public static File flush(Context context) {
        if (context == null) {
            return null;
        }
        try {
            JSONArray latency;
            JSONArray resource;
            synchronized (LOCK) {
                if (LATENCY.isEmpty() && RESOURCE.isEmpty()) {
                    Log.i(TAG, "No perf samples to flush");
                    return null;
                }
                latency = new JSONArray();
                for (JSONObject o : LATENCY) {
                    latency.put(o);
                }
                resource = new JSONArray();
                for (JSONObject o : RESOURCE) {
                    resource.put(o);
                }
            }

            JSONObject root = new JSONObject();
            root.put("generatedAt", isoNow());
            root.put("device", Build.MANUFACTURER + " " + Build.MODEL);
            root.put("sdkInt", Build.VERSION.SDK_INT);
            root.put("packageName", context.getPackageName());
            root.put("latency", latency);
            root.put("resource", resource);
            root.put("latencyPassCount", countPassed(latency));
            root.put("latencyFailCount", latency.length() - countPassed(latency));
            root.put("resourcePassCount", countPassed(resource));
            root.put("resourceFailCount", resource.length() - countPassed(resource));

            String jsonText = root.toString(2);
            String htmlText = renderHtml(root);

            File publicDir = writeToPublicDownloads(context, jsonText, htmlText);
            File appDir = writeToAppFiles(context, jsonText, htmlText);

            File primary = publicDir != null ? publicDir : appDir;
            if (primary == null) {
                Log.e(TAG, "flush failed: no writable location");
                return null;
            }

            Log.i(TAG, "Perf report written under: " + primary.getAbsolutePath());
            Log.i(TAG, "Optional: adb pull /sdcard/Download/thinkinganalytics-perf/");
            // Keep samples so a later @AfterClass (e.g. resource after latency) can rewrite a full report.
            return primary;
        } catch (Exception e) {
            Log.e(TAG, "flush failed", e);
            return null;
        }
    }

    /**
     * Public Downloads path that survives test APK uninstall.
     * Device: {@code /sdcard/Download/thinkinganalytics-perf/}
     */
    private static File writeToPublicDownloads(Context context, String jsonText, String htmlText) {
        File dir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS), PUBLIC_DIR_NAME);
        try {
            if (!dir.exists() && !dir.mkdirs()) {
                Log.w(TAG, "mkdirs failed for " + dir);
            }
            // Remove stale reports (including MediaStore "name (1).json" renames).
            deleteLocalReportFiles(dir);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                deleteMediaStoreReports(context);
            }
            if (dir.exists() && dir.canWrite()) {
                writeText(new File(dir, JSON_NAME), jsonText);
                writeText(new File(dir, HTML_NAME), htmlText);
                // Verify overwrite succeeded (some OEMs block rewriting foreign-owned files).
                File jsonFile = new File(dir, JSON_NAME);
                if (jsonFile.isFile() && jsonFile.length() == jsonText.getBytes("UTF-8").length) {
                    Log.i(TAG, "Wrote public file reports: " + dir.getAbsolutePath());
                    return dir;
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "direct Downloads write failed: " + e.getMessage());
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                deleteMediaStoreReports(context);
                writeViaMediaStore(context, JSON_NAME, "application/json", jsonText);
                writeViaMediaStore(context, HTML_NAME, "text/html", htmlText);
                Log.i(TAG, "Wrote MediaStore Downloads/" + PUBLIC_DIR_NAME);
                return dir;
            } catch (Exception e) {
                Log.w(TAG, "MediaStore write failed: " + e.getMessage());
            }
        }
        return null;
    }

    private static void deleteLocalReportFiles(File dir) {
        if (dir == null || !dir.isDirectory()) {
            return;
        }
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File f : files) {
            String n = f.getName();
            if (n.startsWith("td-api-perf") && (n.endsWith(".json") || n.endsWith(".html"))) {
                //noinspection ResultOfMethodCallIgnored
                f.delete();
            }
        }
    }

    private static void deleteMediaStoreReports(Context context) {
        try {
            Uri collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI;
            // Match canonical and OEM-renamed names: "td-api-perf.json", "td-api-perf (1).json"
            context.getContentResolver().delete(collection,
                    MediaStore.MediaColumns.DISPLAY_NAME + " LIKE ?",
                    new String[]{"td-api-perf%"});
        } catch (Exception e) {
            Log.w(TAG, "MediaStore cleanup failed: " + e.getMessage());
        }
    }

    private static void writeViaMediaStore(Context context, String displayName,
                                           String mime, String text) throws Exception {
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, displayName);
        values.put(MediaStore.MediaColumns.MIME_TYPE, mime);
        values.put(MediaStore.MediaColumns.RELATIVE_PATH,
                Environment.DIRECTORY_DOWNLOADS + "/" + PUBLIC_DIR_NAME);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.MediaColumns.IS_PENDING, 1);
        }
        Uri collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI;
        Uri uri = context.getContentResolver().insert(collection, values);
        if (uri == null) {
            throw new IllegalStateException("MediaStore insert returned null for " + displayName);
        }
        OutputStream os = context.getContentResolver().openOutputStream(uri);
        if (os == null) {
            throw new IllegalStateException("Cannot open MediaStore stream for " + displayName);
        }
        try {
            OutputStreamWriter writer = new OutputStreamWriter(os, Charset.forName("UTF-8"));
            writer.write(text);
            writer.flush();
            writer.close();
        } finally {
            os.close();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear();
            values.put(MediaStore.MediaColumns.IS_PENDING, 0);
            context.getContentResolver().update(uri, values, null, null);
        }
    }

    private static File writeToAppFiles(Context context, String jsonText, String htmlText) {
        try {
            File dir = context.getExternalFilesDir(PUBLIC_DIR_NAME);
            if (dir == null) {
                dir = new File(context.getFilesDir(), PUBLIC_DIR_NAME);
            }
            if (!dir.exists() && !dir.mkdirs()) {
                Log.w(TAG, "Cannot create app perf dir: " + dir);
                return null;
            }
            writeText(new File(dir, JSON_NAME), jsonText);
            writeText(new File(dir, HTML_NAME), htmlText);
            Log.i(TAG, "Wrote app-private reports: " + dir.getAbsolutePath());
            return dir;
        } catch (Exception e) {
            Log.w(TAG, "app-files write failed: " + e.getMessage());
            return null;
        }
    }

    public static void clear() {
        synchronized (LOCK) {
            LATENCY.clear();
            RESOURCE.clear();
        }
    }

    private static int countPassed(JSONArray arr) {
        int n = 0;
        for (int i = 0; i < arr.length(); i++) {
            if (arr.optJSONObject(i) != null && arr.optJSONObject(i).optBoolean("passed")) {
                n++;
            }
        }
        return n;
    }

    private static String isoNow() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(new Date());
    }

    private static void writeText(File file, String text) throws Exception {
        FileOutputStream fos = new FileOutputStream(file);
        OutputStreamWriter writer = new OutputStreamWriter(fos, Charset.forName("UTF-8"));
        writer.write(text);
        writer.close();
        fos.close();
    }

    private static String renderHtml(JSONObject root) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset=\"utf-8\"/>");
        sb.append("<title>ThinkingAnalytics API Perf Report</title>");
        sb.append("<style>");
        sb.append("body{font-family:-apple-system,BlinkMacSystemFont,Segoe UI,sans-serif;");
        sb.append("margin:24px;color:#111;line-height:1.45} table{border-collapse:collapse;width:100%;");
        sb.append("margin:16px 0} th,td{border:1px solid #ddd;padding:8px;text-align:left;");
        sb.append("vertical-align:top} th{background:#f5f5f5}");
        sb.append(".pass{color:#0a7} .fail{color:#c00;font-weight:600}");
        sb.append("h1{margin-bottom:4px} h2{margin-top:28px}");
        sb.append(".meta{color:#666;margin-bottom:16px} .warn{color:#a60;background:#fff8e8;");
        sb.append("padding:10px 12px;border-radius:6px} .cards{display:flex;flex-wrap:wrap;gap:12px;");
        sb.append("margin:12px 0 20px} .card{border:1px solid #e5e5e5;border-radius:8px;");
        sb.append("padding:12px 16px;min-width:140px;background:#fafafa}");
        sb.append(".card .v{font-size:22px;font-weight:600} .card .l{color:#666;font-size:12px}");
        sb.append("code{font-size:12px;word-break:break-all}");
        sb.append("</style></head><body>");
        sb.append("<h1>ThinkingAnalytics API Perf Report</h1>");
        sb.append("<div class=\"meta\">");
        sb.append("generatedAt=").append(esc(root.optString("generatedAt"))).append("<br/>");
        sb.append("device=").append(esc(root.optString("device"))).append("<br/>");
        sb.append("sdkInt=").append(root.optInt("sdkInt")).append("<br/>");
        sb.append("package=").append(esc(root.optString("packageName")));
        sb.append("</div>");

        JSONArray latency = root.optJSONArray("latency");
        JSONArray resource = root.optJSONArray("resource");
        int latencyN = latency == null ? 0 : latency.length();
        int resourceN = resource == null ? 0 : resource.length();

        sb.append("<div class=\"cards\">");
        appendCard(sb, "Latency APIs", String.valueOf(latencyN),
                root.optInt("latencyPassCount") + " pass / "
                        + root.optInt("latencyFailCount") + " fail");
        appendCard(sb, "Resource metrics", String.valueOf(resourceN),
                root.optInt("resourcePassCount") + " pass / "
                        + root.optInt("resourceFailCount") + " fail");
        if (latencyN > 0) {
            JSONObject slowest = findSlowest(latency);
            if (slowest != null) {
                appendCard(sb, "Slowest API",
                        fmtMs(slowest.optDouble("avgMs")) + " ms",
                        slowest.optString("name"));
            }
        }
        sb.append("</div>");

        sb.append("<h2>Latency (call return)</h2>");
        if (latencyN == 0) {
            sb.append("<p class=\"warn\">暂无 latency 数据。请运行 <code>FacadeApiLatencyTest</code>。</p>");
        } else {
            sb.append("<p>pass=").append(root.optInt("latencyPassCount"))
                    .append(" fail=").append(root.optInt("latencyFailCount"))
                    .append(" · 单位 ms，保留三位小数（亚毫秒可见）；按 avg 从高到低排序</p>");
            sb.append("<table><tr><th>API</th><th>avg(ms)</th><th>p50(ms)</th><th>max(ms)</th>");
            sb.append("<th>limit(ms)</th><th>samples(ms)</th><th>result</th></tr>");
            List<JSONObject> rows = new ArrayList<>();
            for (int i = 0; i < latency.length(); i++) {
                rows.add(latency.getJSONObject(i));
            }
            java.util.Collections.sort(rows, new java.util.Comparator<JSONObject>() {
                @Override
                public int compare(JSONObject a, JSONObject b) {
                    return Double.compare(b.optDouble("avgMs"), a.optDouble("avgMs"));
                }
            });
            for (JSONObject row : rows) {
                boolean ok = row.optBoolean("passed");
                sb.append("<tr><td>").append(esc(row.optString("name"))).append("</td>");
                sb.append("<td>").append(fmtMs(row.optDouble("avgMs"))).append("</td>");
                sb.append("<td>").append(fmtMs(row.optDouble("p50Ms"))).append("</td>");
                sb.append("<td>").append(fmtMs(row.optDouble("maxMs"))).append("</td>");
                sb.append("<td>").append(row.optLong("limitMs")).append("</td>");
                sb.append("<td><code>").append(esc(fmtSamples(row.optJSONArray("samplesMs"))))
                        .append("</code></td>");
                sb.append("<td class=\"").append(ok ? "pass" : "fail").append("\">")
                        .append(ok ? "PASS" : "FAIL").append("</td></tr>");
            }
            sb.append("</table>");
        }

        sb.append("<h2>Resource (memory / CPU)</h2>");
        if (resourceN == 0) {
            sb.append("<p class=\"warn\">暂无 resource 数据（heap / PSS / CPU）。");
            sb.append("仅跑 Latency 不会写入本节。请同进程再跑 <code>FacadeResourceMetricsTest</code>。</p>");
        } else {
            sb.append("<p>pass=").append(root.optInt("resourcePassCount"))
                    .append(" fail=").append(root.optInt("resourceFailCount"))
                    .append(" · 仪器化进程相对门禁，非 Profiler / LeakCanary</p>");
            sb.append("<table><tr><th>Case</th><th>metric</th><th>value</th><th>limit</th>");
            sb.append("<th>unit</th><th>details</th><th>result</th></tr>");
            for (int i = 0; i < resource.length(); i++) {
                JSONObject row = resource.getJSONObject(i);
                boolean ok = row.optBoolean("passed");
                String unit = row.optString("unit");
                sb.append("<tr><td>").append(esc(row.optString("name"))).append("</td>");
                sb.append("<td>").append(esc(row.optString("metric"))).append("</td>");
                sb.append("<td>").append(fmtMetric(row.optLong("value"), unit)).append("</td>");
                sb.append("<td>").append(fmtMetric(row.optLong("limit"), unit)).append("</td>");
                sb.append("<td>").append(esc(unit)).append("</td>");
                sb.append("<td><code>").append(esc(fmtExtras(row.optJSONObject("extras"))))
                        .append("</code></td>");
                sb.append("<td class=\"").append(ok ? "pass" : "fail").append("\">")
                        .append(ok ? "PASS" : "FAIL").append("</td></tr>");
            }
            sb.append("</table>");
        }

        sb.append("<h2>How to regenerate</h2>");
        sb.append("<pre>");
        sb.append("./gradlew :thinkingsdk:connectedDebugAndroidTest \\\n");
        sb.append("  -Pandroid.testInstrumentationRunnerArguments.class=\\\n");
        sb.append("cn.thinkingdata.analytics.FacadeApiLatencyTest,\\\n");
        sb.append("cn.thinkingdata.analytics.FacadeResourceMetricsTest\n");
        sb.append("# report on device:\n");
        sb.append("# /sdcard/Download/thinkinganalytics-perf/\n");
        sb.append("adb pull /sdcard/Download/thinkinganalytics-perf/ ./thinkingsdk/build/reports/perf/");
        sb.append("</pre>");
        sb.append("<p class=\"meta\">Note: latency is call-site return time; resource metrics are ");
        sb.append("instrumentation-process relative gates, not Profiler/LeakCanary proofs.</p>");
        sb.append("</body></html>");
        return sb.toString();
    }

    private static void appendCard(StringBuilder sb, String label, String value, String sub) {
        sb.append("<div class=\"card\"><div class=\"l\">").append(esc(label)).append("</div>");
        sb.append("<div class=\"v\">").append(esc(value)).append("</div>");
        sb.append("<div class=\"l\">").append(esc(sub)).append("</div></div>");
    }

    private static JSONObject findSlowest(JSONArray latency) {
        JSONObject best = null;
        double max = -1;
        for (int i = 0; i < latency.length(); i++) {
            JSONObject row = latency.optJSONObject(i);
            if (row == null) {
                continue;
            }
            double avg = row.optDouble("avgMs");
            if (avg > max) {
                max = avg;
                best = row;
            }
        }
        return best;
    }

    private static String fmtMetric(long value, String unit) {
        if ("KB".equalsIgnoreCase(unit)) {
            return value + " KB (" + String.format(Locale.US, "%.2f", value / 1024.0) + " MB)";
        }
        return String.valueOf(value);
    }

    private static String fmtExtras(JSONObject extras) {
        if (extras == null || extras.length() == 0) {
            return "-";
        }
        return extras.toString();
    }

    private static double round3(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }

    private static String fmtMs(double ms) {
        return String.format(Locale.US, "%.3f", ms);
    }

    private static String fmtSamples(JSONArray samples) {
        if (samples == null || samples.length() == 0) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < samples.length(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(fmtMs(samples.optDouble(i)));
        }
        sb.append("]");
        return sb.toString();
    }

    private static String esc(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
