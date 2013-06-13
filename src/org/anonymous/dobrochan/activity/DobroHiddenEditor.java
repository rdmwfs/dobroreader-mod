package org.anonymous.dobrochan.activity;

import greendroid.app.GDListActivity;
import greendroid.widget.ActionBarItem;
import greendroid.widget.ItemAdapter;
import greendroid.widget.item.TextItem;

import org.anonymous.dobrochan.DobroConstants;
import org.anonymous.dobrochan.DobroHelper;
import org.anonymous.dobrochan.DobroNetwork;
import org.anonymous.dobrochan.DobroParser;
import org.anonymous.dobrochan.json.DobroSession;
import org.anonymous.dobrochan.json.DobroThread;
import org.anonymous.dobrochan.reader.R;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

import com.google.gson.JsonObject;

public class DobroHiddenEditor extends GDListActivity {
	private ItemAdapter m_adapter;

	@Override
	public boolean onHandleActionBarItemClick(ActionBarItem item, int position) {
		if (position == -1) {
			Intent i = new Intent(this, DobroTabsList.class);
			i.putExtra(GD_ACTION_BAR_TITLE, "Вкладки");
			i.putExtra(DobroConstants.BOARD, "hidden");
			startActivity(i);
			return true;
		}
		return super.onHandleActionBarItemClick(item, position);
	}

	private class ThreadListLoader extends AsyncTask<Void, Void, DobroSession> {
		ProgressDialog dlg;

		@Override
		protected void onPostExecute(DobroSession result) {
			dlg.dismiss();
			if (result == null)
				return;
			for (DobroThread t : result.getThreads()) {
				if (!t.getLevel().equals("hidden"))
					continue;
				TextItem item = new TextItem(String.format(">>%s/%s",
						t.getBoardName(), t.getDisplay_id()));
				String[] tag = { t.getBoardName(), t.getDisplay_id() };
				item.setTag(tag);
				m_adapter.add(item);
			}
			m_adapter.notifyDataSetChanged();
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			dlg = ProgressDialog.show(DobroHiddenEditor.this,
					getString(R.string.loading), "", true, true,
					new OnCancelListener() {
						@Override
						public void onCancel(DialogInterface dialog) {
							ThreadListLoader.this.cancel(true);
							DobroHiddenEditor.this.finish();
						}
					});
		}

		@Override
		protected DobroSession doInBackground(Void... params) {
			JsonObject obj = DobroNetwork.getInstance().getHiddenJson();
			return DobroParser.getInstance().parceHiddenThreads(obj);
		}

	}

	private class ThreadUnhider extends AsyncTask<String[], Void, Void> {
		@Override
		protected Void doInBackground(String[]... params) {
			DobroNetwork.getInstance().unhideThread(params[0][0], params[0][1]);
			return null;
		}
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		TextItem item = (TextItem) m_adapter.getItem(position);
		String[] tag = (String[]) item.getTag();
		new ThreadUnhider().execute(tag);
		m_adapter.remove(item);
		m_adapter.notifyDataSetChanged();
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
		new ThreadListLoader().execute();
	}
}
