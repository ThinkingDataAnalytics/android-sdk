SDK_VERSION=$(grep VERSION_NAME_SDK gradle.properties|cut -d'=' -f2)
./gradlew thinkingsdk:assemble
mv thinkingsdk/build/outputs/aar/thinkingsdk-release.aar ThinkingSDK.aar
mkdir -p release
pushd release
zip -r ta_android_sdk.zip ../ThinkingSDK.aar
zip -r ta_android_sdk_${SDK_VERSION}.zip ../ThinkingSDK.aar
popd



