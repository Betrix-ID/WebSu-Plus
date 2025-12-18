package com.WebSu.ig.webui;

import android.app.Application;
import android.content.Context;

public class Engine extends Application {
        private static Engine application;

        public static Context getApplication() {
                return application;
            }

        @Override
        public void onCreate() {
                super.onCreate();
                application = this;
            }
    }
