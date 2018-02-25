/**
 * 
 */
package com.yiyuen.screenshot;

import java.io.File;

/**
 * @author Ohad Serfaty
 * 
 */
public class Scrender {

	private final boolean disposeOnFinish;
	private RenderingBrowser browser;

	
	public Scrender(boolean disposeOnFinish) {

		this.disposeOnFinish = disposeOnFinish;
		browser = new RenderingBrowser();
	}

	/**
	 * 
	 */
	public Scrender() {
		this(true);
	}

	public void init() throws Exception {
		browser.init();
	}

	public void render(String url, File saveLocation) throws RenderingException {

		browser.render(url, saveLocation);

		if (disposeOnFinish) {
			browser.dispose();
		}
	}

	public void dispose() {
		browser.dispose();
	}

	public void resetBrowser() {
		browser.resetBrowser(true);
	}
}
