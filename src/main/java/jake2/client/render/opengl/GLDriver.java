package jake2.client.render.opengl;

import jake2.qcommon.exec.Command;
import jake2.qcommon.side.EnvType;
import jake2.qcommon.side.Environment;

import java.awt.Dimension;
import java.awt.DisplayMode;

@Environment(EnvType.CLIENT)
public interface GLDriver {
    
    boolean init(int xpos, int ypos);
    
    int setMode(Dimension dim, int mode, boolean fullscreen);
    
    void shutdown();
    
    void beginFrame(float camera_separation);
    
    void endFrame();

    void appActivate(boolean activate);
    
    void enableLogging(boolean enable);
    
    void logNewFrame();
    
    DisplayMode[] getModeList();

    void updateScreen(Command callback);

    void screenshot();
    
}
