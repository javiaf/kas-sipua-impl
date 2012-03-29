package com.kurento.commons.sip.android;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.kurento.commons.sip.agent.SipEndPointImpl;

public class RegisterService extends Service {
	private final static String LOG_TAG = "RegisterService";
	SipEndPointImpl endpoint;

	@Override
	public IBinder onBind(Intent intent) {
		Bundle boundel = intent.getExtras();

		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onCreate() {
		Log.i(LOG_TAG, "on create method.");

	}

	@Override
	public void onStart(Intent intent, int startId) {
		Log.d(LOG_TAG, "On start method.");
		SipEndPointImpl ep = SecondeService.getEndpoint();
		if (ep != null) {
			int expiresTime = ep.getExpires();
			Log.d(LOG_TAG,
					"Sending register from Second service with expires tiem  "
							+ expiresTime);
			ep.setExpiresAndRegister(expiresTime);
		}
		this.stopSelf();
	}
	

	@Override
	public void onDestroy() {
		Log.d(LOG_TAG, "On destroy method.");
	}

}
