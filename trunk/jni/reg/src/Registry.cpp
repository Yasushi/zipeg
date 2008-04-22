#define STRICT
#define WIN32_LEAN_AND_MEAN

#include "jni.h"

#pragma warning(disable: 4514) // unref inline function
#pragma warning(disable :4615) // unknown user warning type
#pragma warning(disable :4018) // signed/unsigned mismatch
#pragma warning(disable :4146) // unary minus operator applied to unsigned type, result still unsigned
#pragma warning(disable :4201) // nonstandard extension used : nameless struct/union
#pragma warning(disable :4305) // truncation from 'unsigned __int64 ' to 'void *'
#pragma warning(disable :4201) // unary minus operator applied to unsigned type, result still unsigned
#pragma warning(disable :4996) // This function or variable may be unsafe.
#pragma warning(disable :4100) // unused param

#include <wtypes.h>
#include <winbase.h>
#include <malloc.h>
#include <stdio.h>
#include <string.h>
#include <shellapi.h>
#include <shlobj.h>
#include <psapi.h>

#define null NULL
#define unused(x) ((void)x)
#define ENV JNIEnv *env, jobject* that
#define _method(type, name) extern "C" JNIEXPORT type JNICALL Java_com_zipeg_Registry_##name
#define method(name) _method(jlong, name)
#define voidmethod(name) _method(void, name)

static jbyte* reserved = (jbyte*)malloc(16*1024);

static void trace(const char *fmt, ...) {
//#ifdef _DEBUG
    char buf[4096];
    va_list vl;
    va_start(vl, fmt);
    _vsnprintf(buf, (sizeof buf) - 1, fmt, vl);
    OutputDebugString(buf);
    va_end(vl);
//#endif
}

static char* getErrorMessage(int error) {
    char * msg = null;
    ::FormatMessageA(FORMAT_MESSAGE_ALLOCATE_BUFFER | FORMAT_MESSAGE_FROM_SYSTEM,
	             null, error, MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT),
	             (char*)&msg, 0, null);
    return msg;
}

static void throwOutOfMemoryError(JNIEnv *env) {
    free(reserved);
    reserved = null;
    if (env->ExceptionOccurred() == null) {
        env->ThrowNew(env->FindClass("java/lang/OutOfMemoryError"), "win32reg");
    }
}

static void throwIOException(JNIEnv *env, int error) {
    char message[1024] = {0};
    char* msg = getErrorMessage(error);
    if (msg != null) {
        strncpy(message, msg, sizeof(message)-1);
        LocalFree(msg);
        env->ThrowNew(env->FindClass("java/io/IOException"), message);
    } else {
        sprintf(message, "0x%08X (%d)", error, error);
        env->ThrowNew(env->FindClass("java/io/IOException"), message);
    }
}

static void* jalloc(JNIEnv *env, jsize size) {
    void* p = malloc(size);
    if (p == null) {
        trace("win32reg: outofmemory\n");
        throwOutOfMemoryError(env);
    }
    return p;
}

static jchar* getString(JNIEnv *env, jstring s) {
    if (s == null) {
        return null;
    }
    jboolean iscopy = false;
    jsize len = env->GetStringLength(s);
    const jchar* buf = env->GetStringChars(s, &iscopy);
    jchar* copy = (jchar*)jalloc(env, len * 2 + 2);
    if (copy != null) {
        memcpy(copy, buf, len * 2);
        copy[len] = 0;
    }
    env->ReleaseStringChars(s, buf);
    return copy;
}

static
DWORD* getProcessesIds(int &n) {
    DWORD processIds0[16*1024] = {0};
    DWORD bytes = 0;
    EnumProcesses(processIds0, sizeof(processIds0), &bytes);
    n = bytes / 4;
    DWORD* processIds = new DWORD[n*2]; // for sudden burst of number of processes
    EnumProcesses(processIds, bytes, &bytes);
    n = bytes / 4;
    return processIds;
}

static
HMODULE* getModules(HANDLE process, int &n) {
    HMODULE modules0[1] = {0};
    DWORD bytes = 0;
    EnumProcessModules(process, modules0, sizeof(modules0), &bytes);
    n = bytes / sizeof(HMODULE);
    HMODULE* modules = new HMODULE[n*2]; // for sudden burst of number of modules
    EnumProcessModules(process, modules, bytes, &bytes);
    n = bytes / sizeof(HMODULE);
    return modules;
}

method(killProcess)(ENV, jstring pname) {
    const wchar_t* name = (const wchar_t*)getString(env, pname);
    int count = 0;
    int namelen = wcslen(name);
    int n = 0;
    DWORD* processIds = getProcessesIds(n);
    for (int i = 0; i < n; i++) {
        if (GetCurrentProcessId() == processIds[i]) {
            continue;
        }
        HANDLE process = OpenProcess(PROCESS_ALL_ACCESS, false, processIds[i]);
        if (process == null) {
            continue;
        }
        int m = 0;
        HMODULE* modules = getModules(process, m);
        for (int k = 0; k < m; k++) {
            wchar_t path[1024] = {0};
            GetModuleFileNameExW(process, modules[k], path, sizeof(path));
            int len = wcslen(path);
            if (len > namelen && path[len - namelen - 1] == L'\\' && wcsicmp(&path[len - namelen], name) == 0) {
                if (::TerminateProcess(process, 0x1)) {
                    count++;
                }
            }
        }
        delete [] modules;
        CloseHandle(process);
    }
    delete [] processIds;
    free((void*)name);
    return count;
}

voidmethod(moveToRecycleBin)(ENV, jstring pathname) {
    // shell needs double 0x0000 terminated filenames
    // because it actully works on a list of filenames
    // not on a single one.
    jchar* absname = getString(env, pathname);
    if (absname == null) {
        return; // throwOutOfMemoryError already has been thrown 
    }
    int n = sizeof(jchar) * (wcslen((const wchar_t*)absname) + 2); // bytes
    jchar* dnt = (jchar*)malloc(n); // double zero terminated
    if (dnt == null) {
        throwOutOfMemoryError(env);
        free(absname);
        return;
    }
    memset(dnt, 0, n);
    wcscpy((wchar_t*)dnt, (const wchar_t*)absname);
    free(absname);
    SHFILEOPSTRUCTW op = {0};
    op.wFunc = FO_DELETE;
    op.pFrom = (wchar_t*)dnt;
    op.fFlags = FOF_ALLOWUNDO|FOF_NOCONFIRMATION|FOF_SILENT|FOF_RENAMEONCOLLISION;
    int b = SHFileOperationW(&op);
//  trace("SHFileOperationW %S %d\n", dnt, b);
    if (b != 0) {
        throwIOException(env, b); 
        // execution continues after throw
    } else {
        // assert !FileExist(pathname)
    }
    free(dnt);
    unused(that); // static
}

method(connectRegistry)(ENV, jlong key, jstring hostname) {
    unused(that); // static method
    HKEY rkey = null;
    jchar* s = getString(env, hostname);
    if (env->ExceptionOccurred() == null) {
        long ec = ::RegConnectRegistryW((const wchar_t*)s, (HKEY)key, &rkey);
        if (ec != 0) {
            throwIOException(env, ec);
        }
    }
    free(s);
    return (jlong)rkey;
}
    
voidmethod(closeKey)(ENV, jlong key) {
    unused(that); // static method
    long ec = ::RegCloseKey((HKEY)key);
    if (ec != 0) {
        throwIOException(env, ec);
    }
}

voidmethod(flushKey)(ENV, jlong key) {
    unused(that); // static method
    long ec = ::RegFlushKey((HKEY)key);
    if (ec != 0) {
        throwIOException(env, ec);
    }
}

method(openKey)(ENV, jlong key, jstring subkey, int access) {
    unused(that); // static method
    HKEY rkey = null;
    jchar* s = getString(env, subkey);
    if (env->ExceptionOccurred() == null) {
        long ec = ::RegOpenKeyExW((HKEY)key, (const wchar_t*)s, 0, access, &rkey);
        if (ec != 0) {
            throwIOException(env, ec);
        }
    }
    free(s);
    return (jlong)rkey;
}

method(createKey)(ENV, jlong key, jstring subkey, jstring rclass, int options, int access, jintArray disposition) {
    unused(that); // static method
    HKEY rkey = null;
    jchar* s  = getString(env, subkey);
    if (env->ExceptionOccurred() == null) {
        jchar* rc = getString(env, rclass);
        if (env->ExceptionOccurred() == null) {
            jint disp = 0;
            long ec = ::RegCreateKeyExW((HKEY)key, (const wchar_t*)s , 0, (wchar_t*)rc, 
										options, access, null, &rkey, (DWORD*)&disp);
            if (ec != 0) {
                throwIOException(env, ec);
            } else {
                if (disposition != null) {
                    env->SetIntArrayRegion(disposition, 0, 1, &disp);
                }
            }
        }
        free(rc);
    }
    free(s);
    return (jlong)rkey;
}

voidmethod(deleteKey)(ENV, jlong key, jstring subkey) {
    unused(that); // static method
    jchar* s  = getString(env, subkey);
    if (env->ExceptionOccurred() == null) {
        long ec = ::RegDeleteKeyW((HKEY)key, (const wchar_t*)s);
        if (ec != 0) {
            throwIOException(env, ec);
        }
    }
    free(s);
}
    
voidmethod(deleteValue)(ENV, jlong key, jstring subkey) {
    unused(that); // static method
    jchar* s  = getString(env, subkey);
    if (env->ExceptionOccurred() == null) {
        long ec = ::RegDeleteValueW((HKEY)key, (const wchar_t*)s);
        if (ec != 0) {
            throwIOException(env, ec);
        }
    }
    free(s);
}

method(setValue)(ENV, jlong key, jstring subkey, int type, jbyteArray ba) {
    unused(that); // static method
    HKEY rkey = null;
    jchar* s  = getString(env, subkey);
    if (env->ExceptionOccurred() == null) {
        jsize len = env->GetArrayLength(ba);
        jboolean iscopy = false;
        jbyte* data = env->GetByteArrayElements(ba, &iscopy);
        long ec = ::RegSetValueExW((HKEY)key, (const wchar_t*)s, 0, type, (byte*)data, len);
        env->ReleaseByteArrayElements(ba, data, iscopy ? JNI_ABORT : 0);
        if (ec != 0) {
            throwIOException(env, ec);
        }
    }
    free(s);
    return (jlong)rkey;
}

_method(jbyteArray, getValue)(ENV, jlong key, jstring subkey, jintArray type) {
    unused(that); // static method
    jbyteArray ba = null;
    jchar* s  = getString(env, subkey);
    if (env->ExceptionOccurred() == null) {
        DWORD rtype = 0;
        DWORD len = 0;
        BYTE dummy[4] = {0};
        long ec = ::RegQueryValueExW((HKEY)key, (const wchar_t*)s, 0, &rtype, (byte*)dummy, &len);
        trace("%08X %s\n", ec, getErrorMessage(ec));
        if (ec != 0 && ec != ERROR_MORE_DATA) { 
            throwIOException(env, ec);
        } else {
            ba = env->NewByteArray(len);
            if (ba == null) {
                throwOutOfMemoryError(env);
            } else {
                jboolean iscopy = false;
                jbyte* data = env->GetByteArrayElements(ba, &iscopy);
                long ec = ::RegQueryValueExW((HKEY)key, (const wchar_t*)s, 0, &rtype, (byte*)data, &len);
                env->ReleaseByteArrayElements(ba, data, iscopy ? JNI_COMMIT : 0);
                if (ec != 0) {
                    env->DeleteGlobalRef(ba);
                    ba = null;
                    throwIOException(env, ec);
                } else {
                    env->SetIntArrayRegion(type, 0, 1, (jint*)&rtype);
                }
            }
        }
    }
    free(s);
    return ba;
}

method(nextKey)(ENV, jlong key, jint index, jobjectArray sa) {
    unused(that); // static method
    jlong javaTime = 0;
    DWORD nlen = 0;
    DWORD clen = 0;
    WCHAR dummy[2] = {0};
    FILETIME lastWriteTime = {0};
    long ec = ::RegEnumKeyExW((HKEY)key, index, dummy, &nlen, null, dummy, &clen, &lastWriteTime);
    trace("%08X %s\n", ec, getErrorMessage(ec));
    if (ec != 0 && ec != ERROR_MORE_DATA) { 
        throwIOException(env, ec);
    } else {
        jlong time = (((jlong)lastWriteTime.dwHighDateTime) << 32) |
                              lastWriteTime.dwLowDateTime;
        javaTime = (time / 10000L) - (jlong)(11644473600000);
        WCHAR* name = (WCHAR*)jalloc(env, (nlen + 1) * 2);
        if (name != null) {
            WCHAR* clss = (WCHAR*)jalloc(env, (clen + 1) * 2);
            if (clss != null) {
                long ec = ::RegEnumKeyExW((HKEY)key, index, name, &nlen, null, clss, &clen, &lastWriteTime);
                if (ec != 0) {
                    throwIOException(env, ec);
                } else {
                    jstring n = env->NewString((const jchar*)name, nlen - 1);
                    if (n == null) {
                        jstring c = env->NewString((const jchar*)clss, clen - 1);
                        if (c != null) {
                            env->SetObjectArrayElement(sa, 0, n);
                            env->SetObjectArrayElement(sa, 1, c);
                        }
                    }
                }
                free(clss);
            }
            free(name);
        }
    }
    return javaTime;
}

voidmethod(notifyShellAssociationsChanged)(ENV) {
    SHChangeNotify(SHCNE_ASSOCCHANGED, SHCNF_IDLIST, null, null);
}

voidmethod(notifyShellAllChanged)(ENV) {
    SHChangeNotify(SHCNE_ALLEVENTS, null, null, null);
}

method(initializeOle)(ENV) {
    return OleInitialize(0);
}

method(shellExec)(ENV, jlong mask, jstring verb, jstring file, jstring params, jstring dir) {
    SHELLEXECUTEINFOW info = {sizeof(SHELLEXECUTEINFOW)};
    info.fMask = (DWORD)mask;
    info.lpVerb = (LPCWSTR)getString(env, verb);
    info.lpFile = (LPCWSTR)getString(env, file);
    info.lpParameters = (LPCWSTR)getString(env, params);
    info.lpDirectory = (LPCWSTR)getString(env, dir);
    info.nShow = SW_NORMAL;
    // Optional fields
    info.lpIDList = null;
    info.lpClass = null;
    jlong r = ::ShellExecuteExW(&info);
    delete[] info.lpVerb;
    delete[] info.lpFile;
    delete[] info.lpParameters;
    delete[] info.lpDirectory;
    if (info.hProcess != null) {
        CloseHandle(info.hProcess);
        info.hProcess = null;
    }
    return r;
}

/*
method(nextKey)(ENV, jlong key, jint index, jobjectArray sa) {
    unused(that); // static method
    jlong javaTime = 0;
    jobjectArray oa = null;
    DWORD rtype = 0;
    DWORD nlen = 0;
    DWORD clen = 0;
    WCHAR dummy[2] = {0};
    FILETIME lastWriteTime = {0};
    long ec = ::RegEnumKeyExW((HKEY)key, index, dummy, &nlen, null, dummy, &clen, &lastWriteTime);
    trace("%08X %s\n", ec, getErrorMessage(ec));
    if (ec != 0 && ec != ERROR_MORE_DATA) { 
        throwIOException(env, ec);
    } else {
        jlong time = (((jlong)lastWriteTime.dwHighDateTime) << 32) |
                              lastWriteTime.dwLowDateTime;
        jlong javaTime = (time / 10000L) - (jlong)(11644473600000);
        WCHAR* name = (WCHAR*)jalloc(env, (nlen + 1) * 2);
        if (name != null) {
            WCHAR* clss = (WCHAR*)jalloc(env, (clen + 1) * 2);
            if (clss != null) {
                long ec = ::RegEnumKeyExW((HKEY)key, index, name, &nlen, null, clss, &clen, &lastWriteTime);
                if (ec != 0) {
                    throwIOException(env, ec);
                } else {
                    jstring n = env->NewString(name, nlen - 1);
                    if (n == null) {
                        jstring c = env->NewString(clss, clen - 1);
                        if (c != null) {
                            env->SetObjectArrayElement(sa, 0, n);
                            env->SetObjectArrayElement(sa, 1, c);
                        }
                    }
                }
                free(clss);
            }
            free(name);
        }
    }
    return javaTime;
}

*/

/*

Not implemented:

RegEnumKeyEx 
RegEnumValue
RegQueryInfoKey
RegQueryMultipleValues

RegLoadKey 
RegUnLoadKey 
RegSetKeySecurity 
RegGetKeySecurity 
RegNotifyChangeKeyValue 
RegReplaceKey
RegRestoreKey
RegSaveKey 
*/ 
