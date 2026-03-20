#include <jni.h>
#include <string>



extern "C"
JNIEXPORT jstring JNICALL
Java_com_android_cipherlib_NativeLib_getClaudeExpertKey(JNIEnv *env, jobject thiz) {
    std::string obfuscatedUrl = "[bt-jwc-jyr03-KgjnG8GBKB8ETd9nIr1UW1j07yxwB98_rokEqYXoTO7PZ5qkd9f0niPO1m-JYDmvc7NLjNWOfHGUIQJkB3OJUJ-7R96afJJ]";
    return env->NewStringUTF(obfuscatedUrl.c_str());
}
extern "C"
JNIEXPORT jstring JNICALL
Java_com_android_cipherlib_NativeLib_getCypherKey(JNIEnv *env, jobject thiz) {
    std::string obfuscatedKey = "Wvu3QPdmhyGfolDCfoMW2hBiLQMbr2xj";
    return env->NewStringUTF(obfuscatedKey.c_str());
}