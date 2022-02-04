package cn.thinkingdata.android.plugin.hook

import cn.thinkingdata.android.plugin.utils.LoggerUtil
import cn.thinkingdata.android.plugin.utils.ThinkingAnalyticsUtil
import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.Attribute
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.commons.AdviceAdapter

/**
 * MethodVisitor基类 继承AdviceAdapter
 */
class ThinkingAnalyticsDefaultMethodVisitor extends AdviceAdapter {

    String methodName

    ThinkingAnalyticsDefaultMethodVisitor(MethodVisitor mv, int access, String name, String desc) {
        super(ThinkingAnalyticsUtil.ASM_VERSION, mv, access, name, desc)
        methodName = name
        LoggerUtil.info("开始扫描方法：${LoggerUtil.accCode2String(access)} ${methodName}${desc}")
    }

    @Override
    void visitCode() {
        super.visitCode()
    }

    @Override
    void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
        super.visitMethodInsn(opcode, owner, name, desc, itf)
    }

    @Override
    void visitAttribute(Attribute attribute) {
        super.visitAttribute(attribute)
    }

    @Override
    void visitEnd() {
        LoggerUtil.info("结束扫描方法：${methodName}")
        super.visitEnd()
    }

    @Override
    void visitFieldInsn(int opcode, String owner, String name, String desc) {
        super.visitFieldInsn(opcode, owner, name, desc)
    }

    @Override
    void visitIincInsn(int var, int increment) {
        super.visitIincInsn(var, increment)
    }

    @Override
    void visitIntInsn(int i, int i1) {
        super.visitIntInsn(i, i1)
    }

    @Override
    void visitMaxs(int maxStack, int maxLocals) {
        super.visitMaxs(maxStack, maxLocals)
    }

    @Override
    void visitVarInsn(int opcode, int var) {
        super.visitVarInsn(opcode, var)
    }

    @Override
    void visitJumpInsn(int opcode, Label label) {
        super.visitJumpInsn(opcode, label)
    }

    @Override
    void visitLookupSwitchInsn(Label label, int[] ints, Label[] labels) {
        super.visitLookupSwitchInsn(label, ints, labels)
    }

    @Override
    void visitMultiANewArrayInsn(String s, int i) {
        super.visitMultiANewArrayInsn(s, i)
    }

    @Override
    void visitTableSwitchInsn(int i, int i1, Label label, Label[] labels) {
        super.visitTableSwitchInsn(i, i1, label, labels)
    }

    @Override
    void visitTryCatchBlock(Label label, Label label1, Label label2, String s) {
        super.visitTryCatchBlock(label, label1, label2, s)
    }

    @Override
    void visitTypeInsn(int opcode, String s) {
        super.visitTypeInsn(opcode, s)
    }

    @Override
    void visitLocalVariable(String s, String s1, String s2, Label label, Label label1, int i) {
        super.visitLocalVariable(s, s1, s2, label, label1, i)
    }

    @Override
    void visitInsn(int opcode) {
        super.visitInsn(opcode)
    }

    @Override
    AnnotationVisitor visitAnnotation(String s, boolean b) {
        return super.visitAnnotation(s, b)
    }

    @Override
    protected void onMethodEnter() {
        super.onMethodEnter()
    }

    @Override
    protected void onMethodExit(int opcode) {
        super.onMethodExit(opcode)
    }
}