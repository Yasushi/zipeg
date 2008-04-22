/* JNI for class com_zipeg_DefaultRolehandler */
#include <jni.h>
#include <Carbon/Carbon.h>
#include <stdio.h>

// originally developed as command line utility by alx@foinc.com

#define null NULL

static jstring toJString(JNIEnv * env, CFStringRef s) {
    if (s != null) {
      CFIndex len = CFStringGetLength(s);
      UniChar uni[len];
      CFRange range;
      range.location = 0;
      range.length = len;
      CFStringGetCharacters(s, range, uni);
      return env->NewString((jchar*)uni, (jsize)len);
    } else {
      return null;
    }
}

static CFStringRef toCFString(JNIEnv * env, jstring js) {
    if (js != null) {
      const char* utf = env->GetStringUTFChars(js, null);
      CFStringRef cfs = ::CFStringCreateWithCString(NULL, utf, kCFStringEncodingUTF8);
      env->ReleaseStringUTFChars(js, utf);
      return cfs;
    } else {
      return null;
    }
}

static void releaseCFString(CFStringRef s) {
    if (s != null) {
        ::CFRelease(s);
    }
}

extern "C" {

  JNIEXPORT jint Java_com_zipeg_DefaultRoleHandler_setForContentType(JNIEnv * env, jobject _this,
                                                               jstring contentType,
                                                               jstring bundleId,
                                                               jint roles) {
      CFStringRef ct = toCFString(env, contentType);
      CFStringRef id = toCFString(env, bundleId);
      OSStatus r = ::LSSetDefaultRoleHandlerForContentType(ct, roles, id);
      releaseCFString(id);
      releaseCFString(ct);
      return r;
  }

  JNIEXPORT jint Java_com_zipeg_DefaultRoleHandler_setForURLScheme(JNIEnv * env, jobject _this,
                                                               jstring contentType,
                                                               jstring bundleId) {
      CFStringRef ct = toCFString(env, contentType);
      CFStringRef id = toCFString(env, bundleId);
      OSStatus r = ::LSSetDefaultHandlerForURLScheme(ct, id);
      releaseCFString(id);
      releaseCFString(ct);
      return r;
  }

  JNIEXPORT jint Java_com_zipeg_DefaultRoleHandler_setIgnoreCreator(JNIEnv * env, jobject _this,
                                                               jstring contentType,
                                                               jboolean ignoreCreator) {
      CFStringRef ct = toCFString(env, contentType);
      OSStatus r = ::LSSetHandlerOptionsForContentType(ct, ignoreCreator ?
                              kLSHandlerOptionsIgnoreCreator : kLSHandlerOptionsDefault);
      releaseCFString(ct);
      return r;
  }

  JNIEXPORT jstring Java_com_zipeg_DefaultRoleHandler_getForContentType(JNIEnv * env, jobject _this,
                                                               jstring contentType,
                                                               jint role) {
      CFStringRef ct = toCFString(env, contentType);
      CFStringRef id = ::LSCopyDefaultRoleHandlerForContentType(ct, role);
      jstring bundleId = toJString(env, id);
      releaseCFString(id);
      releaseCFString(ct);
      return bundleId;
  }

  JNIEXPORT jstring Java_com_zipeg_DefaultRoleHandler_getForURLScheme(JNIEnv * env, jobject _this,
                                                               jstring contentType) {
      CFStringRef ct = toCFString(env, contentType);
      CFStringRef id = ::LSCopyDefaultHandlerForURLScheme(ct);
      jstring bundleId = toJString(env, id);
      releaseCFString(id);
      releaseCFString(ct);
      return bundleId;
  }

  JNIEXPORT jboolean Java_com_zipeg_DefaultRoleHandler_getIgnoreCreator(JNIEnv * env, jobject _this,
                                                               jstring contentType) {
      CFStringRef ct = toCFString(env, contentType);
      jboolean r = ::LSGetHandlerOptionsForContentType(ct) == kLSHandlerOptionsIgnoreCreator;
      releaseCFString(ct);
      return r;
  }



}

// /System/Library/Frameworks/ApplicationServices.framework/Frameworks/LaunchServices.framework/Support/lsregister -dump
/*
  FSRef fsRef;
  bundleFolder.getFSRef(&fsRef);  // how???
  ::LSRegisterFSRef(&fsRef, true);

*/
