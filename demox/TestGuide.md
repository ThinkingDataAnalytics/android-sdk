## 一键运行
--- goTest.sh ---
终端执行goTest.sh 会自动执行功能测试和单元测试，执行前请参考二者的prepare设置  
每条测试用例执行完会自动上报结果到测试环境服务器的测试报告项目中，可查阅明细
本地会生成对应的测试报告


## FunctionTest
--- prepare ---
注释TDTracker里initThinkingDataSDK(Context context)方法的setUp方法、enable方法以及其他track事件的调用
只保留ThinkingAnalyticsSDK初始化的代码
移除其他诸如MainActivity或者Application中的SDK初始化调用
设置手机或者模拟器为非wifi状态，建议使用google原生机器或者模拟器，国内定制机器会有安装提示，可能阻断流程

--- 手动运行 ---
右键FunctionTest.java选择Run   [src/androidTest/java/cn/thinkingdata/android/FunctionTest.java]

--- 备注 ---
设置上报网络限制暂未测试

## UnitTest

--- 🈚️ ---

测试报告地址
build/test-results/testDebugUnitTest/TEST-cn.thinkingdata.android.UnitTest.xml

