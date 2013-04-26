package org.anonymous.dobrochan;

import greendroid.app.GDActivity;

import org.anonymous.dobrochan.activity.DobroStarredEditor;
import org.anonymous.dobrochan.reader.R;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

import com.google.android.apps.dashclock.api.DashClockExtension;
import com.google.android.apps.dashclock.api.ExtensionData;

public class DCDashClockExtension extends DashClockExtension {

	@Override
	protected void onInitialize(boolean isReconnect) {
		super.onInitialize(isReconnect);
		setUpdateWhenScreenOn(true);
		String[] urls = {"null://org.anonymous.dobrochan.content.favs"};
		addWatchContentUris(urls);
	}

	@Override
	public void onCreate() {
		super.onCreate();
	}

	private String dump = null;
	private String text = null;

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if(intent != null && TextUtils.equals(intent.getAction(),"org.anonymous.dobrochan.favs")) {
			dump = null;
			text = null;
			if(intent.hasExtra(DobroConstants.FAVS_DUMP)) {
				dump = intent.getStringExtra(DobroConstants.FAVS_DUMP);
			}
			if(intent.hasExtra(DobroConstants.FAVS_TITLES)) {
				text = intent.getStringExtra(DobroConstants.FAVS_TITLES);
			}
			getContentResolver().notifyChange(Uri.parse("hull://org.anonymous.dobrochan.content.favs"), null, false);
		}
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	protected void onUpdateData(int arg0) {
		updateInfo();
	}
	
	private void updateInfo() {
		if(dump == null) {
			publishUpdate(null);
			return;
		}
		Context context = DobroApplication.getApplicationStatic();
		Intent notificationIntent = new Intent(context,
				DobroStarredEditor.class);
		notificationIntent.putExtra(GDActivity.GD_ACTION_BAR_TITLE,
				context.getString(R.string.starred));
		notificationIntent.putExtra(DobroConstants.FAVS_DUMP, dump);
		publishUpdate(new ExtensionData()
			.visible(true)
			.icon(R.drawable.dashclock)
			.status("+")
			.expandedTitle("DobroReader")
			.expandedBody("Новые сообщения:\n"+text)
			.clickIntent(notificationIntent));
	}
}
