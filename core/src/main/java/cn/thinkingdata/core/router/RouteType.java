package cn.thinkingdata.core.router;

public enum RouteType {

    PROVIDER,
    PLUGIN,
    UNKNOWN;

    public static RouteType parse(int type) {
        switch (type) {
            case 0:
                return PROVIDER;
            case 1:
                return PLUGIN;
            default:
                break;
        }
        return UNKNOWN;
    }
}
