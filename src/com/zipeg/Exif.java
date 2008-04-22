package com.zipeg;

import java.io.*;

public final class Exif { // see: http://www.exif.org/

    private static class Data {
        byte[] b;
        int jif_offset = 0;
        int jif_length = 0;
        boolean big; // endian
    }

    private static boolean isMarker(byte[] buff, int i) {
        return buff[i] == 'E' && buff[i + 1] == 'x' &&
               buff[i + 2] == 'i' && buff[i + 3] == 'f' &&
               buff[i + 4] ==  0 && buff[i + 5] ==  0;
    }

    private static final byte APP1 = (byte)0xE1;

    public static byte[] read(InputStream is) throws IOException {
        int size = Math.min(is.available(), (64+32) * Util.KB);
        if (size <= 0) {
            throw new IOException("File is too short");
        }
        Data data = new Data();
        data.b = new byte[size];
        int bytes = is.read(data.b);
        assert !Debug.isDebug() || bytes == size : "got " + bytes + " size " + size;
        if (bytes != size) {
            throw new IOException("failed to read data from exif header");
        }
        if (data.b[0] != (byte)0xFF || data.b[1] != (byte)0xD8) {
            throw new IOException("invalid JPEG");
        }
        int len = -1;
        int i = 2;
        int app1 = 0;
        while (i < size - 2) {
            int header = i; // two bytes FF <something>
            assert !Debug.isDebug() || data.b[header] == (byte)0xFF;
            if (data.b[header] != (byte)0xFF) {
                throw new IOException("corrputer exif header at offset " + header);
            }
            i += 2;
            byte tag = data.b[header + 1];
            if (tag == (byte)0xC0) { // SOF
                if (app1 > 0) break;
            }
            if (tag == (byte)0xDA || tag == (byte)0xDB || tag == (byte)0xD8) {
                //           SOS,                 DQT,                 SOI
                break;
            }
            int length = data.b[i++] & 0xFF;
            length = (length << 8) | (data.b[i++] & 0xFF);
            if (length <= 0) {
                throw new IOException("invalid APP1 length");
            }
            length -= 2;
            if (app1 == 0 && data.b[header] == (byte)0xFF && data.b[header + 1] == APP1) {
                app1 = i;
                len = length;
            }
            i += length;
        }
        if (app1 == 0 || len < 6 || !isMarker(data.b, app1)) {
            throw new IOException("no EXIF");
        }
        int exif = app1 + 6;
        int offset = exif;
        data.big = data.b[offset++] != (byte)0x49;
        byte next = data.b[offset++];
        if (next != data.b[offset-2]) {
            throw new IOException("bad format of the APP1");
        }
        int temp = read(data, offset, 2);
        if (temp != 42) { /* magic */
            throw new IOException("APP1 has no 42");
        }
        offset += 2;
        int ifdOffset = read(data, offset, 4);
        try {
            while (ifdOffset != 0 && exif + ifdOffset < size - 20) {
                int n = read(data, exif + ifdOffset, 2);
                int p = readIfd(data, exif, exif + ifdOffset + 2, n);
                if (p < 0) {
                    return null;
                }
                ifdOffset = read(data, p, 4);
            }
        }
        catch (IndexOutOfBoundsException ignore) {
            /* ignore */
        }
        if (data.jif_offset <= 0 || data.jif_length <= 0) {
            throw new IOException("EXIF: no JIF thumbnail");
        }
        int offs = exif + data.jif_offset;
        int length = data.jif_length;
        if (offs + length > size) {
            length = size - offs;
            if (length < 512) {
                return null;
            }
        }
        if (length > 0 && offs + length <= size) {
            byte[] t = new byte[length];
            System.arraycopy(data.b, offs, t, 0, length);
            return t;
        } else {
            return null;
        }
    }

    private static int readIfd(Data data, int exif, int ifd, int n) throws IOException {
        for (int i = 0; i < n ; i++) {
            int tag = read(data, ifd, 2); ifd += 2;
            int type = read(data, ifd, 2); ifd += 2;
            int count = read(data, ifd, 4); ifd += 4;
            if (count <= 0 || count > data.b.length) {
                return -1;
            }
            long[] v = new long[count];
            if (type == 2) {
                // string
            }
            else if (type == 7) { // UNDEFINDED
                if (count > 0 && count <= 4) {
                    v[0] = read(data, ifd, count);
                }
                else if (count > 0) {
                    int offset = exif + read(data, ifd, 4);
                    for (int j = 0; j < count; j++)
                        v[j] = (byte)read(data, offset + j, 1);
                }
            }
            else if (count * getTypeLength(type) <= 4) {
                for (int j = 0; j < count; j++) {
                    switch (type) {
                        case 1: v[j] = read(data, ifd + j, 1); break;
                        case 3: v[j] = (read(data, ifd + j * 2, 2)) & 0xFFFF; break;
                        case 4: v[j] = read(data, ifd + j * 4, 4); break;
                        case 9: v[j] = read(data, ifd + j * 4, 4); break;
                        default: /* unknown type */ return -1;
                    }
                }
            }
            else {
                int offset = exif + read(data, ifd, 4);
                for (int j = 0; j < count; j++) {
                    switch (type) {
                        case 1: v[j] = ((long)data.b[offset + j]) & 0xFF; break;
                        case 3: v[j] = (short)read(data, offset + j * 2, 2); break;
                        case 4: v[j] = read(data, offset + j * 4, 4); break;
                        case 9: v[j] = read(data, offset + j * 4, 4); break;
                        case 5: /* ignore rational */  break;
                        case 10: /* ignore rational */  break;
                        default: /* unknown type */ return -1;
                    }
                }
            }
            ifd += 4;
            switch (tag) {
                case 0: return ifd;
                case 0x201: data.jif_offset = (int)v[0]; break;
                case 0x202: data.jif_length = (int)v[0]; break;
                default: /* ignore */ break;
            }
        }
        return ifd;
    }

    private static int read(Data data, int k, int n) {
        int v = 0;
        if (data.big) {
            for (int i = 0; i < n; i++) {
                v |= (((int)data.b[k + i]) & 0xFF) << (8 * (n - i - 1));
            }
        }
        else {

            for (int i = 0; i < n; i++) {
                v |= (((int)data.b[k + i]) & 0xFF) << (8 * i);
            }
        }
        return v;
    }

    private static int getTypeLength(int type) throws IOException {
        switch (type) {
            case 1: return 1;
            case 3: return 2;
            case 4: return 4;
            case 9: return 4;
            case 5: return 8;
            case 10: return 8;
            default: throw new IOException("Unknow type " + type);
        }
    }

}
