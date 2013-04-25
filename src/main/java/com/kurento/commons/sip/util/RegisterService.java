package com.kurento.commons.sip.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;

public class RegisterService extends Service {

	private static final Logger log = LoggerFactory
			.getLogger(RegisterService.class);

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		log.debug("RegisterService onCreate");
		super.onCreate();
	}

	@Override
	public void onStart(Intent intent, int startId) {
		log.debug("RegisterService onStart");
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

}
