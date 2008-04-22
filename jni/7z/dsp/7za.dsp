# Microsoft Developer Studio Project File - Name="7za" - Package Owner=<4>
# Microsoft Developer Studio Generated Build File, Format Version 5.00
# ** DO NOT EDIT **

# TARGTYPE "Win32 (x86) Dynamic-Link Library" 0x0102

CFG=7za - Win32 Debug
!MESSAGE This is not a valid makefile. To build this project using NMAKE,
!MESSAGE use the Export Makefile command and run
!MESSAGE 
!MESSAGE NMAKE /f "7za.mak".
!MESSAGE 
!MESSAGE You can specify a configuration when running NMAKE
!MESSAGE by defining the macro CFG on the command line. For example:
!MESSAGE 
!MESSAGE NMAKE /f "7za.mak" CFG="7za - Win32 Debug"
!MESSAGE 
!MESSAGE Possible choices for configuration are:
!MESSAGE 
!MESSAGE "7za - Win32 Release" (based on "Win32 (x86) Dynamic-Link Library")
!MESSAGE "7za - Win32 Debug" (based on "Win32 (x86) Dynamic-Link Library")
!MESSAGE 

# Begin Project
# PROP Scc_ProjName ""
# PROP Scc_LocalPath ""
CPP=cl.exe
MTL=midl.exe
RSC=rc.exe

!IF  "$(CFG)" == "7za - Win32 Release"

# PROP BASE Use_MFC 0
# PROP BASE Use_Debug_Libraries 0
# PROP BASE Output_Dir "Release"
# PROP BASE Intermediate_Dir "Release"
# PROP BASE Target_Dir ""
# PROP Use_MFC 0
# PROP Use_Debug_Libraries 0
# PROP Output_Dir "Release"
# PROP Intermediate_Dir "Release"
# PROP Ignore_Export_Lib 0
# PROP Target_Dir ""
# ADD BASE CPP /nologo /MT /W3 /GX /O2 /D "WIN32" /D "NDEBUG" /D "_WINDOWS" /YX /FD /c
# ADD CPP /nologo /MT /W3 /GX /O2 /I ".." /I "../myWindows" /D "_WIN32" /D "WIN32" /D "NDEBUG" /D "_WINDOWS" /D "ENV_WIN32" /D _WIN32_WINNT=0x0500 /U "HAVE_PTHREAD" /FR /FD /c
# SUBTRACT CPP /YX
# ADD BASE MTL /nologo /D "NDEBUG" /mktyplib203 /o NUL /win32
# ADD MTL /nologo /D "NDEBUG" /mktyplib203 /o NUL /win32
# ADD BASE RSC /l 0x409 /d "NDEBUG"
# ADD RSC /l 0x409 /d "NDEBUG"
BSC32=bscmake.exe
# ADD BASE BSC32 /nologo
# ADD BSC32 /nologo
LINK32=link.exe
# ADD BASE LINK32 kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib /nologo /subsystem:windows /dll /machine:I386
# ADD LINK32 kernel32.lib user32.lib oleaut32.lib /nologo /subsystem:windows /dll /machine:I386 /out:"../bin/7za-win-i386.dll"

!ELSEIF  "$(CFG)" == "7za - Win32 Debug"

# PROP BASE Use_MFC 0
# PROP BASE Use_Debug_Libraries 1
# PROP BASE Output_Dir "Debug"
# PROP BASE Intermediate_Dir "Debug"
# PROP BASE Target_Dir ""
# PROP Use_MFC 0
# PROP Use_Debug_Libraries 1
# PROP Output_Dir "Debug"
# PROP Intermediate_Dir "Debug"
# PROP Ignore_Export_Lib 0
# PROP Target_Dir ""
# ADD BASE CPP /nologo /MTd /W3 /Gm /GX /Zi /Od /D "WIN32" /D "_DEBUG" /D "_WINDOWS" /YX /FD /c
# ADD CPP /nologo /MTd /W3 /Gm /GX /Zi /Od /I ".." /I "../myWindows" /D "_WIN32" /D "WIN32" /D "_DEBUG" /D "_WINDOWS" /D "ENV_WIN32" /D _WIN32_WINNT=0x0500 /U "HAVE_PTHREAD" /FD /c
# SUBTRACT CPP /YX
# ADD BASE MTL /nologo /D "_DEBUG" /mktyplib203 /o NUL /win32
# ADD MTL /nologo /D "_DEBUG" /mktyplib203 /o NUL /win32
# ADD BASE RSC /l 0x409 /d "_DEBUG"
# ADD RSC /l 0x409 /d "_DEBUG"
BSC32=bscmake.exe
# ADD BASE BSC32 /nologo
# ADD BSC32 /nologo
LINK32=link.exe
# ADD BASE LINK32 kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib /nologo /subsystem:windows /dll /debug /machine:I386 /pdbtype:sept
# ADD LINK32 kernel32.lib user32.lib oleaut32.lib /nologo /subsystem:windows /dll /debug /machine:I386 /out:"../bin/7za-win-i386.dll" /pdbtype:sept

!ENDIF 

# Begin Target

# Name "7za - Win32 Release"
# Name "7za - Win32 Debug"
# Begin Group "Common"

# PROP Default_Filter ""
# Begin Source File

SOURCE=..\Common\Alloc.cpp
# End Source File
# Begin Source File

SOURCE=..\Common\Alloc.h
# End Source File
# Begin Source File

SOURCE=..\Common\AutoPtr.h
# End Source File
# Begin Source File

SOURCE=..\Common\Buffer.h
# End Source File
# Begin Source File

SOURCE=..\Common\ComTry.h
# End Source File
# Begin Source File

SOURCE=..\Common\CRC.cpp
# End Source File
# Begin Source File

SOURCE=..\Common\CRC.h
# End Source File
# Begin Source File

SOURCE=..\Common\Defs.h
# End Source File
# Begin Source File

SOURCE=..\Common\DynamicBuffer.h
# End Source File
# Begin Source File

SOURCE=..\Common\Exception.h
# End Source File
# Begin Source File

SOURCE=..\Common\IntToString.cpp
# End Source File
# Begin Source File

SOURCE=..\Common\IntToString.h
# End Source File
# Begin Source File

SOURCE=..\Common\ListFileUtils.cpp
# End Source File
# Begin Source File

SOURCE=..\Common\ListFileUtils.h
# End Source File
# Begin Source File

SOURCE=..\Common\MyCom.h
# End Source File
# Begin Source File

SOURCE=..\Common\MyGuidDef.h
# End Source File
# Begin Source File

SOURCE=..\Common\MyInitGuid.h
# End Source File
# Begin Source File

SOURCE=..\Common\MyUnknown.h
# End Source File
# Begin Source File

SOURCE=..\Common\MyWindows.cpp
# End Source File
# Begin Source File

SOURCE=..\Common\MyWindows.h
# End Source File
# Begin Source File

SOURCE=..\Common\NewHandler.h
# End Source File
# Begin Source File

SOURCE=..\Common\StdInStream.cpp
# End Source File
# Begin Source File

SOURCE=..\Common\StdInStream.h
# End Source File
# Begin Source File

SOURCE=..\Common\StdOutStream.cpp
# End Source File
# Begin Source File

SOURCE=..\Common\StdOutStream.h
# End Source File
# Begin Source File

SOURCE=..\Common\String.cpp
# End Source File
# Begin Source File

SOURCE=..\Common\String.h
# End Source File
# Begin Source File

SOURCE=..\Common\StringConvert.cpp
# End Source File
# Begin Source File

SOURCE=..\Common\StringConvert.h
# End Source File
# Begin Source File

SOURCE=..\Common\StringToInt.cpp
# End Source File
# Begin Source File

SOURCE=..\Common\StringToInt.h
# End Source File
# Begin Source File

SOURCE=..\Common\Types.h
# End Source File
# Begin Source File

SOURCE=..\Common\UTFConvert.cpp
# End Source File
# Begin Source File

SOURCE=..\Common\UTFConvert.h
# End Source File
# Begin Source File

SOURCE=..\Common\Vector.cpp
# End Source File
# Begin Source File

SOURCE=..\Common\Vector.h
# End Source File
# Begin Source File

SOURCE=..\Common\Wildcard.cpp
# End Source File
# Begin Source File

SOURCE=..\Common\Wildcard.h
# End Source File
# End Group
# Begin Group "7zip"

# PROP Default_Filter ""
# Begin Group "Common No. 1"

# PROP Default_Filter ""
# Begin Source File

SOURCE=..\7zip\Common\FilePathAutoRename.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Common\FilePathAutoRename.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Common\FileStreams.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Common\FileStreams.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Common\InBuffer.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Common\InBuffer.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Common\InOutTempBuffer.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Common\InOutTempBuffer.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Common\LimitedStreams.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Common\LimitedStreams.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Common\LockedStream.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Common\LockedStream.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Common\LSBFDecoder.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Common\LSBFDecoder.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Common\LSBFEncoder.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Common\LSBFEncoder.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Common\MemBlocks.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Common\MemBlocks.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Common\MSBFDecoder.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Common\MSBFEncoder.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Common\OffsetStream.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Common\OffsetStream.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Common\OutBuffer.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Common\OutBuffer.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Common\OutMemStream.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Common\OutMemStream.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Common\ProgressMt.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Common\ProgressMt.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Common\ProgressUtils.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Common\ProgressUtils.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Common\StreamBinder.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Common\StreamBinder.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Common\StreamObjects.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Common\StreamObjects.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Common\StreamUtils.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Common\StreamUtils.h
# End Source File
# End Group
# Begin Group "Archive"

# PROP Default_Filter ""
# Begin Group "7z"

# PROP Default_Filter ""
# Begin Source File

SOURCE=..\7zip\Archive\7z\7zCompressionMode.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\7z\7zCompressionMode.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\7z\7zDecode.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\7z\7zDecode.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\7z\7zEncode.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\7z\7zEncode.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\7z\7zExtract.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\7z\7zFolderInStream.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\7z\7zFolderInStream.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\7z\7zFolderOutStream.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\7z\7zFolderOutStream.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\7z\7zHandler.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\7z\7zHandler.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\7z\7zHandlerOut.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\7z\7zHeader.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\7z\7zHeader.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\7z\7zIn.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\7z\7zIn.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\7z\7zItem.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\7z\7zMethodID.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\7z\7zMethodID.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\7z\7zMethods.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\7z\7zMethods.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\7z\7zOut.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\7z\7zOut.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\7z\7zProperties.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\7z\7zProperties.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\7z\7zSpecStream.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\7z\7zSpecStream.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\7z\7zUpdate.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\7z\7zUpdate.h
# End Source File
# End Group
# Begin Group "Arj No. 1"

# PROP Default_Filter ""
# Begin Source File

SOURCE=..\7zip\Archive\Arj\ArjHandler.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Arj\ArjHandler.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Arj\ArjHeader.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Arj\ArjIn.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Arj\ArjIn.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Arj\ArjItem.h
# End Source File
# End Group
# Begin Group "BZip2 No. 1"

# PROP Default_Filter ""
# Begin Source File

SOURCE=..\7zip\Archive\BZip2\BZip2Handler.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\BZip2\BZip2Handler.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\BZip2\BZip2HandlerOut.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\BZip2\BZip2Item.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\BZip2\BZip2Update.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\BZip2\BZip2Update.h
# End Source File
# End Group
# Begin Group "Cab"

# PROP Default_Filter ""
# Begin Source File

SOURCE=..\7zip\Archive\Cab\CabBlockInStream.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Cab\CabBlockInStream.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Cab\CabHandler.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Cab\CabHandler.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Cab\CabHeader.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Cab\CabHeader.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Cab\CabIn.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Cab\CabIn.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Cab\CabItem.h
# End Source File
# End Group
# Begin Group "Chm"

# PROP Default_Filter ""
# Begin Source File

SOURCE=..\7zip\Archive\Chm\ChmHandler.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Chm\ChmHandler.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Chm\ChmHeader.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Chm\ChmHeader.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Chm\ChmIn.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Chm\ChmIn.h
# End Source File
# End Group
# Begin Group "Common No. 2"

# PROP Default_Filter ""
# Begin Source File

SOURCE=..\7zip\Archive\Common\CodecsPath.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Common\CodecsPath.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Common\CoderLoader.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Common\CoderLoader.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Common\CoderMixer2.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Common\CoderMixer2.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Common\CoderMixer2MT.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Common\CoderMixer2MT.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Common\CoderMixer2ST.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Common\CoderMixer2ST.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Common\CrossThreadProgress.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Common\CrossThreadProgress.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Common\DummyOutStream.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Common\DummyOutStream.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Common\FilterCoder.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Common\FilterCoder.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Common\InStreamWithCRC.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Common\InStreamWithCRC.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Common\ItemNameUtils.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Common\ItemNameUtils.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Common\MultiStream.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Common\MultiStream.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Common\OutStreamWithCRC.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Common\OutStreamWithCRC.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Common\ParseProperties.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Common\ParseProperties.h
# End Source File
# End Group
# Begin Group "Cpio"

# PROP Default_Filter ""
# Begin Source File

SOURCE=..\7zip\Archive\Cpio\CpioHandler.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Cpio\CpioHandler.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Cpio\CpioHeader.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Cpio\CpioHeader.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Cpio\CpioIn.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Cpio\CpioIn.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Cpio\CpioItem.h
# End Source File
# End Group
# Begin Group "Deb"

# PROP Default_Filter ""
# Begin Source File

SOURCE=..\7zip\Archive\Deb\DebHandler.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Deb\DebHandler.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Deb\DebHeader.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Deb\DebHeader.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Deb\DebIn.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Deb\DebIn.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Deb\DebItem.h
# End Source File
# End Group
# Begin Group "GZip"

# PROP Default_Filter ""
# Begin Source File

SOURCE=..\7zip\Archive\GZip\GZipHandler.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\GZip\GZipHandler.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\GZip\GZipHandlerOut.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\GZip\GZipHeader.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\GZip\GZipHeader.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\GZip\GZipIn.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\GZip\GZipIn.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\GZip\GZipItem.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\GZip\GZipOut.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\GZip\GZipOut.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\GZip\GZipUpdate.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\GZip\GZipUpdate.h
# End Source File
# End Group
# Begin Group "Iso"

# PROP Default_Filter ""
# Begin Source File

SOURCE=..\7zip\Archive\Iso\IsoHandler.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Iso\IsoHandler.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Iso\IsoHeader.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Iso\IsoHeader.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Iso\IsoIn.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Iso\IsoIn.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Iso\IsoItem.h
# End Source File
# End Group
# Begin Group "Lzh No. 1"

# PROP Default_Filter ""
# Begin Source File

SOURCE=..\7zip\Archive\Lzh\LzhCRC.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Lzh\LzhCRC.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Lzh\LzhHandler.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Lzh\LzhHandler.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Lzh\LzhHeader.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Lzh\LzhIn.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Lzh\LzhIn.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Lzh\LzhItem.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Lzh\LzhOutStreamWithCRC.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Lzh\LzhOutStreamWithCRC.h
# End Source File
# End Group
# Begin Group "Nsis"

# PROP Default_Filter ""
# Begin Source File

SOURCE=..\7zip\Archive\Nsis\NsisDecode.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Nsis\NsisDecode.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Nsis\NsisHandler.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Nsis\NsisHandler.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Nsis\NsisIn.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Nsis\NsisIn.h
# End Source File
# End Group
# Begin Group "Rar"

# PROP Default_Filter ""
# Begin Source File

SOURCE=..\7zip\Archive\Rar\RarHandler.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Rar\RarHandler.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Rar\RarHeader.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Rar\RarHeader.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Rar\RarIn.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Rar\RarIn.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Rar\RarItem.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Rar\RarItem.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Rar\RarVolumeInStream.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Rar\RarVolumeInStream.h
# End Source File
# End Group
# Begin Group "RPM"

# PROP Default_Filter ""
# Begin Source File

SOURCE=..\7zip\Archive\RPM\RpmHandler.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\RPM\RpmHandler.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\RPM\RpmHeader.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\RPM\RpmIn.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\RPM\RpmIn.h
# End Source File
# End Group
# Begin Group "Split"

# PROP Default_Filter ""
# Begin Source File

SOURCE=..\7zip\Archive\Split\SplitHandler.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Split\SplitHandler.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Split\SplitHandlerOut.cpp
# End Source File
# End Group
# Begin Group "Tar"

# PROP Default_Filter ""
# Begin Source File

SOURCE=..\7zip\Archive\Tar\TarHandler.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Tar\TarHandler.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Tar\TarHandlerOut.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Tar\TarHeader.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Tar\TarHeader.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Tar\TarIn.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Tar\TarIn.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Tar\TarItem.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Tar\TarOut.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Tar\TarOut.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Tar\TarUpdate.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Tar\TarUpdate.h
# End Source File
# End Group
# Begin Group "Z No. 1"

# PROP Default_Filter ""
# Begin Source File

SOURCE=..\7zip\Archive\Z\ZHandler.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Z\ZHandler.h
# End Source File
# End Group
# Begin Group "Zip No. 1"

# PROP Default_Filter ""
# Begin Source File

SOURCE=..\7zip\Archive\Zip\ZipAddCommon.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Zip\ZipAddCommon.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Zip\ZipCompressionMode.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Zip\ZipHandler.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Zip\ZipHandler.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Zip\ZipHandlerOut.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Zip\ZipHeader.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Zip\ZipHeader.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Zip\ZipIn.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Zip\ZipIn.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Zip\ZipItem.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Zip\ZipItem.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Zip\ZipItemEx.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Zip\ZipOut.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Zip\ZipOut.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Zip\ZipUpdate.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Archive\Zip\ZipUpdate.h
# End Source File
# End Group
# Begin Source File

SOURCE=..\7zip\Archive\IArchive.h
# End Source File
# End Group
# Begin Group "Crypto"

# PROP Default_Filter ""
# Begin Group "7zAES"

# PROP Default_Filter ""
# Begin Source File

SOURCE=..\7zip\Crypto\7zAES\7zAES.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Crypto\7zAES\7zAES.h
# End Source File
# End Group
# Begin Group "AES"

# PROP Default_Filter ""
# Begin Source File

SOURCE=..\7zip\Crypto\AES\aes.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Crypto\AES\AES_CBC.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Crypto\AES\aescpp.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Crypto\AES\aescrypt.c
# End Source File
# Begin Source File

SOURCE=..\7zip\Crypto\AES\aeskey.c
# End Source File
# Begin Source File

SOURCE=..\7zip\Crypto\AES\aesopt.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Crypto\AES\aestab.c
# End Source File
# Begin Source File

SOURCE=..\7zip\Crypto\AES\MyAES.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Crypto\AES\MyAES.h
# End Source File
# End Group
# Begin Group "Hash"

# PROP Default_Filter ""
# Begin Source File

SOURCE=..\7zip\Crypto\Hash\HmacSha1.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Crypto\Hash\HmacSha1.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Crypto\Hash\Pbkdf2HmacSha1.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Crypto\Hash\Pbkdf2HmacSha1.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Crypto\Hash\RandGen.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Crypto\Hash\RandGen.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Crypto\Hash\RotateDefs.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Crypto\Hash\Sha1.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Crypto\Hash\Sha1.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Crypto\Hash\Sha256.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Crypto\Hash\Sha256.h
# End Source File
# End Group
# Begin Group "Rar20"

# PROP Default_Filter ""
# Begin Source File

SOURCE=..\7zip\Crypto\Rar20\Rar20Cipher.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Crypto\Rar20\Rar20Cipher.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Crypto\Rar20\Rar20Crypto.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Crypto\Rar20\Rar20Crypto.h
# End Source File
# End Group
# Begin Group "RarAES"

# PROP Default_Filter ""
# Begin Source File

SOURCE=..\7zip\Crypto\RarAES\RarAES.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Crypto\RarAES\RarAES.h
# End Source File
# End Group
# Begin Group "WzAES"

# PROP Default_Filter ""
# Begin Source File

SOURCE=..\7zip\Crypto\WzAES\WzAES.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Crypto\WzAES\WzAES.h
# End Source File
# End Group
# Begin Group "Zip"

# PROP Default_Filter ""
# Begin Source File

SOURCE=..\7zip\Crypto\Zip\ZipCipher.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Crypto\Zip\ZipCipher.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Crypto\Zip\ZipCrypto.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Crypto\Zip\ZipCrypto.h
# End Source File
# End Group
# End Group
# Begin Group "Compress"

# PROP Default_Filter ""
# Begin Group "Arj"

# PROP Default_Filter ""
# Begin Source File

SOURCE=..\7zip\Compress\Arj\ArjDecoder1.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\Arj\ArjDecoder1.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\Arj\ArjDecoder2.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\Arj\ArjDecoder2.h
# End Source File
# End Group
# Begin Group "Branch"

# PROP Default_Filter ""
# Begin Source File

SOURCE=..\7zip\Compress\Branch\ARM.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\Branch\ARM.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\Branch\ARMThumb.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\Branch\ARMThumb.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\Branch\BranchARM.c
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\Branch\BranchARM.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\Branch\BranchARMThumb.c
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\Branch\BranchARMThumb.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\Branch\BranchCoder.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\Branch\BranchCoder.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\Branch\BranchIA64.c
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\Branch\BranchIA64.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\Branch\BranchPPC.c
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\Branch\BranchPPC.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\Branch\BranchSPARC.c
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\Branch\BranchSPARC.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\Branch\BranchTypes.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\Branch\BranchX86.c
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\Branch\BranchX86.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\Branch\IA64.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\Branch\IA64.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\Branch\PPC.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\Branch\PPC.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\Branch\SPARC.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\Branch\SPARC.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\Branch\x86.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\Branch\x86.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\Branch\x86_2.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\Branch\x86_2.h
# End Source File
# End Group
# Begin Group "BWT"

# PROP Default_Filter ""
# Begin Source File

SOURCE=..\7zip\Compress\BWT\BlockSort.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\BWT\BlockSortBWT.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\BWT\Mtf8.h
# End Source File
# End Group
# Begin Group "ByteSwap"

# PROP Default_Filter ""
# Begin Source File

SOURCE=..\7zip\Compress\ByteSwap\ByteSwap.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\ByteSwap\ByteSwap.h
# End Source File
# End Group
# Begin Group "BZip2"

# PROP Default_Filter ""
# Begin Group "Original"

# PROP Default_Filter ""
# Begin Source File

SOURCE=..\7zip\Compress\BZip2\Original\blocksort.c
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\BZip2\Original\bzip2.c
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\BZip2\Original\bzip2recover.c
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\BZip2\Original\bzlib.c
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\BZip2\Original\bzlib.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\BZip2\Original\bzlib_private.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\BZip2\Original\compress.c
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\BZip2\Original\crctable.c
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\BZip2\Original\decompress.c
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\BZip2\Original\huffman.c
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\BZip2\Original\mk251.c
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\BZip2\Original\randtable.c
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\BZip2\Original\spewG.c
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\BZip2\Original\unzcrash.c
# End Source File
# End Group
# Begin Source File

SOURCE=..\7zip\Compress\BZip2\BZip2Const.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\BZip2\BZip2CRC.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\BZip2\BZip2CRC.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\BZip2\BZip2Decoder.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\BZip2\BZip2Decoder.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\BZip2\BZip2Encoder.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\BZip2\BZip2Encoder.h
# End Source File
# End Group
# Begin Group "Copy"

# PROP Default_Filter ""
# Begin Source File

SOURCE=..\7zip\Compress\Copy\CopyCoder.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\Copy\CopyCoder.h
# End Source File
# End Group
# Begin Group "Deflate"

# PROP Default_Filter ""
# Begin Source File

SOURCE=..\7zip\Compress\Deflate\DeflateConst.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\Deflate\DeflateDecoder.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\Deflate\DeflateDecoder.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\Deflate\DeflateEncoder.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\Deflate\DeflateEncoder.h
# End Source File
# End Group
# Begin Group "Huffman"

# PROP Default_Filter ""
# Begin Source File

SOURCE=..\7zip\Compress\Huffman\HuffmanDecoder.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\Huffman\HuffmanEncoder.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\Huffman\HuffmanEncoder.h
# End Source File
# End Group
# Begin Group "Implode"

# PROP Default_Filter ""
# Begin Source File

SOURCE=..\7zip\Compress\Implode\ImplodeDecoder.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\Implode\ImplodeDecoder.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\Implode\ImplodeHuffmanDecoder.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\Implode\ImplodeHuffmanDecoder.h
# End Source File
# End Group
# Begin Group "LZ"

# PROP Default_Filter ""
# Begin Group "BinTree"

# PROP Default_Filter ""
# Begin Source File

SOURCE=..\7zip\Compress\LZ\BinTree\BinTree.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\LZ\BinTree\BinTree2.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\LZ\BinTree\BinTree3.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\LZ\BinTree\BinTree3Z.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\LZ\BinTree\BinTree4.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\LZ\BinTree\BinTreeMain.h
# End Source File
# End Group
# Begin Group "HashChain"

# PROP Default_Filter ""
# Begin Source File

SOURCE=..\7zip\Compress\LZ\HashChain\HC2.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\LZ\HashChain\HC3.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\LZ\HashChain\HC4.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\LZ\HashChain\HCMain.h
# End Source File
# End Group
# Begin Group "MT"

# PROP Default_Filter ""
# Begin Source File

SOURCE=..\7zip\Compress\LZ\MT\MT.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\LZ\MT\MT.h
# End Source File
# End Group
# Begin Source File

SOURCE=..\7zip\Compress\LZ\IMatchFinder.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\LZ\LZInWindow.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\LZ\LZInWindow.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\LZ\LZOutWindow.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\LZ\LZOutWindow.h
# End Source File
# End Group
# Begin Group "Lzh"

# PROP Default_Filter ""
# Begin Source File

SOURCE=..\7zip\Compress\Lzh\LzhDecoder.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\Lzh\LzhDecoder.h
# End Source File
# End Group
# Begin Group "LZMA"

# PROP Default_Filter ""
# Begin Source File

SOURCE=..\7zip\Compress\LZMA\LZMA.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\LZMA\LZMADecoder.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\LZMA\LZMADecoder.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\LZMA\LZMAEncoder.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\LZMA\LZMAEncoder.h
# End Source File
# End Group
# Begin Group "Lzx"

# PROP Default_Filter ""
# Begin Source File

SOURCE=..\7zip\Compress\Lzx\Lzx.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\Lzx\Lzx86Converter.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\Lzx\Lzx86Converter.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\Lzx\LzxDecoder.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\Lzx\LzxDecoder.h
# End Source File
# End Group
# Begin Group "PPMD"

# PROP Default_Filter ""
# Begin Source File

SOURCE=..\7zip\Compress\PPMD\PPMDContext.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\PPMD\PPMDDecode.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\PPMD\PPMDDecoder.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\PPMD\PPMDDecoder.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\PPMD\PPMDEncode.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\PPMD\PPMDEncoder.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\PPMD\PPMDEncoder.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\PPMD\PPMDSubAlloc.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\PPMD\PPMDType.h
# End Source File
# End Group
# Begin Group "Quantum"

# PROP Default_Filter ""
# Begin Source File

SOURCE=..\7zip\Compress\Quantum\QuantumDecoder.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\Quantum\QuantumDecoder.h
# End Source File
# End Group
# Begin Group "RangeCoder"

# PROP Default_Filter ""
# Begin Source File

SOURCE=..\7zip\Compress\RangeCoder\RangeCoder.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\RangeCoder\RangeCoderBit.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\RangeCoder\RangeCoderBit.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\RangeCoder\RangeCoderBitTree.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\RangeCoder\RangeCoderOpt.h
# End Source File
# End Group
# Begin Group "Rar20 No. 1"

# PROP Default_Filter ""
# Begin Source File

SOURCE=..\7zip\Compress\Rar20\Rar20Const.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\Rar20\Rar20Decoder.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\Rar20\Rar20Decoder.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\Rar20\Rar20ExtConst.h
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\Rar20\Rar20Multimedia.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\Rar20\Rar20Multimedia.h
# End Source File
# End Group
# Begin Group "Rar29"

# PROP Default_Filter ""
# Begin Group "Original No. 1"

# PROP Default_Filter ""
# Begin Source File

SOURCE=..\7zip\Compress\Rar29\Original\crcRar29.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\Rar29\Original\errhnd.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\Rar29\Original\getbits.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\Rar29\Original\int64.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\Rar29\Original\rarvm.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\Rar29\Original\rdwrfn.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\Rar29\Original\resource.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\Rar29\Original\smallfn.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\Rar29\Original\system.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\Rar29\Original\unpack.cpp
# End Source File
# End Group
# Begin Source File

SOURCE=..\7zip\Compress\Rar29\Rar29Decoder.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\Rar29\Rar29Decoder.h
# End Source File
# End Group
# Begin Group "Shrink"

# PROP Default_Filter ""
# Begin Source File

SOURCE=..\7zip\Compress\Shrink\ShrinkDecoder.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\Shrink\ShrinkDecoder.h
# End Source File
# End Group
# Begin Group "Z"

# PROP Default_Filter ""
# Begin Source File

SOURCE=..\7zip\Compress\Z\ZDecoder.cpp
# End Source File
# Begin Source File

SOURCE=..\7zip\Compress\Z\ZDecoder.h
# End Source File
# End Group
# End Group
# Begin Source File

SOURCE=..\7zip\ICoder.h
# End Source File
# Begin Source File

SOURCE=..\7zip\IPassword.h
# End Source File
# Begin Source File

SOURCE=..\7zip\IProgress.h
# End Source File
# Begin Source File

SOURCE=..\7zip\IStream.h
# End Source File
# Begin Source File

SOURCE=..\7zip\MyVersion.h
# End Source File
# Begin Source File

SOURCE=..\7zip\PropID.h
# End Source File
# End Group
# Begin Group "myWindows"

# PROP Default_Filter ""
# Begin Source File

SOURCE=..\myWindows\config.h
# End Source File
# Begin Source File

SOURCE=..\myWindows\myGetNumberOfProcessors.cpp
# End Source File
# Begin Source File

SOURCE=..\myWindows\myModuleFileName.cpp
# End Source File
# Begin Source File

SOURCE=..\myWindows\myPrivate.h
# End Source File
# Begin Source File

SOURCE=..\myWindows\StdAfx.h
# End Source File
# End Group
# Begin Group "Windows"

# PROP Default_Filter ""
# Begin Source File

SOURCE=..\Windows\Defs.h
# End Source File
# Begin Source File

SOURCE=..\Windows\Error.cpp
# End Source File
# Begin Source File

SOURCE=..\Windows\Error.h
# End Source File
# Begin Source File

SOURCE=..\Windows\FileDir.cpp
# End Source File
# Begin Source File

SOURCE=..\Windows\FileDir.h
# End Source File
# Begin Source File

SOURCE=..\Windows\FileFind.cpp
# End Source File
# Begin Source File

SOURCE=..\Windows\FileFind.h
# End Source File
# Begin Source File

SOURCE=..\Windows\FileIO.cpp
# End Source File
# Begin Source File

SOURCE=..\Windows\FileIO.h
# End Source File
# Begin Source File

SOURCE=..\Windows\FileName.cpp
# End Source File
# Begin Source File

SOURCE=..\Windows\FileName.h
# End Source File
# Begin Source File

SOURCE=..\Windows\PropVariant.cpp
# End Source File
# Begin Source File

SOURCE=..\Windows\PropVariant.h
# End Source File
# Begin Source File

SOURCE=..\Windows\PropVariantConversions.cpp
# End Source File
# Begin Source File

SOURCE=..\Windows\PropVariantConversions.h
# End Source File
# Begin Source File

SOURCE=..\Windows\Synchronization.cpp
# End Source File
# Begin Source File

SOURCE=..\Windows\Synchronization.h
# End Source File
# Begin Source File

SOURCE=..\Windows\System.h
# End Source File
# Begin Source File

SOURCE=..\Windows\Thread.h
# End Source File
# Begin Source File

SOURCE=..\Windows\Time.h
# End Source File
# End Group
# End Target
# End Project
