package org.anonymous.dobrochan;

import java.net.MalformedURLException;
import java.net.URL;

import org.anonymous.dobrochan.json.DobroFile.Rating;
import org.anonymous.dobrochan.clear.R;
import org.apache.http.client.methods.HttpGet;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.TextUtils;

public class DobroHelper {
	public static boolean checkSdcard() {
		return Environment.MEDIA_MOUNTED.equals(Environment
				.getExternalStorageState());
	}
	
	public static void setOrientation(Activity c) {
		SharedPreferences prefs = DobroApplication.getApplicationStatic().getDefaultPrefs();
		String rotate = prefs.getString("rotate", "auto");
		if(rotate.equals("land"))
			c.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		else if(rotate.equals("port"))
			c.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
	}

	public static boolean checkAutorun(Context c) {
		SharedPreferences prefs = DobroApplication.getApplicationStatic().getDefaultPrefs();
		String autorun = prefs.getString("autorun_network", "never");
		if (autorun.equalsIgnoreCase("never"))
			return false;
		return true;
	}

	public static void updateCurrentTheme(ContextWrapper a) {
		SharedPreferences prefs = DobroApplication.getApplicationStatic().getDefaultPrefs();
		String t = prefs.getString("theme", "dark");
		int m_theme = R.style.Theme_DobroTheme;
		if (t.equals("light"))
			m_theme = R.style.Theme_DobroLightTheme;
		else if (t.equals("dc"))
			m_theme = R.style.Theme_DobroChanTheme;
		a.setTheme(m_theme);
	}
	
	public static int getDialogTheme(Context a) {
		SharedPreferences prefs = DobroApplication.getApplicationStatic().getDefaultPrefs();
		String t = prefs.getString("theme", "dark");
		if (t.equals("light"))
			return R.style.DialogLight;
		else if (t.equals("dc"))
			return R.style.DialogLight;
		else
			return android.R.style.Theme_Dialog;	
	}

	public static boolean checkNetwork(Context c) {
		SharedPreferences prefs = DobroApplication.getApplicationStatic().getDefaultPrefs();
		String autorun = prefs.getString("autorun_network", "never");
		if (autorun.equalsIgnoreCase("never"))
			return false;
		boolean isConnected = false;
		boolean isWifi = false;
		ConnectivityManager cm = (ConnectivityManager) c
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo ni = cm.getActiveNetworkInfo();
		if (ni != null && ni.isConnected()) {
			isConnected = true;
			if (ni.getType() == ConnectivityManager.TYPE_WIFI)
				isWifi = true;
		}
		if ((autorun.equalsIgnoreCase("always") && isConnected)
				|| (autorun.equalsIgnoreCase("wifi") && isWifi)) {
			return true;
		}
		return false;
	}
	
	public static boolean checkNetworkForPictures(Context c) {
		SharedPreferences prefs = DobroApplication.getApplicationStatic().getDefaultPrefs();
		String autorun = prefs.getString("images_show", "wifi");
		if (autorun.equalsIgnoreCase("never"))
			return false;
		boolean isConnected = false;
		boolean isWifi = false;
		ConnectivityManager cm = (ConnectivityManager) c
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo ni = cm.getActiveNetworkInfo();
		if (ni != null && ni.isConnected()) {
			isConnected = true;
			if (ni.getType() == ConnectivityManager.TYPE_WIFI)
				isWifi = true;
		}
		if ((autorun.equalsIgnoreCase("always") && isConnected)
				|| (autorun.equalsIgnoreCase("wifi") && isWifi)) {
			return true;
		}
		return false;
	}

	public static boolean checkRating(Context c, Rating r) {
		SharedPreferences prefs = DobroApplication.getApplicationStatic().getDefaultPrefs();
		String max_rating = prefs.getString("rating", "swf");
		return (max_rating.compareTo("swf") == 0 && r != Rating.SWF)
				|| (max_rating.compareTo("r15") == 0 && r == Rating.R18)
				|| (max_rating.compareTo("r15") == 0 && r == Rating.R18G)
				|| (max_rating.compareTo("r18") == 0 && r == Rating.R18G)
				|| r == Rating.ILLEGAL;
	}
	
	public static URL formatUrl(Uri uri) throws MalformedURLException {
		String scheme = uri.getScheme();
		if(scheme == null)
			scheme = "http";
		String host = uri.getHost();
		if(host == null)
			host = DobroConstants.DOMAIN;
		String path = uri.getEncodedPath();
		String query = uri.getEncodedQuery();
		return new URL(scheme,host,path+"?"+query);
	}
	
	public static String formatUri(String uri_s){
		Uri uri;
		if(!uri_s.startsWith("http"))
			uri = Uri.parse(Uri.encode(DobroConstants.HOST+uri_s, ":/?&"));
		else
			uri = Uri.parse(Uri.encode(uri_s, ":/?&"));
		String scheme = uri.getScheme();
		if(scheme == null)
			scheme = "http";
		String host = uri.getHost();
		if(host == null)
			host = DobroConstants.DOMAIN;
		String path = uri.getEncodedPath();
		String query = uri.getQuery();
		String result = scheme+"://"+host+path;
		if(query != null && !TextUtils.isEmpty(query))
			result += "?"+Uri.encode(query,"/?&=");
		return result;
	}
}
