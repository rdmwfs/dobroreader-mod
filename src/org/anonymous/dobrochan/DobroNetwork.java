package org.anonymous.dobrochan;

import greendroid.app.GDActivity;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.anonymous.dobrochan.activity.DobroStarredEditor;
import org.anonymous.dobrochan.json.DobroSession;
import org.anonymous.dobrochan.json.DobroThread;
import org.anonymous.dobrochan.sqlite.IThreadsCache;
import org.anonymous.dobrochan.sqlite.IThreadsInfoCache;
import org.anonymous.dobrochan.reader.R;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.impl.cookie.BasicClientCookie2;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.SyncBasicHttpContext;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.DownloadManager;
import android.app.DownloadManager.Query;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;

import com.android.internal.http.multipart.MultipartEntity;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nostra13.universalimageloader.cache.disc.impl.TotalSizeLimitedDiscCache;
import com.nostra13.universalimageloader.cache.memory.impl.UsingFreqLimitedMemoryCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;
import com.nostra13.universalimageloader.core.display.SimpleBitmapDisplayer;
import com.nostra13.universalimageloader.core.download.HttpClientImageDownloader;
import com.nostra13.universalimageloader.utils.StorageUtils;

public class DobroNetwork extends Object{

	public static DobroNetwork getInstance() {
		DobroApplication app = DobroApplication.getApplicationStatic();
		return app.getNetwork();
	}
	
	public TotalSizeLimitedDiscCache disc_cache;
	public UsingFreqLimitedMemoryCache memory_cache;
	public DobroNetwork(DobroApplication context) {

		BasicHttpParams httpParams = new BasicHttpParams();

        ConnManagerParams.setTimeout(httpParams, 10000);
        ConnManagerParams.setMaxConnectionsPerRoute(httpParams, new ConnPerRouteBean(20));
        ConnManagerParams.setMaxTotalConnections(httpParams, 20);

        HttpConnectionParams.setSoTimeout(httpParams, 10000);
        HttpConnectionParams.setTcpNoDelay(httpParams, true);
        HttpConnectionParams.setSocketBufferSize(httpParams, 8192);

        HttpProtocolParams.setVersion(httpParams, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setUserAgent(httpParams, getUserAgent());
        
        SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        schemeRegistry.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));
        ThreadSafeClientConnManager cm = new ThreadSafeClientConnManager(httpParams, schemeRegistry);

        m_http_context = new SyncBasicHttpContext(new BasicHttpContext());
        m_httpclient = new DefaultHttpClient(cm, httpParams);

		m_cookie_store = new BasicCookieStore();
		loadCookies();
		m_http_context.setAttribute(ClientContext.COOKIE_STORE, m_cookie_store);
		createDownloadReceiver();
		

		File cacheDir = StorageUtils.getIndividualCacheDirectory(context);
		disc_cache = new TotalSizeLimitedDiscCache(cacheDir, 30 * 1024 * 1024);
		memory_cache = new UsingFreqLimitedMemoryCache(2 * 1024 * 1024);
		ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(context)
		        .threadPoolSize(5)
		        .threadPriority(Thread.NORM_PRIORITY - 2)
		        .memoryCache(memory_cache)
		        .discCache(disc_cache)
		        .imageDownloader(new HttpClientImageDownloader(context, m_httpclient))
		        .tasksProcessingOrder(QueueProcessingType.LIFO)
		        .defaultDisplayImageOptions(new DisplayImageOptions.Builder()
		        	.cacheInMemory()
		        	.cacheOnDisc()
		        	.imageScaleType(ImageScaleType.IN_SAMPLE_INT)
		        	.bitmapConfig(Bitmap.Config.ARGB_8888)
		        	.displayer(new SimpleBitmapDisplayer())
		        	.build())
		        .enableLogging()
		        .build();
		ImageLoader.getInstance().init(config);
	}
	
	@Override
	protected void finalize() {
		try {
//			((AndroidHttpClient) m_httpclient).close(); // failed on ICS
			m_httpclient = null;
		} catch (Exception e) {
			e.printStackTrace();
		}
		saveCookes();
		//XXX SDK_INT > 8
		if (Build.VERSION.SDK_INT > 8)
			DobroApplication.getApplicationStatic().unregisterReceiver(downloadReceiver);
	}
	
	public static void copyFile(File source, File dest) throws IOException {
		if(!dest.exists()) {
			dest.createNewFile();
		}
		InputStream in = null;
		OutputStream out = null;
		try {
			in = new FileInputStream(source);
			out = new FileOutputStream(dest);
			byte[] buf = new byte[1024];
			int len;
			while ((len = in.read(buf)) > 0) {
				out.write(buf, 0, len);
			}
		}
		finally {
			if(in != null) {
				in.close();
			}
			if(out != null) {
				out.close();
			}
		}
	}
	
	public void loadCookies() {
		String cookieFile_phone = DobroApplication.getApplicationStatic().getFilesDir().getAbsolutePath()+File.separator+DobroConstants.COOKIE_FILE;
		if(DobroHelper.checkSdcard()){
			String cookieFile_sd = String.format(DobroConstants.APP_DIR,
					Environment.getExternalStorageDirectory())
					+ File.separator + DobroConstants.COOKIE_FILE;
			if(new File(cookieFile_sd).exists() && !new File(cookieFile_phone).exists()) {
				try{
					copyFile(new File(cookieFile_sd), new File(cookieFile_phone));
				} catch (IOException e) {
					
				}
			}
		}
		if (new File(cookieFile_phone).exists()) {
			File file = new File(cookieFile_phone);
			try {
				BufferedReader in = new BufferedReader(new FileReader(file));
				String value = in.readLine();
				String expirity = in.readLine();
				in.close();
				BasicClientCookie2 cookie = new BasicClientCookie2(
						DobroConstants.COOKIE_KEY, value);
				cookie.setDomain(DobroConstants.DOMAIN);
				cookie.setPath("/");
				cookie.setExpiryDate(new Date(expirity));
				m_cookie_store.addCookie(cookie);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private void saveCookes() {
		String cookieFile = DobroApplication.getApplicationStatic().getFilesDir().getAbsolutePath()+File.separator+DobroConstants.COOKIE_FILE;
		for (Cookie c : m_cookie_store.getCookies()) {
			if (c.getName().equalsIgnoreCase(DobroConstants.COOKIE_KEY)
					&& c.getDomain().equalsIgnoreCase(DobroConstants.DOMAIN)) {
				File file = new File(cookieFile);
				if(file.exists()) {
					loadCookies();
					return;
				}
				try {
					BufferedWriter out = new BufferedWriter(
							new FileWriter(file));
					out.write(c.getValue());
					out.write("\n");
					out.write(c.getExpiryDate().toGMTString());
					out.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	private BroadcastReceiver downloadReceiver = null;

	private class FavChecker extends AsyncTask<Void, Void, String> {
		String dump = null;
		boolean new_posts = false;
		@Override
		protected String doInBackground(Void... params) {
			String result = "";
			try {
				JsonObject obj = getFavsJson();
				DobroSession sess = DobroParser.getInstance().parceStarredThreads(obj);
				DobroThread[] threads = sess.getThreads();
				dump = DobroApplication.getApplicationStatic().getParser().composeThreads(threads);
				for (DobroThread t : threads) {
					if (t.getLevel().equals("bookmarked") &&
							t.getUnread() > 0) {
						if(result.length() > 0)
							result += "\n";
						result += t.getTitle();
						if(!t.isFromCache())
							new_posts = true;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return new_posts?result:"";
		}

		@SuppressLint("NewApi")
		@Override
		protected void onPostExecute(String result) {
			if (result.length() == 0)
				return;
			Context context = DobroApplication.getApplicationStatic();
			SharedPreferences prefs = DobroApplication.getApplicationStatic().getDefaultPrefs();
			NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
			Notification notification;
			
			CharSequence contentTitle = context.getText(R.string.starred);
			CharSequence contentText = context.getText(R.string.new_posts);
			Intent notificationIntent = new Intent(context,
					DobroStarredEditor.class);
			notificationIntent.putExtra(GDActivity.GD_ACTION_BAR_TITLE,
					context.getString(R.string.starred));
			notificationIntent.putExtra(DobroConstants.FAVS_DUMP, dump);
			PendingIntent contentIntent = PendingIntent.getActivity(
					context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
			
			if(Build.VERSION.SDK_INT < 16) {
				//XXX SDK_INT < 16
				notification = new Notification(R.drawable.icon,
						context.getText(R.string.app_name), System.currentTimeMillis());
				notification.setLatestEventInfo(context, contentTitle, contentText,
						contentIntent);
			} else {
				notification = new Notification.BigTextStyle(
					      new Notification.Builder(context)
					         .setContentTitle(contentTitle)
					         .setContentText(contentText)
					         .setTicker(context.getText(R.string.app_name))
					         .setSmallIcon(R.drawable.icon)
					         .setContentIntent(contentIntent))
					      .bigText(contentText.toString() + "\n" + result)
					      .build();
			}
			notification.defaults = 0;
			if(prefs.getBoolean("notify_vibro_on", true))
				notification.defaults |= Notification.DEFAULT_VIBRATE;
			if(prefs.getBoolean("led_on", true)) {
				notification.flags |= Notification.FLAG_SHOW_LIGHTS;
				notification.ledARGB = Color.GREEN;
			}
			notification.flags |= Notification.FLAG_AUTO_CANCEL;
			if(prefs.getBoolean("notify_sound_on", true))
			{
				String snd_uri = prefs.getString("notify_sound_uri",Settings.System.DEFAULT_NOTIFICATION_URI.toString());
				notification.sound = Uri.parse(snd_uri);
			}
			mNotificationManager.notify(DobroConstants.NOTIFY_ID, notification);
			
			try{
				Intent dashClockIntent = new Intent("org.anonymous.dobrochan.favs");
				dashClockIntent.putExtra(DobroConstants.FAVS_DUMP, dump);
				dashClockIntent.putExtra(DobroConstants.FAVS_TITLES, result);
				context.startService(dashClockIntent);
			} catch (SecurityException e) {
			}
		}

	}
	
	BasicCookieStore m_cookie_store;


	HttpContext m_http_context;
	
	HttpClient m_httpclient;
	
	@TargetApi(9)
	void createDownloadReceiver() {
		if (Build.VERSION.SDK_INT > 8) {
			//XXX SDK_INT > 8
			downloadReceiver = new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					if (!intent.getAction().equals(
							DownloadManager.ACTION_DOWNLOAD_COMPLETE) &&
						!intent.getAction().equals(
							DownloadManager.ACTION_NOTIFICATION_CLICKED))
						return;
					long downloadId = intent.getLongExtra(
							DownloadManager.EXTRA_DOWNLOAD_ID, 0);
					// what about EXTRA_NOTIFICATION_CLICK_DOWNLOAD_IDS on API >= 11 ?
					if(!DobroApplication.getApplicationStatic().delDownloadId(downloadId))
						return;
					DownloadManager dm = (DownloadManager)context.getSystemService(Activity.DOWNLOAD_SERVICE);
					if(intent.getAction().equals(
							DownloadManager.ACTION_NOTIFICATION_CLICKED)) {
						dm.remove(downloadId);
						return;
					}
					Query query = new Query();
					query.setFilterById(downloadId);
					Cursor c = dm.query(query);
					if (c.moveToFirst()) {
						int statusIndex = c
								.getColumnIndex(DownloadManager.COLUMN_STATUS);
						int reasonIndex = c
								.getColumnIndex(DownloadManager.COLUMN_REASON);
						if (DownloadManager.STATUS_SUCCESSFUL == c
								.getInt(statusIndex)
								|| DownloadManager.ERROR_FILE_ALREADY_EXISTS == c
										.getInt(reasonIndex)) {
							String uri_str = c.getString(c
									.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
							if(uri_str == null)
								return;
							Uri uri = Uri
									.parse(uri_str);
							ApiWrapper.openFileInSystem(context, uri);
						}
					}
				}
			};
			IntentFilter filter = new IntentFilter();
			filter.addAction(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
//			filter.addAction(DownloadManager.ACTION_NOTIFICATION_CLICKED);
			DobroApplication.getApplicationStatic().registerReceiver(downloadReceiver, filter);
		}
	}
	
	public JsonObject getBoardJson(String b_name, String page_num) {
		return getUriJson(String.format(DobroConstants.API_BOARD, b_name, page_num));
	}
	
	public Bitmap getCaptcha(String uri, Activity parent) {
		HttpGet httpget = new HttpGet(uri);
		HttpResponse response;
		try {
			response = m_httpclient.execute(httpget, m_http_context);
			final HttpEntity ent = response.getEntity();
			{
				DisplayMetrics metrics = new DisplayMetrics();
				parent.getWindowManager().getDefaultDisplay()
						.getMetrics(metrics);
				BitmapFactory.Options opts = new BitmapFactory.Options();
				Bitmap unscaled = BitmapFactory.decodeStream(ent.getContent(),
						null, opts);
				int width = metrics.widthPixels - 4;
				int heigth = Math.round((width / (float) opts.outWidth)
						* opts.outHeight);
				ent.consumeContent();
				httpget.abort();
				return Bitmap.createScaledBitmap(unscaled, width, heigth, true);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public JsonObject getPostJson(String board, String post) {
		return getUriJson(String.format(DobroConstants.API_DET_POST, board,
				post));
	}
	
	public JsonObject getSessionJson() {
		return getUriJson(DobroConstants.API_SESSION_INFO);
	}

	public JsonObject getHiddenJson() {
		return getUriJson(DobroConstants.API_HIDDEN_INFO);
	}
	
	public JsonObject getFavsJson() {
		return getUriJson(DobroConstants.API_FAVS_INFO);
	}

	public DobroThread getThreadInfoJson(String thread_id) {
		IThreadsInfoCache cache = DobroApplication.getApplicationStatic().getThreadsInfo();
		String LastMod = null;
		String last_mod = new String();
		String dump = cache.getThreadInfo(thread_id);
		DobroThread cached = null;
		boolean cached_edited = false;
		if (dump != null) {
				cached = DobroParser.getInstance().parceThread(dump);
				LastMod = cached.getLastModifiedHeader();
				last_mod = cached.getLastModified();
				cached_edited = cached.isThreadModified();
		}
		JsonObject loaded_json = getUriJson(
				String.format(DobroConstants.API_THREAD_INFO, thread_id),
				LastMod);
		if(loaded_json == null)
			return cached;
		DobroThread loaded = DobroParser.getInstance().parceThread(loaded_json);
		if(loaded.getPosts() != null && loaded.getPosts().length == 1)
			loaded.setLastModified(loaded.getPosts()[0].getDate());
		if (loaded != null) {
				if (last_mod != null) {
					SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					boolean eq = true;
					try {
						Date last_mod_date = df.parse(last_mod);
						Date loaded_date = df.parse(loaded.getLastModified());
						if(last_mod_date.before(loaded_date))
							eq = false;
					} catch (ParseException e) {
						e.printStackTrace();
					}
					if (!eq)
						loaded.setThreadModified(true);
					else
						loaded.setThreadModified(cached_edited);
				} else
					loaded.setThreadModified(cached_edited);
			cache.addThreadInfo(thread_id, DobroParser.getInstance().composeThread(loaded));
			loaded.setFromCache(false);
			return loaded;
		}
		if(cached != null)
			cached.setFromCache(true);
		return cached;
	}

	public JsonObject getThreadJson(String adress, String last_id,
			String count, boolean force) {
		final IThreadsCache cache = DobroApplication.getApplicationStatic().getThreads();
		final IThreadsInfoCache info_cache = DobroApplication.getApplicationStatic().getThreadsInfo();
		if (force)
			cache.deleteThread(adress);
		String url = null;
		if (last_id != null) {
			url = String.format(DobroConstants.API_NEW_POSTS, adress, last_id);
		} else if (count != null) {
			url = String.format(DobroConstants.API_COUNT_POSTS, adress, count);
		} else {
			String json = cache.getThreadData(adress);
			if (json != null) {
				try {
					JsonObject thread = new JsonParser().parse(json).getAsJsonObject();
					return thread;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			url = String.format(DobroConstants.API_FULL_THREAD, adress);
		}
		final JsonObject result_json = getUriJson(url);
		if(result_json == null)
			return null;
		JsonObject tosave = null;
		final JsonArray posts_update = result_json.getAsJsonArray("posts");
		try {
			// save thread info
			final String[] keys = { "display_id", "thread_id",// "last_modified",
					"posts_count", "files_count", "board_id", "archived",
					"title", "__class__", "autosage", "last_hit",
					DobroConstants.LAST_MOD };
			tosave = new JsonObject();
			for(String key : keys)
				tosave.add(key, result_json.get(key));
			if(posts_update != null && posts_update.size()>0)
			{
				tosave.addProperty("last_modified", posts_update.get(posts_update.size()-1).getAsJsonObject().get("date").getAsString());
				tosave.addProperty(DobroConstants.THREAD_MODIFIED, false);
				info_cache.addThreadInfo(result_json.get("thread_id").getAsString(), tosave.toString());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (count == null && last_id == null)
			try{
				long tm = System.currentTimeMillis();
				System.gc();
				cache.addThread(adress, result_json.toString());
				Log.d("TIME", "Adding to cache (full): "+String.valueOf(System.currentTimeMillis()-tm));
			} catch (OutOfMemoryError e) {
				e.printStackTrace();
			}

		final String dump = cache.getThreadData(adress);
		if (count == null && last_id != null && dump != null) {
			// save while updating
			System.gc();
			try {
				long tm = System.currentTimeMillis();
				JsonObject thread = new JsonParser().parse(dump).getAsJsonObject();
				JsonArray posts = thread.getAsJsonArray("posts");
				int posts_cached_old_count = posts.size();
				int posts_real_count = result_json.get("posts_count").getAsInt();
				int posts_updated_count = 0;
				if (result_json.has("posts")) {
					posts_updated_count = posts_update.size();
					for (int i = 0; i < posts_update.size(); i++)
						posts.add(posts_update.get(i).getAsJsonObject());
					tosave.add("posts", posts);
					System.gc();
					try{
						cache.addThread(adress, tosave.toString());
					} catch (OutOfMemoryError e) {
						e.printStackTrace();
					}
					Log.d("TIME", "Adding to cache (upd): "+String.valueOf(System.currentTimeMillis()-tm));
				}
				if (posts_cached_old_count != posts_real_count
						- posts_updated_count) {
					DobroApplication app = DobroApplication.getApplicationStatic();
					app.showToast(app.getString(R.string.force_update),2);
				}
			} catch (Exception e) {
				e.printStackTrace();
			} catch (OutOfMemoryError e) {
				e.printStackTrace();
			}
		}
		result_json.addProperty(DobroConstants.THREAD_MODIFIED, true);
		return result_json;
	}

	public JsonObject getUriJson(String URI) {
		long tm = System.currentTimeMillis();
		JsonObject res = getUriJson(URI, null);
		Log.d("TIME", "Downloading time: "+String.valueOf(System.currentTimeMillis()-tm));
		return res;
	}

	public JsonObject getUriJson(String URI, String LastMod) {
		Log.i("LOADING", URI);
		for(int i = 0; i<3; i++)
		try {
			final HttpGet get = new HttpGet(URI);
			try {
				if (LastMod != null)
					get.setHeader(DobroConstants.IF_MOD, LastMod);
				get.setHeader("Accept-Encoding", "gzip,deflate");
				final HttpResponse r = m_httpclient.execute(get, m_http_context);
				if (r.getStatusLine().getStatusCode() == 304) // not modified
					return null;
				if(r.getStatusLine().getStatusCode() != 200)
					continue;
				final HttpEntity ent = r.getEntity();
				final InputStream is = ent.getContent();
				GZIPInputStream gzip = null;
				if(r.getFirstHeader("Set-cookie") != null)
					saveCookes();
				if (r.getFirstHeader("Content-Encoding") != null
						&& r.getFirstHeader("Content-Encoding").getValue()
								.equals("gzip"))
					gzip = new GZIPInputStream(is);
				final BufferedInputStream buff = new BufferedInputStream(
						gzip == null ? is : gzip);
				final Reader reader = new BufferedReader(
                        new InputStreamReader(buff, "UTF-8"));
				final JsonElement jselmt = new JsonParser().parse(reader);
				reader.close();
					JsonObject obj = jselmt.getAsJsonObject();
					if (obj.has("error")) {
						DobroParser.getInstance().parceError(obj);
						return null;
					} else if (obj.has("result")) {
						obj = obj.getAsJsonObject("result");
						final Header[] hdrs = r.getHeaders(DobroConstants.LAST_MOD);
						if (hdrs.length > 0)
							obj.addProperty(DobroConstants.LAST_MOD, hdrs[0].getValue());
						return obj;
					} else {
						return obj;
					}
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public String getUserAgent() {
		String version_name = "unk";
		PackageInfo pinfo;
		DobroApplication app = DobroApplication.getApplicationStatic();
		try {
			pinfo = app.getPackageManager().getPackageInfo(app.getPackageName(), 0);
			version_name = pinfo.versionName;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		return String.format(DobroConstants.DEFAULT_UA, Build.CPU_ABI,
				Build.VERSION.RELEASE, Build.VERSION.RELEASE, Build.DEVICE,
				version_name);
	}

	public void hideThread(String board, String thread) {
		HttpGet get = new HttpGet(String.format(
				DobroConstants.API_HIDE_THREAD, board, thread));
		HttpResponse r;
		try {
			r = m_httpclient.execute(get, m_http_context);
			DobroApplication app = DobroApplication.getApplicationStatic();
			if (r.getStatusLine().getStatusCode() == 200)
				app.showToast(app.getString(R.string.thread_hidden),1);
			else
				app.showToast(app.getString(R.string.thread_hide_error, r
						.getStatusLine().getStatusCode()),2);
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public HttpResponse postMessage(MultipartEntity entity, String board) {
		HttpPost p = new HttpPost(String.format(DobroConstants.POST_NEW, board));
		final HttpParams params = new BasicHttpParams();
		HttpClientParams.setRedirecting(params, false);
		ConnManagerParams.setTimeout(params, 20000);
        HttpConnectionParams.setSoTimeout(params, 20000);
        HttpConnectionParams.setTcpNoDelay(params, true);
        HttpConnectionParams.setSocketBufferSize(params, 8192);
        
		p.setParams(params);
		p.setEntity(entity);
		try {
			HttpResponse r = m_httpclient.execute(p, m_http_context);
			p.abort();
			return r;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public void starThread(String board, String thread) {
		HttpGet get = new HttpGet(String.format(
				DobroConstants.API_FAV_THREAD, board, thread));
		HttpResponse r;
		try {
			r = m_httpclient.execute(get, m_http_context);
			DobroApplication app = DobroApplication.getApplicationStatic();
			if (r.getStatusLine().getStatusCode() == 200) {
				app.showToast(app.getString(R.string.thread_starred),1);
				getThreadJson(board + "/" + thread, null, null, false);
				DobroParser.getInstance().parceStarredThreads(getFavsJson());
			} else
				app.showToast(app.getString(R.string.thread_star_error, r
						.getStatusLine().getStatusCode()),2);
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void delPost(String board, String thread_id, String post) {
		try {
			String uri = String.format(DobroConstants.API_DEL_POST, board);
			HttpPost get = new HttpPost(uri);
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
			nameValuePairs.add(new BasicNameValuePair(post,
                    thread_id));
			nameValuePairs.add(new BasicNameValuePair("task",
                    "delete"));
			String pass = PreferenceManager.getDefaultSharedPreferences(DobroApplication.getApplicationStatic()).getString("password", "");
			nameValuePairs.add(new BasicNameValuePair("password",
					pass));
			Log.e("PASSWORD",pass);
			HttpParams params = new BasicHttpParams();
			HttpClientParams.setRedirecting(params, false);
			get.setParams(params);
			get.setEntity(new UrlEncodedFormEntity(nameValuePairs));
			HttpResponse r = m_httpclient.execute(get, m_http_context);
			DobroApplication app = DobroApplication.getApplicationStatic();
			if (r.getStatusLine().getStatusCode() != 200) {
				app.showToast(app.getString(R.string.thread_del_ok),1);
			} else
				app.showToast(app.getString(R.string.thread_del_error, r
						.getStatusLine().getStatusCode()),2);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void unhideThread(String board, String thread) {
		HttpGet get = new HttpGet(String.format(
				DobroConstants.API_UNHIDE_THREAD, board, thread));
		HttpResponse r;
		try {
			r = m_httpclient.execute(get, m_http_context);
			DobroApplication app = DobroApplication.getApplicationStatic();
			if (r.getStatusLine().getStatusCode() == 200)
				app.showToast(app.getString(R.string.thread_unhidden),1);
			else
				app.showToast(app.getString(R.string.thread_unhide_error, r
						.getStatusLine().getStatusCode()),2);
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void unStarThread(String board, String thread) {
		HttpGet get = new HttpGet(String.format(
				DobroConstants.API_UNFAV_THREAD, board, thread));
		HttpResponse r;
		try {
			r = m_httpclient.execute(get, m_http_context);
			DobroApplication app = DobroApplication.getApplicationStatic();
			if (r.getStatusLine().getStatusCode() == 200)
				app.showToast(app.getString(R.string.thread_unstarred),1);
			else
				app.showToast(app.getString(R.string.thread_unstar_error, r
						.getStatusLine().getStatusCode()),2);
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public Map<String,Integer> getPostsDiff() {
		Map<String,Integer> result = null;
		try{
		JsonObject uri_json = getUriJson(DobroConstants.API_DIFF);
		if(uri_json == null)
			return null;
		result = new LinkedHashMap<String, Integer>();
		for(Map.Entry<String, JsonElement> entry : uri_json.entrySet())
			result.put(entry.getKey(), entry.getValue().getAsInt());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}
	
	public void queueFavChecking() {
		new FavChecker().execute();
	}
	
	public HttpResponse httpGet(String url) throws ClientProtocolException, IOException {
		HttpGet httpget = new HttpGet(DobroHelper.formatUri(url));
		return m_httpclient.execute(httpget, m_http_context);
	}
}
