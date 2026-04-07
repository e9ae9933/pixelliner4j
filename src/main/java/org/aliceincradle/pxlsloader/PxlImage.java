package org.aliceincradle.pxlsloader;

import org.apache.commons.lang3.Validate;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

public class PxlImage {
    int type;
    PixelLinerKey id;
    BufferedImage I, P;
    
    PxlImage(NoelByteBuffer b, Settings s) {
        type = b.getByte() - 22;
        if (type < 0)
            return;
        b.getByte();
        id = b.getPixelLinerKey();
        if (type == 0 || type == 8) {
            NoelByteBuffer target = b.getSegment();
            if (target.size() > 0)
                I = createFromPngRawData(target);
            NoelByteBuffer target2 = b.getSegment();
            if (target2.size() > 0)
                P = createFromPngRawData(target2);
        }
        Validate.isTrue(isValid(), "Invalid PxlImage");
    }
    
    private BufferedImage createFromPngRawData(NoelByteBuffer buf) {
        try {
            byte[] b = buf.getAllBytes();
            return ImageIO.read(new ByteArrayInputStream(b));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    private boolean isValid() {
        return (type == 0 || type == 8) && (I != null || P != null);
    }
}
