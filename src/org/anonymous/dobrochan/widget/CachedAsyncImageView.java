package org.anonymous.dobrochan.widget;

import java.io.File;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import java.util.regex.Pattern;

import org.anonymous.dobrochan.ApiWrapper;
import org.anonymous.dobrochan.DobroApplication;
import org.anonymous.dobrochan.DobroConstants;
import org.anonymous.dobrochan.DobroHelper;
import org.anonymous.dobrochan.DobroNetwork;
import org.anonymous.dobrochan.R;
import org.anonymous.dobrochan.json.DobroFile.Rating;
import org.anonymous.dobrochan.activity.DobroImageViewer;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageLoadingListener;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.text.ClipboardManager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.view.Window;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

public class CachedAsyncImageView extends ImageView implements
		OnClickListener, OnCreateContextMenuListener {
	public String mCachedUrl = null;
	public Rating mRating = Rating.SWF;
	private Bitmap mBitmap;
	private int mDefaultResId;
	public boolean urlLoaded = true;
	private boolean mForceLoad = false;
	private int sizeH;
	private int sizeW;
	
	private String mInfo;
	
	public void setSize(int h, int w) {
		sizeH = h;
		sizeW = w;
	}
	
	private class BitmapLoader extends AsyncTask<String, Void, Bitmap> {
		@Override
		protected void onPostExecute(Bitmap result) {
			try{
			if(!TextUtils.equals(currentUrl, mCachedUrl))
				return;
			if(result == null) {
				ImageLoader.getInstance().displayImage(mCachedUrl, CachedAsyncImageView.this, new ImageLoadingListener() {

					@Override
					public void onLoadingCancelled(String arg0, View arg1) {
					}

					@Override
					public void onLoadingComplete(String arg0, View arg1,
							Bitmap arg2) {
						setAdjustViewBounds(true);
					}

					@Override
					public void onLoadingFailed(String arg0, View arg1,
							FailReason arg2) {
						urlLoaded = false;
						setImageResource(R.drawable.document_fill_32x32);
					}

					@Override
					public void onLoadingStarted(String arg0, View arg1) {
					}
				});
			} else {
				mBitmap = result;
				setScaleType(ScaleType.FIT_CENTER);
				setImageBitmap(mBitmap);
			}} catch (Exception e) {
				e.printStackTrace();
			}
		}
		private String currentUrl;
		@Override
		protected Bitmap doInBackground(String... params) {
			currentUrl = params[0].substring(0);
			try{
				Bitmap bmp = DobroApplication.getApplicationStatic().getNetwork().memory_cache.get(mCachedUrl);
				if(bmp != null)
					return bmp;
				File cached = DobroApplication.getApplicationStatic().getNetwork().disc_cache.get(mCachedUrl);
				if(cached != null && cached.exists()) {
					bmp = BitmapFactory.decodeFile(cached.getPath());
					if(bmp != null)
						return bmp;
				}
			} catch (Exception e) {
				e.printStackTrace();
			} catch (OutOfMemoryError e) {
				e.printStackTrace();
			}
			return null;
		}
	}

	public void setRating(Rating r) {
		mRating = r;
	}
	
	public void setInfo(String information) {
		mInfo = information;
	}

	@Override
	public void onClick(View v) {
		if (!urlLoaded) {
			urlLoaded = true;
			load();
			return;
		}
		downloadImage(true, "", -1);
	}

	public void setUrl(String url) {
		if(url != null && !url.startsWith("http"))
			url = DobroHelper.formatUri(url);
		if (mBitmap != null && url != null && mCachedUrl != null && url.equals(mCachedUrl)) {
            return;
        }
		mCachedUrl = url;
		if(mBitmap != null)
			mBitmap.recycle();
        mBitmap = null;
        setAdjustViewBounds(false);
		if (TextUtils.isEmpty(url)) {
			if (mRating == Rating.ILLEGAL)
				setDefaultImageResource(R.drawable.illegal);
			else
				setDefaultImage();
			urlLoaded = true;
			return;
        }
		urlLoaded = DobroHelper.checkNetworkForPictures(getContext())
				|| mForceLoad
				|| DobroApplication.getApplicationStatic().getNetwork().memory_cache.get(mCachedUrl) != null
				|| DobroApplication.getApplicationStatic().getNetwork().disc_cache.get(mCachedUrl).exists();
		if (DobroHelper.checkRating(getContext(), mRating)) {
			urlLoaded = false;
			if (mRating == Rating.R15)
				setDefaultImageResource(R.drawable.r15);
			else if (mRating == Rating.R18)
				setDefaultImageResource(R.drawable.r18);
			else if (mRating == Rating.R18G)
				setDefaultImageResource(R.drawable.r18g);
			else if (mRating == Rating.ILLEGAL)
				setDefaultImageResource(R.drawable.illegal);
		}

		if (urlLoaded)
		{
			load();
		}
		else
		{
            setDefaultImage();
		}
	}

	public void load() {
		setDefaultImageResource(android.R.drawable.ic_menu_rotate);
		try{
			new BitmapLoader().execute(mCachedUrl);
		} catch (RejectedExecutionException e) {
			setImageResource(R.drawable.document_fill_32x32);
		}
	}

	public CachedAsyncImageView(Context context) {
		this(context, null);
	}

	public CachedAsyncImageView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public CachedAsyncImageView(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
		setOnClickListener(this);
		setDefaultImageResource(android.R.drawable.ic_menu_gallery);
	}
	
    public void setDefaultImageResource(int resId) {
        mDefaultResId = resId;
        setDefaultImage();
    }
    
    private void setDefaultImage() {
        if (mBitmap == null) try {
        	setImageResource(mDefaultResId);
        } catch (OutOfMemoryError e) {
        	System.gc();
        }
    }
    
    public void setForceLoad(boolean f) {
    	mForceLoad = f;
    }
    
    public void downloadImage(boolean open, String prefix, int size) {
		final String urlTag = (String) getTag();
		if(urlTag == null)
			return;
		if(urlTag.startsWith("http"))
		{
			Uri uri = Uri.parse(urlTag);
			if(TextUtils.equals(uri.getHost(), "youtube.com")||
					TextUtils.equals(uri.getHost(), "www.youtube.com") ||
					TextUtils.equals(uri.getHost(), "youtu.be") ||
					TextUtils.equals(uri.getHost(), "www.youtu.be")
					) {
			Intent i = new Intent(Intent.ACTION_VIEW, uri);
			getContext().startActivity(i);
			return;
			}
		}
    	List<String> segments = Uri.parse(DobroHelper.formatUri(urlTag))
				.getPathSegments();
		if (segments.size() == 0)
			return;
		String fname;
		if (segments.size() >= 2)
			fname = segments.get(segments.size() - 2) + "_"
					+ segments.get(segments.size() - 1);
		else
			fname = segments.get(segments.size() - 1);
		fname = fname.replaceAll(":", "_");
		fname = prefix + fname;
		File picsDir = new File(ApiWrapper.getDownloadDir());
		Uri fileUri = Uri.parse("file://" + picsDir.getAbsolutePath()
				+ File.separator + fname);
		File picsSearchDir = new File (ApiWrapper.getPicturesDir());
		SharedPreferences prefs = DobroApplication.getApplicationStatic().getDefaultPrefs();
		boolean quick_search = prefs.getBoolean("quick_search", true);
		File pic = quick_search ? getFileRecQuick (picsSearchDir, fname) : getFileRec (picsSearchDir, fname);
		if (pic != null){
			if (open)
				ApiWrapper.openFileInSystem (getContext (), 
						Uri.parse("file://" + pic.getAbsoluteFile ()));
			return;
		}
		
		boolean internal_viewer = prefs.getBoolean("internal_viewer", true);
		int max_size;
		try{
			max_size = Integer.parseInt(prefs.getString("imgviewer_max", "0"));
		} catch (Exception e) {
			max_size = 0;
		}
		if(internal_viewer && open && (urlTag.endsWith("jpg") || urlTag.endsWith("jpeg") ||
				urlTag.endsWith("png") || urlTag.endsWith("bmp") || urlTag.endsWith("gif")))
		{
			Intent i = new Intent(getContext(), DobroImageViewer.class);
			i.putExtra("w", sizeW);
			i.putExtra("h", sizeH);
			if(size != -1)
				i.putExtra("sample", String.format(DobroConstants.RESIZER_URL,
						Uri.encode(DobroConstants.HOST+urlTag,":/"),
						size, size));
			else if((sizeW > max_size || sizeH > max_size) &&
					(urlTag.endsWith("png") || urlTag.endsWith("jpeg") || urlTag.endsWith("jpg")) &&
					max_size > 0)
				i.putExtra("sample", String.format(DobroConstants.RESIZER_URL,
						Uri.encode(DobroConstants.HOST+urlTag,":/"),
						max_size, max_size));
			else
				i.putExtra("sample", DobroConstants.HOST+urlTag);
			i.putExtra("preview", mCachedUrl);
			i.putExtra("file", DobroConstants.HOST+urlTag);
			i.putExtra("save_to", fname);
			getContext().startActivity(i);
		}
		else
		{
			Uri uri;
			if(size == -1)
				uri = Uri.parse(DobroConstants.HOST+urlTag);
			else
				uri = Uri.parse(String.format(DobroConstants.RESIZER_URL,
						Uri.encode(DobroConstants.HOST+urlTag,":/"),
						size, size));
			ApiWrapper.download(uri, fileUri, fname, getContext(), open);
		}
    }

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		final String urlTag = (String) getTag();
		menu.add("Загрузить").setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				downloadImage(false, "", -1);
				return true;
			}
		});
		menu.add("Копировать ссылку").setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				ClipboardManager clipboard = 
						(ClipboardManager)getContext().getSystemService(Context.CLIPBOARD_SERVICE);
				clipboard.setText(DobroConstants.HOST+urlTag);
				return true;
			}
		});
		menu.add("Расшарить").setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				final Intent intent = new Intent(Intent.ACTION_SEND);
				intent.setType("text/plain");
				intent.putExtra(Intent.EXTRA_SUBJECT, "Dobrochan");
				intent.putExtra(Intent.EXTRA_TEXT,DobroConstants.HOST+urlTag);
				getContext().startActivity(Intent.createChooser(intent,
				"Select an action for sharing"));
				return true;
			}
		});
		menu.add("Сведения").setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				ScrollView scroll = new ScrollView(getContext());
				scroll.setFillViewport(true);
				scroll.setVerticalScrollBarEnabled(true);
				scroll.setHorizontalScrollBarEnabled(false);
				TextView edit = new TextView(getContext());
				edit.setText(mInfo);
				Dialog d = new Dialog(getContext(),
						DobroHelper.getDialogTheme(getContext()));
				d.requestWindowFeature(Window.FEATURE_NO_TITLE);
				d.setCanceledOnTouchOutside(true);
				scroll.addView(edit);
				d.setContentView(scroll);
				d.show();
				return true;
			}
		});
		if(sizeH <= 0 && sizeW <= 0)
			return;
		if(!(urlTag.endsWith("jpg")||urlTag.endsWith("jpeg")||urlTag.endsWith("png")||urlTag.endsWith("gif")))
			return;
		if(sizeH > 800 && sizeW > 800 && !urlTag.endsWith("gif"))
		menu.add("[800px] Уменьшенная").setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				downloadImage(true, "800p_", 800);
				return true;
			}
		});
		if(Math.max(sizeH, sizeW)/5 <= 4000 && !urlTag.endsWith("gif"))
		menu.add("[20%] Уменьшенная").setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				downloadImage(true, "20_", Math.max(sizeH, sizeW)/5);
				return true;
			}
		});
		if(Math.max(sizeH, sizeW)/5 <= 4000 && !urlTag.endsWith("gif"))
		menu.add("[50%] Уменьшенная").setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				downloadImage(true, "50_", Math.max(sizeH, sizeW)/2);
				return true;
			}
		});
		menu.add("Google Search").setOnMenuItemClickListener(new OnMenuItemClickListener() {

			@Override
			public boolean onMenuItemClick(MenuItem item) {
				String url = "http://www.google.com/searchbyimage?image_url="+DobroHelper.formatUri("http://dobrochan.ru/"+urlTag);
				Intent i = new Intent(Intent.ACTION_VIEW);
				i.setData(Uri.parse(url));
				getContext().startActivity(i);
				return true;
			}
		});
		menu.add("IQDB Search").setOnMenuItemClickListener(new OnMenuItemClickListener() {

			@Override
			public boolean onMenuItemClick(MenuItem item) {
				String url = "http://iqdb.org/?url="+DobroHelper.formatUri("http://dobrochan.ru/"+urlTag);
				Intent i = new Intent(Intent.ACTION_VIEW);
				i.setData(Uri.parse(url));
				getContext().startActivity(i);
				return true;
			}
		});
		menu.add("TinEye Search").setOnMenuItemClickListener(new OnMenuItemClickListener() {

			@Override
			public boolean onMenuItemClick(MenuItem item) {
				String url = "http://tineye.com/search/?url="+DobroHelper.formatUri("http://dobrochan.ru/"+urlTag);
				Intent i = new Intent(Intent.ACTION_VIEW);
				i.setData(Uri.parse(url));
				getContext().startActivity(i);
				return true;
			}
		});
	}
	
	private static File getFileRec (File dir, String fname){
		try{
		String [] children = dir.list ();
		for (int i = 0; i < children.length; i ++){
			File f = new File (dir, children [i]);
			if (f.isDirectory ()){
				if ((f = getFileRec (f, fname)) != null)
					return f;
			}else{
				if (children [i].equals (fname))
					return f;
			}
		}
		} catch (Exception e) {
		}
		return null;
	}

	private static Pattern probablyNoFolder = Pattern.compile ("^.+?\\.(jpg|png|gif|svg)");
	private static File getFileRecQuick (File dir, String fname){
		try{
		String [] children = dir.list ();
		for (int i = 0; i < children.length; i ++){
			File file = null;
			if (children [i].equals (fname)){
				file = new File (dir, children [i]);
				if (!file.isDirectory ())
					return file;
			}
			if (file == null && !probablyNoFolder.matcher (children [i]).matches ()){
				file = new File (dir, children [i]);
				if (!file.isDirectory ())
					file = null;
			}
			if (file != null && (file = getFileRecQuick (file, fname)) != null)
				return file;
		}
		} catch (Exception e) {
		}
		return null;
	}
}
