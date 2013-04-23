package com.kurento.commons.sip.util;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

public class RegisterService extends Service {

	private final static String LOG_TAG = RegisterService.class.getName();

	@Override
	public IBinder onBind(Intent intent) {
		// Bundle boundel = intent.getExtras();
		return null;
	}

	@Override
	public void onCreate() {
		Log.d(LOG_TAG, "on create Service.");
	}

	@Override
	public void onStart(Intent intent, int startId) {
		if (intent != null) {
			Bundle b = intent.getExtras();

			if (b != null) {
				Integer uuid = b.getInt("uuid");
				try {
					AlarmUaTimer.getTaskTable().get(uuid).run();
				} catch (Exception e) {
					// Do nothing
				}
			}
		}
	}

	@Override
	public void onDestroy() {
		Log.d(LOG_TAG, "On destroy method.");
	}

}
