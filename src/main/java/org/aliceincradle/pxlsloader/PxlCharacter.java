package org.aliceincradle.pxlsloader;

import org.jetbrains.annotations.NotNull;
import org.tinylog.Logger;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PxlCharacter {
    transient final List<PxlPose> pxlPoses = new ArrayList<>();
    transient final List<PxlImage> pxlImages = new ArrayList<>();
    transient final List<PxlImageAtlas> pxlImageAtlases = new ArrayList<>();
    
    public PxlCharacter(byte[] b) {
        NoelByteBuffer buf = new NoelByteBuffer(b);
        Settings s = new Settings.Builder().build();
        load(buf, s);
        buf.end();
    }
    
    public PxlCharacter(byte[] b, Settings s) {
        NoelByteBuffer buf = new NoelByteBuffer(b);
        load(buf, s);
        buf.end();
    }
    
    private void load(NoelByteBuffer b, Settings s) {
        // header
        if (b.getInt() != 0x7741A8FF)
            throw new IllegalArgumentException("invalid header 1");
        if (!b.getLengthedString(4).equals("PXLS"))
            throw new IllegalArgumentException("invalid header 2");
        while (b.size() > 0) {
            if (b.size() < 14)
                break;
            String op = b.getLengthedString(14);
            if (op.equals("%IMGD_SECTION%"))
                throw new IllegalArgumentException(
                    "PixelLiner error: does not support for " + "compressed image data (not bug)");
            else if (op.equals("%IMGS_SECTION%"))   // P_IMG
            {
                NoelByteBuffer target = b.getSegment();
                int loadImageCount = b.getInt();
                for (int i = 0; i < loadImageCount; i++) {
                    PxlImage image = new PxlImage(target, s);
                    pxlImages.add(image);
                }
            } else if (op.equals("%PACK_SECTION%")) {// P_IMG_PACKED
                NoelByteBuffer target = b.getSegment();
                int loadImageCount = target.getInt();
                for (int i = 0; i < loadImageCount; i++) {
                    Logger.debug("loading image " + i + " of " + loadImageCount);
                    PxlImageAtlas a = new PxlImageAtlas(target, s, i);
                    pxlImageAtlases.add(a);
                }
            } else if (op.equals("%IMGV_SECTION%")) {     // P_IMG_VECTOR
                NoelByteBuffer target = b.getSegment();
                System.out.println("Ignored segment IMGV with byte(s) " + target.size());
            } else if (op.equals("%POSE_SECTION%")) {
                NoelByteBuffer target = b.getSegment();
                int n = target.getInt();
                for (int i = 0; i < n; i++)
                    pxlPoses.add(new PxlPose(target, s, this, i));
            } else if (op.equalsIgnoreCase("%PTCL_SECTION%")) {// PTCL_SECTION
                NoelByteBuffer target = b.getSegment();
                target.getAllBytes();
            } else
                throw new IllegalArgumentException("invalid section header " + op);
        }
        b.end();
    }
    
    public byte[] serializeToPxlsFile() {
        NoelByteBuffer buf = new NoelByteBuffer();
        this.writeTo(buf);
        return buf.getAllBytes();
    }
    
    void writeTo(NoelByteBuffer buf) {
        buf.putInt(0x7741A8FF);
        buf.putInt(0x50584C53);
        // packed
        writePackSection(buf);
        // pose
        buf.putRawString("%POSE_SECTION%");
        NoelByteBuffer target = new NoelByteBuffer();
        target.putInt(pxlPoses.size());
        for (PxlPose pose : pxlPoses) {
            pose.writeTo(target);
        }
        buf.putSegment(target);
    }
    
    private void writePackSection(NoelByteBuffer buf) {
        buf.putRawString("%PACK_SECTION%");
        NoelByteBuffer packed = new NoelByteBuffer();
        packed.putInt(1);
        
        packed.putByte((byte) 22);
        packed.putByte((byte) 0);
        packed.putByte((byte) 1);
        
        List<PixelLinerKey> keys = this.pxlPoses.stream()
            .flatMap(pose -> Arrays.stream(pose.sequences))
            .flatMap(sequence -> Arrays.stream(sequence.frames))
            .flatMap(frame -> Arrays.stream(frame.layers)).filter(layer -> !layer.isGroup())
            .map(layer -> layer.id).distinct().toList();
        int n = keys.size();
        List<BufferedImage> images = keys.stream().map(key -> this.getImageByKey(key)).toList();
        
        Algorithm.Rect[] rects = new Algorithm.Rect[n];
        for (int i = 0; i < n; i++) {
            rects[i] = new Algorithm.Rect(images.get(i).getWidth(), images.get(i).getHeight());
        }
        int imageSize = 128;
        while (!Algorithm.packRectangles(rects, imageSize, imageSize)) {
            imageSize *= 2;
        }
        packed.putInt(n);
        System.out.println("ended with image size " + imageSize);
        for (int i = 0; i < n; i++) {
            packed.putPixelLinerKey(keys.get(i));
            packed.putInt(rects[i].x);
            packed.putInt(rects[i].y);
            packed.putInt(rects[i].w);
            packed.putInt(rects[i].h);
        }
        BufferedImage img = new BufferedImage(imageSize, imageSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setBackground(new Color(0, 0, 0, 0));
        g.clearRect(0, 0, imageSize, imageSize);
        for (int i = 0; i < n; i++) {
            g.drawImage(images.get(i), rects[i].x, rects[i].y, null);
        }
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "png", baos);
            byte[] imageBytes = baos.toByteArray();
            packed.putSegment(imageBytes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        buf.putSegment(packed);
    }
    
    @NotNull
    public BufferedImage getImageByKey(PixelLinerKey id) {
        var opt = this.pxlImages.stream().filter(image -> id.equals(image.id)).findFirst();
        if (opt.isPresent())
            return opt.get().I == null ? opt.get().P : opt.get().I;
        for (PxlImageAtlas atlas : this.pxlImageAtlases) {
            var opt2 = Arrays.stream(atlas.pos).filter(pos -> id.equals(pos.id)).findFirst();
            if (opt2.isPresent()) {
                PxlImageAtlas.Uv uv = opt2.get();
                BufferedImage original = atlas.image;
                return original.getSubimage(uv.x, uv.y, uv.width, uv.height);
            }
        }
        throw new IllegalArgumentException("could not find image with id " + id);
    }
    
    public List<PxlPose> getPoseList() {
        return List.copyOf(pxlPoses);
    }
    
    // Case-insensitive.
    public PxlPose getPoseByName(String s) {
        return pxlPoses.stream().filter(p -> s.equalsIgnoreCase(p.title)).findFirst().orElse(null);
    }
    
    public PxlPose getPose(int i) {
        return pxlPoses.get(i);
    }
    
    public int getPoseCount() {
        return pxlPoses.size();
    }
}
