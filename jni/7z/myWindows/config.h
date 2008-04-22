
#if !defined(__DJGPP__)

#ifndef __CYGWIN__
  #define FILESYSTEM_IS_CASE_SENSITIVE 1
#endif

  #if !defined(ENV_MACOSX) && !defined(ENV_BEOS)

    /* <wchar.h> */
    #define HAVE_WCHAR_H

    /* <wctype.h> */
    #define HAVE_WCTYPE_H

    /* mbrtowc */
    #define HAVE_MBRTOWC

    /* towupper */
    #define HAVE_TOWUPPER

  #endif /* !ENV_MACOSX && !ENV_BEOS */

  #if !defined(ENV_BEOS)
  #define HAVE_GETPASS
  #endif

  /* lstat, readlink and S_ISLNK */
  #define HAVE_LSTAT

  /* <locale.h> */
  #define HAVE_LOCALE

  /* mbstowcs */
  #define HAVE_MBSTOWCS

  /* wcstombs */
  #define HAVE_WCSTOMBS

#endif /* !__DJGPP__ */

#if !defined(ENV_BEOS) && !defined(WIN32)
#define HAVE_PTHREAD
#endif

#define MAX_PATHNAME_LEN   1024

#ifdef _WINDOWS

#undef HAVE_LSTAT

#define EXCLUDE_COM
#define NO_REGISTRY
#define FORMAT_7Z
#define FORMAT_BZIP2
#define FORMAT_GZIP
#define FORMAT_SPLIT
#define FORMAT_TAR
#define FORMAT_Z
#define FORMAT_ZIP
#define FORMAT_ARJ
#define FORMAT_RAR
#define FORMAT_LZH
#define FORMAT_CHM
#define FORMAT_CAB
#define FORMAT_RPM
#define FORMAT_CPIO
#define FORMAT_ISO
#define FORMAT_NSIS

#undef  COMPRESS_MT
#undef  COMPRESS_MF_MT
#undef  COMPRESS_BZIP2_MT
#define _ST_MODE

#define COMPRESS_BCJ_X86
#define COMPRESS_BCJ2
#define COMPRESS_BZIP2
#define COMPRESS_COPY
#define COMPRESS_DEFLATE
#define COMPRESS_DEFLATE64
#define COMPRESS_IMPLODE
#define COMPRESS_LZMA
#define COMPRESS_PPMD
#define CRYPTO_7ZAES
#define CRYPTO_AES
#define CRYPTO_ZIP
#define _FILE_OFFSET_BITS 64
#define _LARGEFILE_SOURCE

#endif
