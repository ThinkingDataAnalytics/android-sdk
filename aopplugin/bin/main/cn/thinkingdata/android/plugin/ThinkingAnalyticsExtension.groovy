package cn.thinkingdata.android.plugin;

class ThinkingAnalyticsExtension {
    List<String> includes = new ArrayList<>()
    List<String> excludes = new ArrayList<>()

    List<String> ajcArgs=new ArrayList<>()

    boolean enabled = true


    ThinkingAnalyticsExtension include(String...filters) {
        if (filters != null) {
            this.includes.addAll(filters)
        }

        return this
    }

    ThinkingAnalyticsExtension exclude(String...filters) {
        if (filters != null) {
            this.excludes.addAll(filters)
        }

        return this
    }

    ThinkingAnalyticsExtension ajcArgs(String...ajcArgs) {
        if (ajcArgs != null) {
            this.ajcArgs.addAll(ajcArgs)
        }

        return this
    }
}
