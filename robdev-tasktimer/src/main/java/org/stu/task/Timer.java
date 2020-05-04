/*
 * Timer.java Created on 16 September 2006, 06:12
 */

package org.stu.task;

import java.util.List;

/**
 * Class which can be used to time tasks etc
 * 
 * @author Stuart
 */
public class Timer {

	/* total elapsed time for the timer */
	private long elapsedTime;
	/* last system time at which the timer was started */
	private long startTime;
	/* last system time at which the timer was stopped */
	private long stopTime;
	/* flag to indicate whether the timer is running */
	private boolean started;

	/* a list of Timers containing lap times */
	private List laps;

	/**
	 * Starts this timer, setting the start time to now.
	 */
	public void start() {
		if (isStarted()) {
			throw new RuntimeException("Timer is already started. Call stop() first.");
		}
		startTime = System.currentTimeMillis();
		setStarted(true);
	}

	/**
	 * Stops this timer, setting the stop time to now, and adjusting the elapsed time.
	 */
	public void stop() {
		if (!isStarted()) {
			throw new RuntimeException("Timer is not started. Call start() first.");
		}
		stopTime = System.currentTimeMillis();
		elapsedTime += (stopTime - startTime);
		setStarted(false);
	}

	/**
	 * Returns the elapsed time from first start time to now
	 */
	public long getElapsedTimeMillis() {
		if (!isStarted()) {
			return elapsedTime;
		}
		return elapsedTime + (System.currentTimeMillis() - startTime);
	}

	public void setElapsedTimeMillis(long elapsedTime) {
		boolean wasStarted = false;
		if (isStarted()) {
			wasStarted = true;
			stop();
		}
		this.elapsedTime = elapsedTime;
		startTime = System.currentTimeMillis() - elapsedTime;
		if (wasStarted) {
			start();
		}
	}

	/**
	 * @deprecated not currently implemented
	 */
	@Deprecated
	public void lap() {
		//
	}

	/**
	 * Resets all values to zero.
	 */
	public void reset() {
		elapsedTime = 0;
		startTime = 0;
		stopTime = 0;
		setStarted(false);
	}

	public boolean isStarted() {
		return started;
	}

	private void setStarted(boolean started) {
		this.started = started;
	}
}
