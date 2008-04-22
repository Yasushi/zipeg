#include "StdAfx.h"

#include <sys/types.h>
#include <sys/stat.h>

#ifdef _WINDOWS
#pragma warning(disable :4615) // unknown user warning type
#include <windows.h>
#endif

#define NEED_NAME_WINDOWS_TO_UNIX
#include "myPrivate.h"

#include "Common/StringConvert.h"


void myAddExeFlag(LPCTSTR filename)
{
	const char * name = nameWindowToUnix(filename);
	// printf("myAddExeFlag(%s)\n",name);
	chmod(name,0777);
}

void myAddExeFlag(const UString &u_name)
{
	myAddExeFlag(UnicodeStringToMultiByte(u_name, CP_ACP));
}

