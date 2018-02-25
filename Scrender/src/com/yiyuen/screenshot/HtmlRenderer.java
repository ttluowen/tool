/**
 * 
 */
package com.yiyuen.screenshot;

import java.io.File;

/**
 * @author Ohad Serfaty
 * 
 */
public interface HtmlRenderer {

	public void render(String url, File saveLocation) throws Exception;

	public void dispose();

	public void init() throws Exception;

}
