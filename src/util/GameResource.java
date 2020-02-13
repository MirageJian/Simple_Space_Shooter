package util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.File;
import java.io.IOException;

public class GameResource {
    // When resource didn't load, this will be loaded
    static GameResource sBlankTexture = new GameResource("res/blankSprite.png");

    public Image imageTexture;
    public int drawWidth;
    public int drawHeight;
    // Different direction of bullets is different. For player
    public int textureStart;

    public GameResource(String texturePath) {
        File TextureToLoad = new File(texturePath);  //should work okay on OSX and Linux but check if you have issues depending your eclipse install or if your running this without an IDE
        try {
            imageTexture = ImageIO.read(TextureToLoad);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public GameResource(String texturePath, int drawWidth, int drawHeight) {
        this(texturePath);
        this.drawWidth = drawWidth;
        this.drawHeight = drawHeight;
    }
    public GameResource(String texturePath, int drawWidth, int drawHeight, int textureStart) {
        this(texturePath, drawWidth, drawHeight);
        this.textureStart = textureStart;
    }
}
