package cn.thinkingdata.android.plugin.hook

import cn.thinkingdata.android.plugin.config.ThinkingClassNameAnalytics
import cn.thinkingdata.android.plugin.config.ThinkingAnalyticsHookConfig
import cn.thinkingdata.android.plugin.config.ThinkingAnalyticsTransformHelper
import cn.thinkingdata.android.plugin.config.ThinkingFragmentHookConfig
import cn.thinkingdata.android.plugin.entity.ThinkingAnalyticsMethodCell
import cn.thinkingdata.android.plugin.utils.LoggerUtil
import cn.thinkingdata.android.plugin.utils.ThinkingAnalyticsUtil
import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.Handle
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type

/**
 * 插入数据埋点方法
 */
class ThinkingAnalyticsClassVisitor extends ClassVisitor {

    //类名
    private String mClassName
    //父类
    private String mSuperName
    //该类继承的接口
    private String[] mInterfaces

    private ClassVisitor classVisitor

    private ThinkingAnalyticsTransformHelper transformHelper

    //保存已有的fragment中重写的方法  用于补全fragment中的方法
    private HashSet<String> exitedFragMethods = new HashSet<>()

    //private ThinkingClassNameAnalytics classNameAnalytics

    private int version

    //用于解析出lambda表达式真正的方法信息 然后保存起来
    private HashMap<String, ThinkingAnalyticsMethodCell> mLambdaCells = new HashMap<>()

    ThinkingAnalyticsClassVisitor(final ClassVisitor classVisitor, ThinkingClassNameAnalytics classNameAnalytics, ThinkingAnalyticsTransformHelper transformHelper) {
        super(ThinkingAnalyticsUtil.ASM_VERSION, classVisitor)
        this.classVisitor = classVisitor
        this.transformHelper = transformHelper
        //this.classNameAnalytics = classNameAnalytics
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone()
    }

    @Override
    void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        mClassName = name
        mSuperName = superName
        mInterfaces = interfaces
        this.version = version
        super.visit(version, access, name, signature, superName, interfaces)
        LoggerUtil.info("开始扫描：${mClassName}")
        LoggerUtil.info("类详情：version=${version};\taccess=${LoggerUtil.accCode2String(access)};\tname=${name};\tsignature=${signature};\tsuperName=${superName};\tinterfaces=${interfaces.toArrayString()}")
    }

    @Override
    void visitEnd() {
        super.visitEnd()
        if (ThinkingAnalyticsUtil.isFragmentClass(mSuperName)) {
            MethodVisitor mv
            // 保证fragment需要的方法都已经调用
            Iterator<Map.Entry<String, ThinkingAnalyticsMethodCell>> it = ThinkingFragmentHookConfig.TA_FRAGMENT_METHODS.entrySet().iterator()
            while (it.hasNext()) {
                Map.Entry<String, ThinkingAnalyticsMethodCell> e = it.next()
                String key = e.getKey()
                ThinkingAnalyticsMethodCell methodCell = e.getValue()
                if (exitedFragMethods.contains(key)) {
                    continue
                }
                mv = classVisitor.visitMethod(Opcodes.ACC_PUBLIC, methodCell.name, methodCell.desc, null, null)
                mv.visitCode()
                visitMethodParams(mv, Opcodes.INVOKESPECIAL, mSuperName, methodCell.name, methodCell.desc, methodCell.paramsStart, methodCell.paramsCount, methodCell.opcodes)
                visitMethodParams(mv, Opcodes.INVOKESTATIC, ThinkingFragmentHookConfig.THINKING_FRAGMENT_TRACK_HELPER_API, methodCell.agentName, methodCell.agentDesc, methodCell.paramsStart, methodCell.paramsCount, methodCell.opcodes)
                mv.visitInsn(Opcodes.RETURN)
                mv.visitMaxs(methodCell.paramsCount, methodCell.paramsCount)
                mv.visitEnd()
            }
        }
        LoggerUtil.info("结束扫描类：${mClassName}\n")
    }

    @Override
    FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        return super.visitField(access, name, descriptor, signature, value)
    }

    @Override
    MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor methodVisitor = cv.visitMethod(access, name, desc, signature, exceptions)
        ThinkingAnalyticsDefaultMethodVisitor thinkingAnalyticsDefaultMethodVisitor = new ThinkingAnalyticsDefaultMethodVisitor(methodVisitor, access, name, desc) {
            //name拼接desc
            String nameDesc

            int variableID = 0
            boolean isOnClickMethod = false
            boolean isOnItemClickMethod = false

            String eventName = ""
            String eventProperties = ""
            String eventAppId = ""

            boolean isHasTracked = false

            //public 非静态
            boolean pubAndNoStaticAccess
            boolean protectedAndNotStaticAccess

            boolean isThinkingDataTrackViewOnClickAnnotation = false

            ArrayList<Integer> ids

            String appId = ""

            @Override
            void visitEnd() {
                super.visitEnd()
                if (isHasTracked) {
                    if (transformHelper.extension.lambdaEnabled && mLambdaCells.containsKey(nameDesc)) {
                        mLambdaCells.remove(nameDesc)
                    }
                }
            }

            @Override
            void visitInvokeDynamicInsn(String name1, String desc1, Handle bsm, Object... bsmArgs) {
                super.visitInvokeDynamicInsn(name1, desc1, bsm, bsmArgs)
                if (transformHelper.extension.lambdaEnabled) {
                    try {
                        String desc2 = (String) bsmArgs[0]
                        ThinkingAnalyticsMethodCell thinkingAnalyticsMethodCell = ThinkingAnalyticsHookConfig.LAMBDA_METHODS.get(Type.getReturnType(desc1).getDescriptor() + name1 + desc2)
                        if (thinkingAnalyticsMethodCell != null) {
                            Handle it = (Handle) bsmArgs[1]
                            mLambdaCells.put(it.name + it.desc, thinkingAnalyticsMethodCell)
                        }
                    } catch (Exception e) {
                        e.printStackTrace()
                    }
                }
            }

            @Override
            protected void onMethodEnter() {
                super.onMethodEnter()
                nameDesc = name + desc

                pubAndNoStaticAccess = ThinkingAnalyticsUtil.isPublic(access) && !ThinkingAnalyticsUtil.isStatic(access)
                protectedAndNotStaticAccess = ThinkingAnalyticsUtil.isProtected(access) && !ThinkingAnalyticsUtil.isStatic(access)

                if (pubAndNoStaticAccess) {
                    if ((nameDesc == 'onClick(Landroid/view/View;)V')) {
                        isOnClickMethod = true
                        variableID = newLocal(Type.getObjectType("java/lang/Integer"))
                        methodVisitor.visitVarInsn(ALOAD, 1)
                        methodVisitor.visitVarInsn(ASTORE, variableID)
                    } else if (nameDesc == 'onItemClick(Landroid/widget/AdapterView;Landroid/view/View;IJ)V') {
                        ids = new ArrayList<>()
                        isOnItemClickMethod = true

                        int first = newLocal(Type.getObjectType("android/widget/AdapterView"))
                        methodVisitor.visitVarInsn(ALOAD, 1)
                        methodVisitor.visitVarInsn(ASTORE, first)
                        ids.add(first)

                        int second = newLocal(Type.getObjectType("android/view/View"))
                        methodVisitor.visitVarInsn(ALOAD, 2)
                        methodVisitor.visitVarInsn(ASTORE, second)
                        ids.add(second)

                        int third = newLocal(Type.INT_TYPE)
                        methodVisitor.visitVarInsn(ILOAD, 3)
                        methodVisitor.visitVarInsn(ISTORE, third)
                        ids.add(third)
                    } else if (ThinkingAnalyticsUtil.isFragmentClass(mSuperName)
                            && ThinkingFragmentHookConfig.TA_FRAGMENT_METHODS.get(nameDesc) != null) {
                        ThinkingAnalyticsMethodCell thinkingAnalyticsMethodCell = ThinkingFragmentHookConfig.TA_FRAGMENT_METHODS.get(nameDesc)
                        ids = new ArrayList<>()
                        Type[] types = Type.getArgumentTypes(desc)
                        for (int i = 1; i < thinkingAnalyticsMethodCell.paramsCount; i++) {
                            int localId = newLocal(types[i - 1])
                            methodVisitor.visitVarInsn(thinkingAnalyticsMethodCell.opcodes.get(i), i)
                            methodVisitor.visitVarInsn(ThinkingAnalyticsUtil.convertOpcodes(thinkingAnalyticsMethodCell.opcodes.get(i)), localId)
                            ids.add(localId)
                        }
                    } else if (nameDesc == "onCheckedChanged(Landroid/widget/RadioGroup;I)V") {
                        ids = new ArrayList<>()
                        int firstLocalId = newLocal(Type.getObjectType("android/widget/RadioGroup"))
                        methodVisitor.visitVarInsn(ALOAD, 1)
                        methodVisitor.visitVarInsn(ASTORE, firstLocalId)
                        ids.add(firstLocalId)
                        int secondLocalId = newLocal(Type.INT_TYPE)
                        methodVisitor.visitVarInsn(ILOAD, 2)
                        methodVisitor.visitVarInsn(ISTORE, secondLocalId)
                        ids.add(secondLocalId)
                    } else if (nameDesc == "onCheckedChanged(Landroid/widget/CompoundButton;Z)V") {
                        ids = new ArrayList<>()
                        int firstLocalId = newLocal(Type.getObjectType("android/widget/CompoundButton"))
                        methodVisitor.visitVarInsn(ALOAD, 1)
                        methodVisitor.visitVarInsn(ASTORE, firstLocalId)
                        ids.add(firstLocalId)
                    } else if (nameDesc == "onClick(Landroid/content/DialogInterface;I)V") {
                        ids = new ArrayList<>()
                        int firstLocalId = newLocal(Type.getObjectType("android/content/DialogInterface"))
                        methodVisitor.visitVarInsn(ALOAD, 1)
                        methodVisitor.visitVarInsn(ASTORE, firstLocalId)
                        ids.add(firstLocalId)
                        int secondLocalId = newLocal(Type.INT_TYPE)
                        methodVisitor.visitVarInsn(ILOAD, 2)
                        methodVisitor.visitVarInsn(ISTORE, secondLocalId)
                        ids.add(secondLocalId)
                    } else if (ThinkingAnalyticsUtil.isMenuMethodDesc(nameDesc)) {
                        ids = new ArrayList<>()
                        int firstLocalId = newLocal(Type.getObjectType("java/lang/Object"))
                        methodVisitor.visitVarInsn(ALOAD, 0)
                        methodVisitor.visitVarInsn(ASTORE, firstLocalId)
                        ids.add(firstLocalId)
                        int secondLocalId = newLocal(Type.getObjectType("android/view/MenuItem"))
                        methodVisitor.visitVarInsn(ALOAD, 1)
                        methodVisitor.visitVarInsn(ASTORE, secondLocalId)
                        ids.add(secondLocalId)
                    } else if (nameDesc == "onMenuItemClick(Landroid/view/MenuItem;)Z") {
                        ids = new ArrayList<>()
                        int firstLocalId = newLocal(Type.getObjectType("android/view/MenuItem"))
                        methodVisitor.visitVarInsn(ALOAD, 1)
                        methodVisitor.visitVarInsn(ASTORE, firstLocalId)
                        ids.add(firstLocalId)
                    } else if (nameDesc == "onGroupClick(Landroid/widget/ExpandableListView;Landroid/view/View;IJ)Z") {
                        ids = new ArrayList<>()
                        int firstLocalId = newLocal(Type.getObjectType("android/widget/ExpandableListView"))
                        methodVisitor.visitVarInsn(ALOAD, 1)
                        methodVisitor.visitVarInsn(ASTORE, firstLocalId)
                        ids.add(firstLocalId)

                        int secondLocalId = newLocal(Type.getObjectType("android/view/View"))
                        methodVisitor.visitVarInsn(ALOAD, 2)
                        methodVisitor.visitVarInsn(ASTORE, secondLocalId)
                        ids.add(secondLocalId)

                        int thirdLocalId = newLocal(Type.INT_TYPE)
                        methodVisitor.visitVarInsn(ILOAD, 3)
                        methodVisitor.visitVarInsn(ISTORE, thirdLocalId)
                        ids.add(thirdLocalId)
                    } else if (nameDesc == "onChildClick(Landroid/widget/ExpandableListView;Landroid/view/View;IIJ)Z") {
                        ids = new ArrayList<>()
                        int firstLocalId = newLocal(Type.getObjectType("android/widget/ExpandableListView"))
                        methodVisitor.visitVarInsn(ALOAD, 1)
                        methodVisitor.visitVarInsn(ASTORE, firstLocalId)
                        ids.add(firstLocalId)

                        int secondLocalId = newLocal(Type.getObjectType("android/view/View"))
                        methodVisitor.visitVarInsn(ALOAD, 2)
                        methodVisitor.visitVarInsn(ASTORE, secondLocalId)
                        ids.add(secondLocalId)

                        int thirdLocalId = newLocal(Type.INT_TYPE)
                        methodVisitor.visitVarInsn(ILOAD, 3)
                        methodVisitor.visitVarInsn(ISTORE, thirdLocalId)
                        ids.add(thirdLocalId)

                        int fourthLocalId = newLocal(Type.INT_TYPE)
                        methodVisitor.visitVarInsn(ILOAD, 4)
                        methodVisitor.visitVarInsn(ISTORE, fourthLocalId)
                        ids.add(fourthLocalId)
                    } else if (nameDesc == "onItemSelected(Landroid/widget/AdapterView;Landroid/view/View;IJ)V"
                            || nameDesc == "onListItemClick(Landroid/widget/ListView;Landroid/view/View;IJ)V") {
                        ids = new ArrayList<>()
                        int firstLocalId = newLocal(Type.getObjectType("java/lang/Object"))
                        methodVisitor.visitVarInsn(ALOAD, 1)
                        methodVisitor.visitVarInsn(ASTORE, firstLocalId)
                        ids.add(firstLocalId)

                        int secondLocalId = newLocal(Type.getObjectType("android/view/View"))
                        methodVisitor.visitVarInsn(ALOAD, 2)
                        methodVisitor.visitVarInsn(ASTORE, secondLocalId)
                        ids.add(secondLocalId)

                        int thirdLocalId = newLocal(Type.INT_TYPE)
                        methodVisitor.visitVarInsn(ILOAD, 3)
                        methodVisitor.visitVarInsn(ISTORE, thirdLocalId)
                        ids.add(thirdLocalId)
                    } else if (nameDesc == "onStopTrackingTouch(Landroid/widget/SeekBar;)V") {
                        ids = new ArrayList<>()
                        int firstLocalId = newLocal(Type.getObjectType("android/widget/SeekBar"))
                        methodVisitor.visitVarInsn(ALOAD, 1)
                        methodVisitor.visitVarInsn(ASTORE, firstLocalId)
                        ids.add(firstLocalId)
                    } else if (nameDesc == "onRatingChanged(Landroid/widget/RatingBar;FZ)V") {
                        ids = new ArrayList<>()
                        int firstLocalId = newLocal(Type.getObjectType("android/widget/RatingBar"))
                        methodVisitor.visitVarInsn(ALOAD, 1)
                        methodVisitor.visitVarInsn(ASTORE, firstLocalId)
                        ids.add(firstLocalId)
                    }else if(nameDesc == "onTimeSet(Landroid/widget/TimePicker;II)V"){
                        ids = new ArrayList<>()
                        int firstLocalId = newLocal(Type.getObjectType("android/widget/TimePicker"))
                        methodVisitor.visitVarInsn(ALOAD, 1)
                        methodVisitor.visitVarInsn(ASTORE, firstLocalId)
                        ids.add(firstLocalId)
                    } else if (nameDesc == "onDateSet(Landroid/widget/DatePicker;III)V") {
                        ids = new ArrayList<>()
                        int firstLocalId = newLocal(Type.getObjectType("android/widget/DatePicker"))
                        methodVisitor.visitVarInsn(ALOAD, 1)
                        methodVisitor.visitVarInsn(ASTORE, firstLocalId)
                        ids.add(firstLocalId)
                    }
                } else if (protectedAndNotStaticAccess) {
                    if (nameDesc == "onListItemClick(Landroid/widget/ListView;Landroid/view/View;IJ)V") {
                        ids = new ArrayList<>()
                        int firstLocalId = newLocal(Type.getObjectType("java/lang/Object"))
                        methodVisitor.visitVarInsn(ALOAD, 1)
                        methodVisitor.visitVarInsn(ASTORE, firstLocalId)
                        ids.add(firstLocalId)

                        int secondLocalId = newLocal(Type.getObjectType("android/view/View"))
                        methodVisitor.visitVarInsn(ALOAD, 2)
                        methodVisitor.visitVarInsn(ASTORE, secondLocalId)
                        ids.add(secondLocalId)

                        int thirdLocalId = newLocal(Type.INT_TYPE)
                        methodVisitor.visitVarInsn(ILOAD, 3)
                        methodVisitor.visitVarInsn(ISTORE, thirdLocalId)
                        ids.add(thirdLocalId)
                    }
                }

                //适配Lambda
                if (transformHelper.extension.lambdaEnabled) {
                    ThinkingAnalyticsMethodCell lambdaMethodCell = mLambdaCells.get(nameDesc)
                    if (lambdaMethodCell != null) {
                        if (transformHelper.extension.lambdaParamOptimize || ThinkingAnalyticsHookConfig.SAMPLING_LAMBDA_METHODS.contains(lambdaMethodCell)) {
                            Type[] types = Type.getArgumentTypes(lambdaMethodCell.desc)
                            int length = types.length
                            Type[] lambdaTypes = Type.getArgumentTypes(desc)
                            int paramStart = lambdaTypes.length - length
                            if (paramStart < 0) {
                                return
                            } else {
                                for (int i = 0; i < length; i++) {
                                    if (lambdaTypes[paramStart + i].descriptor != types[i].descriptor) {
                                        return
                                    }
                                }
                            }
                            boolean isStaticMethod = ThinkingAnalyticsUtil.isStatic(access)
                            ids = new ArrayList<>()
                            for (int i = paramStart; i < paramStart + lambdaMethodCell.paramsCount; i++) {
                                int localId = newLocal(types[i - paramStart])
                                methodVisitor.visitVarInsn(lambdaMethodCell.opcodes.get(i - paramStart), getVisitPosition(lambdaTypes, i, isStaticMethod))
                                methodVisitor.visitVarInsn(ThinkingAnalyticsUtil.convertOpcodes(lambdaMethodCell.opcodes.get(i - paramStart)), localId)
                                ids.add(localId)
                            }
                        }
                    }
                }

                if (transformHelper.isAddOnMethodEnter) {
                    handleCode()
                }
            }

            @Override
            protected void onMethodExit(int opcode) {
                super.onMethodExit(opcode)
                if (!transformHelper.isAddOnMethodEnter) {
                    handleCode()
                }
            }

            void handleCode() {

                //在fragment的生命周期插入方法
                if (ThinkingAnalyticsUtil.isFragmentClass(mSuperName)) {
                    ThinkingAnalyticsMethodCell thinkingAnalyticsMethodCell = ThinkingFragmentHookConfig.TA_FRAGMENT_METHODS.get(nameDesc)
                    if (thinkingAnalyticsMethodCell != null) {
                        exitedFragMethods.add(nameDesc)
                        methodVisitor.visitVarInsn(ALOAD, 0)
                        for (int i = 1; i < thinkingAnalyticsMethodCell.paramsCount; i++) {
                            methodVisitor.visitVarInsn(thinkingAnalyticsMethodCell.opcodes.get(i), ids[i - 1])
                        }
                        methodVisitor.visitMethodInsn(INVOKESTATIC, ThinkingFragmentHookConfig.THINKING_FRAGMENT_TRACK_HELPER_API, thinkingAnalyticsMethodCell.agentName, thinkingAnalyticsMethodCell.agentDesc, false)
                        isHasTracked = true
                        return
                    }

                }

                if (transformHelper.extension.lambdaEnabled) {
                    ThinkingAnalyticsMethodCell lambdaMethodCell = mLambdaCells.get(nameDesc)
                    if (lambdaMethodCell != null) {
                        Type[] types = Type.getArgumentTypes(lambdaMethodCell.desc)
                        int length = types.length
                        Type[] lambdaTypes = Type.getArgumentTypes(desc)
                        int paramStart = lambdaTypes.length - length
                        if (paramStart < 0) {
                            return
                        } else {
                            for (int i = 0; i < length; i++) {
                                if (lambdaTypes[paramStart + i].descriptor != types[i].descriptor) {
                                    return
                                }
                            }
                        }
                        boolean isStaticMethod = ThinkingAnalyticsUtil.isStatic(access)
                        if (!isStaticMethod) {
                            if (lambdaMethodCell.desc == '(Landroid/view/MenuItem;)Z') {
                                methodVisitor.visitVarInsn(Opcodes.ALOAD, 0)
                                methodVisitor.visitVarInsn(Opcodes.ALOAD, getVisitPosition(lambdaTypes, paramStart, isStaticMethod))
                                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, ThinkingAnalyticsHookConfig.THINKING_ANALYTICS_API, lambdaMethodCell.agentName, '(Ljava/lang/Object;Landroid/view/MenuItem;)V', false)
                                isHasTracked = true
                                return
                            }
                        }
                        if (transformHelper.extension.lambdaParamOptimize || ThinkingAnalyticsHookConfig.SAMPLING_LAMBDA_METHODS.contains(lambdaMethodCell)) {
                            for (int i = paramStart; i < paramStart + lambdaMethodCell.paramsCount; i++) {
                                methodVisitor.visitVarInsn(lambdaMethodCell.opcodes.get(i - paramStart), ids[i - paramStart])
                            }
                        } else {
                            for (int i = paramStart; i < paramStart + lambdaMethodCell.paramsCount; i++) {
                                methodVisitor.visitVarInsn(lambdaMethodCell.opcodes.get(i - paramStart), getVisitPosition(lambdaTypes, i, isStaticMethod))
                            }
                        }
                        methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, ThinkingAnalyticsHookConfig.THINKING_ANALYTICS_API, lambdaMethodCell.agentName, lambdaMethodCell.agentDesc, false)
                        isHasTracked = true
                        return
                    }
                }

                if (!pubAndNoStaticAccess) {
                    if (protectedAndNotStaticAccess) {
                        if (nameDesc == "onListItemClick(Landroid/widget/ListView;Landroid/view/View;IJ)V") {
                            methodVisitor.visitVarInsn(ALOAD, ids.get(0))
                            methodVisitor.visitVarInsn(ALOAD, ids.get(1))
                            methodVisitor.visitVarInsn(ILOAD, ids.get(2))
                            methodVisitor.visitMethodInsn(INVOKESTATIC, ThinkingAnalyticsHookConfig.THINKING_ANALYTICS_API, "trackListView", "(Landroid/widget/AdapterView;Landroid/view/View;I)V", false)
                            isHasTracked = true
                            return
                        }
                    }
                }

                /**
                 * onContextItemSelected  onOptionsItemSelected
                 */
                if (ThinkingAnalyticsUtil.isMenuMethodDesc(nameDesc)) {
                    methodVisitor.visitVarInsn(ALOAD, ids[0])
                    methodVisitor.visitVarInsn(ALOAD, ids[1])
                    methodVisitor.visitMethodInsn(INVOKESTATIC, ThinkingAnalyticsHookConfig.THINKING_ANALYTICS_API, "trackMenuItem", "(Ljava/lang/Object;Landroid/view/MenuItem;)V", false)
                    isHasTracked = true
                    return
                }

                if (isOnClickMethod && mClassName == 'android/databinding/generated/callback/OnClickListener') {
                    trackViewOnClick(methodVisitor, 1)
                    isHasTracked = true
                    return
                }

                if (!ThinkingAnalyticsUtil.isSpecialClass(mClassName)) {
                    if ((mClassName.startsWith('android/') || mClassName.startsWith('androidx/')) && !(mClassName.startsWith("android/support/v17/leanback") || mClassName.startsWith("androidx/leanback"))) {
                        return
                    }
                }

                if (nameDesc == 'onItemSelected(Landroid/widget/AdapterView;Landroid/view/View;IJ)V' || nameDesc == "onListItemClick(Landroid/widget/ListView;Landroid/view/View;IJ)V") {
                    methodVisitor.visitVarInsn(ALOAD, ids.get(0))
                    methodVisitor.visitVarInsn(ALOAD, ids.get(1))
                    methodVisitor.visitVarInsn(ILOAD, ids.get(2))
                    methodVisitor.visitMethodInsn(INVOKESTATIC, ThinkingAnalyticsHookConfig.THINKING_ANALYTICS_API, "trackListView", "(Landroid/widget/AdapterView;Landroid/view/View;I)V", false)
                    isHasTracked = true
                    return
                }

                //在xml中用
                if (isThinkingDataTrackViewOnClickAnnotation && desc == '(Landroid/view/View;)V') {
                    trackViewOnClick(methodVisitor, 1)
                    isHasTracked = true
                    return
                }

                //被注解的事件
                if (eventName != null && eventName.length() != 0) {
                    methodVisitor.visitLdcInsn(eventName)
                    methodVisitor.visitLdcInsn(eventProperties)
                    methodVisitor.visitLdcInsn(eventAppId)
                    methodVisitor.visitMethodInsn(INVOKESTATIC, ThinkingAnalyticsHookConfig.THINKING_ANALYTICS_API, "track", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V", false)
                    isHasTracked = true
                    return
                }
                if (mInterfaces != null && mInterfaces.length > 0) {
                    if (isOnItemClickMethod && mInterfaces.contains('android/widget/AdapterView$OnItemClickListener')) {
                        methodVisitor.visitVarInsn(ALOAD, ids.get(0))
                        methodVisitor.visitVarInsn(ALOAD, ids.get(1))
                        methodVisitor.visitVarInsn(ILOAD, ids.get(2))
                        methodVisitor.visitMethodInsn(INVOKESTATIC, ThinkingAnalyticsHookConfig.THINKING_ANALYTICS_API, "trackListView", "(Landroid/widget/AdapterView;Landroid/view/View;I)V", false)
                        isHasTracked = true
                        return
                    } else if (mInterfaces.contains('android/widget/RadioGroup$OnCheckedChangeListener')
                            && nameDesc == 'onCheckedChanged(Landroid/widget/RadioGroup;I)V') {
                        ThinkingAnalyticsMethodCell thinkingAnalyticsMethodCell = ThinkingAnalyticsHookConfig.INTERFACE_METHODS
                                .get('android/widget/RadioGroup$OnCheckedChangeListeneronCheckedChanged(Landroid/widget/RadioGroup;I)V')
                        if (thinkingAnalyticsMethodCell != null) {
                            methodVisitor.visitVarInsn(ALOAD, ids.get(0))
                            methodVisitor.visitVarInsn(ILOAD, ids.get(1))
                            methodVisitor.visitLdcInsn(appId)
                            methodVisitor.visitMethodInsn(INVOKESTATIC, ThinkingAnalyticsHookConfig.THINKING_ANALYTICS_API, thinkingAnalyticsMethodCell.agentName, thinkingAnalyticsMethodCell.agentDesc, false)
                            isHasTracked = true
                            return
                        }
                    } else if (mInterfaces.contains('android/widget/CompoundButton$OnCheckedChangeListener')
                            && nameDesc == 'onCheckedChanged(Landroid/widget/CompoundButton;Z)V') {
                        ThinkingAnalyticsMethodCell thinkingAnalyticsMethodCell = ThinkingAnalyticsHookConfig.INTERFACE_METHODS
                                .get('android/widget/CompoundButton$OnCheckedChangeListeneronCheckedChanged(Landroid/widget/CompoundButton;Z)V')
                        if (thinkingAnalyticsMethodCell != null) {
                            methodVisitor.visitVarInsn(ALOAD, ids.get(0))
                            methodVisitor.visitLdcInsn(appId)
                            methodVisitor.visitMethodInsn(INVOKESTATIC, ThinkingAnalyticsHookConfig.THINKING_ANALYTICS_API, thinkingAnalyticsMethodCell.agentName, thinkingAnalyticsMethodCell.agentDesc, false)
                            isHasTracked = true
                            return
                        }
                    } else if (mInterfaces.contains('android/content/DialogInterface$OnClickListener')
                            && nameDesc == 'onClick(Landroid/content/DialogInterface;I)V') {
                        ThinkingAnalyticsMethodCell thinkingAnalyticsMethodCell = ThinkingAnalyticsHookConfig.INTERFACE_METHODS
                                .get('android/content/DialogInterface$OnClickListeneronClick(Landroid/content/DialogInterface;I)V')
                        if (thinkingAnalyticsMethodCell != null) {
                            methodVisitor.visitVarInsn(ALOAD, ids.get(0))
                            methodVisitor.visitVarInsn(ILOAD, ids.get(1))
                            methodVisitor.visitMethodInsn(INVOKESTATIC, ThinkingAnalyticsHookConfig.THINKING_ANALYTICS_API, thinkingAnalyticsMethodCell.agentName, thinkingAnalyticsMethodCell.agentDesc, false)
                            isHasTracked = true
                            return
                        }
                    } else if (mInterfaces.contains('android/widget/ExpandableListView$OnGroupClickListener')
                            && nameDesc == 'onGroupClick(Landroid/widget/ExpandableListView;Landroid/view/View;IJ)Z') {
                        ThinkingAnalyticsMethodCell thinkingAnalyticsMethodCell = ThinkingAnalyticsHookConfig.INTERFACE_METHODS
                                .get('android/widget/ExpandableListView$OnGroupClickListeneronGroupClick(Landroid/widget/ExpandableListView;Landroid/view/View;IJ)Z')
                        if (thinkingAnalyticsMethodCell != null) {
                            methodVisitor.visitVarInsn(ALOAD, ids.get(0))
                            methodVisitor.visitVarInsn(ALOAD, ids.get(1))
                            methodVisitor.visitVarInsn(ILOAD, ids.get(2))
                            methodVisitor.visitMethodInsn(INVOKESTATIC, ThinkingAnalyticsHookConfig.THINKING_ANALYTICS_API, thinkingAnalyticsMethodCell.agentName, thinkingAnalyticsMethodCell.agentDesc, false)
                            isHasTracked = true
                            return
                        }
                    } else if (mInterfaces.contains('android/widget/ExpandableListView$OnChildClickListener')
                            && nameDesc == 'onChildClick(Landroid/widget/ExpandableListView;Landroid/view/View;IIJ)Z') {
                        ThinkingAnalyticsMethodCell thinkingAnalyticsMethodCell = ThinkingAnalyticsHookConfig.INTERFACE_METHODS
                                .get('android/widget/ExpandableListView$OnChildClickListeneronChildClick(Landroid/widget/ExpandableListView;Landroid/view/View;IIJ)Z')
                        if (thinkingAnalyticsMethodCell != null) {
                            methodVisitor.visitVarInsn(ALOAD, ids.get(0))
                            methodVisitor.visitVarInsn(ALOAD, ids.get(1))
                            methodVisitor.visitVarInsn(ILOAD, ids.get(2))
                            methodVisitor.visitVarInsn(ILOAD, ids.get(3))
                            methodVisitor.visitMethodInsn(INVOKESTATIC, ThinkingAnalyticsHookConfig.THINKING_ANALYTICS_API, thinkingAnalyticsMethodCell.agentName, thinkingAnalyticsMethodCell.agentDesc, false)
                            isHasTracked = true
                            return
                        }
                    } else if (nameDesc == 'onMenuItemClick(Landroid/view/MenuItem;)Z') {
                        for (interfaceName in mInterfaces) {
                            ThinkingAnalyticsMethodCell thinkingAnalyticsMethodCell = ThinkingAnalyticsHookConfig.INTERFACE_METHODS.get(interfaceName + nameDesc)
                            if (thinkingAnalyticsMethodCell != null) {
                                methodVisitor.visitVarInsn(ALOAD, ids.get(0))
                                methodVisitor.visitMethodInsn(INVOKESTATIC, ThinkingAnalyticsHookConfig.THINKING_ANALYTICS_API, thinkingAnalyticsMethodCell.agentName, thinkingAnalyticsMethodCell.agentDesc, false)
                                isHasTracked = true
                                return
                            }
                        }
                    } else if (mInterfaces.contains('android/widget/SeekBar$OnSeekBarChangeListener')
                            && nameDesc == 'onStopTrackingTouch(Landroid/widget/SeekBar;)V') {
                        ThinkingAnalyticsMethodCell thinkingAnalyticsMethodCell = ThinkingAnalyticsHookConfig.INTERFACE_METHODS
                                .get('android/widget/SeekBar$OnSeekBarChangeListeneronStopTrackingTouch(Landroid/widget/SeekBar;)V')
                        if (thinkingAnalyticsMethodCell != null) {
                            methodVisitor.visitVarInsn(ALOAD, ids.get(0))
                            methodVisitor.visitLdcInsn(appId)
                            methodVisitor.visitMethodInsn(INVOKESTATIC, ThinkingAnalyticsHookConfig.THINKING_ANALYTICS_API, thinkingAnalyticsMethodCell.agentName, thinkingAnalyticsMethodCell.agentDesc, false)
                            isHasTracked = true
                            return
                        }
                    } else if (mInterfaces.contains('android/widget/RatingBar$OnRatingBarChangeListener')
                            && nameDesc == 'onRatingChanged(Landroid/widget/RatingBar;FZ)V') {
                        ThinkingAnalyticsMethodCell thinkingAnalyticsMethodCell = ThinkingAnalyticsHookConfig.INTERFACE_METHODS
                                .get('android/widget/RatingBar$OnRatingBarChangeListeneronRatingChanged(Landroid/widget/RatingBar;FZ)V')
                        if (thinkingAnalyticsMethodCell != null) {
                            methodVisitor.visitVarInsn(ALOAD, ids.get(0))
                            methodVisitor.visitLdcInsn(appId)
                            methodVisitor.visitMethodInsn(INVOKESTATIC, ThinkingAnalyticsHookConfig.THINKING_ANALYTICS_API, thinkingAnalyticsMethodCell.agentName, thinkingAnalyticsMethodCell.agentDesc, false)
                            isHasTracked = true
                            return
                        }
                    }else if(mInterfaces.contains('android/app/TimePickerDialog$OnTimeSetListener')
                            && nameDesc == 'onTimeSet(Landroid/widget/TimePicker;II)V'){
                        ThinkingAnalyticsMethodCell thinkingAnalyticsMethodCell = ThinkingAnalyticsHookConfig.INTERFACE_METHODS
                                .get('android/app/TimePickerDialog$OnTimeSetListeneronTimeSet(Landroid/widget/TimePicker;II)V')
                        if (thinkingAnalyticsMethodCell != null) {
                            methodVisitor.visitVarInsn(ALOAD, ids.get(0))
                            methodVisitor.visitLdcInsn(appId)
                            methodVisitor.visitMethodInsn(INVOKESTATIC, ThinkingAnalyticsHookConfig.THINKING_ANALYTICS_API, thinkingAnalyticsMethodCell.agentName, thinkingAnalyticsMethodCell.agentDesc, false)
                            isHasTracked = true
                            return
                        }
                    } else if (mInterfaces.contains('android/app/DatePickerDialog$OnDateSetListener')
                            && nameDesc == 'onDateSet(Landroid/widget/DatePicker;III)V') {
                        ThinkingAnalyticsMethodCell thinkingAnalyticsMethodCell = ThinkingAnalyticsHookConfig.INTERFACE_METHODS
                                .get('android/app/DatePickerDialog$OnDateSetListeneronDateSet(Landroid/widget/DatePicker;III)V')
                        if (thinkingAnalyticsMethodCell != null) {
                            methodVisitor.visitVarInsn(ALOAD, ids.get(0))
                            methodVisitor.visitLdcInsn(appId)
                            methodVisitor.visitMethodInsn(INVOKESTATIC, ThinkingAnalyticsHookConfig.THINKING_ANALYTICS_API, thinkingAnalyticsMethodCell.agentName, thinkingAnalyticsMethodCell.agentDesc, false)
                            isHasTracked = true
                            return
                        }
                    } else {
                        for (interfaceName in mInterfaces) {
                            ThinkingAnalyticsMethodCell thinkingAnalyticsMethodCell = ThinkingAnalyticsHookConfig.INTERFACE_METHODS.get(interfaceName + nameDesc)
                            if (thinkingAnalyticsMethodCell != null) {
                                visitMethodParams(methodVisitor, INVOKESTATIC, ThinkingAnalyticsHookConfig.THINKING_ANALYTICS_API, thinkingAnalyticsMethodCell.agentName, thinkingAnalyticsMethodCell.agentDesc, thinkingAnalyticsMethodCell.paramsStart, thinkingAnalyticsMethodCell.paramsCount, thinkingAnalyticsMethodCell.opcodes)
                                isHasTracked = true
                                return
                            }
                        }
                    }
                }
                if (isOnClickMethod) {
                    trackViewOnClick(methodVisitor, variableID)
                    isHasTracked = true
                }

            }

            void trackViewOnClick(MethodVisitor mv, int index) {
                mv.visitVarInsn(ALOAD, index)
                mv.visitLdcInsn(appId)
                mv.visitMethodInsn(INVOKESTATIC, ThinkingAnalyticsHookConfig.THINKING_ANALYTICS_API, "trackViewOnClick", "(Landroid/view/View;Ljava/lang/String;)V", false)
            }


            @Override
            void visitFieldInsn(int opcode, String owner, String fieldName, String fieldDesc) {
                super.visitFieldInsn(opcode, owner, fieldName, fieldDesc)
            }

            @Override
            AnnotationVisitor visitAnnotation(String s, boolean b) {
                if (s == "Lcn/thinkingdata/android/ThinkingDataIgnoreTrackOnClick;") {
                    //忽略点击事件
                    appId = "1_"
                    return new AnnotationVisitor(ThinkingAnalyticsUtil.ASM_VERSION) {
                        @Override
                        void visit(String key, Object value) {
                            super.visit(key, value)
                            if ("appId" == key) {
                                appId = "1_" + (String) value
                            }
                        }
                    }
                } else if (s == "Lcn/thinkingdata/android/ThinkingDataTrackViewOnClick;") {
                    //在xml中写onclick方法 用ThinkingDataTrackViewOnClick注解
                    isThinkingDataTrackViewOnClickAnnotation = true
                    appId = "2_"
                    return new AnnotationVisitor(ThinkingAnalyticsUtil.ASM_VERSION) {
                        @Override
                        void visit(String key, Object value) {
                            super.visit(key, value)
                            if ("appId" == key) {
                                appId = "2_" + (String) value
                            }
                        }
                    }
                } else if (s == "Lcn/thinkingdata/android/ThinkingDataTrackEvent;") {
                    return new AnnotationVisitor(ThinkingAnalyticsUtil.ASM_VERSION) {
                        @Override
                        void visit(String key, Object value) {
                            super.visit(key, value)
                            if ("eventName" == key) {
                                eventName = (String) value
                            } else if ("properties" == key) {
                                eventProperties = value.toString()
                            } else if ("appId" == key) {
                                eventAppId = (String) value
                            }
                        }
                    }
                }
                return super.visitAnnotation(s, b)
            }
        }
        //适配java老版本
        if (version <= Opcodes.V1_5) {
            return new ThinkingAnalyticsJSRAdapter(ThinkingAnalyticsUtil.ASM_VERSION, thinkingAnalyticsDefaultMethodVisitor, access, name, desc, signature, exceptions)
        }
        return thinkingAnalyticsDefaultMethodVisitor
    }

    /**
     * 获取相应方法参数的下标
     */
    int getVisitPosition(Type[] types, int index, boolean isStaticMethod) {
        if (types == null || index < 0 || index >= types.length) {
            throw new Error("get position error")
        }
        if (index == 0) {
            return isStaticMethod ? 0 : 1
        } else {
            return getVisitPosition(types, index - 1, isStaticMethod) + types[index - 1].getSize()
        }
    }

    private static void visitMethodParams(MethodVisitor methodVisitor, int opcode, String owner, String methodName, String methodDesc, int start, int count, List<Integer> paramOpcodes) {
        for (int i = start; i < start + count; i++) {
            methodVisitor.visitVarInsn(paramOpcodes[i - start], i)
        }
        methodVisitor.visitMethodInsn(opcode, owner, methodName, methodDesc, false)
    }

}