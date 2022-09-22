package cn.thinkingdata.android.plugin.utils

import org.objectweb.asm.Opcodes

import java.lang.reflect.Field
import java.util.concurrent.ConcurrentHashMap

/**
 *  日志打印工具类
 */
class LoggerUtil {
    private static boolean debug = true

    private static String ERROR_UI = "\033[40;31m"
    private static String NORMAL_UI = "\033[0m"

    /**
     * 设置是否打印日志
     */
    static void setDebug(boolean isDebug) {
        debug = isDebug
    }

    static boolean isDebug() {
        return debug
    }

    def static error(Object msg) {
        try {
            println("${ERROR_UI}[ThinkingAnalytics]: ${msg}${NORMAL_UI}")
        } catch (Exception e) {
            e.printStackTrace()
        }
    }

    /**
     * 打印日志
     */
    def static info(Object msg) {
        if (debug)
            try {
                println "[ThinkingAnalytics]: ${msg}"
            } catch (Exception e) {
                e.printStackTrace()
            }
    }

    public static ConcurrentHashMap<Integer, String> acMap = new ConcurrentHashMap<>()
    public static ConcurrentHashMap<Integer, String> ocMap = new ConcurrentHashMap<>()

    static String accCode2String(int access) {
        def sb = new StringBuilder()
        def map = getMapOfAcc()
        map.each { key, value ->
            if ((key.intValue() & access) > 0) {
                sb.append(value + ' ')
            }
        }
        return sb.toString()
    }

    private static Map<Integer, String> getMapOfAcc() {
        if (acMap.isEmpty()) {
            Field[] fields = Opcodes.class.getDeclaredFields()
            HashMap<Integer, String> tmpMap = [:]
            fields.each {
                if (it.name.startsWith("ACC_")) {
                    if (it.type == Integer.class) {
                        tmpMap[it.get(null) as Integer] = it.name
                    } else {
                        tmpMap[it.getInt(null)] = it.name
                    }
                }
            }
            acMap.putAll(tmpMap)
        }
        return acMap
    }

    static Map<Integer, String> getOpMap() {
        if (ocMap.size() == 0) {
            HashMap<String, Integer> map = [:]
            Field[] fields = Opcodes.class.getDeclaredFields()
            fields.each {
                if (it.type == Integer.class) {
                    map[it.get(null) as Integer] = it.name
                } else {
                    map[it.getInt(null)] = it.name
                }
            }
            ocMap.putAll(map)
        }
        return ocMap
    }
}