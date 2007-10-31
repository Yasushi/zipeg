package com.zipeg;

import java.io.RandomAccessFile;
import java.io.IOException;

// http://developer.apple.com/documentation/mac/MoreToolbox/MoreToolbox-99.html
//
// some info from http://www.amug.org/~glguerin/sw/macbinary/overview.html
//
// because resource offset is 3 bytes the size of resource fork cannot be > 2^24 + map size
// thus 31 bit unsigned int is enough for offsets... (Actually rfork is limited to 16MB according to Apple)

public class ResourceFork {

    private boolean hasCustomIcon;

    private static class ResourceType {
        long type;
        int count;
        int offset;
        ResourceReference[] rr;
    }

    private static class ResourceReference {
        int id;
        short attrs;
        int noffs; // name
        int doffs; // data
        long dlen; // > 2GB of resources? with 2^23 offset and usual map position at the end
        String name;
    }

    ResourceFork() {
    }

    public boolean hasCustomIcon() {
        return hasCustomIcon;
    }

    public void read(RandomAccessFile f) throws IOException {
        f.seek(0);
        int dataOffset = readU4(f);
        int mapOffset = readU4(f);
        int dataLength = readU4(f);
        int mapLength = readU4(f);
        if (dataLength == 0 || mapLength == 0) { // This is the way .DSStore resource fork looks
            assert dataLength == 0 && mapLength == 0;
            return;
        }
/*
        Debug.traceln("data offset " + dataOffset);
        Debug.traceln("data length " + dataLength);
        Debug.traceln("map offset " + mapOffset);
        Debug.traceln("map length " + mapLength);
*/
        f.seek(mapOffset + 16 + 4 + 2); // reserved fields
        int attr = readU2(f); // resource fork attributes
        if (attr != 0) {
//          Debug.traceln("fork attr " + attr);
        }
        int typeListOffset = readU2(f) + mapOffset + 2;
        int nameListOffset = readU2(f) + mapOffset;
        int typesCount = readU2(f) + 1;
        f.seek(typeListOffset);
        ResourceType[] rt = new ResourceType[typesCount];
        for (int i = 0; i < typesCount; i++) {
            rt[i] = new ResourceType();
            rt[i].type = read4(f);
            rt[i].count = readU2(f) + 1;
            rt[i].offset = readU2(f);
            rt[i].offset += typeListOffset - 2;
            rt[i].rr = new ResourceReference[rt[i].count];
        }
        for (int i = 0; i < typesCount; i++) {
            assert f.getFilePointer() == rt[i].offset;
            for (int j = 0; j < rt[i].rr.length; j++) {
                rt[i].rr[j] = new ResourceReference();
                rt[i].rr[j].id = readU2(f);
                // ID -16455 Custom Icon
                hasCustomIcon = hasCustomIcon || ((short)rt[i].rr[j].id) == -16455;
                int n = readS2(f);
                rt[i].rr[j].noffs = n < 0 ? n : n + nameListOffset;
                rt[i].rr[j].attrs = read1(f);
                rt[i].rr[j].doffs = read3(f) + dataOffset;
                read4(f); // reserved
            }
        }
        assert f.getFilePointer() == nameListOffset;
        byte[] buf = new byte[255];
        for (int i = 0; i < typesCount; i++) {
            for (int j = 0; j < rt[i].rr.length; j++) {
                if (rt[i].rr[j].noffs >= 0) {
                    f.seek(rt[i].rr[j].noffs);
                    int len = read1(f);
                    f.read(buf, 0, len);
                    rt[i].rr[j].name = new String(buf, 0, len);
                } else {
                    rt[i].rr[j].name = "";
                }
/*
                Debug.traceln(int2str(rt[i].type) + "." + (short)rt[i].rr[j].id + "." +
                              rt[i].rr[j].name + "=" + rt[i].rr[j].doffs);
*/
            }
        }
        for (int i = 0; i < typesCount; i++) {
            for (int j = 0; j < rt[i].rr.length; j++) {
                f.seek(rt[i].rr[j].doffs);
                rt[i].rr[j].dlen = read4(f);
/*
                Debug.traceln(int2str(rt[i].type) + "." + (short)rt[i].rr[j].id + "." +
                              rt[i].rr[j].name + "=" + rt[i].rr[j].doffs + "[" + rt[i].rr[j].dlen + "]");
*/
            }
        }
//      Debug.traceln();
    }

/*
    public static void main(String[] args) {
        // http://developer.apple.com/documentation/mac/MoreToolbox/MoreToolbox-99.html
        ResourceFork rm = new ResourceFork();
        try {
            rm.read(new RandomAccessFile("/Library/Fonts/Trebuchet MS/rsrc", "rw"));
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private String int2str(long u4) {
        byte[] b = new byte[4];
        b[0] = (byte)(u4 & 0xFF); u4 = u4 >>> 8;
        b[1] = (byte)(u4 & 0xFF); u4 = u4 >>> 8;
        b[2] = (byte)(u4 & 0xFF); u4 = u4 >>> 8;
        b[3] = (byte)(u4 & 0xFF);
        return new String(b);
    }
*/

    private static int readU4(RandomAccessFile f) throws IOException {
        long r = read4(f);
        assert (r & ~0x7FFFFFFF) == 0;
        return (int)r;
    }

    private static long read4(RandomAccessFile f) throws IOException {
        byte b[] = new byte[4];
        f.read(b);
        return (((long)(b[0] & 0xFF)) << 24) | ((b[1] & 0xFF) << 16) | ((b[2] & 0xFF) << 8) | (b[3] & 0xFF);
    }


    private static int read3(RandomAccessFile f) throws IOException {
        byte b[] = new byte[3];
        f.read(b);
        return ((b[0] & 0xFF) << 16) | ((b[1] & 0xFF) << 8) | (b[2] & 0xFF);
    }
    
    private static int readU2(RandomAccessFile f) throws IOException {
        byte b[] = new byte[2];
        f.read(b);
        return ((b[0] & 0xFF) << 8) | (b[1] & 0xFF);
    }

    private static short readS2(RandomAccessFile f) throws IOException {
        byte b[] = new byte[2];
        f.read(b);
        return (short)(((b[0] & 0xFF) << 8) | (b[1] & 0xFF));
    }

    private static short read1(RandomAccessFile f) throws IOException {
        byte b[] = new byte[1];
        f.read(b);
        return (short)(b[0] & 0xFF);
    }

}
