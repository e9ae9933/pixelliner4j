package org.aliceincradle.pxlsloader;

import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class NoelByteBuffer {
	
	public NoelByteBuffer() {
		this(new byte[0]);
	}
	
	public NoelByteBuffer(byte[] bytes) {
		buf=new ArrayDeque<>();
		for(byte b:bytes)
			buf.addLast(b);
	}
	
	private final Deque<Byte> buf;
	
	public int size() {
		return buf.size();
	}
	
	public byte getByte() {
		return buf.removeFirst();
	}
	
	public void putByte(byte b) {
		buf.addLast(b);
	}
	
	public void end() {
		if (size()>0) {
			System.err.println("Warning: left "+size()+" byte(s)");
			List<Byte> l=new ArrayList<>();
			for (int i=0; size()>0&&i<32; i++)
				l.add(getByte());
			System.err.println("printing first "+l.size()+" bytes");
			for (Byte b : l)
				System.err.printf("%02X ", b);
			System.err.println();
			for (Byte b : l)
				System.err.printf("%s ",
				                  Character.isISOControl(b) ? ".." : " "+(char) b.byteValue());
			System.err.println();
			//			data=null;
		}
	}
	
	public short getShort() {
		return (short) ((getByte()&0xFF)<<8|getByte()&0xFF);
	}
	
	public void putShort(short s) {
		putByte((byte) (s >>> 8));
		putByte(((byte) s));
	}
	
	public int getInt() {
		return (getShort()&0xFFFF)<<16|getShort()&0xFFFF;
	}
	
	public void putInt(int i) {
		putShort((short) (i >>> 16));
		putShort((short) i);
	}
	
	public long getLong() {
		return (getInt()&0xFFFFFFFFL)<<32|getInt()&0xFFFFFFFFL;
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
		for (int i=0; i<b.length; i++)
			b[i]=getByte();
	}
	
	public byte[] getNBytes(int len) {
		byte[] b=new byte[len];
		getBytes(b);
		return b;
	}
	
	public byte[] getAllBytes() {
		return getNBytes(size());
	}
	
	public void putBytes(byte[] b) {
		for (int i=0; i<b.length; i++)
			putByte(b[i]);
	}
	
	public boolean getBoolean() {
		return getByte()!=0;
	}
	
	public String getUTFString() {
		return new String(getNBytes(Short.toUnsignedInt(getShort())), StandardCharsets.UTF_8);
	}
	
	public short getUnsignedByte() {
		return (short) (getByte()&0xFF);
	}
	
	public int getUnsignedShort() {
		return Short.toUnsignedInt(getShort());
	}
	
	public long getUnsignedInt() {
		return getInt()&0xFFFFFFFFL;
	}
	
	public PixelLinerKey getPixelLinerKey() {
		int id=getInt();
		double id2=getDouble();
		return new PixelLinerKey(id,id2);
	}
	public void putPixelLinerKey(PixelLinerKey key) {
		putInt(key.id);
		putDouble(key.id2);
	}
	
	public NoelByteBuffer getSegment() {
		int len=getInt();
		return new NoelByteBuffer(getNBytes(len));
	}
	
	public void putUTFString(String s) {
		putShort((short) s.length());
		putBytes(s.getBytes(StandardCharsets.UTF_8));
	}
	public void putRawString(String s) {
		putBytes(s.getBytes(StandardCharsets.UTF_8));
	}
	
	public void putBoolean(boolean b) {
		putByte(b ? (byte) 1 : (byte) 0);
	}
	
	public void putSegment(NoelByteBuffer b) {
		putSegment(b.getAllBytes());
	}
	
	public void putSegment(byte[] data) {
		putInt(data.length);
		putBytes(data);
	}
	
	public String getLengthedString(int len) {
		return new String(getNBytes(len),StandardCharsets.UTF_8);
	}
	
//	@Override
//	@SuppressWarnings("removal")
//	protected void finalize() throws Throwable {
//		this.end();
//	}
}
