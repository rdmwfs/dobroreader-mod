package org.anonymous.dobrochan;

import java.io.File;

import android.annotation.TargetApi;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;

@TargetApi(9)
public class ApiWrapper {
	public static void openFileInSystem(Context context, Uri uri) {
		try {
			context.startActivity(getOpenImageIntent(uri));
		} catch (java.lang.RuntimeException e) {
			e.printStackTrace();
		}
	}
	
	public static File getExternalCacheDir(Context cnt) {
		try{
			File dir;
			if(Build.VERSION.SDK_INT > 7) {
				//XXX SDK_INT > 7
				dir = cnt.getExternalCacheDir();
			} else {
				dir = new File(Environment.getExternalStorageDirectory()+"/Android/data/"+cnt.getApplicationInfo().packageName+"/cache/");
			}
			if(!dir.isDirectory())
				dir.mkdirs();
			return dir;
		} catch (NullPointerException e) {
			return cnt.getCacheDir();
		}
	}
	
	public static String getPicturesDir (){
		/*if (Build.VERSION.SDK_INT > 7)
			return Environment.getExternalStoragePublicDirectory(
					Environment.DIRECTORY_PICTURES).getAbsolutePath();
		else
			return Environment.getExternalStorageDirectory().getAbsolutePath()+File.separator+"Pictures";*/
		SharedPreferences prefs = DobroApplication.getApplicationStatic().getDefaultPrefs();
		return prefs.getString("downloads_folder", Environment.getExternalStorageDirectory().getAbsolutePath()+File.separator+"Pictures");
	}
	
	public static String getDownloadDir() {
		/*if(Build.VERSION.SDK_INT > 7)
			return Environment.getExternalStoragePublicDirectory(
					Environment.DIRECTORY_PICTURES).getAbsolutePath()
					+ File.separator + "Dobrochan";
		else
			return Environment.getExternalStorageDirectory().getAbsolutePath()
					+ File.separator + "Pictures" + File.separator + "Dobrochan";*/
		SharedPreferences prefs = DobroApplication.getApplicationStatic().getDefaultPrefs();
		return prefs.getString("downloads_folder", Environment.getExternalStorageDirectory().getAbsolutePath()+File.separator+"Pictures") + File.separator + "/Dobrochan";
	}
	
	public static Intent getOpenImageIntent(Uri uri) {
		Intent picViewer = new Intent();
		picViewer.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		picViewer.setAction(android.content.Intent.ACTION_VIEW);
		String extension = android.webkit.MimeTypeMap
				.getFileExtensionFromUrl(uri.toString());
		String mimetype = android.webkit.MimeTypeMap.getSingleton()
				.getMimeTypeFromExtension(extension);
		if (mimetype == null || mimetype.startsWith("image/"))
			mimetype = "image/*";
		picViewer.setDataAndType(uri, mimetype);
		return picViewer;
	}
	
	public static void download(Uri from, Uri to, String title, Context c) {
		download(from, to, title, c, true);
	}

	public static void download(Uri from, Uri to, String title, Context c, boolean openAfter) {		
		if(!DobroHelper.checkSdcard())
			return;
		SharedPreferences prefs = DobroApplication.getApplicationStatic().getDefaultPrefs();
		boolean download_service_off = prefs.getBoolean("download_service_off", false);
		File picsDir = new File(getDownloadDir());
		if(!picsDir.isDirectory())
			try{
				picsDir.mkdirs();
			} catch (Exception e) {
				e.printStackTrace();
			}
		if (Build.VERSION.SDK_INT > 8 && !download_service_off) {
			//XXX SDK_INT > 8
			DownloadManager manager = (DownloadManager) DobroApplication
					.getApplicationStatic().getSystemService(
							Context.DOWNLOAD_SERVICE);
			DownloadManager.Request req = new DownloadManager.Request(from)
					.setDestinationUri(to).setTitle(title);
			long id = manager.enqueue(req);
			if(openAfter)
				DobroApplication.getApplicationStatic().addDownloadId(id);
		} else {
			DobroDownloader d = new DobroDownloader();
			d.setContext(c);
			d.setTitle(title);
			d.setFname(to.getPath());
			d.setOpenAfter(openAfter);
			d.execute(from);
		}
	}
}
