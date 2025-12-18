package com.WebSu.ig.webui.interfaces;

import android.app.*;
import android.os.*;
import android.util.*;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import android.view.Window;
import android.os.Environment;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener; 
import android.content.Context;
import android.text.TextUtils;
import android.webkit.JavascriptInterface;
import android.widget.Toast;
import org.json.JSONObject;
import org.json.JSONException; 

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import com.WebSu.ig.ShellExecutor; 

public class EverythingsWebInterface {

        private static final String TAG = "WebSu.EverythingsWebInterface";    
        private final Activity activity;    
        private final WebView webView;    
        private final Handler handler;    

        public EverythingsWebInterface(Activity activity, WebView webView, Handler handler) {    
                this.activity = activity;    
                this.webView = webView;    
                this.handler = handler;    
                log("EverythingsWebInterface initialized");    
            }    

        @JavascriptInterface    
        public void toast(final String msg) {    
                handler.post(new Runnable() {    
                            @Override    
                            public void run() {    
                                    Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show();    
                                }    
                        });    
            }    

        @JavascriptInterface
        public String runScript(String path) {
                try {
                        if (path == null || path.trim().isEmpty()) {
                                return "Error: path kosong";
                            }

                        String cmd = "/system/bin/sh " + path;

                        ShellExecutor.Result r = ShellExecutor.execSync(cmd, activity.getApplicationContext());
                        String out = (r.stdout != null ? r.stdout.trim() : "");
                        String err = (r.stderr != null ? r.stderr.trim() : "");
                        int code = r.exitCode;

                        Log.d(TAG, "runScript(" + path + ") exit=" + code);
                        return "Path=" + path + "\nExitCode=" + code + "\n" + out + (err.isEmpty() ? "" : "\nERR:" + err);
                    } catch (Throwable e) {
                        Log.e(TAG, "runScript failed: " + e);
                        return "Error: " + e.getMessage();
                    }
            }

        @JavascriptInterface
        public String runScriptAuto(String targetName) {
                if (targetName == null || targetName.trim().isEmpty()) {
                        return "Error: targetName kosong";
                    }

                try {
                        final String rel = targetName.replace('\\', '/');

                        File[] roots = new File[] {
                                activity.getFilesDir(),
                                activity.getExternalFilesDir(null),
                                Environment.getExternalStorageDirectory(),
                                new File("/sdcard"),
                                new File("/storage/emulated/0"),
                                new File("/data/user_de/0/com.android.shell/WebSu/"),
                                new File("/mnt"),
                                new File("/")
                            };

                        File found = null;
                        for (int i = 0; i < roots.length; i++) {
                                File r = roots[i];
                                if (r == null) continue;
                                if (!r.exists() || !r.canRead()) continue;
                                found = findFileRecursive(r, rel, 6);
                                if (found != null) break;
                            }

                        if (found == null) {
                                Log.d(TAG, "runScriptAuto: not found -> " + rel);
                                return "Error: file not found: " + rel;
                            }

                        final String path = found.getAbsolutePath();
                        Log.d(TAG, "runScriptAuto: found -> " + path);

                        String cmd = "/system/bin/sh " + path;
                        ShellExecutor.Result r = ShellExecutor.execSync(cmd, activity.getApplicationContext());
                        String out = (r.stdout != null ? r.stdout.trim() : "");
                        String err = (r.stderr != null ? r.stderr.trim() : "");
                        int code = r.exitCode;

                        StringBuilder sb = new StringBuilder();
                        sb.append("Path=").append(path).append("\n");
                        sb.append("ExitCode=").append(code).append("\n");
                        if (!out.isEmpty()) sb.append(out);
                        if (!err.isEmpty()) {
                                if (!out.isEmpty()) sb.append("\n");
                                sb.append("ERR:").append(err);
                            }
                        return sb.toString();

                    } catch (Throwable e) {
                        Log.e(TAG, "runScriptAuto failed", e);
                        return "Error: " + e.getMessage();
                    }
            }

        @JavascriptInterface
        public void fullScreen(final boolean enable) {
                handler.post(new Runnable() {
                            @Override
                            public void run() {
                                    if (activity == null) return;

                                    activity.runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                        Window window = activity.getWindow();
                                                        View decorView = window.getDecorView();

                                                        if (enable) {
                                                                decorView.setSystemUiVisibility(
                                                                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                                                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                                                                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                                                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                                                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                                                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                                                );

                                                                WindowManager.LayoutParams params = window.getAttributes();
                                                                params.layoutInDisplayCutoutMode =
                                                                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
                                                                window.setAttributes(params);
                                                                window.setStatusBarColor(android.graphics.Color.parseColor("#0F1018"));
                                                            } else {
                                                                decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
                                                                window.setStatusBarColor(android.graphics.Color.parseColor("#0F1018"));
                                                            }

                                                        Log.d(TAG, "Fullscreen mode: " + enable);
                                                        Toast.makeText(activity, "Fullscreen: " + enable, Toast.LENGTH_SHORT).show();
                                                    }
                                            });
                                }
                        });
            }

        @JavascriptInterface    
        public String execSync(String cmd) {    
                try {    
                        ShellExecutor.Result r = ShellExecutor.execSync(cmd, activity.getApplicationContext());    
                        return r.stdout != null ? r.stdout.trim() : "";    
                    } catch (Throwable e) {    
                        return "Error: " + e.getMessage();    
                    }    
            }    

        @JavascriptInterface
        public void spawnStart(final String id, final String cmd) {
                new Thread(new Runnable() {
                            @Override
                            public void run() {
                                    boolean ok = ShellExecutor.spawnStart(id, cmd);
                                    Log.d(TAG, "spawnStart(" + id + ")=" + ok);
                                }
                        }).start();
            }

        @JavascriptInterface
        public void spawnKill(final String id) {
                new Thread(new Runnable() {
                            @Override
                            public void run() {
                                    ShellExecutor.spawnKill(id);
                                    Log.d(TAG, "spawnKill(" + id + ")");
                                }
                        }).start();
            }

        @JavascriptInterface    
        public void execAsyncWithCallback(final String cmd, final String options, final String cbId) {    
                new Thread(new Runnable() {    
                            @Override    
                            public void run() {
                                    ShellExecutor.Result r = null;
                                    String tempCmd = cmd; 
                                    boolean handled = false;

                                    try {
                                            if (tempCmd.equals("exec")) {
                                                    JSONObject payload = new JSONObject(options);

                                                    tempCmd = payload.getString("cmd"); 

                                                    Log.w(TAG, "exec API called. Cwd/Env options ignored by JSBridge shim.");
                                                    handled = true;

                                                } else if (tempCmd.equals("getProp")) {
                                                    String key = options.replaceAll("^\"|\"$", "").trim(); 
                                                    tempCmd = "getprop " + key;
                                                    handled = true;

                                                } else if (tempCmd.equals("setProp")) {
                                                    JSONObject json = new JSONObject(options);
                                                    String key = json.getString("key");
                                                    String value = json.getString("value");
                                                    tempCmd = "setprop " + key + " \"" + value + "\"";
                                                    handled = true;

                                                } else if (tempCmd.equals("getModules")) {
                                                    ShellExecutor.Result lsResult = ShellExecutor.execSync("ls /data/adb/modules", activity.getApplicationContext());

                                                    JSONArray moduleList = new JSONArray();
                                                    if (lsResult.exitCode == 0 && lsResult.stdout != null) {
                                                            String[] modules = lsResult.stdout.trim().split("\n");
                                                            for(String module : modules) {
                                                                    if (!module.trim().isEmpty()) {
                                                                            JSONObject modObj = new JSONObject();
                                                                            modObj.put("id", module.trim());
                                                                            modObj.put("name", module.trim());
                                                                            moduleList.put(modObj);
                                                                        }
                                                                }
                                                        }
                                                    r = new ShellExecutor.Result(0, moduleList.toString(), "", 0);
                                                    sendCallback(cbId, r);
                                                    return;
                                                } else if (tempCmd.equals("getModuleConfig")) {
                                                    r = new ShellExecutor.Result(0, "{\"status\":\"ok\",\"config\":\"stub_config\"}", "", 0);
                                                    sendCallback(cbId, r);
                                                    return;

                                                } else if (tempCmd.equals("setModuleConfig")) {
                                                    r = new ShellExecutor.Result(0, "{\"success\":true}", "", 0);
                                                    sendCallback(cbId, r);
                                                    return;
                                                }

                                            r = ShellExecutor.execSync(tempCmd, activity.getApplicationContext());

                                        } catch (Throwable e) {    
                                            r = new ShellExecutor.Result(-1, "", "API/JSON error: " + e.toString(), 0);
                                            Log.e(TAG, "execAsyncWithCallback failed: " + cmd, e);
                                        }    
                                    sendCallback(cbId, r);    
                                }    
                        }).start();    
            }

        @JavascriptInterface
        public void requestFullRoot() {
                handler.post(new Runnable() {
                            @Override
                            public void run() {
                                    Toast.makeText(activity, "Root access requested by module.", Toast.LENGTH_SHORT).show();
                                }
                        });

                new Thread(new Runnable() {
                            @Override
                            public void run() {
                                    try {
                                            ShellExecutor.execSync("id", activity.getApplicationContext());
                                            Log.d(TAG, "requestFullRoot: ShellExecutor confirmed root access.");
                                        } catch (Throwable e) {
                                            Log.e(TAG, "requestFullRoot: ShellExecutor failed to get root access.", e);
                                        }
                                }
                        }).start();
            }


        private void sendCallback(final String cbId, final ShellExecutor.Result r) {    
                handler.post(new Runnable() {    
                            @Override    
                            public void run() {    
                                    if (webView == null) return;    
                                    final String stdout = safeJSON(r.stdout);    
                                    final String stderr = safeJSON(r.stderr);    
                                    String js = "(function(){"    
                                        + "var cb = window._websu_promises && window._websu_promises['" + cbId + "'];"    
                                        + "if(cb){ cb({stdout:" + stdout + ",stderr:" + stderr + ",exitCode:" + r.exitCode + "});"    
                                        + "delete window._websu_promises['" + cbId + "']; }"    
                                        + "})();";    
                                    execJS(js);    
                                }    
                        });    
            }    

        public void injectJSBridgeModule() {    
                if (webView == null) return;    
                handler.post(new Runnable() {    
                            @Override    
                            public void run() {    
                                    final String js = ""    
                                        + "if(!window.JSBridgeModule){"    
                                        + "  window.JSBridgeModule = {"    
                                        + "    exec: function(cmd){"    
                                        + "      return new Promise(function(resolve){"    
                                        + "        var cbId = 'cb_' + Math.random().toString(36).substring(2);"    
                                        + "        window._websu_promises = window._websu_promises || {};"    
                                        + "        window._websu_promises[cbId] = resolve;"    
                                        + "        JSBridge.execAsyncWithCallback(cmd, '{}', cbId);"    
                                        + "      });"    
                                        + "    },"    
                                        + "    runScript: function(path){"    
                                        + "      return new Promise(function(resolve){"    
                                        + "        var cbId = 'cb_' + Math.random().toString(36).substring(2);"    
                                        + "        window._websu_promises = window._websu_promises || {};"    
                                        + "        window._websu_promises[cbId] = resolve;"    
                                        + "        JSBridge.execAsyncWithCallback('/system/bin/sh ' + path, '{}', cbId);"    
                                        + "      });"    
                                        + "    },"    
                                        + "    toast: function(msg){ JSBridge.toast(msg); }"    
                                        + "  };"    
                                        + "  console.log('[JSBridge] Module loaded');"    
                                        + "}";    

                                    execJS(js);    
                                }    
                        });    
            }

        public void injectEverythings() {
                if (webView == null) return;
                handler.post(new Runnable() {
                            @Override
                            public void run() {
                                    final String js = ""
                                        + "(function(){"
                                        + "  if(window._kernelSUPolyfillInjected) return;"
                                        + "  window._kernelSUPolyfillInjected = true;"
                                        + "  window._websu_promises = window._websu_promises || {};"
                                        + "  \n"
                                        + "  // Fungsi utilitas untuk membuat API asinkron (Promise-based)"
                                        + "  function createAsyncApi(cmdName) {"
                                        + "    // API exec/getProp menerima parameter 'param' (options/key) yang perlu di-stringified"
                                        + "    return function(param) {"
                                        + "      return new Promise(function(resolve, reject) {"
                                        + "        var cbId = 'cb_' + Math.random().toString(36).substring(2);"
                                        + "        window._websu_promises[cbId] = resolve;"
                                        + "        // Untuk API KernelSU yang menggunakan options, kita kirimkan JSON.stringify(param)"
                                        + "        var paramStr = (typeof param === 'object' && param !== null) ? JSON.stringify(param) : JSON.stringify(param);" // Perbaikan JSON stringify untuk kasus non-object
                                        + "        try {"
                                        + "          // Panggil JSBridge.execAsyncWithCallback dengan 'cmdName' sebagai fungsi API"
                                        + "          JSBridge.execAsyncWithCallback(cmdName, paramStr, cbId);"
                                        + "        } catch(e) {"
                                        + "          console.error('[KernelSU] JSBridge call failed for ' + cmdName + ':', e);"
                                        + "          reject(e);"
                                        + "        }"
                                        + "      });"
                                        + "    };"
                                        + "  }\n"
                                        + "  \n"
                                        + "  // JSBridgeModule (Bridge dasar ke Java)"
                                        + "  if(!window.JSBridgeModule){"
                                        + "    window.JSBridgeModule = {"
                                        + "      // exec sekarang memanggil createAsyncApi('exec') yang mengirim {cmd, options} sebagai payload ke Java"
                                        + "      exec: function(cmd, options){ "
                                        + "          // Karena kita menggunakan EverythingsWebInterface, kita mapping KernelSU.exec -> JSBridgeModule.exec -> execAsyncWithCallback('exec')"
                                        + "          // Opsi paling aman adalah langsung menggunakan API yang ada di Java: runScript/execSync/execAsyncWithCallback"
                                        + "          // Mari kita buat shim yang memanggil execAsyncWithCallback('exec') di Java"
                                        + "          return createAsyncApi('exec')({cmd:cmd, options:options}); "
                                        + "      },"
                                        + "      toast: function(msg){ JSBridge.toast(msg); },"
                                        + "      setTheme: function(theme){ console.log('[KernelSU] setTheme called:', theme); JSBridge.setTheme && JSBridge.setTheme(theme); },"
                                        + "    };"
                                        + "  }\n"
                                        + "  \n"
                                        + "  // ** KernelSU API Shim **"
                                        + "  if(!window.KernelSU){"
                                        + "    window.KernelSU = {"
                                        + "      // exec: Menggunakan JSBridgeModule.exec yang sudah diubah logicnya"
                                        + "      exec: window.JSBridgeModule.exec, " // Sekarang sama dengan yang di JSBridgeModule
                                        + "      // ** ⚠️ TAMBAHAN PENTING: Implementasi spawn() **"
                                        + "      spawn: function(cmd, args, options){"
                                        + "        var id = 'child_' + Math.random().toString(36).substring(2);"
                                        + "        // Gabungkan cmd dan args menjadi string command tunggal"
                                        + "        var fullCmd = cmd + ' ' + (args||[]).join(' ');"
                                        + "        // Hanya memanggil spawnStart. Karena ini shim, kita hanya mengembalikan objek dummy."
                                        + "        JSBridge.spawnStart(id, fullCmd);" 
                                        + "        return {"
                                        + "          id: id,"
                                        + "          kill: function(){ JSBridge.spawnKill(id); },"
                                        + "          stdout: { on: function(event, callback){ console.log('[KernelSU.spawn] stdout.on stub'); } }," // Stub on('data')
                                        + "          stderr: { on: function(event, callback){ console.log('[KernelSU.spawn] stderr.on stub'); } }," // Stub on('data')
                                        + "          on: function(event, callback){ console.log('[KernelSU.spawn] on stub'); }," // Stub on('exit', 'error')
                                        + "        };"
                                        + "      },"
                                        + "      getProp: createAsyncApi('getProp'),"
                                        + "      setProp: createAsyncApi('setProp'),"
                                        + "      getModules: createAsyncApi('getModules'),"
                                        + "      toast: window.JSBridgeModule.toast,"
                                        + "      fullScreen: function(enable){ console.log('[KernelSU] fullScreen called with', enable); JSBridge.fullScreen && JSBridge.fullScreen(enable); },"
                                        + "      requestFullRoot: function(){ console.log('[KernelSU] requestFullRoot called'); JSBridge.requestFullRoot && JSBridge.requestFullRoot(); }"
                                        + "    };"
                                        + "  }\n"
                                        + "  // ** Shim export module (WAJIB menyertakan 'spawn') **"
                                        + "  window.importShim = function(path){"
                                        + "    if(path && (path.indexOf('kernelsu') !== -1 || path.indexOf('cdn.jsdelivr.net/npm/kernelsu') !== -1)){"
                                        + "      return Promise.resolve({ "
                                        + "        exec: window.KernelSU.exec,"
                                        + "        spawn: window.KernelSU.spawn,"
                                        + "        toast: window.KernelSU.toast,"
                                        + "        fullScreen: window.KernelSU.fullScreen,"
                                        + "        getProp: window.KernelSU.getProp,"
                                        + "      });"
                                        + "    } else {"
                                        + "      if(window.import) return window.import(path);"
                                        + "      return Promise.reject('Cannot import: '+path);"
                                        + "    }"
                                        + "  };\n"
                                        + "})();";

                                    execJS(js);
                                }
                        });
            }


        public void injectDirname() {
                if (webView == null) return;
                handler.post(new Runnable() {
                            @Override
                            public void run() {
                                    final String script =
                                        "(function waitAndInject(){"
                                        + "  const buttons = document.querySelectorAll('button[data-cmd]');"
                                        + "  if(buttons.length === 0){"
                                        + "    setTimeout(waitAndInject, 50);" 
                                        + "    return;"
                                        + "  }"
                                        + "  try {"
                                        + "    // Asumsi DIRNAME sudah didefinisikan sebelumnya"
                                        + "    if(!window.DIRNAME) window.DIRNAME='/data/local/tmp/WebSu/WebSu_Plugins';"
                                        + "    buttons.forEach(function(btn){"
                                        + "      if(btn.dataset.cmd && btn.dataset.cmd.includes('DIRNAME')){"
                                        + "        btn.dataset.cmd = btn.dataset.cmd.replace(/DIRNAME/g, window.DIRNAME);"
                                        + "      }"
                                        + "    });"
                                        + "    console.log('[JSBridge] DIRNAME injected into buttons:', window.DIRNAME);"
                                        + "  } catch(e) {"
                                        + "    console.error('[JSBridge] injectDirname failed:', e);"
                                        + "  }"
                                        + "})();";

                                    webView.evaluateJavascript(script, null);
                                }
                        });
            }

        private void execJS(final String js) {
                handler.post(new Runnable() {
                            @Override
                            public void run() {
                                    try {
                                            webView.evaluateJavascript(js, null);
                                        } catch (Throwable e) {
                                            Log.e(TAG, "JS exec failed: " + e);
                                        }
                                }
                        });
            }

        private static String safeJSON(String s) {
                if (s == null) return "\"\"";
                StringBuilder sb = new StringBuilder("\"");
                for (int i = 0; i < s.length(); i++) {
                        char c = s.charAt(i);
                        switch (c) {
                                case '\\': sb.append("\\\\"); break;
                                case '"': sb.append("\\\""); break;
                                case '\n': sb.append("\\n"); break;
                                case '\r': sb.append("\\r"); break;
                                case '\t': sb.append("\\t"); break;
                                default: sb.append(c);
                            }
                    }
                sb.append("\"");
                return sb.toString();
            }

        private void log(String text) {
                Log.d(TAG, text);
            }

        private File findFileRecursive(File dir, String relativePath, int depth) {
                if (dir == null || !dir.exists() || depth < 0) return null;
                try {
                        File[] children = dir.listFiles();
                        if (children == null) return null;
                        for (int i = 0; i < children.length; i++) {
                                File f = children[i];
                                try {
                                        String can = f.getCanonicalPath().replace('\\', '/');
                                        if (can.endsWith(relativePath)) {
                                                return f;
                                            }
                                    } catch (Throwable ignore) {}
                                if (f.isDirectory()) {
                                        File r = findFileRecursive(f, relativePath, depth - 1);
                                        if (r != null) return r;
                                    }
                            }
                    } catch (Throwable e) {
                    }
                return null;
            }

        public void injectAllJavascript() {
                injectJSBridgeModule();
                injectEverythings(); 
                injectDirname(); 
            }
    }
