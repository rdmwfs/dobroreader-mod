package org.anonymous.dobrochan.activity;

import greendroid.app.GDListActivity;
import greendroid.widget.ActionBarItem;
import greendroid.widget.ItemAdapter;
import greendroid.widget.LoaderActionBarItem;
import greendroid.widget.item.SubtitleItem;

import java.util.Arrays;

import org.anonymous.dobrochan.DobroApplication;
import org.anonymous.dobrochan.DobroConstants;
import org.anonymous.dobrochan.DobroHelper;
import org.anonymous.dobrochan.DobroNetwork;
import org.anonymous.dobrochan.DobroParser;
import org.anonymous.dobrochan.json.DobroSession;
import org.anonymous.dobrochan.json.DobroThread;
import org.anonymous.dobrochan.reader.R;

import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.Toast;

import com.google.gson.JsonObject;

public class DobroStarredEditor extends GDListActivity {
	@Override
	public boolean onHandleActionBarItemClick(ActionBarItem item, int position) {
		if (position == -1) {
			Intent i = new Intent(this, DobroTabsList.class);
			i.putExtra(GD_ACTION_BAR_TITLE, "Вкладки");
			i.putExtra(DobroConstants.BOARD, "starred");
			startActivity(i);
			return true;
		}
		switch (item.getItemId()) {
		case AB_REFRESH:
			m_adapter.clear();
			m_adapter.notifyDataSetChanged();
			if (atask == null)
				atask = (ThreadListLoader) new ThreadListLoader().execute();
			NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			mNotificationManager.cancel(DobroConstants.NOTIFY_ID);

			try {
				Intent dashClockIntent = new Intent(
						"org.anonymous.dobrochan.favs");
				startService(dashClockIntent);
			} catch (SecurityException e) {
			}
			break;
		default:
			// pass
		}
		return super.onHandleActionBarItemClick(item, position);
	}

	private LoaderActionBarItem refresh_item;
	private ItemAdapter m_adapter;
	private ThreadListLoader atask;
	private final static int AB_REFRESH = 0;

	private class ThreadListLoader extends
			AsyncTask<String, Void, DobroThread[]> {
		ProgressDialog dlg;

		@Override
		protected void onPostExecute(DobroThread[] threads) {
			dlg.dismiss();
			refresh_item.setLoading(false);
			if (threads == null || threads.length == 0) {
				atask = null;
				return;
			}
			Arrays.sort(threads);
			for (DobroThread t : threads) {
				if (!t.getLevel().equals("bookmarked"))
					continue;
				String subtitle = String.format(">>%s/%s ", t.getBoardName(),
						t.getDisplay_id());
				String title = t.getTitle();
				if (title == null || title.length() == 0)
					title = getString(R.string.untitled);
				if (t.getUnread() > 0) // FIXME after wakaba update
				{
					// subtitle += getString(R.string.unreaded_posts);
					subtitle += getString(R.string.faw_thread_edited,
							t.getLastModified());
					title = "*" + title;
				}
				SubtitleItem item = new SubtitleItem(title, subtitle);
				String[] tag = { t.getBoardName(), t.getDisplay_id() };
				item.setTag(tag);
				item.enabled = true;
				m_adapter.add(item);
			}
			m_adapter.notifyDataSetChanged();
			atask = null;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			dlg = ProgressDialog.show(DobroStarredEditor.this,
					getString(R.string.loading), "", true, true,
					new OnCancelListener() {
						@Override
						public void onCancel(DialogInterface dialog) {
							ThreadListLoader.this.cancel(true);
							DobroStarredEditor.this.finish();
						}
					});
		}

		@Override
		protected DobroThread[] doInBackground(String... params) {
			DobroThread[] result = null;
			if (params != null && params.length > 0 && params[0] != "")
				result = DobroApplication.getApplicationStatic().getParser()
						.parceThreads(params[0]);
			if (result != null)
				return result;
			JsonObject obj = DobroNetwork.getInstance().getFavsJson();
			DobroSession sess = DobroParser.getInstance().parceStarredThreads(
					obj);
			if (sess != null)
				return sess.getThreads();
			else
				return null;
		}

	}

	private class ThreadUnstarer extends AsyncTask<String[], Void, Void> {
		@Override
		protected Void doInBackground(String[]... params) {
			DobroNetwork.getInstance().unStarThread(params[0][0], params[0][1]);
			return null;
		}
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		SubtitleItem item = (SubtitleItem) m_adapter.getItem(position);
		if (item.text.startsWith("*"))
			item.text = item.text.substring(1);
		m_adapter.notifyDataSetChanged();
		String[] tag = (String[]) item.getTag();
		Intent i = new Intent(this, DobroThreadActivity.class);
		i.putExtra(DobroConstants.BOARD, tag[0]);
		i.putExtra(DobroConstants.THREAD, tag[1]);
		i.putExtra(GD_ACTION_BAR_TITLE, item.text);
		startActivity(i);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		DobroHelper.updateCurrentTheme(this);
		DobroHelper.setOrientation(this);
		super.onCreate(savedInstanceState);
		refresh_item = (LoaderActionBarItem) addActionBarItem(
				ActionBarItem.Type.Refresh, AB_REFRESH);
		this.getListView().setFastScrollEnabled(true);
		this.getListView().setItemsCanFocus(false);
		m_adapter = new ItemAdapter(this);
		setListAdapter(m_adapter);
		registerForContextMenu(this.getListView());

		if (Intent.ACTION_CREATE_SHORTCUT.equals(getIntent().getAction())) {
			Intent shortcutIntent = new Intent(this, DobroStarredEditor.class);
			shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			shortcutIntent.putExtra(GD_ACTION_BAR_TITLE,
					getString(R.string.starred));

			Intent addIntent = new Intent();
			addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
			addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME,
					getString(R.string.starred));
			addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
					Intent.ShortcutIconResource.fromContext(this,
							R.drawable.icon));

			addIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
			sendBroadcast(addIntent);
			finish();
			return;
		}

		if (atask == null)
			atask = (ThreadListLoader) new ThreadListLoader()
					.execute(getIntent().hasExtra(DobroConstants.FAVS_DUMP) ? getIntent()
							.getStringExtra(DobroConstants.FAVS_DUMP) : "");
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.cancel(DobroConstants.NOTIFY_ID);

		try {
			Intent dashClockIntent = new Intent("org.anonymous.dobrochan.favs");
			startService(dashClockIntent);
		} catch (SecurityException e) {
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
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.star_context_menu, menu);
	}

	@Override
	public boolean onContextItemSelected(MenuItem menu) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) menu
				.getMenuInfo();
		switch (menu.getItemId()) {
		case R.id.delete_star: {
			SubtitleItem item = (SubtitleItem) m_adapter.getItem(info.position);
			String[] params = (String[]) item.getTag();
			new ThreadUnstarer().execute(params);
			m_adapter.remove(item);
			m_adapter.notifyDataSetChanged();
			return true;
		}
		case R.id.shortcut: {
			SubtitleItem item = (SubtitleItem) m_adapter.getItem(info.position);
			String[] params = (String[]) item.getTag();
			String name = item.text;
			if (name.startsWith("*"))
				name = name.substring(1);
			Intent shortcutIntent = new Intent(this, DobroThreadActivity.class);
			shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			shortcutIntent.putExtra(GD_ACTION_BAR_TITLE, item.text);
			shortcutIntent.putExtra(DobroConstants.BOARD, params[0]);
			shortcutIntent.putExtra(DobroConstants.THREAD, params[1]);

			Intent addIntent = new Intent();
			addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
			addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, item.text);
			addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
					Intent.ShortcutIconResource.fromContext(this,
							R.drawable.icon));

			addIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
			sendBroadcast(addIntent);

			Toast.makeText(this, "Ярлык создан", Toast.LENGTH_SHORT).show();
			return true;
		}
		default:
			return super.onContextItemSelected(menu);
		}
	}
}
