package org.anonymous.dobrochan.activity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Comparator;

import org.anonymous.dobrochan.ApiWrapper;
import org.anonymous.dobrochan.DobroApplication;
import org.anonymous.dobrochan.R;
import org.anonymous.dobrochan.minoriko.BitmapDownloaderTask;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;

import android.app.Activity;
import android.graphics.Paint;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.androidquery.util.AQUtility;
import com.androidquery.util.WebImage;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageLoadingListener;

public class DobroImageViewer extends Activity {
	ImageDownloader async = null;
	WebView v;
	WebImage wi;
	ImageView splash;
	ProgressBar pb;
	private static Class<?>[] LAYER_TYPE_SIG = {int.class, Paint.class};
	public static final int LAYER_TYPE_SOFTWARE = 1;
	
	private String save_to, file_url;
	
	private class ImageDownloader extends AsyncTask<String, Integer, String> {
		boolean canceled = false;
		@Override
		protected void onCancelled() {
			canceled = true;
		}

		private long size = 0;
		
		@Override
		protected void onPostExecute(String result) {
			if(result != null)
				wi.load(result);
			pb.setVisibility(View.GONE);
			v.setVisibility(View.VISIBLE);
			ImageLoader.getInstance().cancelDisplayTask(splash);
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			if(values[0] > -1)
				pb.setProgress(values[0]);
        	pb.setVisibility(View.VISIBLE);
		}

		@Override
		protected String doInBackground(String... params) {
			clearCache();
			try{
				String fname = BitmapDownloaderTask.getFileName(DobroImageViewer.this, params[0].toString());
				for(int i = 0; i < 3; i++) // 3 попытки скачивания 
				if (!new File(fname).exists()) {
					File temp = new File(fname+".tmp");
					try {
						HttpResponse response = DobroApplication.getApplicationStatic().getNetwork().httpGet(params[0]);
						HttpEntity ent = response.getEntity();
						InputStream input = ent.getContent();
						size = ent.getContentLength();
						FileOutputStream fOut = new FileOutputStream(temp);
						long byteCount = 0;
						byte[] buffer = new byte[4096];
						int bytesRead = -1;
						while ((bytesRead = input.read(buffer)) != -1 && !canceled) {
							fOut.write(buffer, 0, bytesRead);
							byteCount += bytesRead;
							if(size > 0)
								publishProgress((int)(byteCount/(float)size*100));
						}
						fOut.flush();
						fOut.close();
						ent.consumeContent();
						if(byteCount == size) {
							temp.renameTo(new File(fname));
						} else {
							temp.delete();
						}
					} catch (MalformedURLException e) {
						e.printStackTrace();
						if(temp.exists())
							temp.delete();
					} catch (IOException e) {
						e.printStackTrace();
						if(temp.exists())
							temp.delete();
					}
				}
				return fname;
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}
		
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
//		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
//                WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.web_runtime);
		file_url = getIntent().getStringExtra("file");
		save_to = getIntent().getStringExtra("save_to");
		v = (WebView) findViewById(R.id.webview);
		pb = (ProgressBar) findViewById(R.id.loadingbar);
		splash = (ImageView)findViewById(R.id.spoon_preview);
		
		ImageLoader.getInstance().displayImage(getIntent().getStringExtra("preview"), splash);
		
		wi = new WebImage(v, true, false, 0x000000FF); //TODO: background color from settings
		v.setWebChromeClient(new WebChromeClient() {
			public void onProgressChanged(WebView view, int progress) 
               {
//            	pb.setProgress(progress);
//            	pb.setVisibility(View.VISIBLE);
            	if(progress == 100) {
            		wi.done(wi.wv);
        			splash.setVisibility(View.GONE);
            	}
               }
        });
		AQUtility.invokeHandler(v, "setLayerType", false, false, LAYER_TYPE_SIG, LAYER_TYPE_SOFTWARE, null);
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onResume() {
		async = (ImageDownloader) new ImageDownloader().execute(getIntent().getStringExtra("sample"));
		super.onResume();
	}
	

	@Override
	protected void onPause() {
		if(async != null && !async.isCancelled())
			async.cancel(false);
		async = null;
		super.onPause();
	}
	
	private void clearCache() {
		File dir = ApiWrapper.getExternalCacheDir(this);
    	File[] list = dir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String filename) {
				if(!filename.contains(".") || filename.endsWith(".tmp"))
					return true;
				return false;
			}
		});
    	if(list != null && list.length > 10) {
    		Arrays.sort(list, new Comparator<File>(){
				@Override
				public int compare(File arg0, File arg1) {
					if(arg0.lastModified() > arg1.lastModified())
						return 1;
					else if(arg0.lastModified() < arg1.lastModified())
						return -1;
					return 0;
				}
    		});
    		if(list[0].lastModified() > list[list.length-1].lastModified()) {
    			for(int i = list.length-1; i>9; i--)
    				list[i].delete();
    		} else {
    			for(int i = 0; i<list.length-10; i++)
    				list[i].delete();
    		}
    	}
	}

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.imgview, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.imgmenu_save:
        {
        	File picsDir = new File(ApiWrapper.getDownloadDir());
    		Uri fileUri = Uri.parse("file://" + picsDir.getAbsolutePath()
    				+ File.separator + save_to);
        	ApiWrapper.download(Uri.parse(file_url), fileUri, save_to, this, false);
        }
            return true;
        case R.id.imgmenu_save_and_open:
        {
        	File picsDir = new File(ApiWrapper.getDownloadDir());
    		Uri fileUri = Uri.parse("file://" + picsDir.getAbsolutePath()
    				+ File.separator + save_to);
        	ApiWrapper.download(Uri.parse(file_url), fileUri, save_to, this, true);
        }
        	return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
}
