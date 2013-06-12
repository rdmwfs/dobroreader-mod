package org.anonymous.dobrochan.activity;

import java.io.File;

import org.anonymous.dobrochan.DobroApplication;
import org.anonymous.dobrochan.DobroHelper;
import org.anonymous.dobrochan.reader.R;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.utils.StorageUtils;

public class DobroOptions extends PreferenceActivity {
	Preference clearCache;
	Preference pickcolor;
	final Context context = this;

	private class CacheCleaner extends AsyncTask<Void, Integer, Void> {
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			dlg = ProgressDialog.show(DobroOptions.this,
					getString(R.string.loading), "", true, false);
		}

		ProgressDialog dlg;

		@Override
		protected void onPostExecute(Void result) {
			new CacheSizeCounter().execute();
			dlg.dismiss();
		}

		@Override
		protected Void doInBackground(Void... params) {
			// DobroApplication.getApplicationStatic().getCache().clearCache();
			ImageLoader.getInstance().clearMemoryCache();
			ImageLoader.getInstance().clearDiscCache();
			return null;
		}

	}

	private class CacheSizeCounter extends AsyncTask<Void, Long, Long> {

		@Override
		protected void onPostExecute(Long result) {
			clearCache.setSummary(getString(R.string.cache_size,
					humanReadableByteCount(result, true)));
		}

		@Override
		protected void onPreExecute() {
			clearCache.setSummary(getString(R.string.cache_size, "?"));
		}

		public String humanReadableByteCount(long bytes, boolean si) {
			int unit = si ? 1000 : 1024;
			if (bytes < unit)
				return bytes + " B";
			int exp = (int) (Math.log(bytes) / Math.log(unit));
			String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1)
					+ (si ? "" : "i");
			return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
		}

		@Override
		protected void onProgressUpdate(Long... values) {
			clearCache.setSummary(getString(R.string.cache_size,
					humanReadableByteCount(values[0], true)));
			super.onProgressUpdate(values);
		}

		@Override
		protected Long doInBackground(Void... params) {
			try {
				File file = StorageUtils
						.getIndividualCacheDirectory(DobroOptions.this);
				long size = 0;
				if (file.isDirectory()) {
					for (File f : file.listFiles()) {
						size += f.length();
					}
				}
				return size;
			} catch (Exception e) {
				return (long) 0;
			}
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		SharedPreferences prefs = DobroApplication.getApplicationStatic()
				.getDefaultPrefs();
		String autorun = prefs.getString("autorun_network", "wifi");
		if (autorun.equalsIgnoreCase("never"))
			return;
		((DobroApplication) getApplication()).registerAlarm();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		DobroHelper.updateCurrentTheme(this);
		DobroHelper.setOrientation(this);
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.options);
		clearCache = findPreference("clearCache");
		clearCache
				.setOnPreferenceClickListener(new OnPreferenceClickListener() {
					@Override
					public boolean onPreferenceClick(Preference preference) {
						new CacheCleaner().execute();
						return true;
					}
				});
		/*
		 * pickcolor = (Preference) findPreference("img_viewer_bg_color");
		 * pickcolor.setOnPreferenceClickListener(new
		 * OnPreferenceClickListener() { public boolean
		 * onPreferenceClick(Preference preference) { AlertDialog.Builder
		 * builder = new AlertDialog.Builder(DobroOptions.this); // Get the
		 * layout inflater LayoutInflater inflater =
		 * DobroOptions.this.getLayoutInflater();
		 * 
		 * // Inflate and set the layout for the dialog // Pass null as the
		 * parent view because its going in the dialog layout
		 * builder.setView(inflater.inflate(R.layout.color_picker, null)) // Add
		 * action buttons .setPositiveButton("Ok", new
		 * DialogInterface.OnClickListener() {
		 * 
		 * @Override public void onClick(DialogInterface dialog, int id) {
		 * String tempcolor = Integer.toHexString(R.id.color_picker_red);
		 * tempcolor = tempcolor+Integer.toHexString(R.id.color_picker_red);
		 * tempcolor = tempcolor+Integer.toHexString(R.id.color_picker_green);
		 * tempcolor = tempcolor+Integer.toHexString(R.id.color_picker_blue);
		 * tempcolor = tempcolor+Integer.toHexString(R.id.color_picker_alpha);
		 * SharedPreferences bgcolortoedit =
		 * DobroApplication.getApplicationStatic().getDefaultPrefs();
		 * bgcolortoedit.edit().putString("img_viewer_bg_color",
		 * tempcolor).commit(); } }) .setNegativeButton("Cancel", new
		 * DialogInterface.OnClickListener() { public void
		 * onClick(DialogInterface dialog, int id) {
		 * DobroOptions.this.dismissDialog(id); } }); builder.create(); return
		 * true; } });
		 */
		EditTextPreference txt = (EditTextPreference) findPreference("spells");
		txt.getEditText().setSingleLine(false);
		txt.getEditText().setMinLines(3);

		EditTextPreference t2h = (EditTextPreference) findPreference("threads2hide");
		t2h.getEditText().setSingleLine(false);
		t2h.getEditText().setMinLines(3);

		/*
		 * EditTextPreference dt = (EditTextPreference)
		 * findPreference("download_target");
		 * dt.getEditText().setSingleLine(false);
		 * dt.getEditText().setMinLines(3);
		 */

		if (Build.VERSION.SDK_INT < 9) {
			CheckBoxPreference dm = (CheckBoxPreference) findPreference("download_service_off");
			dm.setEnabled(false);
		}

		new CacheSizeCounter().execute();
		((DobroApplication) getApplication()).unregisterAlarm();
	}

}
