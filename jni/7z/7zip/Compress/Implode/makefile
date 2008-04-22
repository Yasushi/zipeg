PROG=../../../bin/Codecs/Implode.so

LOCAL_FLAGS=

MY_WINDOWS=

include ../../../makefile.machine

LOCAL_SHARED=$(LINK_SHARED)
LIBS=$(LOCAL_LIBS)

OBJS = \
../../../Common/MyWindows.o \
../../../Common/Alloc.o \
StreamUtils.o \
OutBuffer.o \
InBuffer.o \
LSBFDecoder.o \
LZOutWindow.o \
ImplodeDecoder.o \
ImplodeHuffmanDecoder.o

include ../../../makefile.glb

