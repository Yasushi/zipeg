// Windows/FileFind.h

#ifndef __WINDOWS_FILEFIND_H
#define __WINDOWS_FILEFIND_H

#include "../Common/String.h"
#include "FileName.h"
#include "Defs.h"

#include <sys/types.h> /* for DIR */
#ifndef _WINDOWS
#include <dirent.h>
#endif

namespace NWindows {
namespace NFile {
namespace NFind {

namespace NAttributes
{
  inline bool IsReadOnly(DWORD attributes) { return (attributes & FILE_ATTRIBUTE_READONLY) != 0; }
  inline bool IsHidden(DWORD attributes) { return (attributes & FILE_ATTRIBUTE_HIDDEN) != 0; }
  inline bool IsSystem(DWORD attributes) { return (attributes & FILE_ATTRIBUTE_SYSTEM) != 0; }
  inline bool IsDirectory(DWORD attributes) { return (attributes & FILE_ATTRIBUTE_DIRECTORY) != 0; }
  inline bool IsArchived(DWORD attributes) { return (attributes & FILE_ATTRIBUTE_ARCHIVE) != 0; }
  inline bool IsCompressed(DWORD attributes) { return (attributes & FILE_ATTRIBUTE_COMPRESSED) != 0; }
  inline bool IsEncrypted(DWORD attributes) { return (attributes & FILE_ATTRIBUTE_ENCRYPTED) != 0; }
}

#ifndef _WINDOWS

class CFileInfoBase
{ 
  bool MatchesMask(UINT32 mask) const  { return ((Attributes & mask) != 0); }
public:
  DWORD Attributes;
  FILETIME CreationTime;  
  FILETIME LastAccessTime; 
  FILETIME LastWriteTime;
  UINT64 Size;
  
  bool IsDirectory() const { return MatchesMask(FILE_ATTRIBUTE_DIRECTORY); }
  const FILETIME& getLastWriteTime() const { return LastWriteTime; }
  const FILETIME& getLastAccessTime() const { return LastAccessTime; }
  const FILETIME& getCreationTime() const { return CreationTime; }
  void setLastWriteTime(const FILETIME& wt) { LastWriteTime = wt; }
  void setSize(UINT64 s) { Size = s; }
  UINT64 getSize() const { return Size; }
  DWORD getAttributes() const { return Attributes; }
};

#else

class CFileInfoBase : public WIN32_FIND_DATA
{ 
  bool MatchesMask(UINT32 mask) const  { return ((dwFileAttributes & mask) != 0); }
public:
  bool IsDirectory() const { return MatchesMask(FILE_ATTRIBUTE_DIRECTORY); }
  const FILETIME& getLastWriteTime() const { return ftLastWriteTime; }
  const FILETIME& getLastAccessTime() const { return ftLastAccessTime; }
  const FILETIME& getCreationTime() const { return ftCreationTime; }
  void setLastWriteTime(const FILETIME& wt) { ftLastWriteTime = wt; }
  const TCHAR* getFileName() const { return cFileName; }
  DWORD getAttributes() const { return dwFileAttributes; }
  UINT64 getSize() const { return (((UINT64)nFileSizeHigh) << 32) | nFileSizeLow; }
  void setSize(UINT64 s) { nFileSizeLow = (DWORD)s; nFileSizeHigh = (DWORD)(s >> 32); }
};

class CFileInfoBaseW : public WIN32_FIND_DATAW
{ 
  bool MatchesMask(UINT32 mask) const  { return ((dwFileAttributes & mask) != 0); }
public:
  bool IsDirectory() const { return MatchesMask(FILE_ATTRIBUTE_DIRECTORY); }
  const FILETIME& getLastWriteTime() const { return ftLastWriteTime; }
  const FILETIME& getLastAccessTime() const { return ftLastAccessTime; }
  const FILETIME& getCreationTime() const { return ftCreationTime; }
  void setLastWriteTime(const FILETIME& wt) { ftLastWriteTime = wt; }
  const WCHAR* getFileName() const { return cFileName; }
  DWORD getAttributes() const { return dwFileAttributes; }
  UINT64 getSize() const { return (((UINT64)nFileSizeHigh) << 32) | nFileSizeLow; }
  void setSize(UINT64 s) { nFileSizeLow = (DWORD)s; nFileSizeHigh = (DWORD)(s >> 32); }
};

#endif

#ifdef _WINDOWS
class CFileInfo: public CFileInfoBase
{ 
public:
  bool IsDots() const;
};
#else
class CFileInfo: public CFileInfoBase
{ 
public:
  CSysString Name;
  bool IsDots() const;
  const TCHAR* getFileName() const { return Name; }
};
#endif

#ifdef _UNICODE
typedef CFileInfo CFileInfoW;
#else

#ifdef _WINDOWS
class CFileInfoW: public CFileInfoBaseW
{ 
public:
  bool IsDots() const;
};
#else
class CFileInfoW: public CFileInfoBase
{ 
public:
  UString Name;
  bool IsDots() const;
  const WCHAR* getFileName() const { return Name; }
};
#endif
#endif

#ifndef _WINDOWS
class CFindFile
{
  friend class CEnumerator;
  HANDLE _handle;
  DIR *_dirp;
  AString _pattern;
  AString _directory;  
public:
  bool IsHandleAllocated() const { return (_dirp != 0); }
  CFindFile(): _dirp(0) {}
  ~CFindFile() {  Close(); }
  bool FindFirst(LPCTSTR wildcard, CFileInfo &fileInfo);
  bool FindNext(CFileInfo &fileInfo);
  #ifndef _UNICODE
  bool FindFirst(LPCWSTR wildcard, CFileInfoW &fileInfo);
  bool FindNext(CFileInfoW &fileInfo);
  #endif
  bool Close();
};
#else
class CFindFile
{
  friend class CEnumerator;
  HANDLE _handle;
  LPWIN32_FIND_DATA _data;
  LPWIN32_FIND_DATAW _dataw;
public:
  bool IsHandleAllocated() const { return (_handle != INVALID_HANDLE_VALUE); }
  CFindFile(): _handle(INVALID_HANDLE_VALUE) {}
  ~CFindFile() {  Close(); }
  bool FindFirst(LPCTSTR wildcard, CFileInfo &fileInfo) {
      _data = &fileInfo;
      _handle = ::FindFirstFile(wildcard, _data);
      return IsHandleAllocated();
  }
  bool FindNext(CFileInfo &fileInfo) {
      return ::FindNextFile(_handle, _data) != 0;
  }
  #ifndef _UNICODE
  bool FindFirst(LPCWSTR wildcard, CFileInfoW &fileInfo) {
      _dataw = &fileInfo;
      _handle = ::FindFirstFileW(wildcard, _dataw);
      return IsHandleAllocated();
  }
  bool FindNext(CFileInfoW &fileInfo) {
      return ::FindNextFileW(_handle, _dataw) != 0;
  }
  #endif
  bool Close() {
      if (_handle != INVALID_HANDLE_VALUE) {
          ::FindClose(_handle);
      }
      _handle = INVALID_HANDLE_VALUE;
      return true;
  }
};
#endif

bool FindFile(LPCTSTR wildcard, CFileInfo &fileInfo);

bool DoesFileExist(LPCTSTR name);
#ifndef _UNICODE
bool FindFile(LPCWSTR wildcard, CFileInfoW &fileInfo);
bool DoesFileExist(LPCWSTR name);
#endif

class CEnumerator
{
  CFindFile _findFile;
  CSysString _wildcard;
  bool NextAny(CFileInfo &fileInfo);
public:
  CEnumerator(): _wildcard(NName::kAnyStringWildcard) {}
  CEnumerator(const CSysString &wildcard): _wildcard(wildcard) {}
  bool Next(CFileInfo &fileInfo);
  bool Next(CFileInfo &fileInfo, bool &found);
};

#ifdef _UNICODE
typedef CEnumerator CEnumeratorW;
#else
class CEnumeratorW
{
  CFindFile _findFile;
  UString _wildcard;
  bool NextAny(CFileInfoW &fileInfo);
public:
  CEnumeratorW(): _wildcard(NName::kAnyStringWildcard) {}
  CEnumeratorW(const UString &wildcard): _wildcard(wildcard) {}
  bool Next(CFileInfoW &fileInfo);
  bool Next(CFileInfoW &fileInfo, bool &found);
};
#endif

}}}

#endif

