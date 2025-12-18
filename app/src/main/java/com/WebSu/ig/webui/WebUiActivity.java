package com.WebSu.ig.webui;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import android.view.View;
import android.net.Uri;
import android.webkit.WebResourceError;
import android.graphics.Bitmap;

import com.WebSu.ig.webui.interfaces.KsuWebInterface;
import com.WebSu.ig.Dirname.WebSuLoader;
import com.WebSu.ig.websuCard.PluginWebuiManager;
import com.WebSu.ig.ShellExecutor;
import com.WebSu.ig.webui.AppIconUtil;
import com.WebSu.ig.webui.interfaces.EverythingsWebInterface;
import com.WebSu.ig.R;
import com.WebSu.ig.webui.logLoadWebUI;
import com.WebSu.ig.websuCard.StatusBarManagerActivity;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.util.Locale;
import java.util.HashMap;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;

public class WebUiActivity extends StatusBarManagerActivity {

        private static final String TAG = "WebUiActivity";

        private static final String PREFS_NAME = "settings";
        private static final String KEY_DEVELOPER_OPTIONS = "enable_developer_options";
        private static final String KEY_ENABLE_API_JS = "enable_api_js";

        private static final String WS_SCHEME = "ws";
        private static final String AX_SCHEME = "ax";
        private static final String CUSTOM_DOMAIN = "websuig.local";
        private static final String KERNEL_JS_DOMAIN = "kernelsu.js";
        private static final String PACKAGE_ICON_DOMAIN = "package.icon";
        private static final String CDN_KERNEL_JS_PATH = "/npm/kernelsu@1.0.6/+esm";
        private static final String CDN_JS_DOMAIN = "cdn.jsdelivr.net";
        private static final String CDN_WEBUI_JS_PATH = "/npm/webuix@0.0.8%20/+esm";
        private static final String KERNELSU_MUI_DOMAIN = "mui.kernelsu.org"; 

        private WebView webView;
        private String moduleId;
        private String modulePath;

        private File moduleWebRoot;
        private WebSuLoader webSuLoader;
        private KsuWebInterface ksuWebInterface;
        private EverythingsWebInterface EverythingsShim;

        private boolean developerOptionsEnabled = false;
        private boolean apiJsEnabled = false;

        private final WebSuLoader.wsPathHandler kernelJsHandler = new WebSuLoader.wsPathHandler() {
                @Override
                public WebResourceResponse handle(WebView view, WebResourceRequest request) {
                        final Uri url = request.getUrl();
                        final boolean isWsImport = WS_SCHEME.equals(url.getScheme()) && KERNEL_JS_DOMAIN.equals(url.getHost());

                        if (!isWsImport && !apiJsEnabled) {
                                logLoadWebUI.writeLog(view.getContext(), "ERROR", "Permintaan kernelsu.js (" + url.toString() + ") ditolak: API JS dinonaktifkan (hanya ws:// yang diizinkan).");
                                return createErrorResponse(403, "Forbidden", "API JS dinonaktifkan oleh pengaturan.");
                            }

                        final InputStream is = getAssetInputStream("js/kernelsu.js");

                        if (is != null) {
                                final String logMsg = (isWsImport && !apiJsEnabled) ?
                                    "kernelsu.js dimuat (ws:// pengecualian)." :
                                    "kernelsu.js dimuat (API diaktifkan).";

                                logLoadWebUI.writeLog(view.getContext(), "SUCCESS", logMsg + " Meng-override request: " + url.toString());
                                return new WebResourceResponse("application/javascript", "utf-8", is);
                            }

                        logLoadWebUI.writeLog(view.getContext(), "ERROR", "kernelsu.js tidak ditemukan di aset.");
                        return createErrorResponse(404, "Not Found", "kernelsu.js");
                    }
            };

        private InputStream getAssetInputStream(final String assetPath) {
                try {
                        return getAssets().open(assetPath);
                    } catch (IOException e) {
                        Log.e(TAG, "Gagal memuat aset internal: " + assetPath, e);
                        logLoadWebUI.writeLog(this, "ASSET_ERROR", "Gagal memuat aset internal: " + assetPath + ": " + e.getMessage());
                        return null;
                    }
            }

        private String erudaConsole() {
                InputStream is = null;
                BufferedReader reader = null;
                try {
                        is = getAssets().open("js/eruda.min.js");
                        reader = new BufferedReader(new InputStreamReader(is));

                        final StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                                sb.append(line).append('\n');
                            }
                        sb.append("eruda.init();\n");
                        return sb.toString();
                    } catch (IOException e) {
                        Log.e(TAG, "Gagal memuat eruda.min.js dari aset", e);
                        logLoadWebUI.writeLog(this, "ASSET_ERROR", "Gagal memuat eruda.min.js: " + e.getMessage());
                        return "";
                    } finally {
                        if (reader != null) {
                                try { reader.close(); } catch (IOException ignored) {}
                            }
                        if (is != null) {
                                try { is.close(); } catch (IOException ignored) {}
                            }
                    }
            }

        private WebResourceResponse createErrorResponse(final int statusCode, final String reasonPhrase, final String path) {
                InputStream errorStream = null;
                try {
                        final String safePath = (path != null) ? path : "Unknown";
                        final String errorMsg = "<html><body><h1>" + statusCode + " "
                            + reasonPhrase + "</h1><p>File: "
                            + safePath + " tidak dapat dibaca/ditemukan.</p></body></html>";

                        errorStream = new ByteArrayInputStream(errorMsg.getBytes("UTF-8"));

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                return new WebResourceResponse("text/html", "utf-8", statusCode, reasonPhrase, null, errorStream);
                            } else {
                                return new WebResourceResponse("text/html", "utf-8", errorStream);
                            }
                    } catch (Exception e) {
                        Log.e(TAG, "Error generating error response", e);
                        if (errorStream != null) {
                                try { errorStream.close(); } catch (IOException ignored) {}
                            }
                        return null;
                    }
            }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);

                final SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                developerOptionsEnabled = prefs.getBoolean(KEY_DEVELOPER_OPTIONS, false);
                apiJsEnabled = prefs.getBoolean(KEY_ENABLE_API_JS, false);

                if (developerOptionsEnabled) {
                        WebView.setWebContentsDebuggingEnabled(true);
                        Log.d(TAG, "WebContentsDebuggingEnabled: true");
                        logLoadWebUI.writeLog(this, "DEV_MODE", "Developer options diaktifkan. Web debugging diaktifkan.");
                    } else {
                        WebView.setWebContentsDebuggingEnabled(false);
                        Log.d(TAG, "WebContentsDebuggingEnabled: false");
                    }

                getModuleData();

                if (modulePath == null) {
                        logLoadWebUI.writeLog(this, "ERROR", "Path modul TIDAK ditemukan.");
                        Toast.makeText(this, "Error: Path modul tidak ditemukan.", Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }

                logLoadWebUI.writeLog(this, "INFO", "Memuat modul WebUI dari path: " + modulePath);
                moduleWebRoot = new File(modulePath, "webroot");

                final String indexFilePath = moduleWebRoot.getAbsolutePath() + File.separator + "index.html";
                final String cmd = String.format(Locale.US,
                                                 "if [ -f \"%s\" ]; then echo 1; else echo 0; fi",
                                                 indexFilePath);

                final ShellExecutor.Result shellResult = ShellExecutor.execSync(cmd, this);

                if (shellResult.exitCode != 0 || !shellResult.stdout.trim().equals("1")) {
                        final String msg = "FATAL: File index.html TIDAK DITEMUKAN di path: " + indexFilePath;
                        Log.e(TAG, msg);
                        logLoadWebUI.writeLog(this, "WEBUI_ERROR", "Gagal memuat WebUI. index.html hilang/unreadable.");
                        webView = new WebView(this);
                        webView.loadData("<html><body><h1>WebUI Error</h1><p>" + msg + "</p></body></html>", "text/html", "utf-8");
                        setContentView(webView);
                        return;
                    } else {
                        logLoadWebUI.writeLog(this, "WEBUI_READY", "BERHASIL: File index.html ditemukan valid. Memulai WebView...");
                    }

                setupWebSuLoader();
                setContentView(R.layout.webui);
                webView = findViewById(R.id.webui_view);

                if (webView != null) {
                        webView.setVisibility(View.VISIBLE);
                    } else {
                        logLoadWebUI.writeLog(this, "FATAL_ERROR", "webView tidak ditemukan dari layout webui.xml!");
                        Toast.makeText(this, "Error: WebView tidak ditemukan di layout.", Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }

                final Handler mainHandler = new Handler(Looper.getMainLooper());
                EverythingsShim = new EverythingsWebInterface(this, webView, mainHandler);

                setupWebView();
                loadModuleWebUi();
            }

        private void setupWebSuLoader() {
                WebSuLoader.Builder builder = new WebSuLoader.Builder();
                final Context appContext = getApplicationContext();

                logLoadWebUI.writeLog(this, "INFO", "Mengatur WebSuLoader...");

                // Handler untuk skema ws://plugin.local/
                builder = builder.addScheme(WS_SCHEME)
                    .addDomain(CUSTOM_DOMAIN)
                    .addPathHandler("/", new wsPathHandler(this, moduleWebRoot))
                    .done();

                // 1. Handler untuk skema kustom lokal: ws://kernelsu.js
                builder = builder.addScheme(WS_SCHEME)
                    .addDomain(KERNEL_JS_DOMAIN)
                    .addPathHandler("/", kernelJsHandler)
                    .done();

                // 2. Handler untuk skema kustom lokal: ax://kernelsu.js (Mendukung skema AxManager)
                builder = builder.addScheme(AX_SCHEME)
                    .addDomain(KERNEL_JS_DOMAIN)
                    .addPathHandler("/", kernelJsHandler)
                    .done();

                // Menambahkan domain https kustom (mui.kernelsu.org) yang memetakan ke webroot plugin.
                builder = builder.addScheme("https")
                    .addDomain(KERNELSU_MUI_DOMAIN) 
                    .addPathHandler("/", new wsPathHandler(this, moduleWebRoot)) 
                    .done();

                // 3. Handler untuk CDN: https://cdn.jsdelivr.net/npm/kernelsu@1.0.6/+esm
                builder = builder.addScheme("https")
                    .addDomain(CDN_JS_DOMAIN)
                    .addPathHandler(CDN_KERNEL_JS_PATH, kernelJsHandler)
                    .done();

                // 3. Handler untuk CDN: https://cdn.jsdelivr.net/npm/webuix@0.0.8%20/+esm
                builder = builder.addScheme("https")
                    .addDomain(CDN_JS_DOMAIN)
                    .addPathHandler(CDN_WEBUI_JS_PATH, kernelJsHandler)
                    .done();

                builder = builder.addScheme(WS_SCHEME)
                    .addDomain(PACKAGE_ICON_DOMAIN)
                    .addPathHandler("/", new WebSuLoader.wsPathHandler() {
                            @Override
                            public WebResourceResponse handle(WebView view, WebResourceRequest request) {
                                    final String path = request.getUrl().getPath();
                                    final String packageName = path.startsWith("/") ? path.substring(1) : path;
                                    final long startTime = System.currentTimeMillis();

                                    if (packageName.isEmpty()) {
                                            logLoadWebUI.writeLog(view.getContext(), "ERROR", "Permintaan ikon paket: Nama paket kosong.");
                                            return createErrorResponse(400, "Bad Request", "Missing package name");
                                        }

                                    final int sizePx = (int) (48 * appContext.getResources().getDisplayMetrics().density);

                                    try {
                                            final Bitmap bitmap = AppIconUtil.loadAppIconSync(packageName, sizePx);
                                            final long endTime = System.currentTimeMillis();
                                            final long duration = endTime - startTime;

                                            if (bitmap == null) {
                                                    Log.w(TAG, String.format("Icon not found for package: %s (Time: %d ms)", packageName, duration));
                                                    logLoadWebUI.writeLog(view.getContext(), "WARNING", String.format("Ikon tidak ditemukan untuk paket: %s. Waktu pemuatan: %d ms.", packageName, duration));
                                                    return createErrorResponse(404, "Not Found", packageName);
                                                }

                                            logLoadWebUI.writeLog(view.getContext(), "SUCCESS", String.format("Ikon paket dimuat untuk: %s. Waktu pemuatan: %d ms.", packageName, duration));
                                            return AppIconUtil.bitmapToWebResponse(bitmap);

                                        } catch (Throwable e) {
                                            Log.e(TAG, "Failed to load icon via handler for " + packageName, e);
                                            logLoadWebUI.writeLog(view.getContext(), "EXCEPTION", "Gagal memuat ikon paket " + packageName + ": " + e.getMessage());
                                            return createErrorResponse(500, "Internal Server Error", packageName);
                                        }
                                }
                        })
                    .done();

                webSuLoader = builder.build();
            }

        @SuppressLint("SetJavaScriptEnabled")
        private void getModuleData() {
                moduleId = getIntent().getStringExtra(PluginWebuiManager.EXTRA_MODULE_ID);
                modulePath = getIntent().getStringExtra(PluginWebuiManager.EXTRA_MODULE_PATH);
                logLoadWebUI.writeLog(this, "INFO", "Data Modul diterima. ID: " + moduleId + ", Path: " + modulePath);
            }

        @SuppressLint({"AddJavascriptInterface", "SetWebContentsDebuggingEnabled"})
        private void setupWebView() {
                final WebSettings settings = webView.getSettings();

                settings.setJavaScriptEnabled(true);
                settings.setDomStorageEnabled(true);
                settings.setAllowFileAccess(true);
                settings.setAllowContentAccess(true);
                settings.setAllowUniversalAccessFromFileURLs(true);
                settings.setAllowFileAccessFromFileURLs(true);
                settings.setDefaultTextEncodingName("utf-8");
                settings.setMediaPlaybackRequiresUserGesture(false);

                logLoadWebUI.writeLog(this, "INFO", "Pengaturan WebView dasar dikonfigurasi.");

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
                    }

                ksuWebInterface = new KsuWebInterface(this, webView, moduleWebRoot);
                webView.addJavascriptInterface(ksuWebInterface, "ksu");
                logLoadWebUI.writeLog(this, "INFO", "KsuWebInterface (ksu) ditambahkan ke WebView.");

                webView.addJavascriptInterface(EverythingsShim, "JSBridge");
                logLoadWebUI.writeLog(this, "INFO", "EverythingsWebInterface (JSBridge) ditambahkan ke WebView.");

                webView.setWebViewClient(new WebViewClient() {
                            @Override
                            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                                    final Uri url = request.getUrl();
                                    WebResourceResponse resp = webSuLoader.shouldInterceptRequest(view, request);

                                    if (resp != null) {
                                            if (url.getPath().endsWith(".js") && !"application/javascript".equals(resp.getMimeType())) {
                                                    Log.w(TAG, "MIME type for JS file is incorrect: " + resp.getMimeType() + " for " + url.toString());
                                                    logLoadWebUI.writeLog(WebUiActivity.this, "WARNING", "MIME type JS salah: " + resp.getMimeType() + " untuk " + url.toString());
                                                }

                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                                    Map<String, String> headers = resp.getResponseHeaders();
                                                    if (headers == null) headers = new HashMap<String, String>();

                                                    headers.put("Access-Control-Allow-Origin", "*");
                                                    headers.put("Access-Control-Allow-Methods", "GET, POST, OPTIONS");

                                                    resp.setResponseHeaders(headers);
                                                }

                                            logLoadWebUI.writeLog(WebUiActivity.this, "DEBUG", "Request ditangani oleh internal loader: " + request.getUrl().toString());
                                            return resp;
                                        }

                                    logLoadWebUI.writeLog(WebUiActivity.this, "DEBUG", "Request diteruskan ke default handler: " + request.getUrl().toString());
                                    return super.shouldInterceptRequest(view, request);
                                }

                            @Override
                            public void onPageFinished(WebView view, String url) {
                                    super.onPageFinished(view, url);
                                    logLoadWebUI.writeLog(WebUiActivity.this, "INFO", "Pemuatan halaman selesai: " + url);

                                    if (apiJsEnabled) {
                                            EverythingsShim.injectEverythings();
                                            Log.d(TAG, "API JS Injected: EverythingsWebInterface methods called.");
                                            logLoadWebUI.writeLog(WebUiActivity.this, "INFO", "KEY_ENABLE_API_JS: true. Semua JS API disuntikkan.");
                                        } else {
                                            Log.d(TAG, "API JS Injected: Skipped.");
                                            logLoadWebUI.writeLog(WebUiActivity.this, "INFO", "KEY_ENABLE_API_JS: false. Injeksi JS API dilewati.");
                                        }

                                    if (developerOptionsEnabled) {
                                            final String erudaScriptAndInit = erudaConsole();
                                            if (!erudaScriptAndInit.isEmpty()) {
                                                    view.evaluateJavascript(erudaScriptAndInit, null);
                                                    Log.d(TAG, "Eruda injected and initialized.");
                                                    logLoadWebUI.writeLog(WebUiActivity.this, "DEV_MODE", "Eruda Console berhasil disuntikkan.");
                                                }
                                        }
                                }

                            @Override
                            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                                    return false;
                                }

                            @Override
                            @SuppressWarnings("deprecation")
                            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                                            Log.e(TAG, String.format("Web View Load Error (Deprecated) (%d): %s for URL: %s", errorCode, description, failingUrl));
                                            logLoadWebUI.writeLog(WebUiActivity.this, "WEBVIEW_ERROR_OLD",
                                                                  String.format("Gagal memuat URL (Deprecated) (%d): %s -> %s", errorCode, failingUrl, description));
                                        }
                                    super.onReceivedError(view, errorCode, description, failingUrl);
                                }

                            @TargetApi(Build.VERSION_CODES.M)
                            @Override
                            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                            final String url = request.getUrl().toString();
                                            if (error.getErrorCode() != -1) {
                                                    logLoadWebUI.writeLog(WebUiActivity.this, "WEBVIEW_ERROR",
                                                                          "Error " + error.getErrorCode() + ": " + error.getDescription() + " -> " + url);
                                                }
                                        }
                                    super.onReceivedError(view, request, error);
                                }

                            @Override
                            public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                            final String url = request.getUrl().toString();
                                            final int status = errorResponse.getStatusCode();
                                            final String reason = errorResponse.getReasonPhrase();
                                            Log.e(TAG, String.format("Web View HTTP Error (%d %s): URL: %s", status, reason, url));
                                            logLoadWebUI.writeLog(WebUiActivity.this, "WEBVIEW_HTTP_ERROR",
                                                                  String.format("Gagal memuat HTTP (%d %s): %s", status, reason, url));
                                        }
                                    super.onReceivedHttpError(view, request, errorResponse);
                                }
                        });
            }

        private void loadModuleWebUi() {
                webView.clearCache(true);
                final String moduleUrl = WS_SCHEME + "://" + CUSTOM_DOMAIN + "/index.html";
                webView.loadUrl(moduleUrl);
                logLoadWebUI.writeLog(this, "INFO", "Memuat WebUI dipicu: " + moduleUrl);
            }

        @Override
        public void onBackPressed() {
                if (webView != null && webView.canGoBack()) {
                        logLoadWebUI.writeLog(this, "NAVIGATION", "Navigasi mundur di WebView.");
                        webView.goBack();
                    } else {
                        logLoadWebUI.writeLog(this, "NAVIGATION", "Keluar dari WebUiActivity.");
                        super.onBackPressed();
                    }
            }
    }

