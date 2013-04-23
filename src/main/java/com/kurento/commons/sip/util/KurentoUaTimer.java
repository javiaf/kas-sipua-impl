package com.kurento.commons.sip.util;

import java.util.Date;

public interface KurentoUaTimer {

	/**
	 * Cancels a scheduled KurentoUaTimerTask.
	 */
	public void cancel(KurentoUaTimerTask task);

	/**
	 * Removes all canceled tasks from the task queue.
	 */
	public void purge();

	/**
	 * Schedule a task for repeated fixed-delay execution after a specific time
	 * has been reached.
	 * 
	 * @param task
	 * @param when
	 * @param period
	 */
	public void schedule(KurentoUaTimerTask task, Date when, long period);

	/**
	 * Schedule a task for repeated fixed-delay execution after a specific
	 * delay.
	 * 
	 * @param task
	 * @param delay
	 * @param period
	 */
	public void schedule(KurentoUaTimerTask task, long delay, long period);

	/**
	 * Schedule a task for single execution.
	 * 
	 * @param task
	 * @param when
	 */
	public void schedule(KurentoUaTimerTask task, Date when);

	/**
	 * Schedule a task for single execution after a specified delay.
	 * 
	 * @param task
	 * @param delay
	 */
	void schedule(KurentoUaTimerTask task, long delay);

	/**
	 * Schedule a task for repeated fixed-rate execution after a specific delay
	 * has passed.
	 * 
	 * @param task
	 * @param delay
	 * @param period
	 */
	void scheduleAtFixedRate(KurentoUaTimerTask task, long delay, long period);

	/**
	 * Schedule a task for repeated fixed-rate execution after a specific time
	 * has been reached.
	 * 
	 * @param task
	 * @param when
	 * @param period
	 */
	void scheduleAtFixedRate(KurentoUaTimerTask task, Date when, long period);

}
