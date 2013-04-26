package org.anonymous.dobrochan.activity;

import greendroid.app.GDActivity;
import greendroid.util.Md5Util;
import greendroid.widget.ActionBarItem;
import greendroid.widget.ItemAdapter;
import greendroid.widget.item.Item;
import greendroid.widget.item.ProgressItem;

import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.anonymous.dobrochan.DobroApplication;
import org.anonymous.dobrochan.DobroConstants;
import org.anonymous.dobrochan.DobroHelper;
import org.anonymous.dobrochan.DobroNetwork;
import org.anonymous.dobrochan.DobroParser;
import org.anonymous.dobrochan.DobroPostItem;
import org.anonymous.dobrochan.DobroTabsHolder;
import org.anonymous.dobrochan.reader.R;
import org.anonymous.dobrochan.json.DobroPost;
import org.anonymous.dobrochan.json.DobroThread;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.ClipboardManager;
import android.text.TextUtils;
import android.text.format.Time;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonObject;

public class DobroThreadActivity extends DobroPostsList {
	MediaPlayer mMediaPlayer;
	int autocheck_interval;
	boolean autocheck_sound;
	@Override
	protected void onStop() {
		try{
			if(future_task != null) {
				future_task.cancel(true);
				future_task = null;
				executor.remove(autoupdater_runnable);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		super.onStop();
	}
	@Override
	public void onLowMemory() {
		Toast.makeText(this, "Недостаточно свободной памяти. Enlarge your heap.", Toast.LENGTH_SHORT).show();
		System.gc();
		super.onLowMemory();
	}
	boolean resume_autoupdate = false;
	@Override
	protected void onPause() {
		if(wl != null)
			wl.release();
		if(future_task != null) {
			future_task.cancel(true);
			future_task = null;
			executor.remove(autoupdater_runnable);
			resume_autoupdate = true;
		}
		String value = null;
		try {
			DobroPostItem dpi = (DobroPostItem) m_adapter.getItem(getListView().getFirstVisiblePosition());
			value = dpi.post.getDisplay_id();
		} catch (Exception e) {
			e.printStackTrace();
		}
		DobroTabsHolder.getInstance().updateScroll(board, thread, value);
		super.onPause();
	}
	@Override
	protected void onResume() {
		if(wl != null)
			wl.acquire();
		if(future_task != null) {
			future_task.cancel(true);
			future_task = null;
		}
		if(resume_autoupdate) {
			resume_autoupdate = false;
			future_task = executor.scheduleAtFixedRate(autoupdater_runnable, 5, autocheck_interval, TimeUnit.SECONDS);
		}
		super.onResume();
	}
	private static final int AB_WAKE = 1;
	private static final int AB_ADD = 2;
	private static final int AB_REFRESH = 0;
	PowerManager.WakeLock wl = null;
	Runnable autoupdater_runnable;
	ScheduledFuture future_task = null;
	ScheduledThreadPoolExecutor executor;
	@Override
	public boolean onHandleActionBarItemClick(ActionBarItem item, int position) {
		if(position == -1) {
			Intent i = new Intent(this, DobroTabsList.class);
			i.putExtra(GD_ACTION_BAR_TITLE, "Вкладки");
			i.putExtra(DobroConstants.BOARD, board);
			i.putExtra(DobroConstants.THREAD, thread);
			i.putExtra(DobroConstants.TITLE, getIntent().getStringExtra(GD_ACTION_BAR_TITLE));
			String value = null;
			try {
				DobroPostItem dpi = (DobroPostItem) m_adapter.getItem(getListView().getFirstVisiblePosition());
				value = dpi.post.getDisplay_id();
			} catch (Exception e) {
				e.printStackTrace();
			}
			i.putExtra(DobroConstants.SCHROLL_TO, value);
			startActivity(i);
			return true;
		}
		switch(position) {
		case AB_ADD:
			Intent i = new Intent(this, DobroNewPostActivity.class);
			i.putExtra(
					GDActivity.GD_ACTION_BAR_TITLE,
					String.format("Ответ в >>%s/%s",
							board,
							thread));
			i.putExtra(DobroConstants.BOARD, this.board);
			i.putExtra(DobroConstants.THREAD, this.thread);
			startActivityForResult(i, DobroConstants.ANSWER_RESULT);
			return true;
		case AB_WAKE:
			if(wl == null) {
				PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
				wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "DobroThread");
				wl.acquire();
				item.setDrawable(R.drawable.sun_32x32);
				Toast.makeText(this, "Отключение подсветки запрещено", Toast.LENGTH_SHORT).show();
			} else {
				wl.release();
				wl = null;
				item.setDrawable(R.drawable.moon_stroke_32x32);
				Toast.makeText(this, "Отключение подсветки разрешено", Toast.LENGTH_SHORT).show();
			}
			return true;
		case AB_REFRESH:
			if(future_task != null) {
				future_task.cancel(true);
				future_task = null;
				executor.remove(autoupdater_runnable);
				item.setDrawable(R.drawable.loop_32x32);
				Toast.makeText(this, "Автообновление отключено", Toast.LENGTH_SHORT).show();
			} else {
				try{
					executor.remove(autoupdater_runnable);
				} catch (Exception e) {
					e.printStackTrace();
				}
				future_task = executor.scheduleAtFixedRate(autoupdater_runnable, 5, autocheck_interval, TimeUnit.SECONDS);
				item.setDrawable(R.drawable.loop_red_32x32);
				Toast.makeText(this, "Автообновление активировано", Toast.LENGTH_SHORT).show();
			}
			return true;
		default:
			return false;
		}
	}
	@Override
	protected void onNewIntent(Intent intent) {
		setIntent(intent);
		if(Intent.ACTION_SEARCH.equals(intent.getAction()))
		{
			String query = intent.getStringExtra(SearchManager.QUERY);
			Pattern p = Pattern.compile(Pattern.quote(query), Pattern.CASE_INSENSITIVE);
			List<String> lst = new LinkedList<String>();
			for(int i = 0; i < m_adapter.getCount(); i++)
			{
				try {
					DobroPostItem item = (DobroPostItem) m_adapter.getItem(i);
					if(p.matcher(item.post.getFormattedText()).find())
						lst.add(">>"+item.post.getDisplay_id());
				} catch ( ClassCastException e ) {
					continue;
				}
			}
			if(lst.size() == 0) {
				Toast.makeText(this, "Ничего не найдено", Toast.LENGTH_SHORT).show();
			} else {
				openLinks(lst);
			}
		}
		if(Intent.ACTION_VIEW.equals(intent.getAction()) && intent.hasExtra(DobroConstants.FAVS_DUMP)) {
			Log.e("onNewIntent","Favs");
			startActivity(intent);
		}
	}
	private ThreadLoader atask = null;

	public String board = null;
	public String thread = null;
	public String thread_id = null;
	public String count = null;
	public ItemAdapter m_adapter = null;
	private String schrollTo = null;

	private class ThreadLoaderParams {
		public String thread_id;
		public String board;
		public String thread;
		public String last_post;
		public String count;
		public boolean force = false;
		public boolean beep;
		
		ThreadLoaderParams(String b, String p, String l, String c, boolean bb) {
			board = b;
			thread = p;
			last_post = l;
			count = c;
			force = false;
			beep = bb;
			this.thread_id = null;
		}

		ThreadLoaderParams(String b, String p, String tid, boolean f) {
			board = b;
			thread = p;
			last_post = null;
			count = null;
			force = f;
			beep = false;
			this.thread_id = tid;
		}

		ThreadLoaderParams(String b, String p, String l, String c) {
			board = b;
			thread = p;
			last_post = l;
			count = c;
			force = false;
			beep = false;
			this.thread_id = null;
		}
	}

	private class ThreadLoader extends
	AsyncTask<ThreadLoaderParams, DobroThread, DobroThread> {
		Handler h = null;
		@Override
		protected void onPreExecute() {
			h = new Handler();
		}

		@Override
		protected void onProgressUpdate(DobroThread... values) {
			onPostExecute(values[0]);
		}

		ThreadLoaderParams m_param;

		@Override
		protected DobroThread doInBackground(ThreadLoaderParams... params) {
			try{
				m_param = params[0];
				JsonObject json;
				String adress;
				if (m_param.thread_id != null)
					adress = m_param.thread_id;
				else
					adress = m_param.board + "/" + m_param.thread;
				/*try{
					String title = getIntent().getStringExtra(GDActivity.GD_ACTION_BAR_TITLE);
					if(title.equalsIgnoreCase("Понитред"))
						new Timer().schedule(new TimerTask() {
							@Override
							public void run() {
								try{
									if(ThreadLoader.this.getStatus() == AsyncTask.Status.RUNNING)
										h.post(new Runnable() {
											@Override
											public void run() {
												tryJoke();
											}
										});
								} catch (Exception e) {
								}
							}
						}, 1000);
					super.onPreExecute();
				} catch (Exception e) {
					e.printStackTrace();
				}*/
				long tm = System.currentTimeMillis();
				json = DobroNetwork.getInstance().getThreadJson(adress, m_param.last_post,
						m_param.count, m_param.force);
				Log.d("TIME", "Network time: "+String.valueOf(System.currentTimeMillis()-tm));
				tm = System.currentTimeMillis();
				DobroThread t = DobroParser.getInstance().parceThread(json);
				Log.d("TIME", "Parse time: "+String.valueOf(System.currentTimeMillis()-tm));
				tm = System.currentTimeMillis();
				/*if (t!=null&&
						t.getTitle()!=null&&
						(Md5Util.md5(t.getTitle())
								.equalsIgnoreCase(DobroConstants.DCOTT)))
					t = null;*/
				if(t.isThreadModified() || t == null)
					return t;
				else
				{
					if(getIntent().getBooleanExtra(DobroConstants.DISABLE_UPDATE, false)) {
						t.setThreadModified(true);
						return t;
					}
					else {
						this.publishProgress(t);
						params[0].last_post = t.getPosts()[t.getPosts().length-1].getDisplay_id();
						return doInBackground(params[0]);
					}
				}
			}
			catch(Exception e)
			{
				e.printStackTrace();
				return null;
			}
		}

		@Override
		protected void onPostExecute(DobroThread result) {
			long tm = System.currentTimeMillis();
			super.onPostExecute(result);
			final ItemAdapter adapter = DobroThreadActivity.this.m_adapter;
			ProgressItem pi = (ProgressItem) adapter
					.getItem(adapter.getCount() - 1);
			pi.isInProgress = false;
			if (result == null || result.getPosts().length == 0) {
				if(result == null) {
					Toast.makeText(DobroThreadActivity.this, "Ошибка", Toast.LENGTH_SHORT).show();
				}
				pi.text = getString(R.string.update);
				pi.enabled = true;
//				adapter.notifyDataSetChanged();
				View v = getListView().getChildAt(adapter.getCount() - 1 - getListView().getFirstVisiblePosition());
				updateProgressView(v, pi);
				return;
			}
			addPosts(result);
			if (!result.getPosts()[0].isOp() && adapter.getCount() > 1)
				adapter.remove(pi);
			if (result.getPosts()[0].isOp() && adapter.getCount() == 1)
				result.getPosts()[0].setOp(false);
			pi.text = result.getTitle();
			pi.enabled = false;
			for (DobroPost post : result.getPosts()) {
				postPositionMap.put(post.getDisplay_id(), adapter.getCount());
				post.setNumber(adapter.getCount());
				adapter.insert(new DobroPostItem(post), adapter.getCount());
			}
			ProgressItem nextPage = new ProgressItem(
					getString(R.string.update), false);
			nextPage.setTag(DobroConstants.UPDATE_TAG + result.getPosts()[result.getPosts().length-1].getDisplay_id());
			adapter.add(nextPage);
			if(!result.isThreadModified())
				nextPage.isInProgress = true;
			adapter.notifyDataSetChanged();
			if(schrollTo != null) {
				Integer pos = postPositionMap.get(schrollTo);
				if(pos != null)
				{
					getListView().setSelection(pos);
					schrollTo = null;
				} else {
					getListView().setSelection(getListAdapter().getCount()-1);
				}
			}
			else if(!result.isThreadModified())
				getListView().setSelection(getListAdapter().getCount()-1);
			else
				getListView().setSelection(postPositionMap.get(result.getPosts()[0].getDisplay_id()));
			atask = null;
			Log.d("TIME", "Post exec time: "+String.valueOf(System.currentTimeMillis()-tm));
			tm = System.currentTimeMillis();
			if(m_param.beep && autocheck_sound) try {
				String snd_uri = DobroApplication.getApplicationStatic().getDefaultPrefs().getString(
						"notify_sound_uri",Settings.System.DEFAULT_NOTIFICATION_URI.toString());
				Uri notification = Uri.parse(snd_uri);
//				Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
//				r.play();
				mMediaPlayer.reset();
				 mMediaPlayer.setDataSource(DobroThreadActivity.this, notification);
				 final AudioManager audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
				 if (audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION) != 0) {
					 mMediaPlayer.setAudioStreamType(AudioManager.STREAM_NOTIFICATION);
					 mMediaPlayer.setLooping(false);
					 mMediaPlayer.prepare();
					 mMediaPlayer.start();
				  }
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		ItemAdapter adapter = (ItemAdapter) getListAdapter();
		Item item = (Item) adapter.getItem(position);
		String tag = (String) item.getTag();
		if (tag != null && tag.startsWith(DobroConstants.UPDATE_TAG)) {
			((ProgressItem) item).isInProgress = true;
			((ProgressItem) item).enabled = false;
			((ProgressItem) item).text = getString(R.string.loading);
//			adapter.notifyDataSetChanged();
			updateProgressView(v, (ProgressItem) item);
			new ThreadLoader().execute(new ThreadLoaderParams(board, thread,
					tag.substring(DobroConstants.UPDATE_TAG.length()), count));
		} else if (tag != null && !tag.startsWith(DobroConstants.HIDDEN_TAG)) {
			/*
			 * DobroPostItem ditem = (DobroPostItem)item; Intent intent = new
			 * Intent(DobroThreadActivity.this, DobroNewPost.class);
			 * intent.putExtra(DobroNewPost.BOARD_EXT, ditem.post.getBoard());
			 * intent.putExtra(DobroNewPost.THREAD_EXT, ditem.post.getThread());
			 * intent.putExtra(DobroNewPost.POST_EXT,
			 * ditem.post.getDisplay_id());
			 * intent.putExtra(GDActivity.GD_ACTION_BAR_TITLE,
			 * String.format("Ответ в >>%s/%s", ditem.post.getBoard(),
			 * ditem.post.getThread())); startActivity(intent);
			 */
		}
	}
	
	void updateProgressView(View v, ProgressItem item) {
		try{
			ProgressBar mProgressBar = (ProgressBar) v.findViewById(R.id.gd_progress_bar);
			mProgressBar.setVisibility(item.isInProgress ? View.VISIBLE : View.GONE);
			TextView mTextView = (TextView) v.findViewById(R.id.gd_text);
			mTextView.setText(item.text);
		} catch (Exception e) {
			e.printStackTrace();
			//m_adapter.notifyDataSetChanged();
		}
	}

	@Override
	protected void onDestroy() {
		try {
			if (atask != null)
				atask.cancel(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
		super.onDestroy();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		DobroHelper.updateCurrentTheme(this);
		DobroHelper.setOrientation(this);
		super.onCreate(savedInstanceState);
		executor = new ScheduledThreadPoolExecutor(1);
		mMediaPlayer = new MediaPlayer();
		SharedPreferences prefs = DobroApplication.getApplicationStatic().getDefaultPrefs();
		autocheck_sound = prefs.getBoolean("autocheck_sound", true);
		try{
			autocheck_interval = Integer.parseInt(prefs.getString("autocheck_interval", "30"));
		} catch (Exception e) {
			autocheck_interval = 30;
		}
		if(autocheck_interval < 30)
			autocheck_interval = 30;
		autoupdater_runnable = new Runnable(){
			@Override
			public void run() {
				DobroThreadActivity.this.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						try{
							ListView v = getListView();
							ItemAdapter adapter = (ItemAdapter) getListAdapter();
							Object item = v.getItemAtPosition(v.getCount()-1);
							ProgressItem pi = (ProgressItem) item;
							String tag = (String) pi.getTag();
							if (!pi.isInProgress && tag != null && tag.startsWith(DobroConstants.UPDATE_TAG)) {
								pi.isInProgress = true;
								pi.enabled = false;
								pi.text = getString(R.string.loading);
//								adapter.notifyDataSetChanged();
								View iv = getListView().getChildAt(adapter.getCount() - 1 - getListView().getFirstVisiblePosition());
								updateProgressView(iv, pi);
								
								new ThreadLoader().execute(new ThreadLoaderParams(board, thread,
										tag.substring(DobroConstants.UPDATE_TAG.length()), count, true));
							}
						} catch(Exception e) {
							e.printStackTrace();
						}
					}
				});
			}
		};
		this.getListView().setFastScrollEnabled(true);
		this.getListView().setItemsCanFocus(false);
		m_adapter = new ItemAdapter(this);
		final ProgressItem load_item = new ProgressItem(
				getString(R.string.loading), true);
		load_item.enabled = false;
		m_adapter.add(load_item);

		if (getIntent().hasExtra(DobroConstants.THREAD))
			this.thread = getIntent().getStringExtra(DobroConstants.THREAD);
		else
			this.thread = null;

		if (getIntent().hasExtra(DobroConstants.BOARD))
			this.board = getIntent().getStringExtra(DobroConstants.BOARD);
		else
			this.board = null;

		if (getIntent().hasExtra(DobroConstants.THREAD_ID))
			this.thread_id = getIntent().getStringExtra(
					DobroConstants.THREAD_ID);
		else
			this.thread_id = null;
		
		if (getIntent().hasExtra(DobroConstants.SCHROLL_TO))
			this.schrollTo = getIntent().getStringExtra(DobroConstants.SCHROLL_TO);
		else
			this.schrollTo = null;

		handleUrl();
		
		addActionBarItem(ActionBarItem.Type.Slideshow, AB_REFRESH).setDrawable(R.drawable.loop_32x32);
		addActionBarItem(ActionBarItem.Type.Eye, AB_WAKE).setDrawable(R.drawable.moon_stroke_32x32);
		addActionBarItem(ActionBarItem.Type.Add, AB_ADD);

		setListAdapter(m_adapter);
		createThread();
	}

	private void handleUrl() {
		String data = this.getIntent().getDataString();
		if(data != null){
			Pattern p = Pattern.compile("dobrochan\\.(ru|org)/(.*)/res/(\\d+)\\.xhtml(#i.*)?");
			Matcher m = p.matcher(data);
			if(m.find()) {
				board = m.group(2);
				thread = m.group(3);
				schrollTo = m.group(4);
				if(schrollTo != null && schrollTo.length() > 2)
					schrollTo = schrollTo.substring(2);
				else
					schrollTo = null;
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.thread_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.search_thread:
			onSearchRequested();
			return true;
		case R.id.force_reload:
			m_adapter.clear();
			postPositionMap.clear();
			final ProgressItem load_item = new ProgressItem(
					getString(R.string.loading), true);
			load_item.enabled = false;
			m_adapter.add(load_item);
			m_adapter.notifyDataSetChanged();

			new ThreadLoader().execute(new ThreadLoaderParams(this.board,
					this.thread, this.thread_id, true));
			return true;
		case R.id.go_to: {
			Intent i = new Intent(this, DobroGotoActivity.class);
			i.putExtra(GD_ACTION_BAR_TITLE, getString(R.string.go_to));
			startActivity(i);
			return true;
		}
		case R.id.copy_link: {
			ClipboardManager clipboard = 
					(ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
			String text = DobroConstants.HOST+board+"/res/"+thread+".xhtml";
			clipboard.setText(text);
			Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
			return true;
		}
		}
		return super.onOptionsItemSelected(item);
	}

	public void createThread() {
		if(atask != null)
			return;
		if (getIntent().hasExtra(DobroConstants.COUNT)) {
			this.count = getIntent().getStringExtra(DobroConstants.COUNT);
			atask = (ThreadLoader) new ThreadLoader()
			.execute(new ThreadLoaderParams(
					DobroThreadActivity.this.board,
					DobroThreadActivity.this.thread,
					DobroThreadActivity.this.thread_id, getIntent()
					.getStringExtra(DobroConstants.COUNT)));
		} else if (getIntent().hasExtra(DobroConstants.LAST)) {
			atask = (ThreadLoader) new ThreadLoader()
			.execute(new ThreadLoaderParams(
					DobroThreadActivity.this.board,
					DobroThreadActivity.this.thread, getIntent()
					.getStringExtra(DobroConstants.LAST),
					DobroThreadActivity.this.thread_id));
		} else
			atask = (ThreadLoader) new ThreadLoader()
		.execute(new ThreadLoaderParams(
				DobroThreadActivity.this.board,
				DobroThreadActivity.this.thread,
				DobroThreadActivity.this.thread_id, false));
	}
	public void addPosts(DobroThread t) {
		if(t == null || t.getPosts() == null || postPositionMap == null || postPositionMap.isEmpty())
			return;
		DobroPost[] posts = t.getPosts();
		for(DobroPost p : posts)
		{
			for(String id : p.getLinks())
			{
				try{
					Integer pos = postPositionMap.get(id);
					if(pos == null)
						continue;
					DobroPostItem i = (DobroPostItem) this.getListAdapter().getItem(pos);
					i.post.getRefs().add(">>"+p.getDisplay_id());
				} catch( Exception e)
				{
					e.printStackTrace();
				}
			}
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(requestCode == DobroConstants.ANSWER_RESULT && resultCode == Activity.RESULT_OK) {
			if(!PreferenceManager.getDefaultSharedPreferences(this).getBoolean("update_after_posting", true))
				return;
			executor.execute(new Runnable(){
				@Override
				public void run() {
					DobroThreadActivity.this.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							try{
								ListView v = getListView();
								ItemAdapter adapter = (ItemAdapter) getListAdapter();
								Object item = v.getItemAtPosition(v.getCount()-1);
								ProgressItem pi = (ProgressItem) item;
								String tag = (String) pi.getTag();
								if (!pi.isInProgress && tag != null && tag.startsWith(DobroConstants.UPDATE_TAG)) {
									pi.isInProgress = true;
									pi.enabled = false;
									pi.text = getString(R.string.loading);
//									adapter.notifyDataSetChanged();
									View iv = getListView().getChildAt(adapter.getCount() - 1 - getListView().getFirstVisiblePosition());
									updateProgressView(iv, pi);
									
									new ThreadLoader().execute(new ThreadLoaderParams(board, thread,
											tag.substring(DobroConstants.UPDATE_TAG.length()), count, false));
								}
							} catch(Exception e) {
								e.printStackTrace();
							}
						}
					});
				}
			});
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
}
