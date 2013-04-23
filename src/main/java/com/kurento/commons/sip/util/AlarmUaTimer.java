package com.kurento.commons.sip.util;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;

public class AlarmUaTimer implements KurentoUaTimer {
	private String LOG = AlarmUaTimer.class.getName();

	private AlarmManager alarmManager;
	private Intent serviceIntent;
	private Context context;
	private PendingIntent pendingIntent;

	private static HashMap<Integer, KurentoUaTimerTask> taskTable = new HashMap<Integer, KurentoUaTimerTask>();

	public AlarmUaTimer(Context context) {
		this.context = context;
		alarmManager = (AlarmManager) this.context
				.getSystemService(Context.ALARM_SERVICE);
		serviceIntent = new Intent(this.context, RegisterService.class);
	}

	public static HashMap<Integer, KurentoUaTimerTask> getTaskTable() {
		return taskTable;
	}

	@Override
	public void cancel(KurentoUaTimerTask task) {
		Integer uuid = -1;
		for (Iterator<Integer> i = taskTable.keySet().iterator(); i.hasNext();) {
			Integer key = (Integer) i.next();
			if (taskTable.get(key).equals(task)) {
				uuid = key;
			}
		}
		pendingIntent = PendingIntent.getService(this.context, uuid,
				serviceIntent, 0);
		alarmManager.cancel(pendingIntent);
		taskTable.remove(uuid);
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
	public void schedule(final KurentoUaTimerTask task, long delay, long period) {
		Integer uuid;
		if (!taskTable.containsValue(task)) {
			// The new Random().nextInt is necessary because we need that
			// pendingIntent will be differents.
			uuid = new Random().nextInt();
			taskTable.put(uuid, task);

			Bundle extras = new Bundle();
			extras.putInt("uuid", uuid);
			serviceIntent.putExtras(extras);

			pendingIntent = PendingIntent.getService(this.context, uuid,
					serviceIntent, 0);

			alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
					SystemClock.elapsedRealtime() + delay, period,
					pendingIntent);
		}
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

}
