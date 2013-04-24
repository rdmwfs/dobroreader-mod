package org.anonymous.dobrochan.activity;

import greendroid.app.GDListActivity;
import greendroid.widget.ActionBarItem;
import greendroid.widget.ItemAdapter;
import greendroid.widget.item.SubtitleItem;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.anonymous.dobrochan.DobroApplication;
import org.anonymous.dobrochan.DobroConstants;
import org.anonymous.dobrochan.DobroHelper;
import org.anonymous.dobrochan.DobroParser;
import org.anonymous.dobrochan.R;
import org.anonymous.dobrochan.json.DobroThread;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;

public class DobroHistoryActivity extends GDListActivity {
	@Override
	public boolean onHandleActionBarItemClick(ActionBarItem item, int position) {
		if(position == -1) {
			Intent i = new Intent(this, DobroTabsList.class);
			i.putExtra(GD_ACTION_BAR_TITLE, "Вкладки");
			i.putExtra(DobroConstants.BOARD, "history");
			startActivity(i);
			return true;
		}
		return super.onHandleActionBarItemClick(item, position);
	}
	@Override
	public boolean onContextItemSelected(MenuItem menu) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) menu.getMenuInfo();
		SubtitleItem item = (SubtitleItem)m_adapter.getItem(info.position);
		String[] params = (String[])item.getTag();
		{
			DobroApplication.getApplicationStatic().getThreads().deleteThread(params[0]+"/"+params[1]);
			DobroApplication.getApplicationStatic().getThreadsInfo().deleteThreadInfo(params[2]);
		}
		if(m_adapter.getCount() == 0)
			finish();
		else
		{
			m_adapter.remove(item);
			m_adapter.notifyDataSetChanged();
		}
		return true;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add("Очистить все").setOnMenuItemClickListener(new OnMenuItemClickListener() {
			
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				new HistoryCleaner().execute();
				return false;
			}
		});
		return true;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		menu.add("Удалить");
	}
	private ItemAdapter m_adapter;
	private HistoryLoader atask;
	
	private class HistoryCleaner extends AsyncTask<Void, Void, Void >
	{
		ProgressDialog dlg;
		@Override
		protected void onPostExecute(Void result) {
			dlg.dismiss();
			finish();
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			dlg = ProgressDialog.show(DobroHistoryActivity.this, getString(R.string.loading), "", true, false);
		}

		@Override
		protected Void doInBackground(Void... params) {
			DobroApplication.getApplicationStatic().getThreads().clearCache();
			DobroApplication.getApplicationStatic().getThreadsInfo().clearCache();
			return null;
		}
		
	}

	private class HistoryLoader extends AsyncTask<Void, Void, List<DobroThread> >
	{
		ProgressDialog dlg;
		@Override
		protected void onPostExecute(List<DobroThread> threads) {
			dlg.dismiss();
			if(threads == null || threads.size() == 0)
				return;
			Collections.sort(threads);
			for(DobroThread t : threads)
			{
				String subtitle = String.format(">>%s/%s ", t.getBoardName(), t.getDisplay_id());
				String title = t.getTitle();
				if(title == null || title.length()==0)
					title = getString(R.string.untitled);
				SubtitleItem item = new SubtitleItem(title,
						subtitle);
				String[] tag = {t.getBoardName(),t.getDisplay_id(), t.getThread_id()};
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
			dlg = ProgressDialog.show(DobroHistoryActivity.this, getString(R.string.loading), "", true, true, new OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
					HistoryLoader.this.cancel(true);
					DobroHistoryActivity.this.finish();
				}
			});
		}

		@Override
		protected List<DobroThread> doInBackground(Void... params) {
			List<String> dumps = DobroApplication.getApplicationStatic().getThreadsInfo().getAllThreads();
			List<DobroThread> threads = new LinkedList<DobroThread>();
			for(String s : dumps)
				threads.add(DobroParser.getInstance().parceThread(s));
			return threads;
		}
		
	}
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		SubtitleItem item = (SubtitleItem)m_adapter.getItem(position);
		String[] tag = (String[])item.getTag();
		Intent i = new Intent(this,DobroThreadActivity.class);
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
		this.getListView().setFastScrollEnabled(true);
		this.getListView().setItemsCanFocus(false);
		m_adapter = new ItemAdapter(this);
		setListAdapter(m_adapter);
		registerForContextMenu(this.getListView());
		if(atask == null)
			atask = (HistoryLoader)new HistoryLoader().execute();
	}
	@Override
	protected void onDestroy() {
		try {
			if(atask != null)
				atask.cancel(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
		super.onDestroy();
	}
}
