package org.aliceincradle.pxlsloader;

import java.util.Arrays;
import java.util.List;

public class PxlSequence {
	short width, height;
	short bodyX, bodyY;
	short shiftX, shiftY;
	short loopTo;
	String[] frameSnd;
	PxlFrame[] frames;
	byte useless;
	
	int aim;
	transient PxlPose fa;
	transient int sequenceIndex;
	
	PxlSequence(NoelByteBuffer base, Settings s, int aim, PxlPose fa, int sequenceIndex) {
		this.fa=fa;
		this.aim=aim;
		this.sequenceIndex=sequenceIndex;
		
		NoelByteBuffer b=base.getSegment();
		useless=b.getByte();
		width=b.getShort();
		height=b.getShort();
		bodyX=b.getShort();
		bodyY=b.getShort();
		shiftX=b.getShort();
		shiftY=b.getShort();
		loopTo=b.getShort();
		int num=b.getUnsignedShort();
		frameSnd=new String[num];
		for (int i=0; i<num; i++)
			frameSnd[i]=b.getUTFString();
		int num2=base.getUnsignedShort();
		frames=new PxlFrame[num2];
		for (int i=0; i<num2; i++) {
			frames[i]=new PxlFrame(base, s, this, i);
		}
	}
	void writeTo(NoelByteBuffer base){
		NoelByteBuffer b=new NoelByteBuffer();
		b.putByte(useless);
		b.putShort(width);
		b.putShort(height);
		b.putShort(bodyX);
		b.putShort(bodyY);
		b.putShort(shiftX);
		b.putShort(shiftY);
		b.putShort(loopTo);
		b.putShort(((short) frameSnd.length));
		for (String s : frameSnd)
			b.putUTFString(s);
		base.putSegment(b);
		base.putShort(((short) frames.length));
		for (PxlFrame frame : frames)
			frame.writeTo(base);
	}
	
	public List<PxlFrame> getFrameList() {
		return List.of(frames);
	}
	
	public PxlFrame getFrameByName(String s) {
		return Arrays.stream(frames).filter(f->s.equalsIgnoreCase(f.name)).findFirst().orElse(null);
	}
	
	public PxlFrame getFrame(int i) {
		return frames[i];
	}
	
	public int getFrameCount() {
		return frames.length;
	}
	
	public int getWidth() {
		return width;
	}
	
	public int getHeight() {
		return height;
	}
	
	public int getBodyX() {
		return bodyX;
	}
	
	public int getBodyY() {
		return bodyY;
	}
	
	public int getShiftX() {
		return shiftX;
	}
	
	public int getShiftY() {
		return shiftY;
	}
	
	public int getLoopTo() {
		return loopTo;
	}
	
	public String[] getFrameSnd() {
		return frameSnd;
	}
	
	public int getAim() {
		return aim;
	}
	
	public byte getUseless() {
		return useless;
	}
	
	public PxlPose getFa() {
		return fa;
	}
	
	public int getSequenceIndex() {
		return sequenceIndex;
	}
	
}
