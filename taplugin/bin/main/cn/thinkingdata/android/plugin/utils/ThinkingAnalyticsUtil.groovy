package cn.thinkingdata.android.plugin.utils

import cn.thinkingdata.android.plugin.config.ThinkingAnalyticsTransformHelper
import groovy.transform.CompileStatic
import org.apache.commons.io.IOUtils
import org.apache.commons.io.output.ByteArrayOutputStream
import org.objectweb.asm.Opcodes

@CompileStatic
class ThinkingAnalyticsUtil {
    public static final int ASM_VERSION = Opcodes.ASM7
    private static final HashSet<String> fragmentClass = new HashSet()
    private static final HashSet<String> menuMethodDesc = new HashSet()
    private static final HashSet<String> sClass = new HashSet()
    private static final HashSet<String> activityClass = new HashSet<>()

    static {

        menuMethodDesc.add("onContextItemSelected(Landroid/view/MenuItem;)Z")
        menuMethodDesc.add("onOptionsItemSelected(Landroid/view/MenuItem;)Z")

        fragmentClass.add('android/app/Fragment')
        fragmentClass.add('android/app/ListFragment')
        fragmentClass.add('android/app/DialogFragment')

        fragmentClass.add('android/support/v4/app/Fragment')
        fragmentClass.add('android/support/v4/app/ListFragment')
        fragmentClass.add('android/support/v4/app/DialogFragment')

        fragmentClass.add('androidx/fragment/app/Fragment')
        fragmentClass.add('androidx/fragment/app/ListFragment')
        fragmentClass.add('androidx/fragment/app/DialogFragment')

        activityClass.add('android/app/Activity')
        activityClass.add('android/support/v7/app/AppCompatActivity')
        activityClass.add('androidx/appcompat/app/AppCompatActivity')
        activityClass.add('android/support/v7/app/FragmentActivity')
        activityClass.add('androidx/fragment/app/FragmentActivity')

        for (className in ThinkingAnalyticsTransformHelper.internal) {
            sClass.add(className.replace('.', '/'))
        }

    }

    /**
     * 是否是public方法
     * @param access
     * @return
     */
    static boolean isPublic(int access) {
        return (access & Opcodes.ACC_PUBLIC) != 0
    }

    /**
     * 是否是static方法
     * @param access
     * @return
     */
    static boolean isStatic(int access) {
        return (access & Opcodes.ACC_STATIC) != 0
    }

    /**
     * 是否是protected方法
     * @param access
     * @return
     */
    static boolean isProtected(int access) {
        return (access & Opcodes.ACC_PROTECTED) != 0
    }

    static boolean isMenuMethodDesc(String nameDesc) {
        return menuMethodDesc.contains(nameDesc)
    }

    static boolean isFragmentClass(String superName) {
        return fragmentClass.contains(superName)
    }

    static boolean isSpecialClass(String className) {
        return sClass.contains(className)
    }

    /**
     * 比较SDK版本大小
     */
    static int compareVersion(String version1, String version2) {
        def v1Array = version1.replace("-pre", "").split("\\.")
        def v2Array = version2.replace("-pre", "").split("\\.")
        def maxLength = Math.max(v1Array.length, v2Array.length)
        String str1, str2
        for (int index = 0; index < maxLength; index++) {
            if (v1Array.length > index) {
                str1 = v1Array[index]
            } else {
                return -1
            }
            if (v2Array.length > index) {
                str2 = v2Array[index]
            } else {
                return 1
            }
            if (str1 != null && str2 != null) {
                try {
                    int num1 = Integer.valueOf(str1)
                    int num2 = Integer.valueOf(str2)
                    if (num1 != num2) {
                        return num1 - num2 > 0 ? 1 : -1
                    }
                } catch (Exception ignored) {
                    return str1 <=> str2
                }
            }
        }
        return 0
    }

    /**
     * LOAD STORE 指令
     */
    static int convertOpcodes(int code) {
        int r = code
        switch (code) {
            case Opcodes.ILOAD:
                r = Opcodes.ISTORE
                break
            case Opcodes.ALOAD:
                r = Opcodes.ASTORE
                break
            case Opcodes.LLOAD:
                r = Opcodes.LSTORE
                break
            case Opcodes.FLOAD:
                r = Opcodes.FSTORE
                break
            case Opcodes.DLOAD:
                r = Opcodes.DSTORE
                break
            case Opcodes.ISTORE:
                r = Opcodes.ILOAD
                break
            case Opcodes.ASTORE:
                r = Opcodes.ALOAD
                break
            case Opcodes.LSTORE:
                r = Opcodes.LLOAD
                break
            case Opcodes.FSTORE:
                r = Opcodes.FLOAD
                break
            case Opcodes.DSTORE:
                r = Opcodes.DLOAD
                break
        }
        return r
    }

    static boolean isActivityClass(String superName) {
        return activityClass.contains(superName)
    }

    static String appendDescBeforeGiven(String givenDesc, String appendDesc) {
        return givenDesc.replaceFirst("\\(", "(" + appendDesc);
    }

    static byte[] toByteArrayStream(InputStream input) throws Exception {
        ByteArrayOutputStream os = null
        try {
            os = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024 * 4]
            int n = 0
            while (-1 != (n = input.read(buffer))) {
                os.write(buffer, 0, n)
            }
            os.flush()
            return os.toByteArray()
        } catch (Exception e) {
            throw e
        } finally {
            IOUtils.closeQuietly(os)
            IOUtils.closeQuietly(input)
        }
    }

}