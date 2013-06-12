package org.anonymous.dobrochan.activity;

import greendroid.app.ActionBarActivity;
import greendroid.app.GDListActivity;
import greendroid.widget.ActionBarItem;
import greendroid.widget.ItemAdapter;
import greendroid.widget.item.SeparatorItem;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import org.anonymous.dobrochan.ApiWrapper;
import org.anonymous.dobrochan.DobroApplication;
import org.anonymous.dobrochan.DobroConstants;
import org.anonymous.dobrochan.DobroHelper;
import org.anonymous.dobrochan.DobroHomeItem;
import org.anonymous.dobrochan.DobroNetwork;
import org.anonymous.dobrochan.reader.R;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class DobroHomeActivity extends GDListActivity {
	private class PostsDiffGetter extends
			AsyncTask<Void, Void, Map<String, Integer>> {
		@Override
		protected void onPostExecute(Map<String, Integer> result) {
			if (result == null)
				return;
			for (int i = 0; i < adapter.getCount(); i++) {
				try {
					DobroHomeItem item = (DobroHomeItem) adapter.getItem(i);
					StringPair p = (StringPair) item.getTag();
					if (result.containsKey(p.board))
						item.text = "/" + p.board + "/\n"
								+ String.valueOf(result.get(p.board));
					else
						item.text = "/" + p.board + "/\n?";
				} catch (ClassCastException e) {
					continue;
				}
			}
			adapter.notifyDataSetChanged();
		}

		@Override
		protected Map<String, Integer> doInBackground(Void... params) {
			return DobroNetwork.getInstance().getPostsDiff();
		}
	}

	private class OldDataCleaner extends AsyncTask<Void, Integer, Void> {
		ProgressDialog dlg;
		boolean canceled = false;
		List<File[]> subFiles = new LinkedList<File[]>();

		@Override
		protected void onPostExecute(Void result) {
			dlg.dismiss();
		}

		@Override
		protected void onPreExecute() {
			String threads_cache_dir = String.format(
					DobroConstants.THREADS_CACHE,
					ApiWrapper.getExternalCacheDir(DobroHomeActivity.this));
			String threads_info_cache_dir = String.format(
					DobroConstants.THREADS_INFO_CACHE,
					ApiWrapper.getExternalCacheDir(DobroHomeActivity.this));
			File dir = ApiWrapper.getExternalCacheDir(DobroHomeActivity.this);
			subFiles.add(dir.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String filename) {
					if (filename.endsWith("sqlite")
							|| filename.endsWith("sqlite-shm")
							|| filename.endsWith("sqlite-wal"))
						return false;
					return true;
				}
			}));
			dir = new File(threads_cache_dir);
			if (dir.isDirectory())
				subFiles.add(dir.listFiles());
			dir = new File(threads_info_cache_dir);
			if (dir.isDirectory())
				subFiles.add(dir.listFiles());
			dlg = new ProgressDialog(DobroHomeActivity.this);
			dlg.setTitle(R.string.cleaning);
			dlg.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			int max = 0;
			for (File[] files : subFiles)
				max += files.length;
			dlg.setMax(max);
			dlg.setOnCancelListener(new OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					canceled = true;
				}
			});
			dlg.show();
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			dlg.setProgress(values[0]);
		}

		@Override
		protected Void doInBackground(Void... params) {
			int progress = 0;
			for (File[] subSubFiles : subFiles)
				for (File file : subSubFiles) {
					if (canceled)
						break;
					if (file.isFile())
						file.delete();
					publishProgress(progress++);
				}
			String threads_cache_dir = String.format(
					DobroConstants.THREADS_CACHE,
					Environment.getExternalStorageDirectory());
			String threads_info_cache_dir = String.format(
					DobroConstants.THREADS_INFO_CACHE,
					Environment.getExternalStorageDirectory());
			File dir = new File(threads_cache_dir);
			if (dir.isDirectory())
				dir.delete();
			dir = new File(threads_info_cache_dir);
			if (dir.isDirectory())
				dir.delete();
			return null;
		}
	}

	private class BannersLoader extends AsyncTask<String, Integer, Boolean> {
		ProgressDialog dlg;
		HttpURLConnection conn;
		boolean terminate = false;

		@Override
		protected void onPostExecute(Boolean result) {
			try {
				dlg.dismiss();
				if (result) {
					Intent intent = getIntent();
					intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					startActivity(intent);
				}
			} catch (Exception e) {

			}
			super.onPostExecute(result);
		}

		@Override
		protected void onPreExecute() {
			dlg = new ProgressDialog(DobroHomeActivity.this);
			dlg.setTitle("Loading banners...");
			dlg.setMessage("Wait, please");
			dlg.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			dlg.setCancelable(true);
			dlg.setMax(0);
			dlg.setOnCancelListener(new OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					terminate = true;
					dlg.dismiss();
				}
			});
			dlg.show();
			super.onPreExecute();
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			dlg.setProgress(values[0]);
			super.onProgressUpdate(values);
		}

		@Override
		protected Boolean doInBackground(String... params) {
			URL url;
			try {
				url = new URL(params[0]);
				conn = (HttpURLConnection) url.openConnection();
				conn.setDoInput(true);
				conn.setConnectTimeout(10000); // timeout 10 secs
				conn.connect();
				InputStream input = conn.getInputStream();
				dlg.setMax(conn.getContentLength());
				String dirname = String.format(DobroConstants.BANNERS_DIR,
						Environment.getExternalStorageDirectory());
				String fname = dirname
						+ url.getFile().substring(
								url.getFile().lastIndexOf('/') + 1);
				FileOutputStream fOut = new FileOutputStream(fname);
				{
					int byteCount = 0;
					byte[] buffer = new byte[4096];
					int bytesRead = -1;
					while ((bytesRead = input.read(buffer)) != -1 && !terminate) {
						fOut.write(buffer, 0, bytesRead);
						byteCount += bytesRead;
						publishProgress(byteCount);
					}
				}
				fOut.flush();
				fOut.close();
				conn.disconnect();
				dlg.setProgress(0);
				dlg.setMax(0);
				if (terminate) {
					new File(fname).delete();
					return false;
				}
				try {
					String _zipFile = fname;
					String _location = dirname;
					FileInputStream fin = new FileInputStream(_zipFile);
					ZipInputStream zin = new ZipInputStream(fin);
					ZipFile zipfile = new ZipFile(_zipFile);
					Enumeration<? extends ZipEntry> zipEntries = zipfile
							.entries();
					int count = 0;
					while (zipEntries.hasMoreElements()) {
						zipEntries.nextElement();
						count++;
					}
					dlg.setMax(count);
					count = 0;
					ZipEntry ze = null;
					while ((ze = zin.getNextEntry()) != null) {
						if (ze.isDirectory()) {
							if (!new File(_location + ze.getName())
									.isDirectory()) {
								new File(_location + ze.getName()).mkdirs();
							}
						} else {
							FileOutputStream fout = new FileOutputStream(
									_location + ze.getName());
							byte[] buffer = new byte[4096];
							int bytesRead = -1;
							while ((bytesRead = zin.read(buffer)) != -1)
								fout.write(buffer, 0, bytesRead);
							zin.closeEntry();
							fout.close();
						}
						publishProgress(count++);
					}
					zin.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
				return true;
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return false;
		}

	}

	@Override
	protected void onStart() {
		super.onStart();
		File dir = new File(String.format(DobroConstants.BANNERS_DIR,
				Environment.getExternalStorageDirectory()));
		if (!dir.isDirectory()) {
			if (!dir.mkdirs())
				return;
		}
		if (DobroHelper.checkSdcard()
				&& !new File(dir.getAbsolutePath() + "/.data_v1").isFile()) {
			new BannersLoader().execute(DobroConstants.BANNERS_SOURCE);
		}
		SharedPreferences dt = DobroApplication.getApplicationStatic()
				.getDefaultPrefs();
		String temp_gt_val = "";
		if (Build.VERSION.SDK_INT > 7)
			temp_gt_val = Environment
					.getExternalStoragePublicDirectory(
							Environment.DIRECTORY_PICTURES).getAbsolutePath()
					.toString()
					+ File.separator;
		else
			temp_gt_val = Environment.getExternalStorageDirectory()
					.getAbsolutePath()
					+ File.separator
					+ "Pictures"
					+ File.separator;
		String temp_gt_val_to_compare = dt.getString("download_target", "");

		if (temp_gt_val_to_compare == "") {
			dt.edit().putString("download_target", temp_gt_val).commit();
		}
		;
		String threads_cache_dir = String.format(DobroConstants.THREADS_CACHE,
				Environment.getExternalStorageDirectory());
		String threads_info_cache_dir = String.format(
				DobroConstants.THREADS_INFO_CACHE,
				Environment.getExternalStorageDirectory());
		File dir1 = new File(threads_cache_dir);
		File dir2 = new File(threads_info_cache_dir);
		if (DobroHelper.checkSdcard()
				&& (dir1.isDirectory() || dir2.isDirectory())) {
			new OldDataCleaner().execute();
		}
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					File cacheDir = ApiWrapper
							.getExternalCacheDir(DobroHomeActivity.this);
					File[] files2del = cacheDir.listFiles(new FilenameFilter() {

						@Override
						public boolean accept(File dir, String filename) {
							if (filename.startsWith("images_cache"))
								return true;
							return false;
						}
					});
					for (File f : files2del)
						f.delete();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
	}

	private final int AB_STAR = 1;
	private final int AB_CHECK = 2;
	private final int AB_SETT = 3;
	AssetManager assets = null;
	Random r = new Random();
	ItemAdapter adapter = new ItemAdapter(this);

	private class StringPair extends Object {
		StringPair(String b, String cap) {
			this.board = b;
			this.caption = cap;
		}

		public String board;
		public String caption;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.default_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.history: {
			Intent i = new Intent(this, DobroHistoryActivity.class);
			i.putExtra(GD_ACTION_BAR_TITLE, getString(R.string.history));
			startActivity(i);
			return true;
		}
		case R.id.options:
			startActivity(new Intent(this, DobroOptions.class));
			return true;
		case R.id.about: {
			String version_name = "unk";
			PackageInfo pinfo;
			try {
				pinfo = getPackageManager().getPackageInfo(getPackageName(), 0);
				version_name = pinfo.versionName;
			} catch (NameNotFoundException e) {
				e.printStackTrace();
			}
			final TextView message = new TextView(this);
			SpannableString text = new SpannableString(getString(
					R.string.about_text, version_name));
			Linkify.addLinks(text, Linkify.ALL);
			message.setText(text);
			message.setMovementMethod(LinkMovementMethod.getInstance());
			message.setPadding(4, 4, 4, 4);
			message.setTextColor(Color.WHITE);
			new AlertDialog.Builder(new ContextThemeWrapper(this,
					R.style.AlertDialogLight)).setIcon(R.drawable.icon)
					.setTitle("About").setPositiveButton("Ok", null)
					.setView(message).show();
			return true;
		}
		case R.id.hidden_threads: {
			Intent i = new Intent(this, DobroHiddenEditor.class);
			i.putExtra(GD_ACTION_BAR_TITLE, getString(R.string.hidden_threads));
			startActivity(i);
			return true;
		}
		case R.id.starred_threads: {
			Intent i = new Intent(this, DobroStarredEditor.class);
			i.putExtra(GD_ACTION_BAR_TITLE, getString(R.string.starred));
			startActivity(i);
			return true;
		}
		case R.id.go_to: {
			Intent i = new Intent(this, DobroGotoActivity.class);
			i.putExtra(GD_ACTION_BAR_TITLE, getString(R.string.go_to));
			startActivity(i);
			return true;
		}
		case R.id.update_diff: {
			new PostsDiffGetter().execute();
			return true;
		}
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		final DobroHomeItem item = (DobroHomeItem) l.getAdapter().getItem(
				position);
		SharedPreferences prefs = DobroApplication.getApplicationStatic()
				.getDefaultPrefs();
		boolean ex_board = prefs.getBoolean("expandable_board", true);
		Intent intent = new Intent(this, (ex_board ? DobroBoardActivityEx.class
				: DobroBoardActivity.class));
		intent.putExtra(DobroConstants.BOARD,
				((StringPair) item.getTag()).board);
		intent.putExtra(ActionBarActivity.GD_ACTION_BAR_TITLE,
				((StringPair) item.getTag()).caption);
		startActivity(intent);
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Toast.makeText(
				getApplicationContext(),
				Environment.getExternalStorageDirectory().getPath()
						.concat("/Pictures/MLP/rarity.png"), Toast.LENGTH_LONG)
				.show();
		DobroHelper.updateCurrentTheme(this);
		DobroHelper.setOrientation(this);
		super.onCreate(savedInstanceState);
		assets = getAssets();
		((DobroApplication) getApplication()).registerAlarm();

		adapter.add(new SeparatorItem("Общее"));
		addMainMenuItem(adapter, new StringPair("b", "Братство"));
		addMainMenuItem(adapter, new StringPair("u", "Университет"));
		addMainMenuItem(adapter, new StringPair("rf", "Refuge"));
		addMainMenuItem(adapter, new StringPair("dt", "Dates and datings"));
		addMainMenuItem(adapter, new StringPair("vg", "Видеоигры"));
		addMainMenuItem(adapter, new StringPair("r", "Просьбы"));
		addMainMenuItem(adapter, new StringPair("cr", "Творчество"));
		addMainMenuItem(adapter, new StringPair("lor", "LOR"));
		addMainMenuItem(adapter, new StringPair("mu", "Музыка"));
		addMainMenuItem(adapter, new StringPair("oe", "Oekaki"));
		addMainMenuItem(adapter, new StringPair("s", "Li/s/p"));
		addMainMenuItem(adapter, new StringPair("w", "Обои"));
		addMainMenuItem(adapter, new StringPair("hr", "Высокое разрешение"));

		adapter.add(new SeparatorItem("Аниме"));
		addMainMenuItem(adapter, new StringPair("a", "Аниме"));
		addMainMenuItem(adapter, new StringPair("ma", "Манга"));
		addMainMenuItem(adapter, new StringPair("sw", "Spice and Wolf"));
		addMainMenuItem(adapter, new StringPair("hau", "When They Cry"));
		addMainMenuItem(adapter, new StringPair("azu", "Azumanga Daioh"));

		adapter.add(new SeparatorItem("На пробу"));
		addMainMenuItem(adapter, new StringPair("tv", "Кино"));
		addMainMenuItem(adapter, new StringPair("cp", "Копипаста"));
		addMainMenuItem(adapter, new StringPair("gf", "Gif/Flash-анимация"));
		addMainMenuItem(adapter, new StringPair("bo", "Книги"));
		addMainMenuItem(adapter, new StringPair("di", "Dining room"));
		addMainMenuItem(adapter, new StringPair("vn", "Visual novels"));
		addMainMenuItem(adapter, new StringPair("ve", "Vehicles"));
		addMainMenuItem(adapter, new StringPair("wh", "Вархаммер"));
		addMainMenuItem(adapter, new StringPair("fur", "Фурри"));
		addMainMenuItem(adapter, new StringPair("to", "Touhou Project"));
		addMainMenuItem(adapter, new StringPair("bg", "Настольные игры"));
		addMainMenuItem(adapter, new StringPair("wn", "События в мире"));
		addMainMenuItem(adapter, new StringPair("slow", "Слоудоска"));
		addMainMenuItem(adapter, new StringPair("mad", "Безумие"));

		adapter.add(new SeparatorItem("Доброчан"));
		addMainMenuItem(adapter, new StringPair("d", "Обсуждение"));
		addMainMenuItem(adapter, new StringPair("news", "Новости"));

		setListAdapter(adapter);
		addActionBarItem(ActionBarItem.Type.Star, AB_STAR);
		ActionBarItem abi = addActionBarItem(ActionBarItem.Type.Eye, AB_CHECK);
		updateAutorunIcon(abi);
		addActionBarItem(ActionBarItem.Type.Settings, AB_SETT);
		new PostsDiffGetter().execute();
		// getActionBar().setTitle(getString(R.string.app_name));// ??
	}

	void updateAutorunIcon(ActionBarItem abi) {
		SharedPreferences prefs = DobroApplication.getApplicationStatic()
				.getDefaultPrefs();
		String autorun = prefs.getString("autorun_network", "wifi");
		if (TextUtils.equals(autorun, "always"))
			abi.setDrawable(R.drawable.spin_28x32);
		else if (TextUtils.equals(autorun, "wifi"))
			abi.setDrawable(R.drawable.rss_alt_32x32);
		else
			abi.setDrawable(R.drawable.x_alt_32x32);
	}

	@Override
	public boolean onHandleActionBarItemClick(ActionBarItem item, int position) {
		if (position == -1) {
			Intent i = new Intent(this, DobroTabsList.class);
			i.putExtra(GD_ACTION_BAR_TITLE, "Вкладки");
			i.putExtra(DobroConstants.BOARD, "home");
			startActivity(i);
			return true;
		}
		switch (item.getItemId()) {
		case AB_STAR: {
			Intent i = new Intent(this, DobroStarredEditor.class);
			i.putExtra(GD_ACTION_BAR_TITLE, getString(R.string.starred));
			startActivity(i);
		}
			break;
		case AB_SETT:
			startActivity(new Intent(this, DobroOptions.class));
			break;
		case AB_CHECK:
			final CharSequence[] items = { "Всегда", "Wifi", "Никогда" };
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.autorun);
			builder.setItems(items, new DialogInterface.OnClickListener() {
				@Override
				@SuppressLint("NewApi")
				public void onClick(DialogInterface dialog, int item) {
					((DobroApplication) getApplication()).unregisterAlarm();
					SharedPreferences prefs = DobroApplication
							.getApplicationStatic().getDefaultPrefs();
					SharedPreferences.Editor prefEditor = prefs.edit();
					switch (item) {
					case 0:
						prefEditor.putString("autorun_network", "always");
						break;
					case 1:
						prefEditor.putString("autorun_network", "wifi");
						break;
					default:
						prefEditor.putString("autorun_network", "never");
						break;
					}
					prefEditor.commit();
					updateAutorunIcon(getActionBar().getItem(AB_CHECK - 1));
					String autorun = prefs.getString("autorun_network", "wifi");
					if (autorun.equalsIgnoreCase("never"))
						return;
					((DobroApplication) getApplication()).registerAlarm();
				}
			});
			AlertDialog alert = builder.create();
			alert.show();
			break;
		default:
			return super.onHandleActionBarItemClick(item, position);
		}
		return true;
	}

	void addMainMenuItem(ItemAdapter adapter, StringPair data) {
		String dirname = String.format(DobroConstants.BANNERS_DIR,
				Environment.getExternalStorageDirectory());
		dirname += data.board + "/";
		String[] list = new File(dirname).list();
		String uri = null;
		if (list != null && list.length > 0)
			uri = "file://" + dirname + list[r.nextInt(list.length)];
		DobroHomeItem board = new DobroHomeItem("/" + data.board + "/", uri);
		board.setTag(data);
		adapter.add(board);
	}
}