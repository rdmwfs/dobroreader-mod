package org.anonymous.dobrochan;

import greendroid.app.GDApplication;

import java.util.LinkedList;
import java.util.List;

//import org.acra.ACRA;
//import org.acra.ReportingInteractionMode;
//import org.acra.annotation.ReportsCrashes;
import org.anonymous.dobrochan.sqlite.FakeCache;
import org.anonymous.dobrochan.sqlite.HiddenPostsOpenHelper;
import org.anonymous.dobrochan.sqlite.IHiddenPosts;
import org.anonymous.dobrochan.sqlite.IThreadsCache;
import org.anonymous.dobrochan.sqlite.IThreadsInfoCache;
import org.anonymous.dobrochan.sqlite.ThreadsCacheOpenHelper;
import org.anonymous.dobrochan.sqlite.ThreadsInfoCacheOpenHelper;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.os.Handler;
import android.os.StrictMode;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.widget.Toast;

//@ReportsCrashes(formKey = "dEV3RHNJTVRrWm5vVU5iMWI1ZHFoVEE6MQ", mode = ReportingInteractionMode.NOTIFICATION, resNotifTickerText = R.string.crash_notif_ticker_text, resNotifTitle = R.string.crash_notif_title, resNotifText = R.string.crash_notif_text, resDialogText = R.string.crash_dialog_text, resDialogCommentPrompt = R.string.crash_dialog_comment_prompt, resDialogOkToast = R.string.crash_dialog_ok_toast, socketTimeout = 7000)
public class DobroApplication extends GDApplication implements OnSharedPreferenceChangeListener {
	private DobroNetwork m_network = null;
	private DobroQuoteHolder m_quote = null;
	private DobroTabsHolder m_tabs = null;
	private DobroParser m_parser = null;
	private IThreadsCache m_threads = null;
	private IThreadsInfoCache m_threads_info = null;
	private IHiddenPosts m_hidden_posts = null;
	private SharedPreferences m_default_prefs = null;
	private List<Long> m_downloads = new LinkedList<Long>();
	private int progress_id = 100;
	private long m_joke;
	
	public boolean show_spoilers = true;
	
	public void addDownloadId(long id) {
		synchronized (m_downloads) {
			m_downloads.add(id);
		}
	}
	public boolean delDownloadId(long id) {
		synchronized (m_downloads) {
			return m_downloads.remove(id);
		}
	}
	public int nextProgressId() {
		progress_id++;
		return progress_id;
	}
	@SuppressWarnings("unused")
	@Override
	public void onCreate() {
		super.onCreate();
//		ACRA.init(this);
		if (false) {
	         StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
	                 .detectAll()
	                 .penaltyLog()
	                 .build());
	         StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
	                 .detectAll()
	                 .penaltyLog()
	                 .penaltyDeath()
	                 .build());
		}
		m_instance = this;
		m_handler = new Handler();
		m_network = new DobroNetwork(this);
		m_quote = new DobroQuoteHolder();
		m_tabs = new DobroTabsHolder();
		m_parser = new DobroParser();
		try{
			m_threads = new ThreadsCacheOpenHelper(this);
		} catch (Exception e) {
			m_threads = new FakeCache();
		}
		try{
			m_threads_info = new ThreadsInfoCacheOpenHelper(this);
		} catch (Exception e) {
			m_threads_info = new FakeCache();
		}
		try{
			m_hidden_posts = new HiddenPostsOpenHelper(this);
		} catch (Exception e) {
			m_hidden_posts = new FakeCache();
		}
		m_default_prefs = PreferenceManager.getDefaultSharedPreferences(this);
		m_joke = System.currentTimeMillis() + 5*60*1000;
		if(Build.VERSION.SDK_INT > 7)
			registerSDCardStateChangeListener();
		DobroHelper.updateCurrentTheme(this);
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		prefs.registerOnSharedPreferenceChangeListener(this);
		show_spoilers = prefs.getBoolean("show_spoilers", true);
	}
	
	public boolean checkJoke() {
		if(System.currentTimeMillis() > m_joke)
		{
			m_joke = System.currentTimeMillis() + 20*60*1000;
			return true;
		}
		return false;
	}

	@Override
	public Class<?> getHomeActivityClass() {
		return null;
	}

	@Override
	public Intent getMainApplicationIntent() {
		return null;
	}

	static private DobroApplication m_instance = null;
	static private Handler m_handler = null;

	static public DobroApplication getApplicationStatic() {
		return m_instance;
	}

	private class ToastExecutor implements Runnable {
		String text;
		int duration;

		public ToastExecutor(String text, int duration) {
			super();
			this.text = text;
			this.duration = duration;
		}

		@Override
		public void run() {
			Toast.makeText(DobroApplication.this, text, duration).show();
		}
	}

	synchronized public void showToast(String text, int time) {
		m_handler.post(new ToastExecutor(text, time));
	}

	public void registerAlarm() {
		Intent i = new Intent(this, DobroNetworkService.class);
		i.putExtra(DobroConstants.ALARM_INTENT, true);
		if (PendingIntent.getService(getApplicationContext(), 0, i,
				PendingIntent.FLAG_NO_CREATE) != null) {
			return;
		}
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		int period = Integer.parseInt(prefs.getString("period", "30"));
		if (period < 5)
			period = 5;
		PendingIntent pending = PendingIntent.getService(
				getApplicationContext(), 0, i, 0);
		AlarmManager alarm = (AlarmManager) getSystemService(ALARM_SERVICE);
		alarm.cancel(pending);
		alarm.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
				SystemClock.elapsedRealtime()+5000, period * 60 * 1000, pending);
	}

	public void unregisterAlarm() {
		Intent i = new Intent(this, DobroNetworkService.class);
		i.putExtra(DobroConstants.ALARM_INTENT, true);
		PendingIntent pending = PendingIntent.getService(this, 0, i,
				PendingIntent.FLAG_NO_CREATE);
		if (pending != null) {
			AlarmManager alarm = (AlarmManager) getSystemService(ALARM_SERVICE);
			alarm.cancel(pending);
			pending.cancel();
		}
	}

	public DobroNetwork getNetwork() {
		return m_network;
	}

	public DobroQuoteHolder getQuoter() {
		return m_quote;
	}
	
	public DobroTabsHolder getTabs() {
		return m_tabs;
	}
	
	public DobroParser getParser() {
		return m_parser;
	}
	
	public IThreadsCache getThreads() {
		return m_threads;
	}
	
	public IThreadsInfoCache getThreadsInfo() {
		return m_threads_info;
	}
	
	public IHiddenPosts getHiddenPosts() {
		return m_hidden_posts;
	}
	public SharedPreferences getDefaultPrefs() {
		return m_default_prefs;
	}
	
	BroadcastReceiver mSDCardStateChangeListener;
	void registerSDCardStateChangeListener() {
        mSDCardStateChangeListener = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if(TextUtils.equals(action,Intent.ACTION_MEDIA_MOUNTED) && m_threads.isFake()) try {
                    m_hidden_posts = null;
                    m_hidden_posts = new HiddenPostsOpenHelper(DobroApplication.this);
                    m_threads = null;
                    m_threads = new ThreadsCacheOpenHelper(DobroApplication.this);
                    m_threads_info = null;
                    m_threads_info = new ThreadsInfoCacheOpenHelper(DobroApplication.this);
//                    showToast("DobroReader: флешка доступна, кеш активирован", 5);
                    m_network.loadCookies();
                } catch (Exception e) {
                    m_hidden_posts = new FakeCache();
                    m_threads = new FakeCache();
                    m_threads_info = new FakeCache();
                }
                else if(TextUtils.equals(action,Intent.ACTION_MEDIA_UNMOUNTED) &&
                		 !m_threads.isFake()) {
                    ((SQLiteOpenHelper)m_hidden_posts).close();
                    m_hidden_posts = null;
                    m_hidden_posts = new FakeCache();
                    ((SQLiteOpenHelper)m_threads).close();
                    m_threads = null;
                    m_threads = new FakeCache();
                    ((SQLiteOpenHelper)m_threads_info).close();
                    m_threads_info = null;
                    m_threads_info = new FakeCache();
//                    showToast("DobroReader: флешка недоступна, кеш отключен", 5);
                }
			}
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        filter.addDataScheme("file");
        registerReceiver(mSDCardStateChangeListener, filter);
    }
	@Override
	public void onSharedPreferenceChanged(SharedPreferences arg0, String arg1) {
		show_spoilers = arg0.getBoolean("show_spoilers", true);
	}
}
