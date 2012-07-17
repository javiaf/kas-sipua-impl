package com.kurento.commons.sip.testutils;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import com.kurento.ua.commons.KurentoUaTimer;
import com.kurento.ua.commons.KurentoUaTimerTask;

public class TestTimer implements KurentoUaTimer {

	private Timer timer;

	private Map<KurentoUaTimerTask, TestTask> taskTable = new HashMap<KurentoUaTimerTask, TestTask>();

	public TestTimer() {
		timer = new Timer();
	}

	@Override
	public void cancel(KurentoUaTimerTask kurentoTask) {
		// Add task to timer
		TestTask task;
		if ((task = taskTable.get(kurentoTask)) != null) {
			task.cancel();
			taskTable.remove(kurentoTask);
		}
		
	}

	@Override
	public void purge() {
		// TODO Auto-generated method stub

	}

	@Override
	public void schedule(KurentoUaTimerTask task, Date when, long period) {
		// TODO Auto-generated method stub

	}

	@Override
	public void schedule(KurentoUaTimerTask kurentoTask, long delay, long period) {
		// Add task to timer
		TestTask task;
		if ((task = taskTable.get(kurentoTask)) == null) {
			task = new TestTask(kurentoTask);
			taskTable.put(kurentoTask, task);
		} 
		
		// Will throw exception if alrady scheluded
		timer.schedule(task, delay, period);

	}

	@Override
	public void schedule(KurentoUaTimerTask task, Date when) {
		// TODO Auto-generated method stub

	}

	@Override
	public void schedule(KurentoUaTimerTask task, long delay) {
		// TODO Auto-generated method stub

	}

	@Override
	public void scheduleAtFixedRate(KurentoUaTimerTask task, long delay,
			long period) {
		// TODO Auto-generated method stub

	}

	@Override
	public void scheduleAtFixedRate(KurentoUaTimerTask task, Date when,
			long period) {
		// TODO Auto-generated method stub

	}

	private class TestTask extends TimerTask {

		private KurentoUaTimerTask task;

		protected TestTask(KurentoUaTimerTask task) {
			this.task = task;
		}

		@Override
		public void run() {
			task.run();
		}

	}

}
