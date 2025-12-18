package com.WebSu.ig.Dirname;

import android.net.Uri;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.util.ArrayList;
import java.util.List;

public class WebSuLoader {
        private final List<Route> routes;

        private WebSuLoader(List<Route> routes) {
                this.routes = routes;
            }

        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                for (Route route : routes) {
                        if (route.matches(request)) {
                                return route.handler.handle(view, request);
                            }
                    }
                return null;
            }
            
        public interface wsPathHandler {
                @WorkerThread
                @Nullable WebResourceResponse handle(WebView view, WebResourceRequest request);
            }

        public static class Builder {
                private final List<Route> routes = new ArrayList<>();

                public TempRouteGroup addScheme(String scheme) {
                        return new TempRouteGroup(this, scheme);
                    }

                protected void addRoute(Route route) {
                        routes.add(route);
                    }

                public WebSuLoader build() {
                        return new WebSuLoader(routes);
                    }
            }
            
        public static class TempRouteGroup {
                private final Builder parent;
                private final String scheme;
                private final List<Route> groupRoutes = new ArrayList<>();
                private final List<String> pendingDomains = new ArrayList<>();

                TempRouteGroup(Builder parent, String scheme) {
                        this.parent = parent;
                        this.scheme = scheme;
                    }

                public TempRouteGroup addDomain(String domain) {
                        pendingDomains.add(domain);
                        return this;
                    }

                public TempRouteGroup addHandler(wsPathHandler handler) {
                        for (String domain : pendingDomains) {
                                groupRoutes.add(new Route(scheme, domain, null, handler));
                            }
                        pendingDomains.clear(); // reset agar tidak dipakai lagi
                        return this;
                    }

                public TempRouteGroup addPathHandler(String path, wsPathHandler handler) {
                        for (String domain : pendingDomains) {
                                groupRoutes.add(new Route(scheme, domain, path, handler));
                            }
                        pendingDomains.clear(); // reset agar tidak dipakai lagi
                        return this;
                    }

                public Builder done() {
                        for (Route route : groupRoutes) {
                                parent.addRoute(route);
                            }
                        return parent;
                    }
            }

        public static class Route {
                private final String scheme;
                private final String domain;
                private final String pathPrefix;
                private final wsPathHandler handler;

                Route(String scheme, String domain, String pathPrefix, wsPathHandler handler) {
                        this.scheme = scheme;
                        this.domain = domain;
                        this.pathPrefix = pathPrefix;
                        this.handler = handler;
                    }

                boolean matches(WebResourceRequest request) {
                        Uri url = request.getUrl();
                        if (url == null || url.getScheme() == null || url.getHost() == null) return false;

                        if (!url.getScheme().equals(scheme)) return false;
                        if (!url.getHost().equals(domain)) return false;

                        String path = url.getPath();
                        if (pathPrefix == null || pathPrefix.isEmpty()) {
                                return true; 
                            }
                        return path != null && path.startsWith(pathPrefix);
                    }
            }
    }
