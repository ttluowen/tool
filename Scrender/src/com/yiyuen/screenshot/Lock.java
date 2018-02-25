/*
 * Created on 10/08/2004
 */
package com.yiyuen.screenshot;

/**
 * @author anat, Ohad Serfaty
 */
public class Lock {

	private boolean done = false;

	public void release() {

		done = true;
		synchronized (this) {
			this.notify();
		}
	}

	public boolean waitFor() {

		return this.waitFor(Long.MAX_VALUE);
	}

	/**
	 * Wait for the lock to be release for the specified timeout.
	 * 
	 * @param milisecondsToWait
	 * @return true if the lock was released before the timeout. false
	 *         otherwise.
	 */
	public boolean waitFor(long milisecondsToWait) {

		long startTime = System.currentTimeMillis();
		boolean result = true;
		synchronized (this) {
			while (done == false && (result = ((startTime + milisecondsToWait - System.currentTimeMillis()) >= 0)))
				try {
					// Logger.log("Waiting for :" + timeLeft);
					this.wait(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
		}

		if (done == true)
			result = true;
		done = false;

		return result;
	}
}
