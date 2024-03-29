package com.yiyuen.screenshot;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.browser.StatusTextEvent;
import org.eclipse.swt.browser.StatusTextListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import com.yy.log.Logger;


/**
 * @author Ohad Serfaty
 */
public class RenderingBrowser extends Thread implements HtmlRenderer {

    public static final int SCREEN_WIDTH = 1280;
    public static final int SCREEN_HEIGHT = 760;

    // Default time to wait for the execution of a Javascript command : (10s)
    private static final int DEFAULT_JAVASCRIPT_TIMEOUT = 15 * 1000;

    // Defualt time to load a URL to the browser : (10s)
    private static final int DEFAULT_SET_URL_TIMEOUT = 15 * 1000;

    int renderingCounter = 1;

    private Shell shell;

    private Display display;

    private Browser browser;

    private int scrollbarX = 0;

    private int scrollbarY = 0;

    private String url;

    private final Lock browserInitializationLock = new Lock();

    private final Lock browserShutdownLock = new Lock();

    private Exception exceptionThrown = null;

    public void run() {
        try {
            // Initialize browser :
            display = new Display();
            shell = new Shell(display, SWT.ON_TOP);
            this.initializeBrowser();
            browserInitializationLock.release();

            // SWT event loop :
            while (!shell.isDisposed()) {
                if (!display.readAndDispatch())
                    display.sleep();
            }
            Logger.log("Screenshot manager stopped.Shell disposed.dispoding display...");
            display.dispose();
            browserShutdownLock.release();
        } catch (Exception e) {
            e.printStackTrace();
            exceptionThrown = e;
        }
    }

    /**
     * Set the URL to the browser . Use the default timeout and the stop the
     * browser.
     *
     * @param url
     */
    @SuppressWarnings("unused")
    private void setUrl(final String url) {
        this.setUrl(url, DEFAULT_SET_URL_TIMEOUT);
    }

    /**
     * Set the browser's URL. use the specified timeout.
     *
     * @param url
     * @param timeout
     */
    private void setUrl(final String url, int timeout) {
        final Lock setUrlLock = new Lock();
        final ProgressListener setUrlProgressListener = new ProgressListener() {
            public void changed(ProgressEvent arg0) {
                Logger.log("Progress Event:" + arg0.current + "/" + arg0.total);
            }

            public void completed(ProgressEvent event) {
                browser.removeProgressListener(this);
                browser.execute("document.body.style.overflow=\"hidden\";");
                setUrlLock.release();
            }
        };

        this.display.syncExec(new Runnable() {
            public void run() {
                browser.addProgressListener(setUrlProgressListener);
                browser.setUrl(url);
            }
        });
        setUrlLock.waitFor(timeout);
    }

    /**
     * Capture the screen's image starting from the specified point in the
     * screen , and save it to a temporary image in the specified directory.
     *
     * @param saveDir
     * @param picnum
     * @param width
     * @param height
     * @param startX
     * @param startY
     */
    private void capture(String saveDir, int picnum, int width, int height, int startX, int startY) {
        GC gc = new GC(browser);
        Image image = new Image(display, width - scrollbarX, height - scrollbarY);

        gc.copyArea(image, startX, startY);
        gc.dispose();
        ImageLoader imageLoader = new ImageLoader();
        imageLoader.data = new ImageData[]{image.getImageData()};
        try {
            FileOutputStream fos = new FileOutputStream(saveDir + "/savedImage" + picnum + ".jpg");
            imageLoader.save(fos, SWT.IMAGE_JPEG);
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        image.dispose();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.dappit.Dapper.Screenshot.renderer.HtmlRenderer#render(java.lang.String
     * , java.io.File)
     */
    public void render(String url, final File saveLocation) throws RenderingException {
        this.resetBrowser();
        this.setUrlStopBrowser(url);
        this.render(saveLocation);
    }

    public boolean resetBrowser() {
        return resetBrowser(false);
    }

    /**
     * Reset the browser according to inner counter
     */
    public boolean resetBrowser(boolean forceReset) {
        final Lock resetLock = new Lock();
        if (forceReset || renderingCounter++ % 10 == 0) {
            display.asyncExec(new Runnable() {
                public void run() {
                    initializeBrowser();
                    resetLock.release();
                }
            });
            Logger.log("Waiting for browser to be reset...");
            return resetLock.waitFor(20000);
        }
        return true;
    }

    /**
     * Set the browser URL and stop the rendering in the browser. this function
     * waits for maximum of DEFAULT_SET_URL_TIMEOUT to the browser to load the
     * URL and then stops it.
     *
     * @param url
     * @throws InterruptedException
     */
    public void setUrlStopBrowser(String url) throws RenderingException {
        this.setUrlStopBrowser(url, DEFAULT_SET_URL_TIMEOUT);
    }

    /**
     * Set the browser's URL and stop the rendering browser .
     *
     * @param url
     * @throws InterruptedException
     */
    public void setUrlStopBrowser(String url, int timeout) throws RenderingException {
        this.url = url;
        this.setUrl(this.url, timeout);
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RenderingException(e);
        }
        this.stopBrowser();
    }

    public void render(final File saveLocation) throws RenderingException {

        final File localSaveLocation = new File(saveLocation.getParentFile().getAbsolutePath());
        localSaveLocation.mkdirs();
        Logger.log("Saving temporary images in directory :" + localSaveLocation.getAbsolutePath());

        String widthStr = null;
        widthStr = this.getJavascriptValue("document.body.scrollWidth+'-'+document.body.scrollHeight");
        if (widthStr == null)
            throw new RenderingException("Could not acquire Document height andf width by javascript.");

        final int width = SCREEN_WIDTH;
        int nonFinalHeight = Math.min(10000, Integer.parseInt(widthStr.split("-")[1]));
        final int height = nonFinalHeight < SCREEN_HEIGHT ? SCREEN_HEIGHT : nonFinalHeight;

        Logger.log("Screen sizes :" + width + " ," + height);
        // for (int i = 0; i < 1 + (height / HEIGHT); i++) {
        for (int i = 0; i < 1; i++) {
            if (i != 0) {
                this.execute("window.scrollTo(0," + i * (SCREEN_HEIGHT - scrollbarY) + ");");
                try {
                    Thread.sleep(700);
                } catch (InterruptedException e) {
                    throw new RenderingException(e);
                }
            }
            final int j = i;
            display.syncExec(new Runnable() {
                public void run() {
                    if (j == 0 || j < (height / SCREEN_HEIGHT))
                        capture(localSaveLocation.getAbsolutePath(), j, SCREEN_WIDTH, SCREEN_HEIGHT, 0, 0);
                    else
                        capture(localSaveLocation.getAbsolutePath(), j, SCREEN_WIDTH, (height % SCREEN_HEIGHT == 0 ? SCREEN_HEIGHT : height
                                % SCREEN_HEIGHT), 0, SCREEN_HEIGHT - (height % SCREEN_HEIGHT));
                }
            });
        }
        display.syncExec(new Runnable() {
            public void run() {
                try {
                    // final Image result = new Image(display, width, height);
                    final Image result = new Image(display, width, SCREEN_HEIGHT);
                    GC gc = new GC(result);
                    // for (int i = 0; i < (1 + height / HEIGHT); i++) {
                    for (int i = 0; i < 1; i++) {

                        ImageLoader imageLoader = new ImageLoader();
                        File partialImageFile = new File(localSaveLocation, "savedImage" + i + ".jpg");
                        FileInputStream fis = new FileInputStream(partialImageFile);
                        Image loadedImage = new Image(display, imageLoader.load(fis)[0]);
                        gc.drawImage(loadedImage, 0, i * (SCREEN_HEIGHT - scrollbarY));
                        loadedImage.dispose();
                        fis.close();
                        // Delete the partial image file once we are done with
                        // it :
                        partialImageFile.delete();
                    }
                    gc.dispose();
                    ImageLoader imageSaver = new ImageLoader();
                    imageSaver.data = new ImageData[]{result.getImageData()};
                    Logger.log("Saving image to location :" + saveLocation);
                    FileOutputStream fos = new FileOutputStream(saveLocation);
                    imageSaver.save(fos, SWT.IMAGE_JPEG);
                    fos.close();
                    result.dispose();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

    }

    /**
     * Stop the rendering browser's rendering and script execution.
     */
    private void stopBrowser() {
        display.syncExec(new Runnable() {
            public void run() {
                Logger.log("Stopping browser rendering...");
                browser.stop();
            }
        });
    }

    /**
     * Initialize the browser - dispose the old browser if existant ,and create
     * a new browser.
     */
    private void initializeBrowser() {
        if (browser != null) {
            browser.dispose();
        }
        browser = new Browser(shell, SWT.NONE);
        GridData data = new GridData();
        data.horizontalAlignment = GridData.FILL;
        data.verticalAlignment = GridData.FILL;
        data.horizontalSpan = 3;
        data.grabExcessHorizontalSpace = true;
        data.grabExcessVerticalSpace = true;
        browser.setLayoutData(data);

        final Label status = new Label(shell, SWT.NONE);
        data = new GridData(GridData.FILL_HORIZONTAL);
        data.horizontalSpan = 2;
        status.setLayoutData(data);

        browser.addStatusTextListener(new StatusTextListener() {
            public void changed(StatusTextEvent event) {
                status.setText(event.text);
            }
        });

        shell.open();
        shell.setSize(SCREEN_WIDTH + 2, SCREEN_HEIGHT + 2);
        browser.setSize(SCREEN_WIDTH, SCREEN_HEIGHT);
        browser.setLocation(0, 0);

    }

    public void execute(final String script) throws RenderingException {
        this.execute(script, DEFAULT_JAVASCRIPT_TIMEOUT);
    }

    boolean executeSuccess;

    /**
     * Execute a Javascript command series on the acting browser.
     *
     * @param script
     * @throws RenderingException
     */
    public void execute(final String script, int timeout) throws RenderingException {
        final Lock lock = new Lock();
        executeSuccess = true;
        display.asyncExec(new Runnable() {
            public void run() {
                Logger.log("running script :" + script);
                final double randomNumber = Math.random();
                browser.addTitleListener(new RandomTitleListener(randomNumber, lock, browser));
                executeSuccess = browser.execute(script + "; document.title='" + randomNumber + "';");
            }
        });
        boolean timeoutSuccess = lock.waitFor(timeout);
        if (!executeSuccess)
            throw new RenderingException("Javascript Execution failed. (Javascript:" + script + ")");
        if (!timeoutSuccess)
            throw new RenderingException("Javascript Execution timeout exceeded. (Javascript:" + script + ")");
    }

    /**
     * Get a javascript value from the DOM. this function will block for the
     * maximum of 2sec. Please note that this function is not entirely reliable
     * , and may on some pages result a null result.
     *
     * @param script the DOM value to be traversed , i.e document.title
     * @return
     */
    public String getJavascriptValue(final String script) {
        final Lock lock = new Lock();
        final double randomNumber = Math.random();
        final ValueTitleListener valueTitleListener = new ValueTitleListener((int) (1000.0 * randomNumber), script, lock, browser);

        display.asyncExec(new Runnable() {
            public void run() {

                Logger.log("running script:" + script);
                browser.addTitleListener(valueTitleListener);
                browser.execute(valueTitleListener.getScript(script));
            }
        });
        lock.waitFor(2000);
        return valueTitleListener.getValue();
    }

    public void dispose() {
        display.syncExec(new Runnable() {
            public void run() {
                shell.dispose();
                browser.dispose();
            }
        });

        Logger.log("waiting for browser to shutdown properly...");
        browserShutdownLock.waitFor(10000);
        try {
            Thread.sleep(5000);
        } catch (Exception e) {
        }
        Logger.log("browser shotdown...");
    }

    /**
     * Initialize the renderer. This function must be called before making any
     * call to the render() function. This function blocks until the browser is
     * well in place.
     *
     * @throws Exception
     */
    public void init() throws Exception {
        this.start();
        browserInitializationLock.waitFor();
        if (this.exceptionThrown != null)
            throw this.exceptionThrown;
    }
}
