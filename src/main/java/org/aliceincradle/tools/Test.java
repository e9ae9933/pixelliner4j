package org.aliceincradle.tools;

import org.aliceincradle.pxlsloader.PxlCharacter;
import org.aliceincradle.pxlsloader.Settings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Test {
    public static void main(String[] args) throws Exception {
        byte[] b = Files.readAllBytes(Path.of("U:\\AliceInCradle_Data\\StreamingAssets\\MapChars" +
                                              "\\TextAsset_sub_mob_general.pxls"));
        Settings s = new Settings();
        s.loadFromPngFunction = i -> {
            try {
                return Files.readAllBytes(Path.of("U:\\AliceInCradle_Data\\StreamingAssets" +
                                                  "\\MapChars\\Texture_sub_mob_general.pxls" +
                                                  ".texture_0.png"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
        PxlCharacter chara = new PxlCharacter(b, s);
    }
}
