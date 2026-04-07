package org.aliceincradle.pxlsloader;

import java.util.ArrayList;
import java.util.List;

public class PxlPose {
    byte useless;
    boolean autoFlip;
    boolean tetraPose;
    String title;
    short width, height;
    short endJumpLoopCount;
    String endJumpTitle;
    String[] aliasTo;
    String comment;
    PxlSequence[] sequences;
    
    transient PxlCharacter fa;
    transient int poseIndex;
    
    PxlPose(NoelByteBuffer b, Settings s, PxlCharacter fa, int poseIndex) {
        this.fa = fa;
        NoelByteBuffer target = b.getSegment();
        useless = target.getByte();
        autoFlip = target.getBoolean();
        tetraPose = target.getBoolean();
        title = target.getUTFString();
        width = target.getShort();
        height = target.getShort();
        endJumpLoopCount = target.getShort();
        endJumpTitle = target.getUTFString();
        int num2 = target.getShort();
        aliasTo = new String[num2];
        for (int i = 0; i < num2; i++)
            aliasTo[i] = target.getUTFString();
        if (useless >= 2)
            comment = target.getUTFString();
        int num3;
        List<PxlSequence> list = new ArrayList<>();
        int top = 0;
        while ((num3 = Byte.toUnsignedInt(b.getByte())) != 0) {
            num3 -= 10;
            list.add(new PxlSequence(b, s, num3, this, top));
            top++;
        }
        sequences = list.toArray(new PxlSequence[0]);
        
        this.poseIndex = poseIndex;
    }
    
    void writeTo(NoelByteBuffer b) {
        NoelByteBuffer target = new NoelByteBuffer();
        target.putByte(useless);
        target.putBoolean(autoFlip);
        target.putBoolean(tetraPose);
        target.putUTFString(title);
        target.putShort(width);
        target.putShort(height);
        target.putShort(endJumpLoopCount);
        target.putUTFString(endJumpTitle);
        target.putShort((short) aliasTo.length);
        for (String s : aliasTo)
            target.putUTFString(s);
        if (useless >= 2)
            target.putUTFString(comment);
        b.putSegment(target);
        for (PxlSequence sequence : sequences) {
            b.putByte(((byte) (sequence.aim + 10)));
            sequence.writeTo(b);
        }
        b.putByte(((byte) 0));
    }
    
    public List<PxlSequence> getSequenceList() {
        return List.of(sequences);
    }
    
    public PxlSequence getSequence(int i) {
        return sequences[i];
    }
    
    public int getSequenceCount() {
        return sequences.length;
    }
    
    public boolean isAutoFlip() {
        return autoFlip;
    }
    
    public boolean isTetraPose() {
        return tetraPose;
    }
    
    public String getTitle() {
        return title;
    }
    
    public int getWidth() {
        return width;
    }
    
    public int getHeight() {
        return height;
    }
    
    public int getEndJumpLoopCount() {
        return endJumpLoopCount;
    }
    
    public String getEndJumpTitle() {
        return endJumpTitle;
    }
    
    public String[] getAliasTo() {
        return aliasTo;
    }
    
    public String getComment() {
        return comment;
    }
    
    public byte getUseless() {
        return useless;
    }
    
    public PxlCharacter getFa() {
        return fa;
    }
    
    public int getPoseIndex() {
        return poseIndex;
    }
}
