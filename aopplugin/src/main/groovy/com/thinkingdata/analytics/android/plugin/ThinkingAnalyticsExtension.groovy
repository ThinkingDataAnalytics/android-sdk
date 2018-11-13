package com.thinkingdata.analytics.android.plugin;

class ThinkingAnalyticsExtension {
    List<String> includeJarFilter = new ArrayList<String>()
    List<String> excludeJarFilter = new ArrayList<String>()
    List<String> ajcArgs = new ArrayList<>();

    public ThinkingAnalyticsExtension includeJarFilter(String... filters) {
        if (filters != null) {
            includeJarFilter.addAll(filters)
        }

        return this
    }

    public ThinkingAnalyticsExtension excludeJarFilter(String... filters) {
        if (filters != null) {
            excludeJarFilter.addAll(filters)
        }

        return this
    }

    public ThinkingAnalyticsExtension ajcArgs(String... ajcArgs) {
        if (ajcArgs != null) {
            this.ajcArgs.addAll(ajcArgs)
        }
        return this
    }
}
