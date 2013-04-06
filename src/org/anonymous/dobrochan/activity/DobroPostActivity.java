package org.anonymous.dobrochan.activity;

import greendroid.app.GDActivity;
import greendroid.widget.ActionBarItem;

import org.anonymous.dobrochan.DobroApplication;
import org.anonymous.dobrochan.DobroConstants;
import org.anonymous.dobrochan.DobroHelper;
import org.anonymous.dobrochan.DobroNetwork;
import org.anonymous.dobrochan.DobroParser;
import org.anonymous.dobrochan.DobroPostItem;
import org.anonymous.dobrochan.R;
import org.anonymous.dobrochan.json.DobroPost;
import org.anonymous.dobrochan.json.DobroThread;
import org.anonymous.dobrochan.widget.DobroPostItemView;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.google.gson.JsonObject;

public class DobroPostActivity extends GDActivity {
	private ThreadLoader atask = null;
	private String m_board;
	private String m_post;
	
	@Override
	public boolean onHandleActionBarItemClick(ActionBarItem item, int position) {
		if(position == -1) {
			Intent i = new Intent(this, DobroTabsList.class);
			i.putExtra(GD_ACTION_BAR_TITLE, "Вкладки");
			i.putExtra(DobroConstants.BOARD, m_board);
			i.putExtra(DobroConstants.POST, m_post);
			i.putExtra(DobroConstants.TITLE, "Сообщение");
			startActivity(i);
			return true;
		}
		return super.onHandleActionBarItemClick(item, position);
	}

	private class ThreadLoaderParams{
		public String board;
		public String post;
		ThreadLoaderParams(String b, String p)
		{
			board = b;
			post = p;
		}
	}

	private class ThreadLoader extends AsyncTask<ThreadLoaderParams, Integer, DobroPost>{
		ProgressDialog dlg;
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			dlg = ProgressDialog.show(DobroPostActivity.this, getString(R.string.loading), "", true, true, new OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
					ThreadLoader.this.cancel(true);
					DobroPostActivity.this.finish();
				}
			});
		}
		@Override
		protected DobroPost doInBackground(ThreadLoaderParams... params) {
				JsonObject json = DobroNetwork.getInstance().getPostJson(params[0].board, params[0].post);
				if(json == null)
					return null;
				DobroPost p = DobroParser.getInstance().parcePost(json, m_board);
				if(p == null)
					return null;
				if(p.getThread_id() != null && p.getThreadDisplay_id() == null)
				{
					String dump = DobroApplication.getApplicationStatic().getThreadsInfo().getThreadInfo(p.getThread_id());
					DobroThread t = null;
					if(dump != null && !TextUtils.isEmpty(dump))
						t = DobroApplication.getApplicationStatic().getParser().parceThread(dump);
					if(t == null)
						t = DobroNetwork.getInstance().getThreadInfoJson(p.getThread_id());
					p.setThread(t);
				}
				return p;
		}

		@Override
		protected void onPostExecute(DobroPost result) {
			super.onPostExecute(result);
			if(result == null)
				return;
			DobroPostItem item = new DobroPostItem(result);
			((DobroPostItemView)findViewById(R.id.postitemview_include)).setObject(item);
			dlg.dismiss();
			atask = null;
		}
		
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.post_menu, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.go_to: {
			Intent i = new Intent(this, DobroGotoActivity.class);
			i.putExtra(GD_ACTION_BAR_TITLE, getString(R.string.go_to));
			startActivity(i);
			return true;
		}
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		DobroHelper.updateCurrentTheme(this);
		DobroHelper.setOrientation(this);
		super.onCreate(savedInstanceState);
		setActionBarContentView(R.layout.single_post);
		((DobroPostItemView)findViewById(R.id.postitemview_include)).prepareItemView();
		this.m_board = getIntent().getStringExtra(DobroConstants.BOARD);
		this.m_post = getIntent().getStringExtra(DobroConstants.POST);
		if(atask == null)
			atask = (ThreadLoader)new ThreadLoader().execute(new ThreadLoaderParams(m_board, m_post));
	}

	@Override
	protected void onDestroy() {
		try {
			if(atask!=null)
				atask.cancel(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
		super.onDestroy();
	}
}
