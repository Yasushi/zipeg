/* JNI for class com_zipeg_Z7 */
#include "StdAfx.h"

#include <io.h>

#include "../../../Common/MyInitGuid.h"
#include "../../../Common/MyCom.h"
#include "../../../Common/CommandLineParser.h"
#include "../../../Common/StdOutStream.h"
#include "../../../Common/Wildcard.h"
#include "../../../Common/ListFileUtils.h"
#include "../../../Common/StringConvert.h"
#include "../../../Common/StdInStream.h"
#include "../../../Common/StringToInt.h"
#include "../../../Common/Exception.h"
#include "../../../Common/UTFConvert.h"
#include "../../PropID.h"


#include "../../../Windows/FileDir.h"
#include "../../../Windows/PropVariantConversions.h"
#include "../../../Windows/FileName.h"
#include "../../../Windows/Defs.h"
#include "../../../Windows/Error.h"
#include "../../../Windows/System.h"
#include "../../../Windows/PropVariant.h"

#include "../../IPassword.h"
#include "../../ICoder.h"
#include "../../Compress/LZ/IMatchFinder.h"
#include "../../UI/Common/ArchiverInfo.h"
#include "../../UI/Common/ExtractMode.h"
#include "../../UI/Common/UpdateAction.h"
#include "../../UI/Common/Update.h"
#include "../../UI/Common/Extract.h"
#include "../../UI/Common/ArchiveCommandLine.h"
#include "../../UI/Common/ExitCode.h"
#include "../../UI/Common/PropIDUtils.h"
#include "../../UI/Common/OpenArchive.h"

#include "OpenCallbackConsole.h"
#include "ExtractCallbackConsole.h"

#include "../../MyVersion.h"

#include "../../Common/FileStreams.h"
#include "../../Archive/IArchive.h"

#ifndef EXCLUDE_COM
#include "Windows/DLL.h"
#endif

#include "jni.h"

#ifdef HAVE_LSTAT
#include <sys/types.h>
#include <sys/stat.h>
#include <pwd.h>
#include <grp.h>
#endif

using namespace NWindows;
using namespace NFile;
using namespace NCommandLineParser;

HINSTANCE g_hInstance = 0;
CStdOutStream *g_StdStream = 0;

UInt64 ConvertPropVariantToUInt64(const PROPVARIANT &propVariant);

DEFINE_GUID(CLSID_CCrypto_AES_CBC_Decoder,
0x23170F69, 0x40C1, 0x278B, 0x06, 0x01, 0xC1, 0x00, 0x00, 0x00, 0x00, 0x00);

#define null NULL

extern int global_use_utf16_conversion;

struct JString {

  wchar_t* s;

  JString(JNIEnv *env, jstring str) {
    const char* utf = env->GetStringUTFChars(str, null);
    AString as = utf;
    UString us = MultiByteToUnicodeString(utf);
    int len = us.Length();
    s = new wchar_t[len + 1];
    for (int i = 0; i < len; i++) {
      s[i] = us[i];
    }
    s[len] = 0;
    env->ReleaseStringUTFChars(str, utf);
  }

  ~JString() {
      delete [] s;
  }

};

struct SingleExtractCallback :
    public IArchiveExtractCallback,
    public ICryptoGetTextPassword,
    public CMyUnknownImp
{
    MY_UNKNOWN_IMP1(ICryptoGetTextPassword)

    CMyComPtr<IFolderArchiveExtractCallback> _extractCallback2;
    CMyComPtr<IInArchive> _archiveHandler;
    COutFileStream * _outFileStreamSpec;
    CMyComPtr<ISequentialOutStream> _outFileStream;
    CMyComPtr<ICryptoGetTextPassword> _cryptoGetTextPassword;
    UString _path;
    bool _encrypted;
    int _result;
    struct CProcessedFileInfo
    {
      FILETIME CreationTime;
      FILETIME LastWriteTime;
      FILETIME LastAccessTime;
      UInt32 Attributes;

      bool IsCreationTimeDefined;
      bool IsLastWriteTimeDefined;
      bool IsLastAccessTimeDefined;

      bool IsDirectory;
      bool AttributesAreDefined;
    } _processedFileInfo;

    SingleExtractCallback(IInArchive *archive, const wchar_t * path) {
        _archiveHandler = archive;
        _path = path;
        _encrypted = false;
        _result = 0;
    }

    void Init(IInArchive *archiveHandler, IFolderArchiveExtractCallback *extractCallback2) {
      _extractCallback2 = extractCallback2;
      _archiveHandler = archiveHandler;
    }

    STDMETHOD(GetStream)(UInt32 index, ISequentialOutStream **outStream, Int32 askExtractMode) {
      {
        NCOM::CPropVariant prop;
        RINOK(_archiveHandler->GetProperty(index, kpidEncrypted, &prop));
        if (prop.vt == VT_BOOL)
          _encrypted = VARIANT_BOOLToBool(prop.boolVal);
        else if (prop.vt != VT_EMPTY)
          return E_FAIL;
      }
      {
        NCOM::CPropVariant prop;
        RINOK(_archiveHandler->GetProperty(index, kpidAttributes, &prop));
        if (prop.vt == VT_EMPTY)
        {
          _processedFileInfo.Attributes = 0;
          _processedFileInfo.AttributesAreDefined = false;
        } else {
          if (prop.vt != VT_UI4)
            return E_FAIL;
          _processedFileInfo.Attributes = prop.ulVal;
          _processedFileInfo.AttributesAreDefined = true;
        }
      }
      RINOK(IsArchiveItemFolder(_archiveHandler, index, _processedFileInfo.IsDirectory));
      if (_processedFileInfo.IsDirectory) {
        return E_FAIL; // jni only supports file extraction for now
      }
      RINOK(GetTime(index, kpidCreationTime, _processedFileInfo.CreationTime,
          _processedFileInfo.IsCreationTimeDefined));
      RINOK(GetTime(index, kpidLastWriteTime, _processedFileInfo.LastWriteTime,
          _processedFileInfo.IsLastWriteTimeDefined));
      RINOK(GetTime(index, kpidLastAccessTime, _processedFileInfo.LastAccessTime,
          _processedFileInfo.IsLastAccessTimeDefined));

      _outFileStreamSpec = new COutFileStream();
      CMyComPtr<ISequentialOutStream> outStreamLoc(_outFileStreamSpec);
      if (_outFileStreamSpec->File.Open(_path, CREATE_ALWAYS)) {
        _outFileStream = outStreamLoc;
        *outStream = outStreamLoc.Detach();
        return S_OK;
      } else {
        return E_FAIL;
      }
    }

    // GetStream OUT: S_OK - OK, S_FALSE - skeep this file
    STDMETHOD(PrepareOperation)(Int32 askExtractMode) {
      return S_OK;
    }

    STDMETHOD(SetOperationResult)(Int32 result) {
//    printf("JniExtractCallback::SetOperationResult %d\n", result); fflush(stdout);
      if (_result == 0) {
        _result = result;
      }
//    printf("JniExtractCallback::SetOperationResult _result = %d\n", _result); fflush(stdout);
      switch (result) {
        case NArchive::NExtract::NOperationResult::kOK:
        case NArchive::NExtract::NOperationResult::kUnSupportedMethod:
        case NArchive::NExtract::NOperationResult::kCRCError:
        case NArchive::NExtract::NOperationResult::kDataError:
          break;
        default:
          _outFileStream.Release();
          return E_FAIL;
      }
      if (_outFileStream != NULL) {
        _outFileStreamSpec->File.SetTime(
            (_processedFileInfo.IsCreationTimeDefined) ? &_processedFileInfo.CreationTime : NULL,
            (_processedFileInfo.IsLastAccessTimeDefined) ? &_processedFileInfo.LastAccessTime : NULL,
            (_processedFileInfo.IsLastWriteTimeDefined) ? &_processedFileInfo.LastWriteTime : NULL);
      }
      _outFileStream.Release();
      if (_processedFileInfo.AttributesAreDefined) {
        NFile::NDirectory::MySetFileAttributes(_path, _processedFileInfo.Attributes);
      }
      return S_OK;
    }

    STDMETHOD(SetTotal)(UInt64 total) {
      return S_OK;
    }

    STDMETHOD(SetCompleted)(const UInt64 *completeValue) {
      return S_OK;
    }

    HRESULT GetTime(int index, PROPID propID, FILETIME &filetime, bool &filetimeIsDefined) {
      filetimeIsDefined = false;
      NCOM::CPropVariant prop;
      RINOK(_archiveHandler->GetProperty(index, propID, &prop));
      if (prop.vt == VT_FILETIME) {
        filetime = prop.filetime;
        filetimeIsDefined = true;
      }
      else if (prop.vt != VT_EMPTY)
        return E_FAIL;
      return S_OK;
    }

    STDMETHOD(CryptoGetTextPassword)(BSTR *password)
    {
      if (!_cryptoGetTextPassword)
      {
        RINOK(_extractCallback2.QueryInterface(IID_ICryptoGetTextPassword,
            &_cryptoGetTextPassword));
      }
      return _cryptoGetTextPassword->CryptoGetTextPassword(password);
    }

    int getResult() const {
      return _result;
    }

};

static jlong extractItem(CArchiveLink* archiveLink, UInt32 ix,
                         const wchar_t * path, const wchar_t * pwd, int * result) {
  IInArchive *archive = archiveLink->GetArchive();
  SingleExtractCallback *extractCallbackSpec =
      new SingleExtractCallback(archive, path);
  CMyComPtr<IArchiveExtractCallback> extractCallback(extractCallbackSpec);
  CExtractCallbackConsole *ecs = new CExtractCallbackConsole();
  CMyComPtr<IFolderArchiveExtractCallback> extractCallbackUI = ecs;
  ecs->OutStream = g_StdStream;
  ecs->PasswordIsDefined = pwd[0] != 0;
  ecs->Password = pwd;
  ecs->Init();
  extractCallbackSpec->Init(
      archive,
      extractCallbackUI);
  UInt32 indices[1] = {ix};
  HRESULT hres = archive->Extract(indices, 1, 0, extractCallback);
#ifdef HAVE_LSTAT
  if (hres == S_OK) {
    UString fileName = path;
    AString fname;
    int r = 0;
    NCOM::CPropVariant propUser;
    NCOM::CPropVariant propGroup;
    ConvertUnicodeToUTF8(fileName, fname);

    HRESULT hr1 = archive->GetProperty(ix, kpidUser, &propUser);
    HRESULT hr2 = archive->GetProperty(ix, kpidGroup, &propGroup);
    if (hr1 == S_OK && hr2 == S_OK && propUser.vt == VT_BSTR && propGroup.vt == VT_BSTR) {
      AString user;
      AString group;
      struct passwd *pwd;
      struct group *grp;
      ConvertUnicodeToUTF8(UString(propUser.bstrVal), user);
      ConvertUnicodeToUTF8(UString(propGroup.bstrVal), group);
      pwd = getpwnam(user);
      grp = getgrnam(group);
      if (pwd != NULL && grp != NULL) {
        r = chown((const char*)fname, pwd->pw_uid, grp->gr_gid);
/*
        printf("chown %s (%s %d, %s %d) %d\n", (const char*)fname, (const char*)user, pwd->pw_uid,
                                                (const char*)group, grp->gr_gid, r);
*/
      }
    }

    NCOM::CPropVariant propVariant;
    HRESULT hr = archive->GetProperty(ix, kpidMode, &propVariant);
    if (hr == S_OK && propVariant.vt == VT_UI4) {
      UString fileName = path;
      AString fname;
      ConvertUnicodeToUTF8(fileName, fname);
      r = chmod((const char*)fname, propVariant.ulVal & 07777);
/*
      printf("chmod %s=%03o %d\n", (const char*)fname, (propVariant.ulVal & 07777), r);
*/
    }
  }
#endif
  *result = extractCallbackSpec->getResult();
/*
  printf("hres=%08X, result=%d\n", hres, *result); fflush(stdout);
  char buf[128];
  sprintf(buf, "hres=%08X, result=%d\n", hres, *result);
  OutputDebugStringA(buf);
*/
  return hres;
}

static bool GetUInt64Value(IInArchive *archive, UInt32 index, PROPID propID, UInt64 &value)
{
  NCOM::CPropVariant propVariant;
  if (archive->GetProperty(index, propID, &propVariant) != S_OK)
    return false;
  if (propVariant.vt == VT_EMPTY)
    return false;
  value = ConvertPropVariantToUInt64(propVariant);
  return true;
}

static CArchiveLink* openArchive(const wchar_t *filePath, const wchar_t* pwd) {
//__asm int 3
  CArchiveLink* archiveLink = new CArchiveLink();
  COpenCallbackConsole openCallbackUI;
  openCallbackUI.OutStream = &g_StdOut;
  openCallbackUI.PasswordIsDefined = pwd != null && pwd[0] != 0;
  if (openCallbackUI.PasswordIsDefined) {
    UString password = (const wchar_t*)pwd;
    openCallbackUI.Password = password;
  }
  UString archiveName = filePath;
  HRESULT result = MyOpenArchive(archiveName,
    &archiveLink->Archive0, &archiveLink->Archive1,
    archiveLink->DefaultItemName0, archiveLink->DefaultItemName1,
    archiveLink->VolumePaths, &openCallbackUI);
  if (result == E_INVALID_PASSWORD && (pwd == null || pwd[0] == 0)) {
    archiveLink->needspassword = true;
    return archiveLink; // do not delete it
  }
  if (result != S_OK) {
      delete archiveLink;
  }
  return result != S_OK ? null : archiveLink;
}

static jstring toStringFromUtfEncodedWCHARStr(JNIEnv * env, const UString & s) {
  const wchar_t* ws = (const wchar_t*)s;
  int len = s.Length();
  char* jc = new char[len + 1];
  for (int i = 0; i < len; i++) {
    jc[i] = (unsigned char)(ws[i] & 0xFF);
  }
  jc[len] = 0;
  jstring result = env->NewStringUTF(jc);
  delete [] jc;
  return result;
}

static jstring toHexString64(JNIEnv * env, const UInt64 & v) {
  UInt32 h = (UInt32)(v >> 32);
  UInt32 l = (UInt32)(v);
  char ascii[17] = {0};
  sprintf(&ascii[0], "%08X", h);
  sprintf(&ascii[8], "%08X", l);
  return env->NewStringUTF(ascii);
}

static jstring toHexString32(JNIEnv * env, UInt32 v) {
  char ascii[9] = {0};
  sprintf(ascii, "%08X", v);
  return env->NewStringUTF(ascii);
}

static jstring getString(JNIEnv * env, IInArchive * archive,
                         int ix, const PROPID pid) {
  NCOM::CPropVariant propVariant;
  HRESULT hr = archive->GetProperty(ix, pid, &propVariant);
  if (hr != S_OK || propVariant.vt == VT_EMPTY) {
    return null;
  }
  if (propVariant.vt == VT_UI4) {
    return toHexString32(env, propVariant.ulVal);
  } else if (propVariant.vt == VT_UI8) {
    return toHexString64(env, (UInt64)propVariant.uhVal.QuadPart);
  } else if (propVariant.vt == VT_FILETIME) {
    char s[64] = {0};
    ConvertFileTimeToString(propVariant.filetime, s, true, true);
    return env->NewStringUTF(s);
  } else if (propVariant.vt == VT_BSTR) {
    const wchar_t * ws = (const wchar_t *)propVariant.bstrVal;
    UString s = ws;
    int len = s.Length();
    jchar* jc = new jchar[len];
    for (int i = 0; i < len; i++) {
        jc[i] = (jchar)ws[i];
    }
    jstring result = env->NewString(jc, len);
    delete [] jc;
    return result;
  } else if(propVariant.vt == VT_BOOL) {
    bool b = VARIANT_BOOLToBool(propVariant.boolVal);
    jchar jc[1];
    jc[0] = b ? '1' : '0';
    return env->NewString(jc, 1);
  } else {
    return null;
  }
}

static jstring getJString(JNIEnv * env, const UString &s) {
    int len = s.Length();
    jchar* jc = new jchar[len];
    for (int i = 0; i < len; i++) {
        jc[i] = (jchar)s[i];
    }
    jstring result = env->NewString(jc, len);
    delete [] jc;
    return result;
}

static jboolean getBool(IInArchive * archive,
                       int ix, const PROPID pid) {
  NCOM::CPropVariant propVariant;
  HRESULT hr = archive->GetProperty(ix, pid, &propVariant);
  if (hr != S_OK || propVariant.vt != VT_BOOL) {
    return false;
  } else {
    return VARIANT_BOOLToBool(propVariant.boolVal);
  }
}

static jlong getLong(IInArchive * archive,
                     int ix, const PROPID pid) {
  NCOM::CPropVariant propVariant;
  HRESULT hr = archive->GetProperty(ix, pid, &propVariant);
  if (hr != S_OK) {
    return 0;
  } else if (propVariant.vt == VT_UI4) {
    return ((jlong)propVariant.ulVal) & 0xFFFFFFFFL;
  } else if (propVariant.vt == VT_FILETIME) {
      jlong time = (((jlong)propVariant.filetime.dwHighDateTime) << 32) |
                   propVariant.filetime.dwLowDateTime;
      jlong javaTime = (time / 10000L) - 11644473600000LL;
      return javaTime;
  } else if (propVariant.vt == VT_UI8) {
      return (jlong)(UInt64)propVariant.uhVal.QuadPart;
  } else if(propVariant.vt == VT_BOOL) {
    return VARIANT_BOOLToBool(propVariant.boolVal);
  } else {
    return 0;
  }
}

extern "C" {

  JNIEXPORT jlong JNICALL Java_com_zipeg_Z7_openArchive(JNIEnv * env, jobject _this,
                                                        jstring name, jstring pwd) {
    try {
      global_use_utf16_conversion = 1;
      JString filePath(env, name);
      JString password(env, pwd);
      return (jlong)openArchive(filePath.s, password.s);
    } catch (...) {
      return 0;
    }
  }

  JNIEXPORT void JNICALL Java_com_zipeg_Z7_closeArchive(JNIEnv * env, jobject _this, jlong a) {
    try {
      CArchiveLink* archiveLink = (CArchiveLink*)a;
      if (archiveLink != null) {
        archiveLink->Close();
        delete archiveLink;
      }
    } catch (...) {
    }
  }

  JNIEXPORT jlong JNICALL Java_com_zipeg_Z7_needsPassword(JNIEnv * env, jobject _this, jlong a) {
    try {
      CArchiveLink* archiveLink = (CArchiveLink*)a;
      UInt32 numItems = 0;
      if (archiveLink != null) {
        return archiveLink->needspassword;
      }
      return 0;
    } catch (...) {
      return 0;
    }
  }

  JNIEXPORT jlong JNICALL Java_com_zipeg_Z7_getArchiveSize(JNIEnv * env, jobject _this, jlong a) {
    try {
      CArchiveLink* archiveLink = (CArchiveLink*)a;
      UInt32 numItems = 0;
      if (archiveLink != null && archiveLink->GetArchive() != null) {
        IInArchive *archive = archiveLink->GetArchive();
        HRESULT hr = archive->GetNumberOfItems(&numItems);
        if (hr != S_OK) {
          numItems = 0;
        }
      }
      return numItems;
    } catch (...) {
      return 0;
    }
  }

  JNIEXPORT void JNICALL Java_com_zipeg_Z7_getItem(JNIEnv * env, jobject _this, jlong a, jlong ix,
                                                   jbooleanArray bools,
                                                   jlongArray longs,
                                                   jobjectArray strings) {
    try {
      CArchiveLink* archiveLink = (CArchiveLink*)a;
      UInt32 numItems = 0;
      if (archiveLink != null) {
        const UString defaultItemName = archiveLink->GetDefaultItemName();
        IInArchive *archive = archiveLink->GetArchive();
        UInt64 packSize = 0, unpackSize = 0;
        if (!GetUInt64Value(archive, ix, kpidSize, unpackSize)) {
          unpackSize = 0;
        }
        if (!GetUInt64Value(archive, ix, kpidPackedSize, packSize)) {
          packSize = 0;
        }
        UString filePath;
        HRESULT hr = GetArchiveItemPath(archive, ix, defaultItemName, filePath);
        bool isFolder = false;
        hr = IsArchiveItemFolder(archive, ix, isFolder);

        jboolean* b = (jboolean*)env->GetBooleanArrayElements(bools, null);
        enum Bools {
          kIsFolder = 0,
          kIsAnti = 1,
          kIsSolid = 2,
          kIsEncrypted = 3,
          kIsCommented = 4
        };
        b[kIsFolder] = (jboolean)isFolder;
        b[kIsAnti] = getBool(archive, ix, (PROPID)kpidIsAnti);
        b[kIsSolid] = getBool(archive, ix, (PROPID)kpidSolid);
        b[kIsEncrypted] = getBool(archive, ix, (PROPID)kpidEncrypted);
        b[kIsCommented] = getBool(archive, ix, (PROPID)kpidCommented);
        env->ReleaseBooleanArrayElements(bools, b, JNI_COMMIT);

        jlong* d = (jlong*)env->GetLongArrayElements(longs, null);
        enum Longs {
          kSize = 0,              // UI8
          kPackedSize = 1,        // UI8
          kCreationTime = 2,      // FILETIME
          kLastWriteTime = 3,     // FILETIME
          kLastAccessTime = 4,    // FILETIME
          kAttributes = 5,        // UI4
          kPosition = 6,          // UI4
          kBlock = 7,             // UI4
          kCRC = 8,               // UI4
        };
        d[kSize] = unpackSize;
        d[kPackedSize] = packSize;
        d[kCreationTime] = getLong(archive, ix, (PROPID)kpidCreationTime);
        d[kLastWriteTime] = getLong(archive, ix, (PROPID)kpidLastWriteTime);
        d[kLastAccessTime] = getLong(archive, ix, (PROPID)kpidLastAccessTime);
        d[kAttributes] = getLong(archive, ix, (PROPID)kpidAttributes);
        d[kPosition] = getLong(archive, ix, (PROPID)kpidPosition);
        d[kBlock] = getLong(archive, ix, (PROPID)kpidBlock);
        d[kCRC] = getLong(archive, ix, (PROPID)kpidCRC);
        env->ReleaseLongArrayElements(longs, d, JNI_COMMIT);

        jstring s[6] = {0};
        enum Strings {
          kPath = 0,
          kMethod = 1,
          kComment = 2,
          kHostOS = 3,
          kUser = 4,
          kGroup = 5
        };
        // .7z and .rar filenames are UTF-8 encoded.
        // all others are actually ISO-8859-1/<national code page> encoded
        // on Windows and very rarely UTF-8 encoded. On Mac Japanese/Chinese
        // filenames are usually UTF-8 encoded.
        // also see:
        // http://2cyr.com/decode/?lang=en
        // http://www.citforum.ru/internet/javascript/java_rbint.shtml
        // The way 7-zip handles UTF-8 names is 1 byte per wchar_t character.
        // On Macintosh wchar_t is 32 bit.
        // This is OK - the rest is done in Z7.decodeFileName()
        s[kPath] = getJString(env, filePath);
        s[kMethod] = getString(env, archive, ix, (PROPID)kpidMethod);
        s[kComment] = b[kIsCommented] ? getString(env, archive, ix, (PROPID)kpidHostOS) : null;
        s[kHostOS] = getString(env, archive, ix, (PROPID)kpidHostOS);
        s[kUser] = getString(env, archive, ix, (PROPID)kpidUser);
        s[kGroup] = getString(env, archive, ix, (PROPID)kpidUser);
        for (int i = 0; i < sizeof(s) / sizeof(s[0]); i++) {
          env->SetObjectArrayElement(strings, i, s[i]);
        }
      }
    } catch (...) {
    }
  }

  JNIEXPORT jlong JNICALL Java_com_zipeg_Z7_extractItem(JNIEnv * env,
                  jobject _this, jlong a, jlong ix, jstring filepath, jstring password) {
    try {
      CArchiveLink* archiveLink = (CArchiveLink*)a;
      if (archiveLink != null) {
        JString path(env, filepath);
        JString pwd(env, password);
        int res = 0;
        jlong hr = extractItem(archiveLink, ix, path.s, pwd.s, &res) & 0xFFFFFFFFLL;
        if (hr == 0) {
          hr = res;
        }
        return res;
      } else {
        return E_FAIL;
      }
    } catch (...) {
        return 0x80020009LL; // DISP_E_EXCEPTION
    }
  }

} // extern "C"
