package org.anonymous.dobrochan;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;

public class DobroNetworkStateReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context arg0, Intent arg1) {
		SharedPreferences prefs = DobroApplication.getApplicationStatic().getDefaultPrefs();
		String autorun = prefs.getString("autorun_network", "wifi");
		if (autorun.equalsIgnoreCase("never"))
			return;
		boolean isConnected = false;
		boolean isWifi = false;
		ConnectivityManager cm = (ConnectivityManager) arg0
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo ni;
		try {
			ni = cm.getActiveNetworkInfo();
		} catch (SecurityException e) {
			ni = null;
			isConnected = true; // пофиг же
		}
		if (ni != null && ni.isConnected()) {
			isConnected = true;
			if (ni.getType() == ConnectivityManager.TYPE_WIFI)
				isWifi = true;
		}
		Intent i = new Intent(arg0, DobroNetworkService.class);
		i.putExtra(DobroConstants.ALARM_INTENT, true);
		if ((autorun.equalsIgnoreCase("always") && isConnected)
				|| (autorun.equalsIgnoreCase("wifi") && isWifi)) {
			Intent srv = new Intent(arg0,DobroNetworkService.class);
			arg0.startService(srv);
		} else {
			Intent srv = new Intent(arg0,DobroNetworkService.class);
			srv.putExtra(DobroConstants.DISALARM_INTENT, true);
			arg0.startService(srv);
		}
	}

}
