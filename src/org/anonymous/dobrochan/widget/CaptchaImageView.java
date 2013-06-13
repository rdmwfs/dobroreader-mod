package org.anonymous.dobrochan.widget;

import greendroid.widget.AsyncImageView;

import java.io.File;
import java.io.FileOutputStream;

import org.anonymous.dobrochan.ApiWrapper;
import org.anonymous.dobrochan.DobroApplication;
import org.anonymous.dobrochan.activity.DobroNewPostActivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.widget.Toast;

public class CaptchaImageView extends AsyncImageView implements
		OnMenuItemClickListener {

	public CaptchaImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public CaptchaImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public CaptchaImageView(Context context) {
		super(context);
	}

	@Override
	protected void onCreateContextMenu(ContextMenu menu) {
		menu.add("Сохранить").setOnMenuItemClickListener(
				new OnMenuItemClickListener() {
					@Override
					public boolean onMenuItemClick(MenuItem item) {
						String dir = ApiWrapper.getPicturesDir();
						File dirFile = new File(dir);
						if (!dirFile.isDirectory())
							dirFile.mkdirs();
						saveBitmap(
								drawableToBitmap(getDrawable()),
								dir
										+ File.separator
										+ String.format("captcha_%s.png",
												System.currentTimeMillis()));
						DobroApplication.getApplicationStatic().showToast("OK",
								Toast.LENGTH_SHORT);
						return true;
					}
				});
		menu.add("Прикрепить").setOnMenuItemClickListener(
				new OnMenuItemClickListener() {
					@Override
					public boolean onMenuItemClick(MenuItem item) {
						String path = getContext().getCacheDir().getPath()
								+ File.separator
								+ String.format("captcha_%s.png",
										System.currentTimeMillis());
						saveBitmap(drawableToBitmap(getDrawable()), path);
						if (getContext() instanceof DobroNewPostActivity)
							try {
								((DobroNewPostActivity) getContext()).addImage(
										path, true);
							} catch (Exception e) {
								e.printStackTrace();
							}
						return true;
					}
				});
	}

	private Bitmap drawableToBitmap(Drawable drawable) {
		if (drawable instanceof BitmapDrawable) {
			return ((BitmapDrawable) drawable).getBitmap();
		}

		Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
				drawable.getIntrinsicHeight(), Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
		drawable.draw(canvas);

		return bitmap;
	}

	private void saveBitmap(Bitmap bmp, String filename) {
		try {
			FileOutputStream out = new FileOutputStream(filename);
			bmp.compress(Bitmap.CompressFormat.PNG, 90, out);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean onMenuItemClick(MenuItem item) {
		// TODO меню
		return false;
	}
}
