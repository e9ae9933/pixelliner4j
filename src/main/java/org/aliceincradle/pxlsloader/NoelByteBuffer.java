package org.aliceincradle.pxlsloader;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class NoelByteBuffer {
    private byte[] buf;
    private int readPos;
    private int writePos;
    
    public NoelByteBuffer() {
        this.buf = new byte[1024];
        this.readPos = 0;
        this.writePos = 0;
    }
    
    public NoelByteBuffer(byte[] bytes) {
        this.buf = bytes;
        this.readPos = 0;
        this.writePos = bytes.length;
    }
    
    private void ensureCapacity(int need) {
        if (writePos + need > buf.length) {
            int newCap = Math.max(buf.length * 2, writePos + need);
            buf = Arrays.copyOf(buf, newCap);
        }
    }
    
    public int size() {
        return writePos - readPos;
    }
    
    public byte getByte() {
        return buf[readPos++];
    }
    
    public void putByte(byte b) {
        ensureCapacity(1);
        buf[writePos++] = b;
    }
    
    public short getShort() {
        return (short) (((buf[readPos++] & 0xFF) << 8) | (buf[readPos++] & 0xFF));
    }
    
    public void putShort(short s) {
        ensureCapacity(2);
        buf[writePos++] = (byte) (s >>> 8);
        buf[writePos++] = (byte) s;
    }
    
    public int getInt() {
        return ((buf[readPos++] & 0xFF) << 24) |
               ((buf[readPos++] & 0xFF) << 16) |
               ((buf[readPos++] & 0xFF) << 8)  |
               ((buf[readPos++] & 0xFF));
    }
    
    public void putInt(int i) {
        ensureCapacity(4);
        buf[writePos++] = (byte) (i >>> 24);
        buf[writePos++] = (byte) (i >>> 16);
        buf[writePos++] = (byte) (i >>> 8);
        buf[writePos++] = (byte) i;
    }
    
    public long getLong() {
        return ((long) getInt() << 32) | (getInt() & 0xFFFFFFFFL);
    }
    
    public void putLong(long l) {
        putInt((int) (l >>> 32));
        putInt((int) l);
    }
    
    public float getFloat() {
        return Float.intBitsToFloat(getInt());
    }
    
    public void putFloat(float f) {
        putInt(Float.floatToRawIntBits(f));
    }
    
    public double getDouble() {
        return Double.longBitsToDouble(getLong());
    }
    
    public void putDouble(double d) {
        putLong(Double.doubleToRawLongBits(d));
    }
    
    public void getBytes(byte[] b) {
        System.arraycopy(buf, readPos, b, 0, b.length);
        readPos += b.length;
    }
    
    public byte[] getNBytes(int len) {
        byte[] b = new byte[len];
        getBytes(b);
        return b;
    }
    
    public byte[] getAllBytes() {
        return Arrays.copyOfRange(buf, readPos, writePos);
    }
    
    public void putBytes(byte[] b) {
        ensureCapacity(b.length);
        System.arraycopy(b, 0, buf, writePos, b.length);
        writePos += b.length;
    }
    
    public boolean getBoolean() {
        return getByte() != 0;
    }
    
    public void putBoolean(boolean b) {
        putByte(b ? (byte) 1 : (byte) 0);
    }
    
    public void putUTFString(String s) {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        putShort((short) bytes.length);
        putBytes(bytes);
    }
    
    public String getUTFString() {
        int len = Short.toUnsignedInt(getShort());
        String s = new String(buf, readPos, len, StandardCharsets.UTF_8);
        readPos += len;
        return s;
    }
    
    public void putRawString(String s) {
        putBytes(s.getBytes(StandardCharsets.UTF_8));
    }
    
    public String getLengthedString(int len) {
        String s = new String(buf, readPos, len, StandardCharsets.UTF_8);
        readPos += len;
        return s;
    }
    
    public short getUnsignedByte() {
        return (short) (getByte() & 0xFF);
    }
    
    public int getUnsignedShort() {
        return Short.toUnsignedInt(getShort());
    }
    
    public long getUnsignedInt() {
        return getInt() & 0xFFFFFFFFL;
    }
    
    public PixelLinerKey getPixelLinerKey() {
        return new PixelLinerKey(getInt(), getDouble());
    }
    
    public void putPixelLinerKey(PixelLinerKey key) {
        putInt(key.id);
        putDouble(key.id2);
    }
    
    public NoelByteBuffer getSegment() {
        return new NoelByteBuffer(getNBytes(getInt()));
    }
    
    public void putSegment(NoelByteBuffer b) {
        putSegment(b.getAllBytes());
    }
    
    public void putSegment(byte[] data) {
        putInt(data.length);
        putBytes(data);
    }
    
    public void end() {
        if (size() > 0) {
            System.err.println("Warning: left " + size() + " byte(s)");
        }
    }
}