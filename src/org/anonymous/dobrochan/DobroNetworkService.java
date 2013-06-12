package org.anonymous.dobrochan;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class DobroNetworkService extends Service {

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		if (intent != null
				&& intent.getBooleanExtra(DobroConstants.ALARM_INTENT, false)
				&& DobroHelper.checkNetwork(this)) {
			DobroNetwork.getInstance().queueFavChecking();
		} else if (intent != null
				&& intent
						.getBooleanExtra(DobroConstants.DISALARM_INTENT, false)) {
			((DobroApplication) getApplication()).unregisterAlarm();
		} else if (DobroHelper.checkAutorun(this)
				&& DobroHelper.checkNetwork(this)) {
			((DobroApplication) getApplication()).registerAlarm();
		}
		stopSelf();
		return START_NOT_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}