/**
 *
 */
package com.yiyuen.screenshot;

import java.io.File;

/**
 * @author Ohad Serfaty
 */
public interface HtmlRenderer {

    void render(String url, File saveLocation) throws Exception;

    void dispose();

    void init() throws Exception;

}
