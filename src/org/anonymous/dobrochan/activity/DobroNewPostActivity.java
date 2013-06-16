package org.anonymous.dobrochan.activity;

import greendroid.app.GDActivity;
import greendroid.util.Md5Util;
import greendroid.widget.ActionBarItem;
import greendroid.widget.AsyncImageView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.anonymous.dobrochan.ApiWrapper;
import org.anonymous.dobrochan.DobroApplication;
import org.anonymous.dobrochan.DobroConstants;
import org.anonymous.dobrochan.DobroHelper;
import org.anonymous.dobrochan.DobroNetwork;
import org.anonymous.dobrochan.DobroParser;
import org.anonymous.dobrochan.DobroQuoteHolder;
import org.anonymous.dobrochan.json.DobroSession;
import org.anonymous.dobrochan.json.DobroToken;
import org.anonymous.dobrochan.reader.R;
import org.apache.http.HttpResponse;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.MediaStore.MediaColumns;
import android.text.Editable;
import android.text.TextUtils;
import android.text.format.Time;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.http.multipart.FilePart;
import com.android.internal.http.multipart.MultipartEntity;
import com.android.internal.http.multipart.Part;
import com.android.internal.http.multipart.StringPart;
import com.google.gson.JsonObject;

public class DobroNewPostActivity extends GDActivity implements
		Dialog.OnClickListener {
	boolean back_pressed = false;
	boolean always_show_captcha = false;

	@Override
	public boolean onHandleActionBarItemClick(ActionBarItem item, int position) {
		if (position == -1) {
			Intent i = new Intent(this, DobroTabsList.class);
			i.putExtra(GD_ACTION_BAR_TITLE, "Вкладки");
			i.putExtra(DobroConstants.BOARD, board);
			i.putExtra(DobroConstants.THREAD, thread);
			i.putExtra(DobroConstants.SCHROLL_TO, post);
			startActivity(i);
			return true;
		}
		return super.onHandleActionBarItemClick(item, position);
	}

	@Override
	public void onBackPressed() {
		back_pressed = true;
		DobroQuoteHolder.getInstance().setText(
				message_edit.getText().toString());
		DobroQuoteHolder.getInstance().setPictures(attachments);
		super.onBackPressed();
	}

	private class BitmapDownloader extends AsyncTask<String, Integer, Bitmap> {
		boolean captcha_not_needed = false;

		@Override
		protected Bitmap doInBackground(String... params) {
			if (!error_while_posting)
				try {
					JsonObject json = DobroNetwork.getInstance()
							.getSessionJson();
					DobroSession sess = DobroParser.getInstance()
							.parceHiddenThreads(json);
					for (DobroToken tkn : sess.getTokens()) {
						if (tkn.getToken().equals("no_user_captcha")) {
							captcha_not_needed = true;
						}
					}
				} catch (NullPointerException e) {
					e.printStackTrace();
				}
			if (captcha_not_needed && !always_show_captcha)
				return null;
			return DobroNetwork.getInstance().getCaptcha(params[0],
					DobroNewPostActivity.this);
		}

		@Override
		protected void onPostExecute(Bitmap result) {
			super.onPostExecute(result);
			if (result != null) {
				captcha_img.setVisibility(View.VISIBLE);
				captcha_img.setAdjustViewBounds(true);
				captcha_img.setImageBitmap(result);
				if (captcha_not_needed) {
					captcha_edit.setText(R.string.no_user_captcha);
					captcha_edit.setEnabled(false);
				} else {
					if (!captcha_edit.isEnabled())
						captcha_edit.setText("");
					captcha_edit.setEnabled(true);
				}
			} else {
				captcha_img.setVisibility(View.GONE);
				captcha_edit.setText(R.string.no_user_captcha);
				captcha_edit.setEnabled(false);
			}
		}

	}

	public class NewPostAttachment {
		public boolean delete_after;
		public String fname;
		public String rating;
		public ImageView imageview;

		public void setImageView(ImageView view) {
			this.imageview = view;
		}

		public NewPostAttachment(String file, String rate) {
			this.fname = file;
			this.rating = rate;
			this.delete_after = false;
		}
	}

	private class NewPoster extends
			AsyncTask<MultipartEntity, Integer, HttpResponse> {
		ProgressDialog dlg;

		@Override
		protected HttpResponse doInBackground(MultipartEntity... params) {
			return DobroNetwork.getInstance().postMessage(params[0], board);
		}

		@Override
		protected void onPostExecute(HttpResponse response) {
			setResult(Activity.RESULT_OK);
			super.onPostExecute(response);

			try {
				// если не сможем определить статус отправки, можно будет еще
				// раз ткнуть в кнопку
				Button sendButton = (Button) findViewById(R.id.sendButton);
				sendButton.setEnabled(true);
			} catch (Exception e) {

			}

			if (!dlg.isShowing()) {
				// Почему-то ProgressDialog скрыт о_О
				// Ну и хуй с ним
				Log.d("DIALOG", "!isShowing()");
				DobroNewPostActivity.this.finish(); // TODO: check
				return;
			}
			dlg.dismiss();

			if (response == null) {
				// Сервер не ответил на отправку
				Log.d("RESPONSE", "NULL");
				Toast.makeText(DobroNewPostActivity.this,
						"Не удалось установить статус отправки сообщения.",
						Toast.LENGTH_SHORT).show();
				error_while_posting = true;
				updateCaptchaImg();
				return;
			}

			boolean ok = false;
			try {
				// если НЕошибка - будет ok == true
				ok = !response.getFirstHeader("Location").getValue()
						.startsWith("http://dobrochan.ru/error/");
			} catch (NullPointerException e) {
				e.printStackTrace();
				ok = false;
			}
			if (!ok) {
				// таки ошибка
				Toast.makeText(DobroNewPostActivity.this,
						"Ошибка отправки сообщения!", Toast.LENGTH_SHORT)
						.show();
				error_while_posting = true;
				updateCaptchaImg();
			} else {
				// таки все ок
				DobroNewPostActivity.this.finish();
			}
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			dlg = ProgressDialog.show(DobroNewPostActivity.this,
					"Подождите...", "Идет отправка сообщения");
			dlg.setCancelable(true);
			dlg.setOnCancelListener(new OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					// FIXME нихрена не остановит. Нужен Thread canceller,
					// который, если надо, будет делать HttpResponse.abort()
					NewPoster.this.cancel(true);
				}
			});
		}

	}

	private static final int IMAGE_MAX_SIZE = 1024;
	public final static int SELECT_DANBOORU = 2;
	public final static int SELECT_PHOTO = 3;
	public final static int SELECT_PICTURE = 1;
	public final static int SELECT_FILE = 5;
	public final static int IMAGE_CROP = 4;
	private final List<NewPostAttachment> attachments = new ArrayList<NewPostAttachment>();
	String board;
	String thread;
	String post;
	EditText captcha_edit;
	EditText message_edit;
	EditText name_edit;
	EditText title_edit;
	AsyncImageView captcha_img;
	String temp_file;
	NewPostAttachment crop_attach = null;
	private boolean error_while_posting = false;
	private String password = "empty";
	private final CharSequence[] ratings = { "SFW", "R-15", "R-18", "R-18G" };

	public void addImage(String uri, boolean temporary) {
		addImage(uri, temporary, "SFW");
	}

	private void addImage(String uri, boolean temporary, final String rating) {
		if (attachments.size() >= 5)
			return;
		AsyncImageView image = new AsyncImageView(this);
		image.setScaleType(ScaleType.CENTER_INSIDE);
		Bitmap b = loadBitmap(uri);
		if (b == null)
			return;
		image.setImageBitmap(b);
		image.setTag(attachments.size());
		image.setAdjustViewBounds(true);
		image.setPadding(4, 2, 4, 2);
		registerForContextMenu(image);
		image.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
			@SuppressLint("DefaultLocale")
			@Override
			public void onCreateContextMenu(ContextMenu menu, View v,
					ContextMenuInfo menuInfo) {
				// menu.setHeaderTitle("Select rating");
				int i;
				String r = attachments.get((Integer) v.getTag()).rating;
				for (i = 0; i < ratings.length; i++) {
					if (r.equals(ratings[i].toString().toLowerCase())) {
						menu.add(Menu.NONE, (Integer) v.getTag(), i,
								((String) ratings[i]).concat(" (selected)"));
					} else {
						menu.add(Menu.NONE, (Integer) v.getTag(), i, ratings[i]);
					}
				}
				menu.add(Menu.NONE, (Integer) v.getTag(), i++, R.string.crop);
				menu.add(Menu.NONE, (Integer) v.getTag(), i++, R.string.resize);
				menu.add(Menu.NONE, (Integer) v.getTag(), i++, R.string.delete);
				Toast.makeText(getApplicationContext(), r, Toast.LENGTH_SHORT)
						.show();
			}
		});
		LinearLayout scroll = (LinearLayout) findViewById(R.id.picsScroll);
		scroll.addView(image);
		NewPostAttachment at = new NewPostAttachment(uri, rating);
		at.setImageView(image);
		if (temporary)
			at.delete_after = true;
		attachments.add(at);
	}

	@Override
	public void finish() {
		if (!back_pressed)
			for (NewPostAttachment a : attachments)
				if (a.delete_after)
					new File(a.fname).delete();
		super.finish();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent imageReturnedIntent) {
		if (resultCode == RESULT_OK) {
			if (requestCode == SELECT_PHOTO) {
				String filePath = temp_file;
				temp_file = null;
				if (requestCode == SELECT_PHOTO) {
					try {
						// remove exif
						ExifInterface exif = new ExifInterface(filePath);
						exif.setAttribute(ExifInterface.TAG_MODEL, "");
						exif.setAttribute(ExifInterface.TAG_MAKE, "");
						if (Build.VERSION.SDK_INT > 8) {
							exif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE,
									"");
							exif.setAttribute(
									ExifInterface.TAG_GPS_ALTITUDE_REF, "");
						}
						if (Build.VERSION.SDK_INT > 7) {
							exif.setAttribute(ExifInterface.TAG_GPS_DATESTAMP,
									"");
							exif.setAttribute(
									ExifInterface.TAG_GPS_PROCESSING_METHOD, "");
							exif.setAttribute(ExifInterface.TAG_GPS_TIMESTAMP,
									"");
						}
						exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, "");
						exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF,
								"");
						exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, "");
						exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF,
								"");
						exif.saveAttributes();
					} catch (IOException e) {
						Toast.makeText(DobroNewPostActivity.this,
								"Can't clean EXIF!", Toast.LENGTH_SHORT).show();
					}
					addImage(filePath, true);
				}
			} else if ((requestCode == SELECT_PICTURE || requestCode == SELECT_FILE)
					&& imageReturnedIntent != null) {
				// FIXME при выборе файла может вывалиться RuntimeException за
				// PermissionDenial. У меня на AndroZip сделалось. RootExplorer
				// и Dropbox работают
				Uri selectedImage = imageReturnedIntent.getData();
				String filePath;
				if (selectedImage.getScheme().equals("content")) {
					String[] filePathColumn = { MediaColumns.DATA };
					Cursor cursor = getContentResolver().query(selectedImage,
							filePathColumn, null, null, null);
					cursor.moveToFirst();
					int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
					filePath = cursor.getString(columnIndex);
					cursor.close();
				} else if (selectedImage.getScheme().equals("file"))
					filePath = selectedImage.getPath();
				else
					return; // unknown scheme
				addImage(filePath, false);
			} else if (requestCode == SELECT_DANBOORU
					&& imageReturnedIntent != null) {
				for (String fname : imageReturnedIntent
						.getStringArrayExtra(MediaStore.EXTRA_OUTPUT))
					addImage(fname, true);
			} else if (requestCode == IMAGE_CROP && crop_attach != null) {
				File f = new File(crop_attach.fname);
				String fname = f.getName();
				String crop_output = String
						.format(DobroConstants.TEMP_FILE, Environment
								.getExternalStorageDirectory().getPath(),
								fname.substring(0, fname.lastIndexOf("."))
										+ "_cropped", "jpg");
				Bitmap b = loadBitmap(crop_output);
				if (b == null)
					return;
				crop_attach.imageview.setImageBitmap(b);
				crop_attach.fname = crop_output;
				crop_attach.delete_after = true;
				crop_attach = null;
			}
		}
	}

	private Bitmap loadBitmap(String fname) {
		try {
			return loadBitmap(fname, false);
		} catch (Exception e) {
			return BitmapFactory.decodeResource(getResources(),
					R.drawable.document_fill_32x32);
		}
	}

	private Bitmap loadBitmap(String fname, boolean throw_exeption)
			throws Exception {
		Bitmap b = null;
		try {
			BitmapFactory.Options o = new BitmapFactory.Options();
			o.inJustDecodeBounds = true;
			FileInputStream fis = new FileInputStream(fname);
			BitmapFactory.decodeStream(fis, null, o);
			fis.close();
			int scale = 1;
			if (o.outHeight > IMAGE_MAX_SIZE || o.outWidth > IMAGE_MAX_SIZE) {
				scale = (int) Math.pow(
						2,
						(int) Math.round(Math.log(IMAGE_MAX_SIZE
								/ (double) Math.max(o.outHeight, o.outWidth))
								/ Math.log(0.5)));
			}
			BitmapFactory.Options o2 = new BitmapFactory.Options();
			o2.inSampleSize = scale;
			fis = new FileInputStream(fname);
			b = BitmapFactory.decodeStream(fis, null, o2);
			fis.close();
		} catch (Exception e) {
			b = null;
		} catch (OutOfMemoryError e) {
			b = null;
		}
		if (b == null) {
			if (throw_exeption)
				throw new Exception("Can't load bitmap");
			else
				return BitmapFactory.decodeResource(getResources(),
						R.drawable.document_fill_32x32);
		}
		return b;
	}

	public void onAddBooruClick(View v) {
		if (attachments.size() >= 5) {
			Toast.makeText(DobroNewPostActivity.this, R.string.max_files,
					Toast.LENGTH_SHORT).show();
			return;
		}
		Intent booruPickerIntent = new Intent(Intent.ACTION_PICK);
		booruPickerIntent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
		booruPickerIntent.setType("danbooru/minoriko");
		try {
			booruPickerIntent.putExtra(MediaStore.EXTRA_OUTPUT,
					ApiWrapper.getExternalCacheDir(this));
		} catch (NullPointerException e) {
			return;
		}
		try {
			startActivityForResult(booruPickerIntent, SELECT_DANBOORU);
		} catch (ActivityNotFoundException e) {
			try {
				Intent intent = new Intent(
						Intent.ACTION_VIEW,
						Uri.parse("market://details?id=org.anonymous.dobrochan.minoriko"));
				intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
				startActivity(intent);
			} catch (ActivityNotFoundException ee) {
				Toast.makeText(DobroNewPostActivity.this,
						R.string.minoriko_needed, Toast.LENGTH_SHORT).show();
			}
		}
	}

	public void onAddImageClick(View v) {
		if (attachments.size() >= 5) {
			Toast.makeText(DobroNewPostActivity.this, R.string.max_files,
					Toast.LENGTH_SHORT).show();
			return;
		}
		try {
			Intent photoPickerIntent = new Intent(
					Intent.ACTION_PICK,
					android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
			photoPickerIntent.setType("image/*");
			photoPickerIntent
					.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
			startActivityForResult(photoPickerIntent, SELECT_PICTURE);
		} catch (Exception e) {
			Toast.makeText(DobroNewPostActivity.this,
					R.string.open_intent_error, Toast.LENGTH_SHORT).show();
		}
	}

	public void onAddFileClick(View v) {
		if (attachments.size() >= 5) {
			Toast.makeText(DobroNewPostActivity.this, R.string.max_files,
					Toast.LENGTH_SHORT).show();
			return;
		}
		try {
			Intent photoPickerIntent = new Intent(Intent.ACTION_GET_CONTENT);
			photoPickerIntent.setType("file/*");
			photoPickerIntent
					.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
			startActivityForResult(photoPickerIntent, SELECT_FILE);
		} catch (Exception e) {
			Toast.makeText(DobroNewPostActivity.this,
					R.string.open_intent_error, Toast.LENGTH_SHORT).show();
		}
	}

	public void onAddPhotoClick(View v) {
		if (attachments.size() >= 5) {
			Toast.makeText(DobroNewPostActivity.this, R.string.max_files,
					Toast.LENGTH_SHORT).show();
			return;
		}
		File dir = new File(String.format(DobroConstants.TEMP_COMMON,
				Environment.getExternalStorageDirectory().getPath()));
		if (!dir.isDirectory())
			dir.mkdirs();
		try {
			Intent cameraIntent = new Intent(
					android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
			cameraIntent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
			temp_file = getTempFileName();
			cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT,
					Uri.parse("file://" + temp_file));
			startActivityForResult(cameraIntent, SELECT_PHOTO);
		} catch (Exception e) {
			Toast.makeText(DobroNewPostActivity.this,
					R.string.open_intent_error, Toast.LENGTH_SHORT).show();
		}
	}

	public void onBoldClick(View v) {
		int sel_start = Math.min(message_edit.getSelectionStart(),
				message_edit.getSelectionEnd());
		int sel_end = Math.max(message_edit.getSelectionStart(),
				message_edit.getSelectionEnd());
		Editable text = message_edit.getText();
		text.insert(sel_end, "**");
		text.insert(sel_start, "**");
		if (sel_end == sel_start)
			message_edit.setSelection(sel_start + 2);
	}

	public void onBoldItalicClick(View v) {
		int sel_start = Math.min(message_edit.getSelectionStart(),
				message_edit.getSelectionEnd());
		int sel_end = Math.max(message_edit.getSelectionStart(),
				message_edit.getSelectionEnd());
		Editable text = message_edit.getText();
		text.insert(sel_end, "**_");
		text.insert(sel_start, "_**");
		if (sel_end == sel_start)
			message_edit.setSelection(sel_start + 3);
	}

	public void onItalicClick(View v) {
		int sel_start = Math.min(message_edit.getSelectionStart(),
				message_edit.getSelectionEnd());
		int sel_end = Math.max(message_edit.getSelectionStart(),
				message_edit.getSelectionEnd());
		Editable text = message_edit.getText();
		text.insert(sel_end, "*");
		text.insert(sel_start, "*");
		if (sel_end == sel_start)
			message_edit.setSelection(sel_start + 1);
	}

	public void onSpoilerClick(View v) {
		int sel_start = Math.min(message_edit.getSelectionStart(),
				message_edit.getSelectionEnd());
		int sel_end = Math.max(message_edit.getSelectionStart(),
				message_edit.getSelectionEnd());
		Editable text = message_edit.getText();
		if (sel_end == sel_start) {
			text.insert(sel_end, "%%");
			text.insert(sel_start, "%%");
			message_edit.setSelection(sel_start + 2);
		} else {
			CharSequence sub = text.subSequence(sel_start, sel_end);
			if (TextUtils.indexOf(sub, "\n") >= 0) {
				text.insert(sel_end, "\n%%\n");
				text.insert(sel_start, "\n%%\n");
			} else {
				text.insert(sel_end, "%%");
				text.insert(sel_start, "%%");
			}
		}
	}

	public void onCodeClick(View v) {
		int sel_start = Math.min(message_edit.getSelectionStart(),
				message_edit.getSelectionEnd());
		int sel_end = Math.max(message_edit.getSelectionStart(),
				message_edit.getSelectionEnd());
		Editable text = message_edit.getText();
		if (sel_end == sel_start) {
			text.insert(sel_end, "``");
			text.insert(sel_start, "``");
			message_edit.setSelection(sel_start + 2);
		} else {
			CharSequence sub = text.subSequence(sel_start, sel_end);
			if (TextUtils.indexOf(sub, "\n") >= 0) {
				text.insert(sel_end, "\n``\n");
				text.insert(sel_start, "\n``\n");
			} else {
				text.insert(sel_end, "``");
				text.insert(sel_start, "``");
			}
		}
	}

	public void onStrikeClick(View v) {
		int sel_start = Math.min(message_edit.getSelectionStart(),
				message_edit.getSelectionEnd());
		int sel_end = Math.max(message_edit.getSelectionStart(),
				message_edit.getSelectionEnd());
		Editable text = message_edit.getText();
		text.insert(sel_end,
				new String(new char[sel_end - sel_start]).replace("\0", "^H"));
		message_edit.setSelection(sel_end, sel_end + (sel_end - sel_start) * 2);
	}

	public String getTempFileName() {
		return String.format(DobroConstants.TEMP_FILE, Environment
				.getExternalStorageDirectory().getPath(), System
				.currentTimeMillis(), "jpg");
	}

	public void onCaptchaClick(View v) {
		Toast.makeText(DobroNewPostActivity.this, "Обновление капчи...",
				Toast.LENGTH_SHORT).show();
		updateCaptchaImg();
	}

	/**
	 * ID элемента, на котором было вызвано onContextMenuItemSelected
	 */
	private int id;

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		id = item.getItemId();
		int order = item.getOrder();
		if (order < 4) {
			attachments.get(id).rating = item.getTitle().toString();
		} else if (order == 4) {
			/*
			 * may be this is better? Intent { act=android.intent.action.VIEW
			 * dat=file:///mnt/sdcard/Pictures/....jpg
			 * cmp=com.alensw.PicFolder/.CropActivity (has extras) }
			 */
			new File(String.format(DobroConstants.TEMP_COMMON, Environment
					.getExternalStorageDirectory().getPath())).mkdirs();
			crop_attach = attachments.get(item.getItemId());
			File f = new File(attachments.get(id).fname);
			Uri photoUri = Uri.fromFile(f);
			String fname = f.getName();
			Uri crop_output = Uri.parse("file://"
					+ String.format(DobroConstants.TEMP_FILE, Environment
							.getExternalStorageDirectory().getPath(),
							fname.substring(0, fname.lastIndexOf("."))
									+ "_cropped", "jpg"));

			Intent intent = new Intent("com.android.camera.action.CROP");
			intent.setDataAndType(photoUri, "image/*");
			intent.putExtra("crop", "true");
			intent.putExtra("scale", true); // ???
			intent.putExtra("return-data", false);
			intent.putExtra(MediaStore.EXTRA_OUTPUT, crop_output);
			startActivityForResult(intent, IMAGE_CROP);
		} else if (order == 5) {
			BitmapFactory.Options o = new BitmapFactory.Options();
			o.inJustDecodeBounds = true;
			try {
				FileInputStream fis = new FileInputStream(
						attachments.get(id).fname);
				BitmapFactory.decodeStream(fis, null, o);
				fis.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				return true;
			} catch (IOException e) {
				e.printStackTrace();
				return true;
			}
			final int oldWidth = o.outWidth;
			final int oldHeight = o.outHeight;

			/* == Creation of dialog == */
			final AlertDialog.Builder adb = new AlertDialog.Builder(this);

			LayoutInflater systemInflater = (LayoutInflater) (getApplicationContext()
					.getSystemService(LAYOUT_INFLATER_SERVICE));
			View dialogView = systemInflater.inflate(R.layout.resize_dialog,
					null);

			adb.setView(dialogView).setTitle("Масштабирование")
					.setPositiveButton(android.R.string.ok, this)
					.setNegativeButton(android.R.string.cancel, this);

			final TextView x = (TextView) dialogView.findViewById(R.id.resizeX);
			x.setText(String.valueOf(o.outWidth));
			final TextView y = (TextView) dialogView.findViewById(R.id.resizeY);
			y.setText(String.valueOf(o.outHeight));

			final SeekBar resSeekBar = (SeekBar) dialogView
					.findViewById(R.id.resizeBar);
			resSeekBar
					.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
						@Override
						public void onStopTrackingTouch(SeekBar seekBar) {
						}

						@Override
						public void onStartTrackingTouch(SeekBar seekBar) {
						}

						@Override
						public void onProgressChanged(SeekBar seekBar,
								int progress, boolean fromUser) {
							int width = oldWidth * (progress + 1) / 1000;
							if (width == 0)
								width = 1;
							int height = oldHeight * (progress + 1) / 1000;
							if (height == 0)
								height = 1;
							x.setText(String.valueOf(width));
							y.setText(String.valueOf(height));
						}
					});
			adb.show();
		} else {
			LinearLayout scroll = (LinearLayout) findViewById(R.id.picsScroll);
			NewPostAttachment a = attachments.get(id);
			scroll.removeView(a.imageview);
			attachments.remove(a);
			for (NewPostAttachment at : attachments) {
				at.imageview.setTag(attachments.indexOf(at));
			}
		}
		return true;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		DobroHelper.updateCurrentTheme(this);
		DobroHelper.setOrientation(this);
		super.onCreate(savedInstanceState);
		setActionBarContentView(R.layout.new_post_view);
		if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(
				"show_format", false)) {
			Button b = (Button) findViewById(R.id.strikeoutButton);
			b.setPaintFlags(b.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
			findViewById(R.id.formatPanel).setVisibility(View.VISIBLE);
		}
		always_show_captcha = PreferenceManager.getDefaultSharedPreferences(
				DobroNewPostActivity.this).getBoolean("always_show_captcha",
				false);
		message_edit = (EditText) findViewById(R.id.message);
		title_edit = (EditText) findViewById(R.id.title);
		captcha_edit = (EditText) findViewById(R.id.captcha);
		name_edit = (EditText) findViewById(R.id.name);
		captcha_img = (AsyncImageView) findViewById(R.id.captcha_img);
		registerForContextMenu(captcha_img);
		Intent intent = getIntent();
		board = intent.getStringExtra(DobroConstants.BOARD);
		thread = intent.getStringExtra(DobroConstants.THREAD);
		post = intent.getStringExtra(DobroConstants.POST);
		String text = DobroQuoteHolder.getInstance().getAndClearQuote();
		if (post != null && !text.contains(">>" + post)) {
			if (text.length() > 0)
				text += "\n";
			text += ">>" + post + "\n";
		}
		message_edit.setText(text);
		if (!DobroHelper.checkSdcard()) {
			findViewById(R.id.addBooruButton).setEnabled(false);
			findViewById(R.id.addButton).setEnabled(false);
			findViewById(R.id.takePhoto).setEnabled(false);
		} else {
			findViewById(R.id.addBooruButton).setEnabled(true);
			findViewById(R.id.addButton).setEnabled(true);
			findViewById(R.id.takePhoto).setEnabled(true);
		}
		updateCaptchaImg();
		for (NewPostAttachment att : DobroQuoteHolder.getInstance()
				.getAndClearImages())
			addImage(att.fname, att.delete_after, att.rating);
	}

	public void onSendButtonClick(View v) {
		SharedPreferences prefs = DobroApplication.getApplicationStatic()
				.getDefaultPrefs();
		if (prefs.getBoolean("send_confirm", true)) {
			// не вынес Listener-ы, потому что, во-1, нельзя просто так взять и
			// вынести из 2 разных диалогов, и во-2, потому что пофиг на 1-2
			// строчники
			new AlertDialog.Builder(new ContextThemeWrapper(this,
					R.style.AlertDialogLight))
					.setMessage(R.string.send_confirm_dialog)
					.setCancelable(false)
					.setPositiveButton(android.R.string.yes,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int id) {
									dialog.dismiss();
									sendMessage();
								}
							})
					.setNegativeButton(android.R.string.no,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int id) {
									dialog.dismiss();
								}
							}).show();
		} else
			sendMessage();
	}

	private void sendMessage() {
		Button sendButton = (Button) findViewById(R.id.sendButton);
		sendButton.setEnabled(false);
		String charset = "UTF-8";
		List<Part> parts = new LinkedList<Part>();
		parts.add(new StringPart("message", message_edit.getText().toString(),
				charset));
		if (captcha_edit.isEnabled())
			parts.add(new StringPart("captcha", captcha_edit.getText()
					.toString(), charset));
		parts.add(new StringPart("thread_id", thread, charset));
		parts.add(new StringPart("task", "post", charset));
		parts.add(new StringPart("name", name_edit.getText().toString(),
				charset));
		parts.add(new StringPart("subject", title_edit.getText().toString(),
				charset));
		parts.add(new StringPart("new_post", "Отправить", charset));
		password = PreferenceManager.getDefaultSharedPreferences(
				DobroNewPostActivity.this).getString("password", "");
		if (TextUtils.equals(password, "")) {
			// если в SharedPrefs нет пароля - создаем новый и пихаем туда
			Log.e("Sending post", "Password is missing");
			Time now = new Time();
			now.setToNow();
			password = Md5Util.md5(now.toString());
			Editor ed = PreferenceManager.getDefaultSharedPreferences(
					DobroNewPostActivity.this).edit();
			ed.putString("password", password);
			ed.commit();

		}
		parts.add(new StringPart("password", password, charset));
		parts.add(new StringPart("post_files_count", String.valueOf(attachments
				.size()), charset));
		parts.add(new StringPart("goto", "thread", charset));
		for (int i = 0; i < 6; i++) {
			String file_key = String.format("file_%s", i + 1);
			String rating_key = String.format("file_%s_rating", i + 1);
			if (attachments.size() > i)
				try {
					parts.add(new FilePart(file_key, new File(attachments
							.get(i).fname)));
					parts.add(new StringPart(rating_key,
							attachments.get(i).rating, charset));
				} catch (FileNotFoundException e) {
					parts.add(new StringPart(file_key, "", charset));
					parts.add(new StringPart(rating_key, "SFW", charset));
				}
			else {
				parts.add(new StringPart(file_key, "", charset));
				parts.add(new StringPart(rating_key, "SFW", charset));
			}
		}
		if (((CheckBox) findViewById(R.id.sage)).isChecked())
			parts.add(new StringPart("sage", "on", charset));
		MultipartEntity entity = new MultipartEntity(parts.toArray(new Part[0]));
		new NewPoster().execute(entity);
	}

	protected void updateCaptchaImg() {
		String uri = String.format("http://dobrochan.ru/captcha/%s/%s.png",
				board, System.currentTimeMillis());
		new BitmapDownloader().execute(uri);
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		switch (which) {
		case DialogInterface.BUTTON_POSITIVE:
			System.out.println("Positive");
			View x = ((AlertDialog) dialog).findViewById(R.id.resizeX);
			View y = ((AlertDialog) dialog).findViewById(R.id.resizeY);
			int width = Integer.parseInt(((TextView) x).getText().toString());
			int height = Integer.parseInt(((TextView) y).getText().toString());
			NewPostAttachment a = attachments.get(id);
			new File(String.format(DobroConstants.TEMP_COMMON, Environment
					.getExternalStorageDirectory().getPath())).mkdirs();
			File f = new File(a.fname);
			String fname = f.getName();
			Bitmap out = null;
			try {
				out = Bitmap.createScaledBitmap(loadBitmap(a.fname, true),
						width, height, true);
			} catch (Exception e1) {
				dialog.dismiss();
				Toast.makeText(getApplicationContext(), e1.getMessage(),
						Toast.LENGTH_LONG);
				return;
			}
			String resize_output = String.format(DobroConstants.TEMP_FILE,
					Environment.getExternalStorageDirectory().getPath(),
					fname.substring(0, fname.lastIndexOf(".")) + "_resized",
					"jpg");
			FileOutputStream out_stream;
			try {
				out_stream = new FileOutputStream(resize_output);
				out.compress(Bitmap.CompressFormat.JPEG, 90, out_stream);
				out_stream.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				return;
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
			Bitmap b = loadBitmap(resize_output);
			if (b == null)
				return;
			a.imageview.setImageBitmap(b);
			a.fname = resize_output;
			a.delete_after = true;
			dialog.dismiss();
			break;

		case DialogInterface.BUTTON_NEGATIVE:
			System.out.println("negative");
			dialog.dismiss();
			break;
		}
	}
}