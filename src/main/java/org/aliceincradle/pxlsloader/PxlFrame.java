package org.aliceincradle.pxlsloader;

import java.util.Arrays;
import java.util.List;

public class PxlFrame {
	short crf60=10;
	String name="";
	byte vers;
	
	PxlLayer[] layers;
	transient PxlSequence fa;
	transient int frameIndex;
	
	PxlFrame(NoelByteBuffer base, Settings s, PxlSequence fa, int frameIndex) {
		this.fa=fa;
		this.frameIndex=frameIndex;
		
		NoelByteBuffer b=base.getSegment();
		vers=b.getByte();
		crf60=b.getShort();
		name=b.getUTFString();
		if (b.size()!=0)
			System.err.println("left "+b.size()+" byte(s)");
		int num=base.getShort();
		layers=new PxlLayer[num];
		for (int i=0; i<num; i++) {
			layers[i]=new PxlLayer(base, s, this);
		}
	}
	void writeTo(NoelByteBuffer base){
		NoelByteBuffer b=new NoelByteBuffer();
		b.putByte(vers);
		b.putShort(crf60);
		b.putUTFString(name);
		base.putSegment(b);
		base.putShort(((short) layers.length));
		for (PxlLayer layer : layers)
			layer.writeTo(base);
	}
	
	public List<PxlLayer> getLayerList() {
		return List.of(layers);
	}
	
	public PxlLayer getLayerByName(String s) {
		return Arrays.stream(layers).filter(l->s.equalsIgnoreCase(l.name)).findFirst().orElse(null);
	}
	
	public PxlLayer getLayer(int i) {
		return layers[i];
	}
	
	public int getCrf60() {
		return crf60;
	}
	
	public String getName() {
		return name;
	}
	
	public byte getVers() {
		return vers;
	}
	
	public int getFrameIndex() {
		return frameIndex;
	}
	
	public PxlSequence getFa() {
		return fa;
	}
}
