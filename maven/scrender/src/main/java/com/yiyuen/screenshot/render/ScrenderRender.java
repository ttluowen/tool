/**
 *
 */
package com.yiyuen.screenshot.render;

import com.yiyuen.screenshot.Scrender;
import com.yy.java.config.JavaConfig;
import com.yy.log.Logger;

import java.io.File;

/**
 *
 */
public class ScrenderRender {

    static {
        JavaConfig.setSystemPath(System.getProperty("user.dir"));
    }


    public static void main(String[] args) {

        String url = args.length > 0 ? args[0] : null;
        String file = args.length > 1 ? args[1] : null;


        if (url == null) {
            url = "http://tu.yiyuen.com";
        }
        if (file == null) {
            file = "D:/qq.jpg";
        }


        Scrender scrender = new Scrender();

        try {
            scrender.init();
            scrender.render(url, new File(file));
        } catch (Exception e) {
            Logger.log(e.getMessage());
        } finally {
            try {
                scrender.dispose();
            } catch (Exception e) {
                Logger.log(e.getMessage());
            }
        }
    }
}
