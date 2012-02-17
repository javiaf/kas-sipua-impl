package com.kurento.commons.sip.android;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.kurento.commons.sip.agent.SipEndPointImpl;
import com.kurento.commons.sip.transaction.CRegister;
import com.kurento.commons.ua.exception.ServerInternalErrorException;

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
			CRegister register;
			try {
				int  expirestTime = ep.getExpires();
				Log.d(LOG_TAG, "Sending register from Second service with expires tiem  " + expirestTime);
				register = new CRegister(ep);
				register.sendRequest(null);
			} catch (ServerInternalErrorException e) {
	
			}
		}
		this.stopSelf();
	}
	

	@Override
	public void onDestroy() {
		Log.d(LOG_TAG, "On destroy method.");
	}

}
