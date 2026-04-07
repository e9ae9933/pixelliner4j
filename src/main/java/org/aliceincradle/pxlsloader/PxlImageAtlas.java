package org.aliceincradle.pxlsloader;

import org.tinylog.Logger;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Objects;

public class PxlImageAtlas {
    Uv[] pos;
    BufferedImage image;
    
    PxlImageAtlas(NoelByteBuffer b, Settings s, int id) {
        int type = b.getByte() - 22;
        if (type >= 0) {
            int num = Byte.toUnsignedInt(b.getByte());
            int margin = Byte.toUnsignedInt(b.getByte());
            if (margin != 1)
                throw new IllegalArgumentException("what?! not 1 margin?!");
            int num2 = b.getInt();
            Logger.debug("num2 = " + num2);
            pos = new Uv[num2];
            for (int i = 0; i < num2; i++) {
                Uv t = new Uv();
                t.id = b.getPixelLinerKey();
                t.x = b.getInt();
                t.y = b.getInt();
                t.width = b.getInt();
                t.height = b.getInt();
                pos[i] = t;
            }
            Logger.debug("num = " + num);
            if ((num & 1) != 0) {
                try {
                    int w = b.getInt();
                    int h = b.getInt();
                    image =
                        ImageIO.read(new ByteArrayInputStream(Objects.requireNonNull(s.getLoadFromPngFunction()
                                                                                             .apply(id))));
                    if (w != image.getWidth() || h != image.getHeight()) {
                        System.out.println(
                            "not equals image width and height: w=" + w + " h=" + h + " width=" +
                            image.getWidth() + " height=" + image.getHeight());
                        BufferedImage another = new BufferedImage(w, h,
                                                                  BufferedImage.TYPE_INT_ARGB);
                        Graphics g2 = another.getGraphics();
                        g2.drawImage(image, 0, 0, w, h, null);
                        image = another;
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else {
                Logger.debug("ready to read img");
                NoelByteBuffer img = b.getSegment();
                Logger.debug("end to read img");
                try {
                    image = ImageIO.read(new ByteArrayInputStream(img.getAllBytes()));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
    
    class Uv {
        PixelLinerKey id;
        int x, y, width, height;
        
        public Uv() {
        }
    }
}
