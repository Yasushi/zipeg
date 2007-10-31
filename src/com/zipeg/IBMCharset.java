package com.zipeg;


public class IBMCharset extends java.nio.charset.Charset {

    private sun.io.CharToByteSingleByte c2b;
    private sun.io.ByteToCharSingleByte b2c;

    public IBMCharset(int num) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        super("IBM" + num, new String[]{"cp" + num});
        Class classB2C = Class.forName("sun.io.ByteToCharCp" + num);
        Class classC2B = Class.forName("sun.io.CharToByteCp" + num);
        c2b = (sun.io.CharToByteSingleByte)classC2B.newInstance();
        b2c = (sun.io.ByteToCharSingleByte)classB2C.newInstance();
    }

    public boolean contains(java.nio.charset.Charset charset) {
        return charset == this;
    }

    public java.nio.charset.CharsetDecoder newDecoder() {
        return new Decoder(this, b2c);
    }

    public java.nio.charset.CharsetEncoder newEncoder() {
        return new Encoder(this, c2b);
    }

    private static class Decoder extends sun.nio.cs.SingleByteDecoder {

        public Decoder(java.nio.charset.Charset charset, sun.io.ByteToCharSingleByte b2c) {
            super(charset, b2c.getCharacterEncoding());
        }
    }

    private static class Encoder extends sun.nio.cs.SingleByteEncoder {

        public Encoder(java.nio.charset.Charset charset, sun.io.CharToByteSingleByte c2b) {
            super(charset, c2b.getIndex1(), c2b.getIndex2(), c2b.getMaxBytesPerChar(), 0, 255);
            Encoder e = this;
            Debug.traceln("averageBytesPerChar()=" + e.averageBytesPerChar());
        }
    }

}