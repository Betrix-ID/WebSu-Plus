package com.WebSu.ig.webui.interfaces;

import android.util.Log;
import android.webkit.WebView;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import com.WebSu.ig.LogUtils; 

public class StreamGobbler extends Thread {
        private final InputStream inputStream;
        private final String streamName; 
        private final String callbackFunc;
        private final WebView webView;
        private final String TAG = "StreamGobbler";

        public StreamGobbler(InputStream is, String name, String func, WebView wv) {
                this.inputStream = is;
                this.streamName = name;
                this.callbackFunc = func;
                this.webView = wv;
            }

        private void closeQuietly(Closeable c) {
                if (c != null) {
                        try {
                                c.close();
                            } catch (Throwable ignored) {
                            }
                    }
            }

        @Override
        public void run() {
                BufferedReader br = null;
                try {
                        br = new BufferedReader(new InputStreamReader(inputStream));
                        String line;
                        while ((line = br.readLine()) != null) {
                                final String escapedData = escapeJsString(line + "\n");

                                final String jsCode =
                                    "javascript: (function() { try { " + callbackFunc + "." + streamName + ".emit('data', '" +
                                    escapedData + 
                                    "'); } catch(e) { console.error('emitData for " + streamName + "', e); } })();";

                                // Wajib memanggil loadUrl di UI Thread
                                webView.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                    webView.loadUrl(jsCode);
                                                }
                                        });
                            }
                    } catch (IOException e) {
                    } finally {
                        closeQuietly(br);
                        Log.d(TAG, streamName + " reading finished for " + callbackFunc);
                    }
            }
            
        private String escapeJsString(String s) {
                if (s == null) return "";
                return s.replace("\\", "\\\\") 
                    .replace("'", "\\'")   
                    .replace("\n", "\\n")  
                    .replace("\r", "\\r");  
            }
    }
