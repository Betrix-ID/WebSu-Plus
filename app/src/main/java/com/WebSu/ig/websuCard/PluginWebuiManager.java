package com.WebSu.ig.websuCard; 

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap; 
import android.graphics.BitmapFactory; 
import android.net.Uri; 
import android.os.Handler; 
import android.os.Looper; 
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView; 
import android.widget.LinearLayout;
import android.widget.RelativeLayout; 
import android.widget.TextView;
import android.widget.Toast;

import android.content.Intent;
import com.WebSu.ig.webui.WebUiActivity;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout; 

import com.WebSu.ig.LogUtils; 
import com.WebSu.ig.R;
import com.WebSu.ig.ShellExecutor;
import com.WebSu.ig.logd; 
import com.WebSu.ig.viewmodel.ZipFilePicker;
import com.WebSu.ig.websuCard.UpdatePlugin;
import com.WebSu.ig.websuCard.ChangelogDialog; 

import org.json.JSONObject; 
import java.io.BufferedReader; 
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader; 
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader; 
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PluginWebuiManager { 

        private static final String TAG = "PluginWebuiManager";
        private static PluginWebuiManager instance;

        public static final String WEBUI_BASE_DIR = "/data/user_de/0/com.android.shell/WebSu/webui/";
        public static final String AXMANAGER_PLUGIN_BASE_DIR = "/data/data/com.android.shell/AxManager/plugins/"; 
        public static final String ADB_MODULES_BASE_DIR = "/data/adb/modules/"; 

        private static final String BANNER_SUB_DIR = "webui_banners";
        private static final String DISABLE_LOCK_FILE = "disabled.lock"; 
        private static final String TEMP_UPDATE_JSON = "_update.json"; 

        public static final String EXTRA_MODULE_ID = "com.WebSu.ig.websuCard.MODULE_ID";
        public static final String EXTRA_MODULE_PATH = "com.WebSu.ig.websuCard.MODULE_PATH";

        private final Context context; 
        private View rootView;

        private LinearLayout moduleListLayout;
        private TextView noWebuiText;
        private LayoutInflater layoutInflater; 

        private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor(); 
        private final Handler mainHandler = new Handler(Looper.getMainLooper()); 
        private List<ModuleData> cachedModuleData = null;
        private OnModuleActionListener moduleActionListener;

        public interface OnModuleActionListener {
                void onDeleteModule(String moduleId, String moduleBasePath);
            }

        public void setModuleActionListener(OnModuleActionListener listener) {
                this.moduleActionListener = listener;
            }

        private static class ModuleData {
                String id;
                String name;
                String version;
                int versionCode; 
                String author;
                String description;
                String moduleBasePath;
                String bannerPath; 
                String updateJsonUrl; 

                boolean needsUpdate = false; 
                String latestVersion = null; 
                String latestZipUrl = null; 
                String latestChangelog = null; 
                String cachedSize = null;
                Bitmap cachedBannerBitmap = null; 
                boolean isEnabled;

                public ModuleData(String id, String name, String version, int versionCode, String author, String description,
                                  String moduleBasePath, String bannerPath, String updateJsonUrl) {
                        this.id = id;
                        this.name = name;
                        this.version = version;
                        this.versionCode = versionCode; 
                        this.author = author;
                        this.description = description;
                        this.moduleBasePath = moduleBasePath;
                        this.bannerPath = bannerPath;
                        this.updateJsonUrl = updateJsonUrl;
                    }
            }

        private PluginWebuiManager(Context context, View webuiRootView) {
                this.context = context;
                this.layoutInflater = LayoutInflater.from(context);
                updateRootView(webuiRootView); 
            }

        public static PluginWebuiManager getInstance(Context context, View webuiRootView) {
                if (instance == null) {
                        instance = new PluginWebuiManager(context, webuiRootView);
                    } else {
                        instance.updateRootView(webuiRootView);
                    }
                return instance;
            }

        private void updateRootView(View webuiRootView) {
                this.rootView = webuiRootView;
                this.moduleListLayout = webuiRootView.findViewById(R.id.module_list_layout);
                this.noWebuiText = webuiRootView.findViewById(R.id.text_no_webui);
                if (cachedModuleData != null) {
                        mainHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                            displayModules(cachedModuleData);
                                        }
                                });
                    }
            }

        public void refreshPlugins() {
                LogUtils.writeLog(context, "INFO", "Refreshing plugins requested.");
                cachedModuleData = null; 
                loadModules();
            }

        public void loadModules() {
                if (cachedModuleData != null) {
                        mainHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                            displayModules(cachedModuleData);
                                            SwipeRefreshLayout srl = rootView.findViewById(R.id.swipe_refresh_webui);
                                            if (srl != null) srl.setRefreshing(false);
                                        }
                                });
                        return;
                    }

                mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                    SwipeRefreshLayout srl = rootView.findViewById(R.id.swipe_refresh_webui);
                                    if (srl != null) srl.setRefreshing(true);
                                    if (moduleListLayout != null) moduleListLayout.removeAllViews();
                                }
                        });

                ioExecutor.execute(new Runnable() {
                            @Override
                            public void run() {
                                    loadInstalledModules();
                                }
                        });
            }

        private void loadInstalledModules() {
                final SwipeRefreshLayout swipeRefreshLayout = rootView.findViewById(R.id.swipe_refresh_webui);
                final List<ModuleData> modulesToDisplay = new ArrayList<ModuleData>();

                final List<String> baseDirs = new ArrayList<String>();
                baseDirs.add(WEBUI_BASE_DIR);
                baseDirs.add(AXMANAGER_PLUGIN_BASE_DIR); 
                baseDirs.add(ADB_MODULES_BASE_DIR);

                boolean foundAnyModule = false;
                LogUtils.writeLog(context, "INFO", "Memulai pemeriksaan multi-direktori basis untuk WebUI/Plugin.");

                for (String baseDir : baseDirs) {
                        if (checkDirectoryExistsInShell(baseDir)) {
                                final String[] moduleNames = getModuleDirsFromShell(baseDir);

                                if (moduleNames != null) {
                                        for (int i = 0; i < moduleNames.length; i++) {
                                                String moduleName = moduleNames[i];
                                                final String fullModulePath = baseDir + moduleName; 
                                                String propPath = fullModulePath + File.separator + "module.prop";

                                                LogUtils.writeLog(context, "INFO", "Modul ditemukan: " + moduleName + " di " + fullModulePath);

                                                if (checkFileExistsInShell(propPath)) {
                                                        foundAnyModule = true; 

                                                        LogUtils.writeLog(context, "INFO", "File module.prop ditemukan untuk " + moduleName + ". Memuat properti...");

                                                        String id = readPropertyFromShell(propPath, "id");
                                                        String vCodeStr = readPropertyFromShell(propPath, "versionCode");
                                                        int vCode = 0;
                                                        if (vCodeStr != null && !vCodeStr.isEmpty()) {
                                                                try { vCode = Integer.parseInt(vCodeStr); } catch (NumberFormatException ignored) {}
                                                            }

                                                        ModuleData newModule = new ModuleData(
                                                            id != null ? id : moduleName,
                                                            readPropertyFromShell(propPath, "name"),
                                                            readPropertyFromShell(propPath, "version"),
                                                            vCode,
                                                            readPropertyFromShell(propPath, "author"),
                                                            readPropertyFromShell(propPath, "description"),
                                                            fullModulePath, 
                                                            readPropertyFromShell(propPath, "banner"),
                                                            readPropertyFromShell(propPath, "updateJson")
                                                        );

                                                        newModule.cachedSize = calculateModuleSizeSync(newModule.moduleBasePath);
                                                        newModule.isEnabled = isModuleEnabled(newModule.moduleBasePath); 

                                                        LogUtils.writeLog(context, "INFO", String.format("Modul berhasil dimuat: ID=%s, Nama=%s, Versi=%s. Enabled=%b", newModule.id, newModule.name, newModule.version, newModule.isEnabled));


                                                        if (isUrl(newModule.bannerPath)) {
                                                                newModule.cachedBannerBitmap = loadBannerFromUrlSync(newModule.bannerPath, newModule.id);
                                                            } else if (newModule.bannerPath != null && !newModule.bannerPath.isEmpty()) {
                                                                newModule.cachedBannerBitmap = loadLocalBannerSync(newModule);
                                                            }
                                                        modulesToDisplay.add(newModule);
                                                    } else {
                                                        LogUtils.writeLog(context, "WARNING", "File module.prop TIDAK ditemukan di: " + propPath);
                                                    }
                                            }
                                    } else {
                                        LogUtils.writeLog(context, "INFO", "Direktori " + baseDir + " kosong atau gagal diakses isinya.");
                                    }
                            } else {
                                LogUtils.writeLog(context, "WARNING", "Direktori basis " + baseDir + " TIDAK ditemukan atau tidak dapat diakses.");
                            }
                    }

                if (!foundAnyModule) {
                        LogUtils.writeLog(context, "INFO", "Tidak ada WebUI/Plugin yang ditemukan di semua direktori basis.");
                        mainHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                            if (noWebuiText != null) noWebuiText.setVisibility(View.VISIBLE);
                                            if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                                        }
                                });
                        return;
                    }

                cachedModuleData = new ArrayList<ModuleData>(modulesToDisplay);

                LogUtils.writeLog(context, "INFO", "Total " + cachedModuleData.size() + " modul berhasil dimuat. Memperbarui UI...");

                mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                    displayModules(cachedModuleData); 
                                    if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                                }
                        });
                checkForUpdates(cachedModuleData);
            }

        private void checkForUpdates(List<ModuleData> modules) {
                for (int i = 0; i < modules.size(); i++) {
                        final ModuleData module = modules.get(i); 
                        if (module.updateJsonUrl != null && isUrl(module.updateJsonUrl)) {
                                ioExecutor.execute(new Runnable() {
                                            @Override
                                            public void run() {
                                                    downloadUpdateJson(module.id, module.updateJsonUrl);
                                                }
                                        });
                            }
                    }
            }

        private void downloadChangelogContent(final ModuleData data) {
                mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                    Toast.makeText(context, "Memuat Changelog...", Toast.LENGTH_SHORT).show();
                                }
                        });

                ioExecutor.execute(new Runnable() {
                            @Override
                            public void run() {
                                    String urlString = data.latestChangelog;
                                    HttpURLConnection connection = null;
                                    InputStream input = null;

                                    try {
                                            URL url = new URL(urlString);
                                            connection = (HttpURLConnection) url.openConnection();
                                            connection.setConnectTimeout(10000);
                                            connection.setReadTimeout(10000);
                                            connection.connect();

                                            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                                                    input = connection.getInputStream();
                                                    BufferedReader reader = new BufferedReader(new InputStreamReader(input));
                                                    StringBuilder sb = new StringBuilder();
                                                    String line;
                                                    while ((line = reader.readLine()) != null) {
                                                            sb.append(line).append("\n");
                                                        }
                                                    reader.close();
                                                    final String changelogContent = sb.toString();

                                                    mainHandler.post(new Runnable() {
                                                                @Override
                                                                public void run() {
                                                                        showChangelogDialogWithContent(data, changelogContent);
                                                                    }
                                                            });

                                                } else {
                                                    throw new IOException("HTTP error code: " + connection.getResponseCode());
                                                }
                                        } catch (IOException e) {
                                            LogUtils.writeLog(context, "ERROR", "Gagal mengunduh konten Changelog: " + e.getMessage());
                                            mainHandler.post(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                                Toast.makeText(context, "Gagal memuat changelog.", Toast.LENGTH_SHORT).show();
                                                            }
                                                    });
                                        } finally {
                                            try { if (input != null) input.close(); } catch (IOException ignored) {}
                                            if (connection != null) connection.disconnect();
                                        }
                                }
                        });
            }

        private void showChangelogDialogWithContent(final ModuleData data, String changelogContent) {
                String fixedDialogTitle = "Changelog";
                final ChangelogDialog changelogDialog = new ChangelogDialog(context, fixedDialogTitle, changelogContent);

                changelogDialog.setInstallButtonListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                    handleUpdateConfirmation(data);
                                }
                        });

                changelogDialog.setDismissButtonListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                    Toast.makeText(context, "Pembaruan dibatalkan.", Toast.LENGTH_SHORT).show();
                                }
                        });

                changelogDialog.show();

            }

        private File getManualExternalCacheDir() {
                File externalFilesDir = context.getExternalFilesDir(null);
                if (externalFilesDir == null) return null;

                File appSpecificRoot = externalFilesDir.getParentFile();
                if (appSpecificRoot == null) return null;

                File manualCacheDir = new File(appSpecificRoot, "cache");

                if (!manualCacheDir.exists()) {
                        boolean created = manualCacheDir.mkdirs();
                        if (!created) {
                                LogUtils.writeLog(context, "ERROR", "Gagal membuat folder cache eksternal manual.");
                                return null;
                            }
                    }
                return manualCacheDir;
            }

        private void downloadUpdateJson(final String moduleId, final String urlString) {
                HttpURLConnection connection = null;
                InputStream input = null;
                FileOutputStream output = null;

                File externalCacheDir = getManualExternalCacheDir();
                if (externalCacheDir == null) {
                        LogUtils.writeLog(context, "ERROR", "External Cache Dir manual tidak tersedia untuk JSON update.");
                        return;
                    }

                final File tempFile = new File(externalCacheDir, moduleId + TEMP_UPDATE_JSON); 

                try {
                        URL url = new URL(urlString);
                        connection = (HttpURLConnection) url.openConnection();
                        connection.setConnectTimeout(10000);
                        connection.setReadTimeout(10000);
                        connection.connect();

                        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                                input = connection.getInputStream();
                                output = new FileOutputStream(tempFile);
                                byte[] data = new byte[4096];
                                int count;
                                while ((count = input.read(data)) != -1) {
                                        output.write(data, 0, count);
                                    }
                                output.flush();
                                processUpdateJson(moduleId, tempFile);
                            } else {
                                LogUtils.writeLog(context, "WARNING", "HTTP status non-OK (" + connection.getResponseCode() + ") for " + moduleId + " update JSON.");
                                if (tempFile.exists()) tempFile.delete(); 
                            }
                    } catch (IOException e) {
                        LogUtils.writeLog(context, "ERROR", "Update JSON error " + moduleId + ": " + e.getMessage());
                        if (tempFile.exists()) tempFile.delete(); 
                    } finally {
                        try { if (output != null) output.close(); } catch (IOException ignored) {}
                        try { if (input != null) input.close(); } catch (IOException ignored) {}
                        if (connection != null) connection.disconnect();
                    }
            }

        private void processUpdateJson(final String moduleId, final File jsonFile) { 
                ModuleData targetModuleTmp = null;
                if (cachedModuleData != null) {
                        for (int i = 0; i < cachedModuleData.size(); i++) {
                                ModuleData module = cachedModuleData.get(i);
                                if (module.id.equals(moduleId)) {
                                        targetModuleTmp = module;
                                        break;
                                    }
                            }
                    }
                final ModuleData targetModule = targetModuleTmp; 
                if (targetModule == null) {
                        if (jsonFile.exists()) jsonFile.delete(); // Hapus file jika modul target tidak ditemukan
                        return;
                    }

                try {
                        String jsonString = readFileToString(jsonFile);
                        if (jsonString != null && !jsonString.isEmpty()) {
                                JSONObject updateJson = new JSONObject(jsonString);
                                int serverVersionCode = updateJson.optInt("versionCode", 0);
                                String rawVersion = updateJson.optString("version", "");
                                String zipUrl = updateJson.optString("zipUrl", "");
                                String changelog = updateJson.optString("changelog", "");

                                if (serverVersionCode > targetModule.versionCode && !zipUrl.isEmpty()) {
                                        targetModule.needsUpdate = true;
                                        targetModule.latestVersion = rawVersion.isEmpty() ? String.valueOf(serverVersionCode) : rawVersion;
                                        targetModule.latestZipUrl = zipUrl;
                                        targetModule.latestChangelog = changelog;
                                    }
                            }
                    } catch (Exception e) {
                        LogUtils.writeLog(context, "ERROR", "JSON process error: " + e.getMessage());
                    }

                mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                    if (jsonFile.exists()) jsonFile.delete(); 
                                    if (cachedModuleData != null) displayModules(cachedModuleData);
                                }
                        });
            }

        private void displayModules(List<ModuleData> modulesToDisplay) {
                if (moduleListLayout == null) return;
                moduleListLayout.removeAllViews();
                if (modulesToDisplay.isEmpty()) { 
                        if (noWebuiText != null) noWebuiText.setVisibility(View.VISIBLE);
                    } else {
                        if (noWebuiText != null) noWebuiText.setVisibility(View.GONE);
                        for (int i = 0; i < modulesToDisplay.size(); i++) {
                                moduleListLayout.addView(addModuleCard(modulesToDisplay.get(i)));
                            }
                    }
            }

        private View addModuleCard(final ModuleData data) {
                final View moduleCard = layoutInflater.inflate(R.layout.module_card_item, null, false); 

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 
                    LinearLayout.LayoutParams.WRAP_CONTENT
                );
                final float density = context.getResources().getDisplayMetrics().density; 
                params.bottomMargin = (int) (10 * density + 0.5f);
                moduleCard.setLayoutParams(params);

                TextView nameView = moduleCard.findViewById(R.id.module_name);
                TextView versionView = moduleCard.findViewById(R.id.module_version);
                TextView authorView = moduleCard.findViewById(R.id.module_author);
                TextView descView = moduleCard.findViewById(R.id.module_description);
                TextView jsonView = moduleCard.findViewById(R.id.module_json);
                final TextView tagSizeView = moduleCard.findViewById(R.id.module_size);
                final TextView moduleTagView = moduleCard.findViewById(R.id.module_tag);

                final ImageView bannerView = moduleCard.findViewById(R.id.module_banner);
                final TextView updateTagView = moduleCard.findViewById(R.id.module_update); 
                final Button btnOpen = moduleCard.findViewById(R.id.btn_open); 
                final Button btnUpdate = moduleCard.findViewById(R.id.btn_update);
                final com.WebSu.ig.logd moduleToggle = moduleCard.findViewById(R.id.module_toggle); 

                nameView.setText(data.name); 
                versionView.setText("Version: " + data.version);
                authorView.setText("Author: " + data.author);
                descView.setText(data.description);
                jsonView.setText(data.updateJsonUrl != null && !data.updateJsonUrl.isEmpty() ? "updateJson: Oldest  " : "updateJson: Latest ");

                if (data.cachedSize != null) {
                        tagSizeView.setText(data.cachedSize);
                    } else {
                        tagSizeView.setText("N/A"); 
                    }
                moduleToggle.setChecked(data.isEnabled);
                if (data.cachedBannerBitmap != null) {
                        bannerView.setImageBitmap(data.cachedBannerBitmap);
                        bannerView.setAlpha(0.5f);
                    } else {
                        setBannerBackgroundColor(bannerView);
                    }

                final Runnable updateUIStatus = new Runnable() {
                        @Override
                        public void run() {
                                boolean isChecked = moduleToggle.isChecked();
                                btnOpen.setEnabled(isChecked);

                                int textColor = ContextCompat.getColor(context, R.color.bgOpen);
                                int bgDrawableRes = isChecked ? R.drawable.bg_nav_active : R.drawable.bg_ui;

                                btnOpen.setTextColor(textColor);
                                btnOpen.setBackgroundDrawable(ContextCompat.getDrawable(context, bgDrawableRes));

                                if (isChecked) {
                                        moduleTagView.setText("WEBUI");
                                        moduleTagView.setBackgroundResource(R.drawable.bg_nav_active);
                                    } else {
                                        moduleTagView.setText("GET DISABLE");
                                        moduleTagView.setBackgroundResource(R.drawable.bg_get);
                                    }

                                boolean showUpdate = data.needsUpdate && isChecked;

                                updateTagView.setVisibility(showUpdate ? View.VISIBLE : View.GONE);
                                btnUpdate.setVisibility(showUpdate ? View.VISIBLE : View.GONE);
                            }
                    };
                updateUIStatus.run();

                moduleToggle.setOnCheckedChangeListener(new com.WebSu.ig.logd.OnCheckedChangeListener() {
                            @Override
                            public void onCheckedChanged(View view, boolean isChecked) {
                                    data.isEnabled = isChecked; 
                                    updateUIStatus.run();
                                    saveModuleEnableStatus(data.id, data.moduleBasePath, isChecked); 
                                }
                        });

                btnOpen.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                    Intent intent = new Intent(context, WebUiActivity.class);
                                    intent.putExtra(EXTRA_MODULE_ID, data.id);
                                    intent.putExtra(EXTRA_MODULE_PATH, data.moduleBasePath);
                                    context.startActivity(intent);
                                }
                        });

                btnUpdate.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                    if (data.latestZipUrl != null && !data.latestZipUrl.isEmpty()) {
                                            if (isUrl(data.latestChangelog)) {
                                                    downloadChangelogContent(data);
                                                } else {
                                                    String changelogContent = data.latestChangelog != null && !data.latestChangelog.isEmpty() 
                                                        ? data.latestChangelog 
                                                        : "Pembaruan versi " + data.latestVersion + " tersedia.";

                                                    showChangelogDialogWithContent(data, changelogContent);
                                                }
                                        } else {
                                            Toast.makeText(context, "URL update tidak valid atau belum dimuat.", Toast.LENGTH_SHORT).show();
                                        }
                                }
                        });

                moduleCard.findViewById(R.id.btn_delete).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                    if (moduleActionListener != null) {
                                            moduleActionListener.onDeleteModule(data.id, data.moduleBasePath);
                                        }
                                }
                        });

                return moduleCard; 
            }

        private void handleUpdateConfirmation(final ModuleData data) {
                final View currentRootView = rootView; 
                UpdatePlugin.OnUpdateFlowListener updateListener = new UpdatePlugin.OnUpdateFlowListener() {

                        @Override
                        public void onChangelogReady(String changelog, String latestVersion) {
                            } 

                        @Override
                        public void showToast(final String message) { 
                                mainHandler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show(); 
                                                }
                                        });
                            }

                        @Override
                        public void setLoading(boolean isLoading) {
                            }

                        @Override
                        public void onInstallReady(final Uri fileUri) { 
                                mainHandler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                    if (context instanceof Activity) {
                                                            if (currentRootView != null) {
                                                                    ZipFilePicker picker = new ZipFilePicker((Activity) context, currentRootView);
                                                                    picker.startInstallProcess(fileUri, ZipFilePicker.DEFAULT_REQUEST_CODE_INSTALL);
                                                                } else {
                                                                    Toast.makeText(context, "Gagal memulai instalasi: RootView tidak tersedia.", Toast.LENGTH_LONG).show();
                                                                }
                                                        }
                                                }
                                        });
                            }

                        @Override
                        public void onError(final String message) { 
                                mainHandler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                    Toast.makeText(context, "Error Update: " + message, Toast.LENGTH_SHORT).show(); 
                                                }
                                        });
                            }
                    };

                UpdatePlugin updatePlugin = new UpdatePlugin(context, updateListener);
                updatePlugin.startDownloadConfirmed(data.latestZipUrl, data.id + "_" + data.latestVersion + ".zip");
                Toast.makeText(context, "Memulai proses pembaruan untuk " + data.name + "...", Toast.LENGTH_SHORT).show();
            }

        private Bitmap loadLocalBannerSync(ModuleData data) {
                final String absPath = data.moduleBasePath + File.separator + data.bannerPath;
                if (checkFileExistsInShell(absPath)) {
                        final String cachePath = context.getExternalFilesDir(null) + File.separator + BANNER_SUB_DIR + File.separator + data.id + "_" + new File(absPath).getName();
                        File target = new File(cachePath);
                        if (!target.exists()) copyFileToExternalFilesDir(absPath, data.id);
                        return BitmapFactory.decodeFile(cachePath);
                    }
                return null;
            }

        private Bitmap loadBannerFromUrlSync(final String url, final String moduleId) {
                String fileName = Uri.parse(url).getLastPathSegment();
                if (fileName == null || fileName.isEmpty()) {
                        fileName = "banner.png"; 
                    }

                File externalCacheRoot = getManualExternalCacheDir();
                if (externalCacheRoot == null) {
                        LogUtils.writeLog(context, "ERROR", "External Cache Dir manual tidak tersedia untuk Banner.");
                        return null;
                    }

                File cacheDir = new File(externalCacheRoot, BANNER_SUB_DIR);
                if (!cacheDir.exists()) cacheDir.mkdirs();

                final File cacheFile = new File(cacheDir, moduleId + "_" + fileName); 
                Bitmap bitmap = null;

                if (cacheFile.exists()) {
                        bitmap = BitmapFactory.decodeFile(cacheFile.getAbsolutePath());
                    }

                if (bitmap == null) {
                        HttpURLConnection connection = null;
                        InputStream input = null;
                        FileOutputStream output = null;
                        try {
                                URL imageUrl = new URL(url);
                                connection = (HttpURLConnection) imageUrl.openConnection();
                                connection.setConnectTimeout(10000);
                                connection.setReadTimeout(10000);
                                connection.connect();

                                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                                        input = connection.getInputStream();
                                        output = new FileOutputStream(cacheFile);
                                        byte[] data = new byte[4096];
                                        int count;
                                        while ((count = input.read(data)) != -1) {
                                                output.write(data, 0, count);
                                            }
                                        output.flush();
                                        bitmap = BitmapFactory.decodeFile(cacheFile.getAbsolutePath());
                                    }
                            } catch (IOException e) {
                                LogUtils.writeLog(context, "ERROR", "Banner download error: " + e.getMessage());
                                if (cacheFile.exists()) cacheFile.delete(); 
                            } finally {
                                try { if (output != null) output.close(); } catch (IOException ignored) {}
                                try { if (input != null) input.close(); } catch (IOException ignored) {}
                                if (connection != null) connection.disconnect();
                            }
                    }
                return bitmap;
            }

        private void setBannerBackgroundColor(ImageView bannerView) {
                bannerView.setBackgroundColor(ContextCompat.getColor(context, R.color.colorBgd));
                bannerView.setImageDrawable(null);
                bannerView.setAlpha(1.0f);
            }

        private String calculateModuleSizeSync(final String modulePath) {
                String cmd = String.format("du -sb %s | cut -f1", escapeShellArg(modulePath));
                ShellExecutor.Result result = ShellExecutor.execSync(cmd, context);

                if (result.exitCode == 0 && result.stdout != null) {
                        try {
                                long bytes = Long.parseLong(result.stdout.trim());
                                return formatFileSize(bytes);
                            } catch (Exception e) {
                                return "0 B";
                            }
                    } else {
                        return "0 B";
                    }
            }

        private String formatFileSize(long bytes) {
                if (bytes <= 0) return "0 B";

                final String[] units = new String[] { "B", "KB", "MB", "GB", "TB", "PB" };
                int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));

                if (digitGroups >= units.length) digitGroups = units.length - 1;
                return String.format("%.2f %s", bytes / Math.pow(1024, digitGroups), units[digitGroups]);
            }

        private String readFileToString(File file) throws IOException {
                BufferedReader reader = null;
                try {
                        reader = new BufferedReader(new FileReader(file));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) sb.append(line);
                        return sb.toString();
                    } finally {
                        if (reader != null) try { reader.close(); } catch (IOException ignored) {}
                    }
            }

        private String readPropertyFromShell(String propPath, String key) {
                String cmd = String.format("grep '^%s=' %s | head -n 1 | cut -d= -f2-", key, escapeShellArg(propPath));
                ShellExecutor.Result res = ShellExecutor.execSync(cmd, context);
                if (res.exitCode == 0 && res.stdout != null) {
                        String val = res.stdout.trim().replaceAll("[\\r\\n]", "");
                        return val.isEmpty() ? null : val;
                    }
                return null;
            }

        private boolean checkFileExistsInShell(String filePath) {
                String cmd = String.format("find %s -type f -maxdepth 0 2>/dev/null", escapeShellArg(filePath));
                return ShellExecutor.execSync(cmd, context).exitCode == 0;
            }

        private boolean checkDirectoryExistsInShell(String dirPath) {
                String cmd = String.format("find %s -type d -maxdepth 0 2>/dev/null", escapeShellArg(dirPath));
                return ShellExecutor.execSync(cmd, context).exitCode == 0;
            }

        private String[] getModuleDirsFromShell(String baseDirPath) {
                String cmd = String.format("ls -1 %s 2>/dev/null", escapeShellArg(baseDirPath));
                ShellExecutor.Result res = ShellExecutor.execSync(cmd, context);
                if (res.exitCode == 0 && res.stdout != null) {
                        String[] lines = res.stdout.trim().split("\n");
                        List<String> list = new ArrayList<String>();
                        for (int i = 0; i < lines.length; i++) {
                                if (!lines[i].trim().isEmpty()) list.add(lines[i].trim());
                            }
                        return list.toArray(new String[list.size()]);
                    }
                return null;
            }

        private boolean isModuleEnabled(String moduleBasePath) {
                return !checkFileExistsInShell(moduleBasePath + File.separator + DISABLE_LOCK_FILE);
            }

        private void saveModuleEnableStatus(String moduleId, String path, boolean enable) {
                final String finalPath = path; 
                final boolean finalEnable = enable; 

                ioExecutor.execute(new Runnable() {
                            @Override
                            public void run() {
                                    String lockPath = finalPath + File.separator + DISABLE_LOCK_FILE;
                                    String cmd = finalEnable ? ("rm -f " + escapeShellArg(lockPath)) : ("touch " + escapeShellArg(lockPath));
                                    ShellExecutor.execSync(cmd, context);
                                }
                        });
            }

        private void copyFileToExternalFilesDir(String sourcePath, String moduleId) {
                File bannerDir = new File(context.getExternalFilesDir(null), BANNER_SUB_DIR);
                if (!bannerDir.exists()) bannerDir.mkdirs();
                String destPath = bannerDir.getAbsolutePath() + File.separator + moduleId + "_" + new File(sourcePath).getName();
                ShellExecutor.execSync(String.format("cp %s %s", escapeShellArg(sourcePath), escapeShellArg(destPath)), context);
            }

        private boolean isUrl(String path) {
                return path != null && (path.startsWith("http://") || path.startsWith("https://"));
            }

        private static String escapeShellArg(String arg) {
                if (arg == null) return "''";
                return "'" + arg.replace("'", "'\\''") + "'";
            }
    }

