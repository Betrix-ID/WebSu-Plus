package com.WebSu.ig.webui.interfaces;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.Window;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.Toast;
import android.util.TypedValue;

import com.WebSu.ig.ShellExecutor;
import com.WebSu.ig.ShellExecutor.Result;
import com.WebSu.ig.webui.logLoadWebUI;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KsuWebInterface {

        private static final String TAG = "KsuWebInterface";

        // Pola Regex untuk mendeteksi perintah 'echo' yang menggunakan kutip tunggal.
        private static final Pattern ECHO_SINGLE_QUOTE_PATTERN = 
        Pattern.compile("^\\s*echo\\s+'(.+?)'\\s*$", Pattern.DOTALL);

        private static final String GLOBAL_SHELL_SBIN_PATH = "/data/user_de/0/com.android.shell/WebSu//system/sbin";

        private final Context context;
        private final WebView webView;
        private final File modDir;
        private final Map<String, String> packageIconCache = Collections.synchronizedMap(new HashMap<String, String>());
        private final Handler mainHandler = new Handler(Looper.getMainLooper());
        private static final int PADDING_VIEW_INDEX = 0;

        public KsuWebInterface(Context context, WebView webView, File modDir) {
                this.context = context;
                this.webView = webView;

                String modDirPath = modDir.getAbsolutePath();
                if (modDirPath.endsWith(File.separator + "webroot")) {
                        modDirPath = modDirPath.substring(0, modDirPath.length() - (File.separator + "webroot").length());
                    }
                this.modDir = new File(modDirPath);

                logLoadWebUI.writeLog(context, "INFO", "KsuWebInterface initialized. Module Dir: " + this.modDir.getAbsolutePath());
            }

        @JavascriptInterface
        public String getPLUGINBIN() {
                return GLOBAL_SHELL_SBIN_PATH;
            }

        private String replacePlaceholders(String cmd) {
                if (cmd == null) return cmd;

                final String modPath = modDir.getAbsolutePath();
                String result = cmd;
                result = result.replaceAll("(?i)DIRNAME", modPath);
                result = result.replaceAll("(?i)MODPATH", modPath);
                return result;
            }

        private String sanitizeComplexCommand(String cmd) {
                String trimmed = cmd.trim();
                if (trimmed.startsWith("$(") && trimmed.endsWith(")")) {
                        String sanitized = trimmed.substring(2, trimmed.length() - 1).trim();
                        Log.d(TAG, "Sanitizing command: Removed leading $() from: " + cmd + " -> " + sanitized);
                        return sanitized;
                    }
                return cmd;
            }

        private String replaceSingleQuotesWithDoubleQuotesForEvaluation(String cmd) {
                Matcher matcher = ECHO_SINGLE_QUOTE_PATTERN.matcher(cmd);

                if (matcher.matches()) {
                        // Ambil konten di dalam kutip tunggal
                        String content = matcher.group(1);

                        // Buat ulang perintah menggunakan kutip ganda (memungkinkan $() dievaluasi)
                        // Pastikan kutip ganda di dalam konten di-escape.
                        String finalCmd = "echo \"" + content.replace("\"", "\\\"") + "\"";
                        Log.d(TAG, "Adjusted command for $() evaluation: " + cmd + " -> " + finalCmd);
                        return finalCmd;
                    }
                return cmd;
            }


        @JavascriptInterface
        public String exec(String cmd) {
                try {
                        final String rawCommand = sanitizeComplexCommand(cmd);
                        // Perintah rawCommand akan diproses di buildFinalCommand
                        final String finalCommand = buildFinalCommand(rawCommand, null);

                        final Result result = ShellExecutor.execSync(finalCommand, context);

                        if (result.exitCode == 0) {
                                return result.stdout;
                            } else {
                                final JSONObject jsonError = new JSONObject();
                                jsonError.put("exitCode", result.exitCode);
                                jsonError.put("stdout", result.stdout);
                                jsonError.put("stderr", result.stderr);

                                Log.e(TAG, "exec failed (sync): " + finalCommand + " -> " + jsonError.toString());
                                return jsonError.toString();
                            }

                    } catch (Throwable t) {
                        final StringWriter sw = new StringWriter();
                        t.printStackTrace(new PrintWriter(sw));

                        try {
                                final JSONObject jsonError = new JSONObject();
                                jsonError.put("exitCode", -999);
                                jsonError.put("stdout", "");
                                jsonError.put("stderr", "Execution Error: " + sw.toString());
                                return jsonError.toString();
                            } catch (JSONException ignored) {
                                return "{\"exitCode\":-999,\"stdout\":\"\",\"stderr\":\"Internal JSON Error\"}";
                            }
                    }
            }

        @JavascriptInterface
        public void exec(final String cmd, final String callbackFunc) {
                exec(cmd, null, callbackFunc);
            }

        private String buildFinalCommand(String cmd, String options) throws JSONException {

                // 1. APLIKASIKAN PERBAIKAN KUTIP TUNGGAL KE GANDA
                String adjustedCmd = replaceSingleQuotesWithDoubleQuotesForEvaluation(cmd);

                // 2. APLIKASIKAN PLACEHOLDERS (DIRNAME/MODPATH)
                final String processedCmdWithPlaceholders = replacePlaceholders(adjustedCmd);

                final StringBuilder sb = new StringBuilder();
                final JSONObject opts = options == null ? new JSONObject() : new JSONObject(options);

                // 3. TAMBAHKAN CWD
                final String cwd = opts.optString("cwd");
                if (!TextUtils.isEmpty(cwd)) {
                        sb.append("cd ").append(cwd).append(" && ");
                    }

                // 4. TAMBAHKAN PLUGIN PATH
                final String pluginPath = getPLUGINBIN();
                sb.append("[ -d ").append(pluginPath).append(" ] && [ -n \"$(ls -A ").append(pluginPath).append(" 2>/dev/null)\" ] && export PATH=").append(pluginPath).append(":$PATH; ");

                // 5. TAMBAHKAN ENVIRONMENT VARIABLES
                final JSONObject env = opts.optJSONObject("env");
                if (env != null) {
                        for (Iterator<String> it = env.keys(); it.hasNext(); ) {
                                final String key = it.next();
                                sb.append("export ").append(key).append("=").append(JSONObject.quote(env.getString(key))).append("; ");
                            }
                    }

                // 6. TAMBAHKAN PERINTAH UTAMA YANG SUDAH DIPROSES
                sb.append(processedCmdWithPlaceholders);

                return sb.toString().trim();
            }

        @JavascriptInterface
        public void exec(
            final String cmd,
            final String options,
            final String callbackFunc
        ) {
                new Thread(new Runnable() {
                            @Override
                            public void run() {
                                    String finalCommand;
                                    try {
                                            final String rawCommand = sanitizeComplexCommand(cmd);
                                            finalCommand = buildFinalCommand(rawCommand, options);
                                        } catch (JSONException e) {
                                            finalCommand = "echo 'Error processing JSON options: " + e.getMessage() + "' >&2; exit 1;";
                                            Log.e(TAG, "JSON error in exec async options", e);
                                        }


                                    final Result result = ShellExecutor.execSync(finalCommand, context);

                                    // Output dikumpulkan/dikompilasi menjadi string tunggal (seperti logika di Kotlin)
                                    final String outQuoted = JSONObject.quote(result.stdout);
                                    final String errQuoted = JSONObject.quote(result.stderr);

                                    // Callback JS
                                    final String jsCode =
                                        "javascript: (function() { try { " + callbackFunc + "(" + result.exitCode + ", " + outQuoted + ", " + errQuoted + "); } catch(e) { console.error('Callback error in exec: ' + e); } })();";

                                    webView.post(new Runnable() {
                                                @Override
                                                public void run() {
                                                        webView.loadUrl(jsCode);
                                                    }
                                            });
                                }
                        }).start();
            }

        @JavascriptInterface
        public void spawn(final String command, final String args, final String options, final String callbackFunc) {
                new Thread(new Runnable() {
                            @Override
                            public void run() {
                                    Process p = null;
                                    StreamGobbler stdoutGobbler = null;
                                    StreamGobbler stderrGobbler = null;

                                    String finalCommand = "";
                                    int exitCode = -1;

                                    try {
                                            final StringBuilder rawCmdBuilder = new StringBuilder(command);
                                            if (!TextUtils.isEmpty(args)) {
                                                    final JSONArray argsArray = new JSONArray(args);
                                                    for (int i = 0; i < argsArray.length(); i++) {
                                                            rawCmdBuilder.append(" ").append(argsArray.getString(i));
                                                        }
                                                }
                                            final String rawCmd = rawCmdBuilder.toString();
                                            final String sanitizedCmd = sanitizeComplexCommand(rawCmd);

                                            finalCommand = buildFinalCommand(sanitizedCmd, options);

                                            p = ShellExecutor.execProcess(finalCommand, context);

                                            stdoutGobbler = new StreamGobbler(p.getInputStream(), "stdout", callbackFunc, webView);
                                            stderrGobbler = new StreamGobbler(p.getErrorStream(), "stderr", callbackFunc, webView);

                                            stdoutGobbler.start();
                                            stderrGobbler.start();

                                            exitCode = p.waitFor();

                                            try { stdoutGobbler.join(); } catch (InterruptedException ignored) {}
                                            try { stderrGobbler.join(); } catch (InterruptedException ignored) {}

                                            final int finalExitCode = exitCode;
                                            final String exitJsCode =
                                                "javascript: (function() { try { " + callbackFunc + ".emit('exit', " + finalExitCode + "); } catch(e) { console.error('emitExit error: ' + e); } })();";
                                            webView.post(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                                webView.loadUrl(exitJsCode);
                                                            }
                                                    });

                                            if (finalExitCode != 0) {
                                                    final String errMsg = "Process exited with code " + finalExitCode;
                                                    final String errJsCode =
                                                        "javascript: (function() { try { var err = new Error(); err.exitCode = " + finalExitCode + "; err.message = " +
                                                        JSONObject.quote(errMsg) +
                                                        ";" + callbackFunc + ".emit('error', err); } catch(e) { console.error('emitErr error: ' + e); } })();";
                                                    webView.post(new Runnable() {
                                                                @Override
                                                                public void run() {
                                                                        webView.loadUrl(errJsCode);
                                                                    }
                                                            });
                                                }

                                        } catch (Throwable t) {
                                            final String errorMsg = "Failed to spawn process: " + t.getMessage();
                                            Log.e(TAG, "Spawn error for command: " + finalCommand, t);

                                            // Emit fatal 'error'
                                            final String errJsCode =
                                                "javascript: (function() { try { var err = new Error(); err.exitCode = -1; err.message = " +
                                                JSONObject.quote(errorMsg) +
                                                ";" + callbackFunc + ".emit('error', err); } catch(e) { console.error('emitFatalErr error: ' + e); } })();";
                                            webView.post(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                                webView.loadUrl(errJsCode);
                                                            }
                                                    });
                                        } finally {
                                            if (p != null) p.destroy();
                                        }
                                }
                        }).start();
            }

        private static class StreamGobbler extends Thread {
                private final InputStream is;
                private final String type;
                private final String callbackFunc;
                private final WebView webView;

                public StreamGobbler(InputStream is, String type, String callbackFunc, WebView webView) {
                        this.is = is;
                        this.type = type;
                        this.callbackFunc = callbackFunc;
                        this.webView = webView;
                    }

                @Override
                public void run() {
                        try {
                                final InputStreamReader isr = new InputStreamReader(is);
                                final BufferedReader br = new BufferedReader(isr);
                                String line = null;

                                while ((line = br.readLine()) != null) {
                                        final String dataQuoted = JSONObject.quote(line + "\n");
                                        final String jsCode =
                                            "javascript: (function() { try { " + callbackFunc + "." + type + ".emit('data', " + dataQuoted + "); } catch(e) { console.error('emitData error: ' + e); } })();";
                                        webView.post(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                            webView.loadUrl(jsCode);
                                                        }
                                                });
                                    }
                            } catch (IOException ioe) {
                                Log.e(TAG, type + " stream error: " + ioe.getMessage());
                            }
                    }
            }

        @JavascriptInterface
        public void toast(final String msg) {
                webView.post(new Runnable() {
                            @Override
                            public void run() {
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();

                                }
                        });
            }

        @JavascriptInterface
        public void fullScreen(final boolean enable) {
                if (context instanceof Activity) {
                        final Activity activity = (Activity) context;
                        mainHandler.post(new Runnable() {
                                    @Override
                                    public void run() {

                                            final Window window = activity.getWindow();
                                            final android.view.View decorView = window.getDecorView();
                                            android.view.View paddingView = null;
                                            try {
                                                    android.view.ViewGroup parent = (android.view.ViewGroup) webView.getParent();
                                                    if (parent != null && parent.getChildCount() > PADDING_VIEW_INDEX) {
                                                            paddingView = parent.getChildAt(PADDING_VIEW_INDEX);
                                                        }
                                                } catch (Exception e) {
                                                    Log.e(TAG, "Gagal menemukan View padding di layout parent.", e);
                                                }

                                            int defaultStatusBarColor = 0;
                                            final TypedValue typedValue = new TypedValue();
                                            if (activity.getTheme().resolveAttribute(android.R.attr.windowBackground, typedValue, true)) {
                                                    defaultStatusBarColor = typedValue.data;
                                                }

                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                                    window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                                                }

                                            if (enable) {
                                                    if (paddingView != null) {
                                                            paddingView.setVisibility(android.view.View.GONE);
                                                            Log.d(TAG, "Menyembunyikan View padding.");
                                                        }

                                                    final int flags = android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                                        | android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                                                        | android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                                        | android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                                        | android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                                        | android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;

                                                    decorView.setSystemUiVisibility(flags);
                                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                                            android.view.WindowManager.LayoutParams params = window.getAttributes();
                                                            params.layoutInDisplayCutoutMode =
                                                                android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
                                                            window.setAttributes(params);
                                                        }

                                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                                            window.setStatusBarColor(android.graphics.Color.parseColor("#000000"));
                                                        }

                                                } else {

                                                    decorView.setSystemUiVisibility(android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE | android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
                                                    if (paddingView != null) {
                                                            paddingView.setVisibility(android.view.View.VISIBLE);
                                                            Log.d(TAG, "Menampilkan View padding.");
                                                        }

                                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                                            android.view.WindowManager.LayoutParams params = window.getAttributes();
                                                            params.layoutInDisplayCutoutMode =
                                                                android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT;
                                                            window.setAttributes(params);
                                                        }
                                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                                            window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                                                            window.setStatusBarColor(defaultStatusBarColor);
                                                        }
                                                }

                                            Log.d(TAG, "Fullscreen mode set to: " + enable);
                                            Toast.makeText(activity, "Fullscreen: " + enable, Toast.LENGTH_SHORT).show();
                                        }
                                });
                    }
            }

        @JavascriptInterface
        public String moduleInfo() {
                final JSONObject currentModuleInfo = new JSONObject();
                try {
                        currentModuleInfo.put("moduleDir", modDir.getAbsolutePath());
                        // Menggunakan nama direktori modul sebagai ID
                        currentModuleInfo.put("id", modDir.getName());
                    } catch (JSONException e) {
                        Log.e(TAG, "Error building moduleInfo", e);

                    }
                return currentModuleInfo.toString();
            }

        private List<String> getPackageNames(boolean isSystem) {
                final PackageManager pm = context.getPackageManager();
                final List<PackageInfo> installedPackages = pm.getInstalledPackages(0);
                final List<String> packageNames = new ArrayList<String>();

                for (final PackageInfo pkg : installedPackages) {
                        final ApplicationInfo appInfo = pkg.applicationInfo;
                        if (appInfo != null) {
                                final boolean isPkgSystem = (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
                                if (isSystem == isPkgSystem) {
                                        packageNames.add(pkg.packageName);
                                    }
                            }
                    }

                Collections.sort(packageNames);
                return packageNames;
            }


        @JavascriptInterface
        public String listSystemPackages() {
                final List<String> packageNames = getPackageNames(true);
                return new JSONArray(packageNames).toString();
            }

        @JavascriptInterface
        public String listUserPackages() {
                final List<String> packageNames = getPackageNames(false);
                return new JSONArray(packageNames).toString();
            }

        @JavascriptInterface
        public String listAllPackages() {
                final PackageManager pm = context.getPackageManager();
                final List<PackageInfo> installedPackages = pm.getInstalledPackages(0);
                final List<String> packageNames = new ArrayList<String>();

                for (final PackageInfo pkg : installedPackages) {
                        packageNames.add(pkg.packageName);
                    }
                Collections.sort(packageNames);

                return new JSONArray(packageNames).toString();
            }

        @JavascriptInterface
        public String getPackagesInfo(String packageNamesJson) {

                final PackageManager pm = context.getPackageManager();
                final JSONArray jsonArray = new JSONArray();

                try {
                        final JSONArray packageNames = new JSONArray(packageNamesJson);
                        for (int i = 0; i < packageNames.length(); i++) {
                                final String pkgName = packageNames.getString(i);
                                final JSONObject obj = new JSONObject();
                                obj.put("packageName", pkgName);

                                try {
                                        final PackageInfo pkg = pm.getPackageInfo(pkgName, 0);
                                        final ApplicationInfo appInfo = pkg.applicationInfo;

                                        obj.put("versionName", pkg.versionName != null ? pkg.versionName : "");

                                        long versionCode = 0;
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                                versionCode = pkg.getLongVersionCode();
                                            } else {
                                                versionCode = pkg.versionCode;
                                            }
                                        obj.put("versionCode", versionCode);

                                        if (appInfo != null) {
                                                obj.put("appLabel", pm.getApplicationLabel(appInfo).toString());
                                                obj.put("isSystem", (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0);
                                                obj.put("uid", appInfo.uid);
                                            } else {
                                                obj.put("appLabel", "");
                                                obj.put("isSystem", false);
                                                obj.put("uid", JSONObject.NULL);
                                            }
                                    } catch (Exception e) {
                                        obj.put("error", "Package not found or inaccessible");
                                    }
                                jsonArray.put(obj);
                            }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing packageNamesJson", e);

                    }
                return jsonArray.toString();
            }

        private Bitmap drawableToBitmap(final Drawable drawable, final int size) {
                // Optimalisasi: Jika sudah BitmapDrawable dan ukurannya sesuai, gunakan langsung
                if (drawable instanceof BitmapDrawable) {
                        final Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
                        if (bitmap.getWidth() == size && bitmap.getHeight() == size) {
                                return bitmap;
                            }
                    }

                // Buat bitmap baru
                final Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
                final Canvas canvas = new Canvas(bitmap);
                drawable.setBounds(0, 0, size, size);
                drawable.draw(canvas);
                return bitmap;
            }

        @JavascriptInterface
        public void cacheAllPackageIcons(final int size) {
                // Runnable dijalankan di background thread
                new Thread(new Runnable() {
                            @Override
                            public void run() {
                                    final PackageManager pm = context.getPackageManager();
                                    final List<PackageInfo> packages = pm.getInstalledPackages(0);
                                    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                                    int count = 0;

                                    for (final PackageInfo pkg : packages) {
                                            final String pkgName = pkg.packageName;
                                            // Lompati jika sudah ada di cache
                                            if (packageIconCache.containsKey(pkgName)) continue;

                                            try {
                                                    final ApplicationInfo appInfo = pkg.applicationInfo;
                                                    if (appInfo == null) continue;

                                                    final Drawable drawable = pm.getApplicationIcon(appInfo);
                                                    final Bitmap bitmap = drawableToBitmap(drawable, size);

                                                    outputStream.reset();
                                                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                                                    final byte[] byteArray = outputStream.toByteArray();
                                                    final String iconBase64 = "data:image/png;base64," + Base64.encodeToString(byteArray, Base64.NO_WRAP);

                                                    // Gunakan cache thread-safe
                                                    packageIconCache.put(pkgName, iconBase64);
                                                    count++;
                                                } catch (Exception e) {
                                                    packageIconCache.put(pkgName, "");
                                                    Log.w(TAG, "Failed to cache icon for: " + pkgName, e);
                                                }
                                        }

                                    // Tutup stream
                                    try { outputStream.close(); } catch (IOException ignored) {}

                                    Log.d(TAG, "Finished caching " + count + " package icons.");

                                    // Beri tahu WebView bahwa proses caching selesai
                                    final String jsCode = "javascript: (function() { try { if (typeof ksu !== 'undefined' && typeof ksu.onIconCacheComplete === 'function') { ksu.onIconCacheComplete(); } } catch(e) { console.error('Icon cache completion error: ' + e); } })();";
                                    webView.post(new Runnable() {
                                                @Override
                                                public void run() {
                                                        webView.loadUrl(jsCode);
                                                    }
                                            });
                                }
                        }).start();
            }

        @JavascriptInterface
        public String getPackagesIcons(final String packageNamesJson, final int size) {
                final PackageManager pm = context.getPackageManager();
                final JSONArray jsonArray = new JSONArray();
                ByteArrayOutputStream outputStream = null;

                try {
                        outputStream = new ByteArrayOutputStream();
                        final JSONArray packageNames = new JSONArray(packageNamesJson);

                        for (int i = 0; i < packageNames.length(); i++) {
                                final String pkgName = packageNames.getString(i);
                                final JSONObject obj = new JSONObject();
                                obj.put("packageName", pkgName);

                                String iconBase64 = packageIconCache.get(pkgName);

                                // Cache miss: Lakukan pemuatan sinkron (RISIKO PERFORMA)
                                if (iconBase64 == null) {
                                        try {
                                                final ApplicationInfo appInfo = pm.getApplicationInfo(pkgName, 0);
                                                final Drawable drawable = pm.getApplicationIcon(appInfo);
                                                final Bitmap bitmap = drawableToBitmap(drawable, size);

                                                outputStream.reset();
                                                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                                                final byte[] byteArray = outputStream.toByteArray();
                                                iconBase64 = "data:image/png;base64," + Base64.encodeToString(byteArray, Base64.NO_WRAP);
                                            } catch (Exception e) {
                                                Log.w(TAG, "Synchronous icon load failed for: " + pkgName, e);
                                                iconBase64 = ""; // Kembalikan string kosong jika gagal
                                            }
                                        packageIconCache.put(pkgName, iconBase64);
                                    }

                                obj.put("icon", iconBase64);
                                jsonArray.put(obj);
                            }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing packageNamesJson in getPackagesIcons", e);

                    } finally {
                        if (outputStream != null) {
                                try { outputStream.close(); } catch (IOException ignored) {}
                            }
                    }

                return jsonArray.toString();
            }
    }

