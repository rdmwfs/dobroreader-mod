package org.anonymous.dobrochan.activity;

import greendroid.app.GDActivity;
import greendroid.app.GDExpandableListActivity;
import greendroid.widget.ActionBarItem;
import greendroid.widget.ItemAdapter;
import greendroid.widget.item.Item;
import greendroid.widget.item.ProgressItem;
import greendroid.widget.item.SubtextItem;
import greendroid.widget.item.TextItem;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.anonymous.dobrochan.AbstractMap;
import org.anonymous.dobrochan.DobroApplication;
import org.anonymous.dobrochan.DobroConstants;
import org.anonymous.dobrochan.DobroHelper;
import org.anonymous.dobrochan.DobroNetwork;
import org.anonymous.dobrochan.DobroParser;
import org.anonymous.dobrochan.DobroPostItem;
import org.anonymous.dobrochan.ExpandablePostAdapter;
import org.anonymous.dobrochan.json.DobroBoard;
import org.anonymous.dobrochan.json.DobroPost;
import org.anonymous.dobrochan.json.DobroThread;
import org.anonymous.dobrochan.reader.R;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.widget.ListView;

import com.google.gson.JsonObject;

public class DobroBoardActivityEx extends GDExpandableListActivity implements IPostsList {
	@Override
	public boolean onHandleActionBarItemClick(ActionBarItem item, int position) {
		if(position == -1) {
			Intent i = new Intent(this, DobroTabsList.class);
			i.putExtra(GD_ACTION_BAR_TITLE, "Вкладки");
			i.putExtra(DobroConstants.BOARD, board);
			i.putExtra(DobroConstants.TITLE, getIntent().getStringExtra(GD_ACTION_BAR_TITLE));
			startActivity(i);
			return true;
		}
		return super.onHandleActionBarItemClick(item, position);
	}
	private ThreadLoader atask = null;
	public String board = null;
	public ExpandablePostAdapter<Item,Item> m_adapter = null;

	public Map<String,Integer[]> postPositionMap = new HashMap<String, Integer[]>();

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
			final ExpandablePostAdapter adapter = DobroBoardActivityEx.this.m_adapter;
			ProgressItem pi = (ProgressItem) adapter.getGroup(adapter.getGroupCount() - 1);
			pi.isInProgress = false;
			if (result == null)
			{
				pi.text = DobroBoardActivityEx.this.getString(R.string.board_next_page);
				pi.enabled = true;
				adapter.notifyDataSetChanged();
				return;
			}
			pi.text = getString(R.string.board_page_N, page_s);
			pi.enabled = false;
			SharedPreferences prefs = DobroApplication.getApplicationStatic().getDefaultPrefs();
			String[] hide_rules = prefs.getString("threads2hide", "").split("\n");
			for (DobroThread thread : result.getThreads()) {
				if(thread == null)
					continue;
				/*if (thread != null
						&& thread.getTitle() != null
						&& (Md5Util.md5(thread.getTitle()).equalsIgnoreCase(
								DobroConstants.DCOTT)))
					continue;*/
				if (thread != null && thread.getTitle() != null)
				{
					boolean skip_thread = false;
					for(String rule : hide_rules)
						if(!TextUtils.isEmpty(rule))
						{
							try{
								if(Pattern.compile(rule, Pattern.CASE_INSENSITIVE).matcher(thread.getTitle()).find())
									skip_thread = true;
							} catch (Exception e) {
								 if(TextUtils.equals(rule, thread.getTitle()))
									 skip_thread = true;
							}
							if(skip_thread)
								break;
						}
					if(skip_thread)
					{
						DobroNetwork.getInstance().hideThread(thread.getBoardName(), thread.getDisplay_id());
						continue;
					}
				}
				for (DobroPost post : thread.getPosts()) {
					DobroPostItem postItem = new DobroPostItem(post);
					postItem.setTag(DobroConstants.POST_TAG);
					Integer[] arr = {post.isOp()?adapter.getGroupCount():adapter.getGroupCount()-1,
							post.isOp()?-1:adapter.getChildrenCount(adapter.getGroupCount()-1)};
					postPositionMap.put(post.getDisplay_id(),arr);
					if(post.isOp())
					{
						Entry ent = new AbstractMap.SimpleEntry<Item, List<Item>>(postItem, new LinkedList<Item>());
						adapter.add(ent);

					final int skipped_count = post.getThread().getPostsCount() - 11;
					final int skipped_count_last_d = skipped_count % 10;
					final int skipped_count_prelast_d = (skipped_count % 100) / 10;
					int skipped_string;
					if (skipped_count_prelast_d == 1)
						skipped_string = R.string.board_skipped_ex;
					else {
						switch (skipped_count_last_d) {
							case 1:  skipped_string = R.string.board_skipped_ex_1;
							         break;
							case 2:
							case 3:
							case 4:  skipped_string = R.string.board_skipped_ex_2_3_4;
							         break;
							default: skipped_string = R.string.board_skipped_ex;
							break;
						}
					}

						String s = DobroBoardActivityEx.this
								.getString(skipped_string,
										String.valueOf(skipped_count),
										String.valueOf(thread.getFilesCount()));
						String[] sarr = s.split("\n");
//						if (post.isOp() && thread.getPostsCount() > 11)
						final TextItem gr = new SubtextItem(sarr[0], sarr[1]);
						gr.enabled = true;
						Entry gr_ent = new AbstractMap.SimpleEntry<Item, List<Item>>(gr, new LinkedList<Item>());
						if(skipped_count > 1)
							adapter.add(gr_ent);
					}
					else
						adapter.addChild(adapter.getGroup(adapter.getGroupCount()-1),postItem);
				}
			}
			ProgressItem nextPage = new ProgressItem(
					DobroBoardActivityEx.this.getString(R.string.board_next_page),
					false);
			nextPage.setTag(DobroConstants.NEXT
					+ String.valueOf(Integer.parseInt(page_s) + 1));
			List list = new LinkedList<Item>();
			Entry ent = new AbstractMap.SimpleEntry<Item, List<Item>>(nextPage, list);
			adapter.add(ent);
			adapter.notifyDataSetChanged();
			atask = null;
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
		switch (item.getItemId()) {
		case R.id.update_board:
			m_adapter = new ExpandablePostAdapter<Item, Item>(this, 0, 0, 0, new LinkedList<Entry<Item, List<Item>>>());
			atask = null;
			final ProgressItem load_item = new ProgressItem(
					getString(R.string.loading), true);
			load_item.enabled = false;
			load_item.setTag(DobroConstants.NEXT + "0");
			List list = new LinkedList<Item>();
			Entry ent = new AbstractMap.SimpleEntry<Item, List<Item>>(load_item, list);
			m_adapter.add(ent);
			setListAdapter(m_adapter);
			createThread("0");
			return true;
		case R.id.go_to: {
			Intent i = new Intent(this, DobroGotoActivity.class);
			i.putExtra(GD_ACTION_BAR_TITLE, getString(R.string.go_to));
			startActivity(i);
			return true;
		}
		case R.id.new_thread: {
			Intent intent = new Intent(this, DobroNewPostActivity.class);
			intent.putExtra(DobroConstants.BOARD, board);
			intent.putExtra(DobroConstants.THREAD, "0");
			intent.putExtra(GDActivity.GD_ACTION_BAR_TITLE,
					String.format("Новый тред в >>%s", board));
			startActivity(intent);
			return true;
		}
		}
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
//			DobroNetwork.getInstance().clearPendingUrls();
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
		this.getExpandableListView().setFastScrollEnabled(true);
		this.getExpandableListView().setItemsCanFocus(false);
		this.getExpandableListView().setGroupIndicator(new ColorDrawable(Color.TRANSPARENT));
		m_adapter = new ExpandablePostAdapter<Item, Item>(this, 0, 0, 0, new LinkedList<Entry<Item, List<Item>>>());
		final ProgressItem load_item = new ProgressItem(
				getString(R.string.loading), true);
		load_item.enabled = false;
		Intent i = getIntent();
		String page = "0";
		String data = i.getDataString();
		if (data != null) {
			Pattern p = Pattern.compile("dobrochan\\.(ru|org)/(.*?)/(.*?)\\.xhtml");
			Matcher m = p.matcher(data);
			if(m.find()) {
				board = m.group(2);
				page = m.group(3);
				if(TextUtils.equals(page, "index"))
					page = "0";
			}
		}
		if(board == null)
		{
			if (getIntent().hasExtra(DobroConstants.BOARD)) {
					this.board = getIntent().getStringExtra(DobroConstants.BOARD);
			} else {
					this.board = null;
			}
		}
		load_item.setTag(DobroConstants.NEXT + page);
		List list = new LinkedList<Item>();
		Entry ent = new AbstractMap.SimpleEntry<Item, List<Item>>(load_item, list);
		m_adapter.add(ent);
		setListAdapter(m_adapter);
		createThread(page);
	}

	public void createThread(String page) {
		if (this.board != null && atask == null)
			atask = (ThreadLoader) new ThreadLoader()
					.execute(new ThreadLoaderParams(
							DobroBoardActivityEx.this.board, page));
	}

	@Override
	public void onGroupCollapse(int groupPosition) {
		super.onGroupCollapse(groupPosition);
		ExpandablePostAdapter<Item, Item> adapter = (ExpandablePostAdapter<Item, Item>) getExpandableListAdapter();
		Item item = (Item) adapter.getGroup(groupPosition);
		String tag = (String) item.getTag();
		if (tag != null && tag.startsWith(DobroConstants.NEXT) && item.enabled) {
			((ProgressItem) item).isInProgress = true;
			((ProgressItem) item).enabled = false;
			((ProgressItem) item).text = getString(R.string.loading);
			adapter.notifyDataSetChanged();
			new ThreadLoader().execute(new ThreadLoaderParams(board, tag
					.substring(DobroConstants.NEXT.length())));
		}
	}

	@Override
	public void onGroupExpand(int groupPosition) {
		super.onGroupExpand(groupPosition);
		ExpandablePostAdapter<Item, Item> adapter = (ExpandablePostAdapter<Item, Item>) getExpandableListAdapter();
		Item item = (Item) adapter.getGroup(groupPosition);
		String tag = (String) item.getTag();
		if (tag != null && tag.startsWith(DobroConstants.NEXT) && item.enabled) {
			((ProgressItem) item).isInProgress = true;
			((ProgressItem) item).enabled = false;
			((ProgressItem) item).text = getString(R.string.loading);
			adapter.notifyDataSetChanged();
			new ThreadLoader().execute(new ThreadLoaderParams(board, tag
					.substring(DobroConstants.NEXT.length())));
		}
	}

	@Override
	public void openLinks(List<String> list) {
		List<DobroPostItem> items = new LinkedList<DobroPostItem>();
		for(String ref : list)
		{
			String post = ref.subSequence(2, ref.length()).toString();
			Integer[] pos = postPositionMap.get(post);
			DobroPostItem old;
			try{
				if(pos[1]==-1)
					old = (DobroPostItem) m_adapter.getGroup(pos[0]);
				else
					old = (DobroPostItem) m_adapter.getChild(pos[0], pos[1]);
			} catch (IndexOutOfBoundsException e) {
				old = null;
			}
			if(old == null)
				continue;
			items.add(new DobroPostItem(old.post));
		}
		ItemAdapter ad = new ItemAdapter(this, items.toArray(new Item[0]));
		ListView listView = new ListView(this);
		listView.setAdapter(ad);

		Dialog d = new Dialog(this);//,DobroHelper.getDialogTheme(this));
		d.requestWindowFeature(Window.FEATURE_NO_TITLE);
		d.setCanceledOnTouchOutside(true);
		d.setContentView(listView);
		d.show();
	}

	@Override
	public boolean openLink(CharSequence post) {
		Integer[] pos = postPositionMap.get(post);
		if(pos != null && getExpandableListView() != null)
		{
			List<String> l = new LinkedList<String>();
			l.add(">>"+post.toString());
			openLinks(l);
			return true;
		}
		return false;
	}
}
