package org.aliceincradle.pxlsloader;

import org.jetbrains.annotations.NotNull;

import java.awt.image.BufferedImage;

public class PxlLayer {
    PixelLinerKey id;
    byte type = 0;
    String name = "";
    short alpha = 10000;
    float x, y;
    double zmx = 1, zmy = 1;
    double rotR = 0;
    short blendVariable = 0;
    int useless1;
    byte useless2, useless3;
    
    int groupPreserveContainLayers;
    
    transient PxlFrame fa;
    
    PxlLayer(NoelByteBuffer b, Settings s, PxlFrame fa) {
        this.fa = fa;
        id = b.getPixelLinerKey();
        type = b.getByte();
        name = b.getUTFString();
        alpha = b.getShort();
        if ((type & 8) > 0) {
            // is group.
            groupPreserveContainLayers = b.getInt();
            return;
        }
        x = b.getShort() / 10f;
        y = b.getShort() / 10f;
        zmx = b.getDouble();
        zmy = b.getDouble();
        rotR = b.getDouble();
        blendVariable = b.getShort();
        useless1 = b.getInt();
        useless2 = b.getByte();
        useless3 = b.getByte();
    }
    
    void writeTo(NoelByteBuffer b) {
        b.putPixelLinerKey(id);
        b.putByte(type);
        b.putUTFString(name);
        b.putShort(alpha);
        if ((type & 8) > 0) {
            b.putInt(groupPreserveContainLayers);
            return;
        }
        b.putShort((short) Math.round(x * 10f));
        b.putShort((short) Math.round(y * 10f));
        b.putDouble(zmx);
        b.putDouble(zmy);
        b.putDouble(rotR);
        b.putShort(blendVariable);
        b.putInt(useless1);
        b.putByte(useless2);
        b.putByte(useless3);
    }
    
    @NotNull
    public BufferedImage getImage() {
        return fa.fa.fa.fa.getImageByKey(this.id);
    }
    
    public byte getType() {
        return type;
    }
    
    public String getName() {
        return name;
    }
    
    public short getAlpha() {
        return alpha;
    }
    
    public float getX() {
        return x;
    }
    
    public float getY() {
        return y;
    }
    
    public double getZmx() {
        return zmx;
    }
    
    public double getZmy() {
        return zmy;
    }
    
    public double getRotR() {
        return rotR;
    }
    
    public short getBlendVariable() {
        return blendVariable;
    }
    
    public int getUseless1() {
        return useless1;
    }
    
    public byte getUseless2() {
        return useless2;
    }
    
    public byte getUseless3() {
        return useless3;
    }
    
    public PxlFrame getFa() {
        return fa;
    }
    
    public boolean isGroup() {
        return (this.type & 8) > 0;
    }
    
    public boolean isImport() {
        return (this.type & 1) > 0;
    }
    
    public boolean isVector() {
        return (this.type & 2) > 0;
    }
    
    public boolean isRelevance() {
        return isGroup() || isImport();
    }
    
    @NotNull
    public PxlLayer getImportSource() {
        //		if(!isRelevance()) throw new IllegalStateException("Must be relevance");
        PxlCharacter chara = this.fa.fa.fa.fa;
        for (PxlPose pose : chara.getPoseList())
            for (PxlSequence sequence : pose.getSequenceList())
                for (PxlFrame frame : sequence.getFrameList())
                    for (PxlLayer layer : frame.getLayerList())
                        if (!layer.isRelevance() && layer.id.equals(this.id))
                            return layer;
        throw new IllegalStateException("Relevance layer not found");
    }
}
