PROG=../../../bin/Formats/Zip.so

LOCAL_FLAGS=-DCOMPRESS_MT

MY_WINDOWS= \
	$(MY_HOME)/wine_date_and_time.o \
	$(MY_HOME)/myModuleFileName.o \
	$(MY_HOME)/myGetNumberOfProcessors.o \
	$(MY_HOME)/myGetTickCount.o

include ../../../makefile.machine

LOCAL_SHARED=$(LINK_SHARED)
LIBS=$(LOCAL_LIBS_DLL)

OBJS = \
../../../Common/MyWindows.o \
../../../Common/Vector.o \
../../../Common/Alloc.o \
../../../Common/StringConvert.o \
../../../Common/String.o \
../../../Common/CRC.o \
../../../Common/StringToInt.o \
../../../Common/IntToString.o \
DLL.o \
FileFind.o \
PropVariant.o \
Synchronization.o \
CodecsPath.o \
CoderLoader.o \
CrossThreadProgress.o \
FilterCoder.o \
InStreamWithCRC.o \
ItemNameUtils.o \
OutStreamWithCRC.o \
ParseProperties.o \
LimitedStreams.o \
OffsetStream.o \
ProgressUtils.o \
StreamBinder.o \
StreamObjects.o \
StreamUtils.o \
ZipAddCommon.o \
ZipHandler.o \
ZipHandlerOut.o \
ZipHeader.o \
ZipIn.o \
ZipItem.o \
ZipOut.o \
ZipUpdate.o \
CopyCoder.o \
ZipCipher.o \
ZipCrypto.o \
7zMethodID.o \
7zMethods.o \
InBuffer.o \
OutBuffer.o \
ShrinkDecoder.o \
ImplodeDecoder.o \
ImplodeHuffmanDecoder.o \
LZOutWindow.o \
LSBFDecoder.o \
MemBlocks.o \
OutMemStream.o \
ProgressMt.o \
HmacSha1.o \
Pbkdf2HmacSha1.o \
RandGen.o \
Sha1.o \
WzAES.o

include ../../../makefile.glb

