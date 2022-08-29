package cn.thinkingdata.android.plugin.hook;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.JSRInlinerAdapter;

/**
 * 适配java 1.5以前的版本
 */
public class ThinkingAnalyticsJSRAdapter extends JSRInlinerAdapter {

    protected ThinkingAnalyticsJSRAdapter(int api, MethodVisitor mv, int access, String name, String desc, String signature, String[] exceptions) {
        super(api, mv, access, name, desc, signature, exceptions);
    }

}
