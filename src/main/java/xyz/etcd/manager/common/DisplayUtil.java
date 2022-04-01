package xyz.etcd.manager.common;

import java.awt.*;

public class DisplayUtil {
    public static int getDefaultDisplayWidth(){
        DisplayMode displayMode = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()[0].getDisplayMode();
        int displayWidth = displayMode.getWidth();
        return displayWidth;
    }

    public static int getDefaultDisplayHeight(){
        DisplayMode displayMode = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()[0].getDisplayMode();
        int displayHeight = displayMode.getHeight();
        return displayHeight;
    }

    public static Rectangle getDefaultDisplayCenterRectangle(int width,int height){
        Rectangle rectangle=new Rectangle();
        rectangle.x=(int)(getDefaultDisplayWidth()/2-width/2);
        rectangle.y=(int)(getDefaultDisplayHeight()/2-height/2);
        rectangle.width=width;
        rectangle.height=height;
        return rectangle;
    }
}
