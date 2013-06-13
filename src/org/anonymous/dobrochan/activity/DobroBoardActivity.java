package org.anonymous.dobrochan.activity;

import greendroid.app.ActionBarActivity;
import greendroid.widget.ActionBarItem;
import greendroid.widget.ItemAdapter;
import greendroid.widget.item.Item;
import greendroid.widget.item.ProgressItem;
import greendroid.widget.item.SeparatorItem;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.anonymous.dobrochan.DobroApplication;
import org.anonymous.dobrochan.DobroConstants;
import org.anonymous.dobrochan.DobroHelper;
import org.anonymous.dobrochan.DobroNetwork;
import org.anonymous.dobrochan.DobroParser;
import org.anonymous.dobrochan.DobroPostItem;
import org.anonymous.dobrochan.json.DobroBoard;
import org.anonymous.dobrochan.json.DobroPost;
import org.anonymous.dobrochan.json.DobroThread;
import org.anonymous.dobrochan.reader.R;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import com.google.gson.JsonObject;

public class DobroBoardActivity extends DobroPostsList {
	@Override
	public boolean onHandleActionBarItemClick(ActionBarItem item, int position) {
		if (position == -1) {
			Intent i = new Intent(this, DobroTabsList.class);
			i.putExtra(GD_ACTION_BAR_TITLE, "Вкладки");
			i.putExtra(DobroConstants.BOARD, board);
			i.putExtra(DobroConstants.TITLE,
					getIntent().getStringExtra(GD_ACTION_BAR_TITLE));
			startActivity(i);
			return true;
		}
		return super.onHandleActionBarItemClick(item, position);
	}

	private ThreadLoader atask = null;
	public String board = null;
	public ItemAdapter m_adapter = null;

	private class ThreadLoaderParams {
		public String board;
		public String page;

		ThreadLoaderParams(String b, String p) {
			board = b;
			page = p;
		}
	}

	private class ThreadLoader extends
			AsyncTask<ThreadLoaderParams, Integer, DobroBoard> {
		String page_s;

		@Override
		protected DobroBoard doInBackground(ThreadLoaderParams... params) {
			page_s = String.valueOf(params[0].page);
			JsonObject json = DobroNetwork.getInstance().getBoardJson(
					params[0].board, params[0].page);
			return DobroParser.getInstance().parceBoard(json);
		}

		@Override
		protected void onPostExecute(DobroBoard result) {
			super.onPostExecute(result);
			final ItemAdapter adapter = DobroBoardActivity.this.m_adapter;
			ProgressItem pi = (ProgressItem) adapter
					.getItem(adapter.getCount() - 1);
			pi.isInProgress = false;
			if (result == null) {
				pi.text = DobroBoardActivity.this.getString(R.string.next_page);
				pi.enabled = true;
				adapter.notifyDataSetChanged();
				return;
			}
			pi.text = getString(R.string.page, page_s);
			pi.enabled = false;
			SharedPreferences prefs = DobroApplication.getApplicationStatic()
					.getDefaultPrefs();
			String[] hide_rules = prefs.getString("threads2hide", "").split(
					"\n");
			for (DobroThread thread : result.getThreads()) {
				if (thread == null)
					continue;
				/*
				 * if (thread != null && thread.getTitle() != null &&
				 * (Md5Util.md5(thread.getTitle()).equalsIgnoreCase(
				 * DobroConstants.DCOTT))) continue;
				 */
				if (thread != null && thread.getTitle() != null) {
					boolean skip_thread = false;
					for (String rule : hide_rules)
						if (!TextUtils.isEmpty(rule)) {
							try {
								if (Pattern
										.compile(rule, Pattern.CASE_INSENSITIVE)
										.matcher(thread.getTitle()).find())
									skip_thread = true;
							} catch (Exception e) {
								if (TextUtils.equals(rule, thread.getTitle()))
									skip_thread = true;
							}
							if (skip_thread)
								break;
						}
					if (skip_thread) {
						DobroNetwork.getInstance().hideThread(
								thread.getBoardName(), thread.getDisplay_id());
						continue;
					}
				}
				for (DobroPost post : thread.getPosts()) {
					DobroPostItem postItem = new DobroPostItem(post);
					postItem.setTag(DobroConstants.POST_TAG);
					postPositionMap.put(post.getDisplay_id(),
							adapter.getCount());
					adapter.add(postItem);

					final int skipped_count = post.getThread().getPostsCount() - 11;
					final int skipped_count_last_d = skipped_count % 10;
					final int skipped_count_prelast_d = (skipped_count % 100) / 10;
					int skipped_string;
					if (skipped_count_prelast_d == 1)
						skipped_string = R.string.skipped;
					else {
						switch (skipped_count_last_d) {
						case 1:
							skipped_string = R.string.skipped_1;
							break;
						case 2:
						case 3:
						case 4:
							skipped_string = R.string.skipped_2_3_4;
							break;
						default:
							skipped_string = R.string.skipped;
							break;
						}
					}

					if (post.isOp() && thread.getPostsCount() > 11)
						adapter.add(new SeparatorItem(DobroBoardActivity.this
								.getString(skipped_string,
										String.valueOf(skipped_count),
										String.valueOf(thread.getFilesCount()))));
				}
			}
			ProgressItem nextPage = new ProgressItem(
					DobroBoardActivity.this.getString(R.string.next_page),
					false);
			nextPage.setTag(DobroConstants.NEXT
					+ String.valueOf(Integer.parseInt(page_s) + 1));
			adapter.add(nextPage);
			adapter.notifyDataSetChanged();
			atask = null;
		}

	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		ItemAdapter adapter = (ItemAdapter) getListAdapter();
		Item item = (Item) adapter.getItem(position);
		String tag = (String) item.getTag();
		if (tag != null && tag.startsWith(DobroConstants.NEXT)) {
			((ProgressItem) item).isInProgress = true;
			((ProgressItem) item).enabled = false;
			((ProgressItem) item).text = getString(R.string.loading);
			adapter.notifyDataSetChanged();
			new ThreadLoader().execute(new ThreadLoaderParams(board, tag
					.substring(DobroConstants.NEXT.length())));
		}
		if (tag != null && tag.equalsIgnoreCase(DobroConstants.POST_TAG)) {
			/*
			 * Intent intent = new Intent(DobroDeskActivity.this,
			 * DobroThreadActivity.class); intent.putExtra(DobroConstants.BOARD,
			 * ((DobroPostItem)item).post.getBoard());
			 * intent.putExtra(DobroConstants.THREAD,
			 * ((DobroPostItem)item).post.getThread());
			 * intent.putExtra(GDActivity.GD_ACTION_BAR_TITLE,
			 * ((DobroPostItem)item).post.getMessage()); startActivity(intent);
			 */
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.board_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int tempid = item.getItemId();
		if (tempid == R.id.update_board) {
			m_adapter = new ItemAdapter(this);
			atask = null;
			final ProgressItem load_item = new ProgressItem(
					getString(R.string.loading), true);
			load_item.enabled = false;
			load_item.setTag(DobroConstants.NEXT + "0");
			m_adapter.add(load_item);
			setListAdapter(m_adapter);
			createThread("0");
			return true;
		} else if (tempid == R.id.go_to) {
			Intent i = new Intent(this, DobroGotoActivity.class);
			i.putExtra(GD_ACTION_BAR_TITLE, getString(R.string.go_to));
			startActivity(i);
			return true;
		} else if (tempid == R.id.new_thread) {
			Intent intent = new Intent(this, DobroNewPostActivity.class);
			intent.putExtra(DobroConstants.BOARD, board);
			intent.putExtra(DobroConstants.THREAD, "0");
			intent.putExtra(ActionBarActivity.GD_ACTION_BAR_TITLE,
					String.format("Новый тред в >>%s", board));
			startActivity(intent);
			return true;
		}

		else
			return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onDestroy() {
		try {
			// DobroNetwork.getInstance().clearPendingUrls();
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
		SharedPreferences prefs = DobroApplication.getApplicationStatic()
				.getDefaultPrefs();
		boolean ex_board = prefs.getBoolean("expandable_board", true);
		if (ex_board) {
			Intent ex_intent = getIntent();
			ex_intent.setClass(this, DobroBoardActivityEx.class);
			ex_intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION
					| Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(ex_intent);
			finish();
			return;
		}
		this.getListView().setFastScrollEnabled(true);
		this.getListView().setItemsCanFocus(false);
		m_adapter = new ItemAdapter(this);
		final ProgressItem load_item = new ProgressItem(
				getString(R.string.loading), true);
		load_item.enabled = false;
		Intent i = getIntent();
		String page = "0";
		String data = i.getDataString();
		if (data != null) {
			Pattern p = Pattern
					.compile("dobrochan\\.(ru|org)/(.*?)/(.*?)\\.xhtml");
			Matcher m = p.matcher(data);
			if (m.find()) {
				board = m.group(2);
				page = m.group(3);
				if (TextUtils.equals(page, "index"))
					page = "0";
			}
		}
		if (board == null) {
			if (getIntent().hasExtra(DobroConstants.BOARD)) {
				this.board = getIntent().getStringExtra(DobroConstants.BOARD);
			} else {
				this.board = null;
			}
		}
		load_item.setTag(DobroConstants.NEXT + page);
		m_adapter.add(load_item);
		setListAdapter(m_adapter);
		createThread(page);
	}

	public void createThread(String page) {
		if (this.board != null && atask == null)
			atask = (ThreadLoader) new ThreadLoader()
					.execute(new ThreadLoaderParams(
							DobroBoardActivity.this.board, page));
	}
}
