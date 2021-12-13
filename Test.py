#!/usr/bin/env python
#-*- coding:UTF-8 -*-（添加）
import os
import webbrowser
isExit = False;
androidSDKPath = os.getcwd();
# iOSSDKPath = '/Users/halewang/Desktop/SDk/ios-sdk'

def AndroidUnitTest():
    print('***************开始安卓单元测试***************')
    os.chdir(androidSDKPath)
    os.system('pwd')
    os.system('./gradlew demox:testReleaseUnitTest')
    webbrowser.open_new_tab('file:///'+androidSDKPath+'/demox/build/test-results/testReleaseUnitTest/TEST-cn.thinkingdata.android.UnitTest.xml')
    print('***************安卓单元测试结束***************')

def AndroidUITest():
    print('***************开始安卓UI自动化测试***************')
    os.chdir(androidSDKPath)
    os.system('pwd')
    os.system('./gradlew demox:connectedAndroidTest')
    webbrowser.open_new_tab('file:///'+androidSDKPath+'/demox/build/reports/androidTests/connected/index.html')
    print('***************安卓UI自动化测试结束***************')

def iOSUnitTest():
    print('***************开始iOS单元测试***************')
    os.chdir(iOSSDKPath)
    os.system('pwd')
    os.system('xcodebuild test  -workspace ThinkingSDK.xcworkspace  -scheme ThinkingSDKTests  -destination \'platform=iOS Simulator,name=iPhone X,OS=12.4\' build-for-testing | tee xcodebuild.log | xcpretty -s -r html --output {}'.format(iOSSDKPath+'ios_UnitTest.html'))
    webbrowser.open_new_tab('file:///'+iOSSDKPath+'ios_UnitTest.html')
    print('***************iOS单元测试结束***************')

def AndroidFunctionTest():
    print('***************开始安卓功能测试***************')
    os.chdir(androidSDKPath)
    os.system('pwd')
    os.system('./gradlew demox:connectedAndroidTest')
    webbrowser.open_new_tab('file:///'+androidSDKPath+'/demox/build/reports/androidTests/connected/cn.thinkingdata.android.FunctionTest.html')
    print('***************安卓功能测试结束***************')

while isExit is False:
    print(os.getcwd())
    operator = input('请选择你的操作:\n安卓单元测试       请输入1\n安卓UI自动化测试   请输入2\niOS单元测试        请输入3\niOSUI自动化测试    请输入4\n安卓功能测试       请输入5\n退出               请输入q\n\n')
    if operator == 1:
        AndroidUnitTest()
    elif operator == 2:
        AndroidUITest()
    elif operator == 3:
        iOSUnitTest()
    elif operator == 4:
        iOSUnitTest()
    elif operator == 5:
        AndroidFunctionTest()
    elif operator == 'q':
        print('***************退出成功***************')
        isExit = True
    else:
        print('***************非法操作!!!!!!***************')








