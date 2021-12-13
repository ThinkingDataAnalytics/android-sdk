#!/bin/bash
checkDevice=$(adb devices)
echo "------- SDK AutoTest Start -------"
echo "choose your test device =>"
echo "--------------------------------------------"
array=(${checkDevice//List of devices attached/ })
laterArray=(${array[@]//device/ })
count=0
for var in ${laterArray[@]}
do
   echo $var-------[$count]
   count=$(( $count + 1 ))
done
echo "--------------------------------------------"
read device
echo ${laterArray[$device]}
# check one device or more than one
device=${laterArray[$device]}
pwd
#check install
if [ $count > 1 ]
  then
    addStr="-s "
    device=$addStr$device
  else
    device=" "
fi
#start build
../gradlew assembleAndroidTest
../gradlew assemble

#start install
installBaseApk=$(adb $device install build/outputs/apk/debug/demox-debug.apk)
installBaseResult=$(echo $installBaseApk | grep -io "success")
if [[ $installBaseResult == "success" || $installBaseResult == "Success" ]]
    then
      installTestApk=$(adb $device install build/outputs/apk/androidTest/debug/demox-debug-androidTest.apk)
      installTestResult=$(echo $installTestApk | grep -io "success")
      if [[ $installTestResult == "success" || $installTestResult == "Success" ]]
          then
            echo "install test apk success"
            echo "FunctionTest start ==>"
            echo "--------------------------------------"
            adb $device shell am instrument -w -m -e clearPackageData true   -e debug false -e class 'cn.thinkingdata.android.FunctionTest' cn.thinkingdata.android.demo.test/cn.thinkingdata.android.TestRunner
            echo "FunctionTest end ==>"
#            echo "please check testResult build/test-results/testDebugUnitTest/TEST-cn.thinkingdata.android.UnitTest.xml"
            echo "UnitTest start =>"
            ../gradlew cleanTestDebugUnitTest testDebugUnitTest --tests "cn.thinkingdata.android.UnitTest"
            echo "UnitTest end =>"
            echo "please check testResult build/test-results/testDebugUnitTest/TEST-cn.thinkingdata.android.UnitTest.xml"
          else
            echo "Test Apk install Failed!!!"
            exit
      fi
    else
      echo "Base Apk install Failed!!!"
      exit
fi
echo "------- SDK AutoTest End -------"