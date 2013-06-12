package org.anonymous.dobrochan.activity;

import greendroid.app.GDListActivity;
import greendroid.widget.ActionBarItem;
import greendroid.widget.ItemAdapter;
import greendroid.widget.item.SubtitleItem;

import org.anonymous.dobrochan.DobroApplication;
import org.anonymous.dobrochan.DobroConstants;
import org.anonymous.dobrochan.DobroHelper;
import org.anonymous.dobrochan.DobroTabsHolder;
import org.anonymous.dobrochan.Reversed;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;

public class DobroTabsList extends GDListActivity {
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add("Очистить все").setOnMenuItemClickListener(
				new OnMenuItemClickListener() {

					@Override
					public boolean onMenuItemClick(MenuItem item) {
						DobroTabsHolder.getInstance().clear();
						finish();
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

	@Override
	public boolean onContextItemSelected(MenuItem menu) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) menu
				.getMenuInfo();
		SubtitleItem item = (SubtitleItem) m_adapter.getItem(info.position);
		if (item.getTag() == null)
			return true;
		String[] params = (String[]) item.getTag();
		DobroTabsHolder.getInstance().remove(params);
		if (m_adapter.getCount() == 0)
			finish();
		else {
			m_adapter.remove(item);
			m_adapter.notifyDataSetChanged();
		}
		return true;
	}

	@Override
	public void onBackPressed() {
		// DobroTabsHolder.getInstance().removeLast();
		super.onBackPressed();
	}

	@Override
	public boolean onHandleActionBarItemClick(ActionBarItem item, int position) {
		if (position == -1) {
			Intent homeIntent = new Intent(this, DobroHomeActivity.class);
			homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(homeIntent);
			return true;
		}
		return super.onHandleActionBarItemClick(item, position);
	}

	@Override
	protected void onResume() {
		super.onResume();
		m_adapter.clear();
		for (String[] s : Reversed.reversed(DobroTabsHolder.getInstance()
				.getTabs())) {
			String board = s[0];
			String thread = s[1];
			String title = s[3];
			String post = s[4];
			SubtitleItem item = null;
			if (board != null && post != null)
				item = new SubtitleItem(title, String.format(">>%s/%s", board,
						post));
			else if (board != null && thread != null)
				item = new SubtitleItem(title, String.format(">>%s/%s", board,
						thread));
			else if (board != null) {
				if (TextUtils.equals(board, "history"))
					item = new SubtitleItem("История", "");
				else if (TextUtils.equals(board, "starred"))
					item = new SubtitleItem("Избранное", "");
				else if (TextUtils.equals(board, "hidden"))
					item = new SubtitleItem("Скрытые треды", "");
				else if (TextUtils.equals(board, "home"))
					item = new SubtitleItem("Домашняя страница", "");
				else
					item = new SubtitleItem(title, String.format(">>%s", board));
			}
			if (item != null) {
				item.setTag(s);
				m_adapter.add(item);
			}
		}
		{
			SubtitleItem item = new SubtitleItem("Новая вкладка", "> > > >");
			item.setTag(null);
			m_adapter.add(item);
		}
		m_adapter.notifyDataSetChanged();
	}

	private ItemAdapter m_adapter;

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		int flags = (Build.VERSION.SDK_INT >= 11) ? Intent.FLAG_ACTIVITY_NEW_TASK
				| Intent.FLAG_ACTIVITY_CLEAR_TASK
				: Intent.FLAG_ACTIVITY_NEW_TASK
						| Intent.FLAG_ACTIVITY_CLEAR_TOP;
		super.onListItemClick(l, v, position, id);
		SubtitleItem item = (SubtitleItem) m_adapter.getItem(position);
		String[] tag = (String[]) item.getTag();
		if (tag == null) {
			Intent i = new Intent(this, DobroHomeActivity.class);
			i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
					| Intent.FLAG_ACTIVITY_CLEAR_TASK); // TODO: API < 11 ?
			startActivity(i);
		} else {
			String board = tag[0];
			String thread = tag[1];
			String scroll_to = tag[2];
			// String title = tag[3];
			String post = tag[4];
			if (board != null && post != null) {
				Intent i = new Intent(this, DobroPostActivity.class);
				i.putExtra(DobroConstants.BOARD, board);
				i.putExtra(DobroConstants.POST, post);
				i.putExtra(GD_ACTION_BAR_TITLE,
						String.format(">>%s/%s", board, post));
				i.setFlags(flags);
				startActivity(i);
			} else if (board != null && thread != null) {
				Intent i = new Intent(this, DobroThreadActivity.class);
				i.putExtra(DobroConstants.BOARD, board);
				i.putExtra(DobroConstants.THREAD, thread);
				i.putExtra(DobroConstants.SCHROLL_TO, scroll_to);
				i.putExtra(DobroConstants.DISABLE_UPDATE, true);
				i.putExtra(GD_ACTION_BAR_TITLE, item.text);
				i.setFlags(flags);
				startActivity(i);
			} else if (board != null) {
				if (TextUtils.equals(board, "history")) {
					Intent i = new Intent(this, DobroHistoryActivity.class);
					i.putExtra(GD_ACTION_BAR_TITLE, item.text);
					i.setFlags(flags);
					startActivity(i);
				} else if (TextUtils.equals(board, "starred")) {
					Intent i = new Intent(this, DobroStarredEditor.class);
					i.putExtra(GD_ACTION_BAR_TITLE, item.text);
					i.setFlags(flags);
					startActivity(i);
				} else if (TextUtils.equals(board, "hidden")) {
					Intent i = new Intent(this, DobroHiddenEditor.class);
					i.putExtra(GD_ACTION_BAR_TITLE, item.text);
					i.setFlags(flags);
					startActivity(i);
				} else if (TextUtils.equals(board, "home")) {
					Intent i = new Intent(this, DobroHomeActivity.class);
					i.putExtra(GD_ACTION_BAR_TITLE, item.text);
					i.setFlags(flags);
					startActivity(i);
				} else {
					SharedPreferences prefs = DobroApplication
							.getApplicationStatic().getDefaultPrefs();
					boolean ex_board = prefs.getBoolean("expandable_board",
							true);
					Intent intent = new Intent(this,
							(ex_board ? DobroBoardActivityEx.class
									: DobroBoardActivity.class));
					intent.putExtra(DobroConstants.BOARD, board);
					intent.putExtra(GD_ACTION_BAR_TITLE, item.text);
					intent.setFlags(flags);
					startActivity(intent);
				}
			}
		}
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
		Intent i = getIntent();
		if (i != null) {
			String board = i.getStringExtra(DobroConstants.BOARD);
			String thread = i.getStringExtra(DobroConstants.THREAD);
			String post = i.getStringExtra(DobroConstants.POST);
			String scroll = i.getStringExtra(DobroConstants.SCHROLL_TO);
			String text = i.getStringExtra(DobroConstants.TITLE);
			DobroTabsHolder.getInstance().addTab(board, thread, scroll, text,
					post);
		}
	}
}
