package org.anonymous.dobrochan;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.anonymous.dobrochan.minoriko.BitmapDownloaderTask;
import org.anonymous.dobrochan.R;
import org.apache.http.HttpResponse;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

class DobroDownloader extends AsyncTask<Uri, Integer, Boolean> {
		Context context;
		String title;
		String fname;
		boolean terminate = false;
		boolean open_after = true;
		int notify_id;
		long next_publish = 0;
		
		Notification notification;
		int size = 0;
		NotificationManager notificationManager;
		BroadcastReceiver res;
		PendingIntent pendingIntent;
		
		private final String INTENT_NOTIFY_ACTION = "dobroreader.NOTIFICATION";
		
		public void setOpenAfter(boolean b) {
			open_after = b;
		}
		
		public void setTitle(String s) {
			this.title = s;
		}
		
		public void setFname(String fname) {
			this.fname = fname;
		}

		public void setContext(Context c) {
			this.context = c;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			try{
				context.unregisterReceiver(res);
			} catch (Exception e) {
			}
			notificationManager.cancel(notify_id);
			pendingIntent.cancel();
			if(result && !terminate && open_after)
				try {
					Intent i = ApiWrapper.getOpenImageIntent(Uri.parse("file://"+fname));
					context.startActivity(i);
				} catch (Exception e) {
					Toast.makeText(context, "Невозможно открыть файл", Toast.LENGTH_SHORT).show();
				}
			super.onPostExecute(result);
		}

		@Override
		protected void onPreExecute() {
				res = new BroadcastReceiver() {
					@Override
					public void onReceive(Context context, Intent intent) {
						if(TextUtils.equals(INTENT_NOTIFY_ACTION,intent.getAction()) && intent.getIntExtra("id", 0) == notify_id)
						{
							terminate = true;
							notificationManager.cancel(notify_id);
							DobroApplication.getApplicationStatic().showToast("Отменено", 1);
						}
					}
				};
				IntentFilter f = new IntentFilter();
				f.addAction(INTENT_NOTIFY_ACTION);
				try{
					context.registerReceiver(res, f);
				} catch (Exception e) {
				}

				notify_id = DobroApplication.getApplicationStatic().nextProgressId();
				Intent intent = new Intent(INTENT_NOTIFY_ACTION);
				intent.putExtra("id", notify_id);
		        pendingIntent = PendingIntent.getBroadcast(context.getApplicationContext(), notify_id, intent, 0);
				notification = new Notification(R.drawable.ic_stat_downloading, "Download", System
						.currentTimeMillis());
				notification.flags = notification.flags | Notification.FLAG_ONGOING_EVENT;
				notification.contentView = new RemoteViews(context.getApplicationContext().getPackageName(), R.layout.download_progress);
	        	notification.contentIntent = pendingIntent;
				notification.contentView.setTextViewText(R.id.status_text, title==null?"Downloading":title);
				notification.contentView.setProgressBar(R.id.status_progress, 100, 0, false);

				notificationManager = (NotificationManager) context.getApplicationContext().getSystemService(
						context.getApplicationContext().NOTIFICATION_SERVICE);

				notificationManager.notify(notify_id, notification);
			super.onPreExecute();
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			if(System.currentTimeMillis() > next_publish && !terminate)
			{
				notification.contentView.setProgressBar(R.id.status_progress, size, values[0], false);
				notificationManager.notify(notify_id, notification);
				next_publish = System.currentTimeMillis() + 500;
			}
			super.onProgressUpdate(values);
		}
		private static final int MAX_BUFFER_SIZE = 4096; //1kb
		@Override
		protected Boolean doInBackground(Uri... params) {
			try{
				File cache = new File(BitmapDownloaderTask.getFileName(context, params[0].toString()));
				if (cache.exists()) {
					int nRead;
					int downloaded = 0;
					byte[] buffer = new byte[MAX_BUFFER_SIZE];
					FileOutputStream fileOutput = new FileOutputStream(fname);
					InputStream cacheInput = new FileInputStream(cache);
					while ((nRead = cacheInput.read(buffer, 0, MAX_BUFFER_SIZE))
							> 0) {
						fileOutput.write(buffer, 0, nRead);
						downloaded += nRead;
						publishProgress(downloaded);
					}
					fileOutput.close();
					cacheInput.close();
					Log.i("FROM CACHE",params[0].toString());
					return true;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			try {
				Uri uri = params[0];
				HttpResponse response = DobroApplication.getApplicationStatic().getNetwork().httpGet(uri.toString());
				InputStream input = response.getEntity().getContent();
				size = (int) response.getEntity().getContentLength();
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
				response.getEntity().consumeContent();
				if(terminate)
					new File(fname).delete();
				return true;
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return false;
		}

	}