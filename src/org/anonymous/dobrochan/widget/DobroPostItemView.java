package org.anonymous.dobrochan.widget;

import greendroid.app.GDActivity;
import greendroid.widget.item.Item;
import greendroid.widget.itemview.ItemView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.anonymous.dobrochan.DobroApplication;
import org.anonymous.dobrochan.DobroConstants;
import org.anonymous.dobrochan.DobroNetwork;
import org.anonymous.dobrochan.DobroPostItem;
import org.anonymous.dobrochan.DobroQuoteHolder;
import org.anonymous.dobrochan.DobroTabsHolder;
import org.anonymous.dobrochan.R;
import org.anonymous.dobrochan.TextViewWithSpoilers;
import org.anonymous.dobrochan.activity.CopyPasteActivity;
import org.anonymous.dobrochan.activity.DobroNewPostActivity;
import org.anonymous.dobrochan.activity.DobroThreadActivity;
import org.anonymous.dobrochan.activity.IPostsList;
import org.anonymous.dobrochan.json.DobroFile;
import org.anonymous.dobrochan.json.DobroFile.Rating;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.LauncherActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.PreferenceManager;
import android.text.ClipboardManager;
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TextView.BufferType;
import android.widget.Toast;

public class DobroPostItemView extends RelativeLayout implements ItemView,
		View.OnCreateContextMenuListener {
	final static int QA_OPEN = 0;
	final static int QA_ANSWER = 1;
	final static int QA_QUOTE = 2;
	final static int QA_LAST = 3;
	final static int QA_FAV = 4;
	final static int QA_HIDE = 5;
	final static int QA_DELETE = 6;
	final static int QA_COPY = 7;

	String board;
	String thread;
	TextView metadataLeftView;
	TextView metadataRightView;
	TextViewWithSpoilers messageView;
	TextView titleView;
	TextView refsButton;
	TextView numberView;
	LinearLayout imagesLayout;
	LinearLayout metadataLayout;
	public DobroPostItem dobroitem = null;
	boolean to_hide = false;
	boolean to_star = false;
	boolean to_del = false;
	
	private int mImageHeight;
	
	public Context m_context = null;

	public DobroPostItemView(Context context) {
		this(context, null);
	}

	public DobroPostItemView(Context context, AttributeSet attrs) {
		super(context, attrs);
		setOnCreateContextMenuListener(this);
	}

	@Override
	public void prepareItemView() {
		messageView = (TextViewWithSpoilers) findViewById(R.id.messageView);
		SharedPreferences prefs = DobroApplication.getApplicationStatic().getDefaultPrefs();
		int font_size = Integer.parseInt(prefs.getString("font_size", "16"));
		if (font_size < 10)
			font_size = 10;
		messageView.setTextSize(TypedValue.COMPLEX_UNIT_SP, font_size);
		metadataLeftView = (TextView) findViewById(R.id.metadataLeftView);
		metadataRightView = (TextView) findViewById(R.id.metadataRightView);
		numberView = (TextView) findViewById(R.id.postNumber);
		imagesLayout = (LinearLayout) findViewById(R.id.imagesLayout);
		ViewGroup.LayoutParams p = imagesLayout.getLayoutParams();
		mImageHeight = Integer.parseInt(prefs.getString("images_size", "200"));
		if(mImageHeight <= 0 || mImageHeight > 200)
			mImageHeight = 200;
		p.height = mImageHeight;
		imagesLayout.setLayoutParams(p);
		titleView = (TextView) findViewById(R.id.titleView);
		metadataLayout = (LinearLayout) findViewById(R.id.metadataLayout);
//		refsView = (TextView) findViewById(R.id.refsView);
		refsButton = (TextView) findViewById(R.id.refsButton);
		refsButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				IPostsList activity = null;
				try{
				activity = (IPostsList)getContext();
				} catch (ClassCastException e) {
				}
				if(activity==null)
					return;
				activity.openLinks(dobroitem.post.getRefs());
			}
		});
		// QA
		messageView.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				DobroPostItemView.this.onShowBar(v);
				return true;
			}
		});

		metadataLayout.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				DobroPostItemView.this.onShowBar(v);
				return true;
			}
		});

		titleView.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				DobroPostItemView.this.onShowBar(v);
				return true;
			}
		});

		metadataLayout.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				DobroPostItemView.this.onAnyViewClick(v);
				if(!DobroApplication.getApplicationStatic().show_spoilers)
					messageView.performClick();
			}
		});

		titleView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				DobroPostItemView.this.onAnyViewClick(v);
			}
		});
	}

	public void onAnyViewClick(View v) {
		if (dobroitem.post.isOp() && !dobroitem.post.isIn_thread()) {
			Intent intent = new Intent(getContext(), DobroThreadActivity.class);
			if (dobroitem.post.getThreadDisplay_id() == null)
				intent.putExtra(DobroConstants.THREAD_ID,
						dobroitem.post.getThread_id());
			intent.putExtra(DobroConstants.BOARD,
					((DobroPostItem) dobroitem).post.getBoardName());
			intent.putExtra(DobroConstants.THREAD,
					((DobroPostItem) dobroitem).post.getThreadDisplay_id());
			if (((DobroPostItem) dobroitem).post.getThread() != null)
				intent.putExtra(GDActivity.GD_ACTION_BAR_TITLE,
						((DobroPostItem) dobroitem).post.getThread().getTitle());
			else
				intent.putExtra(GDActivity.GD_ACTION_BAR_TITLE,
						((DobroPostItem) dobroitem).post.getMessage());
			getContext().startActivity(intent);
		} else {
			/*
			Intent intent = new Intent(getContext(), DobroNewPostActivity.class);
			intent.putExtra(DobroConstants.BOARD, dobroitem.post.getBoardName());
			intent.putExtra(DobroConstants.THREAD,
					dobroitem.post.getThreadDisplay_id());
			intent.putExtra(DobroConstants.POST, dobroitem.post.getDisplay_id());
			intent.putExtra(
					GDActivity.GD_ACTION_BAR_TITLE,
					String.format("Ответ в >>%s/%s",
							dobroitem.post.getBoardName(),
							dobroitem.post.getThreadDisplay_id()));
			getContext().startActivity(intent);
		*/
		}
	}

	@Override
	public void setObject(Item item) {
		try {
			dobroitem = (DobroPostItem) item;
		} catch (ClassCastException e) {
			e.printStackTrace();
			return;
		}
		if (dobroitem == null || dobroitem.post == null)
			return;
		dobroitem.post.setLastContext(getContext());
		this.board = dobroitem.post.getBoardName();
		this.thread = dobroitem.post.getThreadDisplay_id();
		((HorizontalScrollView) findViewById(R.id.horizScroll)).scrollTo(0, 0);

		CharSequence txt = dobroitem.post.getFormattedText();
		if (txt.length() > 800) {
			Pattern p1 = Pattern.compile("\\.|\n");
			Matcher m1 = p1.matcher(txt);
			Pattern p2 = Pattern.compile("\\b");
			Matcher m2 = p2.matcher(txt);
			if (m1.find(500) && m1.end() < 700)
				txt = txt.subSequence(0, m1.end());
			else if (m2.find(500) && m2.start() < 700)
				txt = txt.subSequence(0, m2.start());
			else
				txt = txt.subSequence(0, 500);
			messageView.setText(txt, BufferType.SPANNABLE);
			SpannableStringBuilder builder = new SpannableStringBuilder(
					"\nСообщение слишком длинное. Полная версия");
			builder.setSpan(new ClickableSpan() {
				@Override
				public void onClick(View widget) {
					try{
						messageView.setText(dobroitem.post.getFormattedText(),
								BufferType.SPANNABLE);
					} catch (IndexOutOfBoundsException e) {
						
					}
				}
			}, 1, builder.length(), 0);
			messageView.append(builder.subSequence(0, builder.length()));
		} else
			messageView.setText(txt, BufferType.SPANNABLE);
		// FIXME
		messageView.setMovementMethod(LinkMovementMethod.getInstance());
		messageView.setClickable(false);
		messageView.setFocusable(false);
		messageView.setFocusableInTouchMode(false);
		/*
		refsView.setMovementMethod(LinkMovementMethod.getInstance());
		refsView.setClickable(false);
		refsView.setFocusable(false);
		refsView.setFocusableInTouchMode(false);
		*/
		//
		imagesLayout.removeAllViewsInLayout();
		if (dobroitem.post.isOp()) {
			numberView.setVisibility(GONE);
			titleView
					.setText(dobroitem.post.getSubject().length() == 0 ? getContext()
							.getString(R.string.untitled) : dobroitem.post
							.getSubject());
			titleView.setVisibility(VISIBLE);
		} else {
			int pos = dobroitem.post.getNumber();
			if(pos > 0) {
				numberView.setVisibility(VISIBLE);
				numberView.setText(String.valueOf(pos));
			} else
				numberView.setVisibility(GONE);
			titleView.setText("");
			titleView.setVisibility(GONE);
		}
		

		TypedValue backgroundRef = new TypedValue();
		getContext().getTheme().resolveAttribute(R.attr.dcPicrelatedColor, backgroundRef, true);
		int backgroundColor = getContext().getResources().getColor(backgroundRef.resourceId);
		SharedPreferences prefs = DobroApplication.getApplicationStatic().getDefaultPrefs();
		boolean show_info = prefs.getBoolean("show_fileinfo", true);
		boolean force_op_load = prefs.getBoolean("op_pictures_force", false);
		
		if (dobroitem.post.getFiles().length > 0) {
			for (DobroFile file : dobroitem.post.getFiles()) {
				CachedAsyncImageView imageView = new CachedAsyncImageView(
						getContext());
				VerticalTextView textView = null;
				String fname1 = file.getThumb().substring(file.getThumb().lastIndexOf("/")+1);
				int e = fname1.lastIndexOf("s.");
				if(e>0)
					fname1 = fname1.substring(0, e)+file.getSrc().substring(file.getSrc().lastIndexOf("."));
				String fname2 = file.getSrc().substring(file.getSrc().lastIndexOf("/")+1);
				String size = humanReadableByteCount(file.getSize(), true);
				String date = getThumbnailDate(file.getThumb());
				if(date == null) try{
					date = file.getSrc().split("/")[2];
					date = date.substring(2)+"."+date.substring(0, 2);
				} catch (Exception ex) {
					date = "?";
				}
				imageView.setInfo(getContext().getString(R.string.file_info_parretn, fname1,fname2,size,date,file.getRating(),
						getMetadataText(file.getMetadata())));
				if(file.getMetadata().width != null)
					imageView.setSize(file.getMetadata().height, file.getMetadata().width);
				if(show_info)
				{
					textView = new VerticalTextView(getContext(), null);
					textView.setLinkedImageView(imageView);
					String text = "";
					if(dobroitem.post.getBoardName().equals("b") ||
							dobroitem.post.getBoardName().equals("rf"))
						text += fname1;
					else
						text += fname2;
					text += "\n"+size;
					if(file.getMetadata() != null &&
							file.getMetadata().width != null &&
							file.getMetadata().height != null)
					{
						text += ", "+
								String.valueOf(file.getMetadata().width)
								+"x"+
								String.valueOf(file.getMetadata().height);
					}
					textView.setText(text);
					textView.setGravity(Gravity.BOTTOM);
					textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
				}
				imageView.setRating(file.getRat());
				imageView.setForceLoad(force_op_load && dobroitem.post.isOp());
				imageView.setUrl(file.getThumb());
				final String urlTag = file.getSrc();
				imageView.setTag(urlTag);
				imageView.setPadding(2, 2, 2, 2);
				imageView.setAdjustViewBounds(true);
				imageView.setMaxHeight(mImageHeight);
				imageView.setMaxWidth(mImageHeight);
				imageView.setBackgroundColor(backgroundColor);
				imageView.setScaleType(ScaleType.CENTER);
				imageView.setOnCreateContextMenuListener(imageView);
				float h = mImageHeight;
				int w = Math
						.min(Math.round((h / (float) file.getThumb_height())
								* file.getThumb_width()), file.getThumb_width());
				imageView.setLayoutParams(new LayoutParams(w+4, mImageHeight+4));
				if(show_info)
				{
					textView.setWidth(mImageHeight+4);
					textView.setLinkedImageView(imageView);
					imagesLayout.addView(textView);
				}
				imagesLayout.addView(imageView);
			}
			imagesLayout.setVisibility(VISIBLE);
		} else {
			imagesLayout.setVisibility(GONE);
		}
		if(prefs.getBoolean("youtube", true))
		{
			Spanned s = null;
			boolean cast_ok = false;
			try {
				s = (Spanned) dobroitem.post.getFormattedText();
				cast_ok = true;
			} catch (ClassCastException e) {
				cast_ok = false;
			}
			if(cast_ok)
			{
			URLSpan[] spans = s.getSpans(0, s.length(), URLSpan.class);
			for(URLSpan span : spans) {
				String url = span.getURL();
				Pattern p = Pattern.compile("(youtube\\.com/watch\\?v=|youtu\\.be/)(\\S{11})");
				Matcher m = p.matcher(url);
				if(!m.find())
					continue;
				String yt_id = m.group(2);
				String text = s.subSequence(s.getSpanStart(span), s.getSpanEnd(span)).toString();
				CachedAsyncImageView imageView = new CachedAsyncImageView(getContext());
				imageView.setLayoutParams(new LayoutParams((int) ((mImageHeight+4)/0.75), mImageHeight+4));
				
				imageView.setRating(Rating.SWF);
				imageView.setForceLoad(false);
				imageView.setTag(url);
				imageView.setPadding(2, 2, 2, 2);
				imageView.setAdjustViewBounds(true);
				imageView.setMaxHeight(mImageHeight);
				imageView.setMaxWidth((int) ((mImageHeight+4)/0.75));

				imageView.setUrl("http://img.youtube.com/vi/"+yt_id+"/0.jpg");
				
				imageView.setBackgroundColor(backgroundColor);
				imageView.setScaleType(ScaleType.CENTER);
				
				VerticalTextView textView = new VerticalTextView(getContext(), null);
				textView.setGravity(Gravity.BOTTOM);
				textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
				textView.setText(text);
				textView.setLinkedImageView(imageView);
				textView.setWidth(mImageHeight+4);
				
				if(show_info)
					imagesLayout.addView(textView);
				imagesLayout.addView(imageView);
				imagesLayout.setVisibility(VISIBLE);
			}
			}
		}
		metadataRightView.setText(getContext().getString(
						R.string.post_date,
						String.valueOf(dobroitem.post.getDate().replace(' ',
								'\n'))));
		metadataLeftView.setText(dobroitem.post.getName()+
				"\n"+
				getContext().getString(R.string.post_id,
				String.valueOf(dobroitem.post.getDisplay_id())));
		List<String> refs = dobroitem.post.getRefs();
		if (!refs.isEmpty()) {
			/*
			CharSequence seq = getContext().getText(R.string.answers);
			SpannableStringBuilder s = new SpannableStringBuilder(seq);
			s.setSpan(new StyleSpan(Typeface.BOLD_ITALIC), 0, seq.length(), 0);
			s.append(" ");
			for (String ref : refs) {
				int start = s.length();
				s.append(ref + ", ");
				s.setSpan(
						new DobroLinkSpan(dobroitem.post.getBoardName(), ref
								.substring(2), getContext(), null), start, start
								+ ref.length(), 0);
			}
			refsView.setText(s.subSequence(0, s.length() - 2),
					BufferType.SPANNABLE);
			 */
			refsButton.setText(String.format("/%d",refs.size()));
			refsButton.setVisibility(View.VISIBLE);
		} else
			refsButton.setVisibility(View.GONE);
		checkSpells();
	}
	
	private void checkSpells() {
		if(dobroitem.post.isHidden())
			hidePost(dobroitem.post.getHideComment());
	}
	private boolean hidden = false;
	public void hidePost(String reason) {
		hidden = true;
		imagesLayout.setVisibility(View.GONE);
		SpannableStringBuilder builder = new SpannableStringBuilder(
				"\nСообщение скрыто: "+reason+"\n");
		builder.setSpan(new ClickableSpan() {
			@Override
			public void onClick(View widget) {
				unhidePost();
			}
		}, 1, builder.length(), 0);
		messageView.setText(builder.subSequence(0, builder.length()));
		return;
	}
	
	public void unhidePost() {
		hidden = false;
		messageView.setText(dobroitem.post.getFormattedText(),
				BufferType.SPANNABLE);
		if(imagesLayout.getChildCount() > 0)
			imagesLayout.setVisibility(View.VISIBLE);
	}

	public void onMenuOpenClicked() {
			Intent intent = new Intent(getContext(), DobroThreadActivity.class);
			if (dobroitem.post.getThreadDisplay_id() == null)
				intent.putExtra(DobroConstants.THREAD_ID,
						dobroitem.post.getThread_id());
			intent.putExtra(DobroConstants.BOARD, dobroitem.post.getBoardName());
			intent.putExtra(DobroConstants.THREAD,
					dobroitem.post.getThreadDisplay_id()==null?
							dobroitem.post.getDisplay_id():
							dobroitem.post.getThreadDisplay_id());
			if (dobroitem.post.getThread() != null)
				intent.putExtra(GDActivity.GD_ACTION_BAR_TITLE, dobroitem.post
						.getThread().getTitle());
			else
				intent.putExtra(GDActivity.GD_ACTION_BAR_TITLE,
						dobroitem.post.getMessage());
			getContext().startActivity(intent);
		}

		public void onMenulastClicked() {
			SharedPreferences prefs = DobroApplication.getApplicationStatic().getDefaultPrefs();
			Intent intent = new Intent(getContext(), DobroThreadActivity.class);
			if (dobroitem.post.getThreadDisplay_id() == null)
				intent.putExtra(DobroConstants.THREAD_ID,
						dobroitem.post.getThread_id());
			intent.putExtra(DobroConstants.BOARD,
					((DobroPostItem) dobroitem).post.getBoardName());
			intent.putExtra(DobroConstants.THREAD,
					((DobroPostItem) dobroitem).post.getThreadDisplay_id());
			intent.putExtra(DobroConstants.COUNT,
					prefs.getString("last_count", "50"));
			if (((DobroPostItem) dobroitem).post.getThread() != null)
				intent.putExtra(GDActivity.GD_ACTION_BAR_TITLE,
						((DobroPostItem) dobroitem).post.getThread().getTitle());
			else
				intent.putExtra(GDActivity.GD_ACTION_BAR_TITLE,
						((DobroPostItem) dobroitem).post.getMessage());
			getContext().startActivity(intent);
		}

		public void onMenuQuoteClicked() {
			String q = ">>" + dobroitem.post.getDisplay_id() + "\n";
			q += (dobroitem.post.getMessage()==null?dobroitem.post.getFormattedText().toString():dobroitem.post.getMessage());
			DobroQuoteHolder.getInstance().addQuote(q);
			Toast.makeText(getContext(), R.string.quoted, Toast.LENGTH_SHORT).show();
		}

		public void onMenuHideClicked() {
			to_hide = true;
			new ThreadManager().execute();
		}

		public void onMenuAnswerClicked() {
			Intent intent = new Intent(getContext(), DobroNewPostActivity.class);
			intent.putExtra(DobroConstants.BOARD, dobroitem.post.getBoardName());
			intent.putExtra(DobroConstants.THREAD,
					dobroitem.post.getThreadDisplay_id());
			intent.putExtra(DobroConstants.POST, dobroitem.post.getDisplay_id());
			intent.putExtra(
					GDActivity.GD_ACTION_BAR_TITLE,
					String.format("Ответ в >>%s/%s",
							dobroitem.post.getBoardName(),
							dobroitem.post.getThreadDisplay_id()));
			if(getContext() instanceof DobroThreadActivity) {
				Activity parent = (DobroThreadActivity)getContext();
				parent.startActivityForResult(intent, DobroConstants.ANSWER_RESULT);
			} else {
				getContext().startActivity(intent);
			}
		}

		public void onMenuFavClicked() {
			to_star = true;
			new ThreadManager().execute();
		}

		public void onMenuDelClicked() {
			to_del = true;
			new ThreadManager().execute();
		}

		@SuppressLint("NewApi")
		public void onMenuCopyClicked() {
			String text = dobroitem.post.getMessage()==null?dobroitem.post.getFormattedText().toString():dobroitem.post.getMessage();
			/*
			Intent i = new Intent(getContext(), CopyPasteActivity.class);
			i.putExtra("text", );
			i.putExtra(GDActivity.GD_ACTION_BAR_TITLE, "Скопируйте текст");
			getContext().startActivity(i);
			*/
			View v = null;
            if (Build.VERSION.SDK_INT < 11)
            {
                    EditText edit = new EditText(getContext());
                    edit.setPadding(10, 10, 10, 10);
                    edit.setGravity(Gravity.TOP);
                    edit.setTextColor(Color.BLACK);
                    edit.setBackgroundColor(Color.WHITE);
                    edit.setInputType(InputType.TYPE_NULL);
                    edit.setHorizontallyScrolling(false);
                    edit.setSingleLine(false);
                    edit.setText(text);
                    v = edit;
            } else {
            	SharedPreferences prefs = DobroApplication.getApplicationStatic().getDefaultPrefs();
    			int font_size = 10;
    			try{
    				font_size = Integer.parseInt(prefs.getString("font_size", "16"));
    			} catch (Exception e){}
    			if (font_size < 10)
    				font_size = 10;
    			
                    TextView edit = new TextView(getContext());
                    edit.setPadding(10, 10, 10, 10);
                    edit.setTextSize(font_size);
                    edit.setTextColor(Color.BLACK);
                    edit.setBackgroundColor(Color.WHITE);
                    edit.setFocusable(true);
                    edit.setTextIsSelectable(true);
                    edit.setText(text);
                    v = edit;
            }
            Dialog d = new Dialog(getContext(), android.R.style.Theme_NoTitleBar_Fullscreen);
            d.requestWindowFeature(Window.FEATURE_NO_TITLE);
            d.setCanceledOnTouchOutside(true);
            d.setContentView(v);
            d.show();
		}

	public void onShowBar(View v) {
		this.showContextMenu();
	}

	private class ThreadManager extends AsyncTask<Void, Void, Void> {
		@Override
		protected Void doInBackground(Void... params) {
			if (to_hide)
				DobroNetwork.getInstance().hideThread(board, thread);
			if (to_star)
				DobroNetwork.getInstance().starThread(board, thread);
			if (to_del)
				DobroNetwork.getInstance().delPost(board, dobroitem.post.getThread_id(), dobroitem.post.getDisplay_id());
			to_hide = false;
			to_star = false;
			to_del  = false;
			return null;
		}
	}
	
	public static String humanReadableByteCount(long bytes, boolean si) {
	    int unit = si ? 1000 : 1024;
	    if (bytes < unit) return bytes + " B";
	    int exp = (int) (Math.log(bytes) / Math.log(unit));
	    String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
	    return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		if(menu.hasVisibleItems())
			return;
		boolean show_open = false;
		try {
			@SuppressWarnings("unused")
			DobroThreadActivity a = (DobroThreadActivity) getContext();
		} catch (ClassCastException e) {
			show_open = true;
		}
		if(show_open)
		menu.add(R.string.open).setIcon(R.drawable.eye_32x24)
		.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem arg0) {
				DobroPostItemView.this.onMenuOpenClicked();
				return true;
			}
		});
		menu.add(R.string.answer).setIcon(R.drawable.mail_32x24)
		.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem arg0) {
				DobroPostItemView.this.onMenuAnswerClicked();
				return true;
			}
		});
		menu.add(R.string.quote).setIcon(R.drawable.right_quote_32x24)
		.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem arg0) {
				DobroPostItemView.this.onMenuQuoteClicked();
				return true;
			}
		});
		if(show_open)
		menu.add(R.string.last_50).setIcon(R.drawable.chat_32x32)
		.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem arg0) {
				DobroPostItemView.this.onMenulastClicked();
				return true;
			}
		});
		if(show_open)
			menu.add("Открыть в фоне").setIcon(R.drawable.chat_32x32)
			.setOnMenuItemClickListener(new OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem arg0) {
					DobroTabsHolder.getInstance().addTab(board, thread, null, dobroitem.post.getThread().getTitle(), null);
					new Thread(new Runnable() {
				        public void run() {
				        	DobroNetwork.getInstance().getThreadJson(String.format("%s/%s", board, thread), null, null, false);
				        }
				    }).start();
					return true;
				}
			});
		menu.add(R.string.fav).setIcon(R.drawable.heart_fill_32x38)
		.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem arg0) {
				DobroPostItemView.this.onMenuFavClicked();
				return true;
			}
		});
		menu.add(R.string.hide_thread).setIcon(R.drawable.minus_alt_32x32)
		.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem arg0) {
				DobroPostItemView.this.onMenuHideClicked();
				return true;
			}
		});
		menu.add(R.string.delete).setIcon(R.drawable.x_28x28)
		.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem arg0) {
				DobroPostItemView.this.onMenuDelClicked();
				return true;
			}
		});
		menu.add("Выделить").setIcon(R.drawable.right_quote_32x24)
		.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem arg0) {
				DobroPostItemView.this.onMenuCopyClicked();
				return true;
			}
		});
		menu.add("Копировать ссылку").setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				ClipboardManager clipboard = 
				(ClipboardManager)getContext().getSystemService(Context.CLIPBOARD_SERVICE);
				String text = DobroConstants.HOST+board+"/res/"+thread+".xhtml#i"+dobroitem.post.getDisplay_id();
				clipboard.setText(text);
				Toast.makeText(getContext(), text, Toast.LENGTH_SHORT).show();
				return true;
			}
		});
		if(!hidden)
		menu.add("Скрыть пост").setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				DobroApplication.getApplicationStatic().getHiddenPosts().hidePost(dobroitem.post.getBoardName()+"/"+dobroitem.post.getDisplay_id());
				dobroitem.post.setHidden(true);
				dobroitem.post.setHideComment("вручную");
				hidePost("вручную");
				return true;
			}
		});
		else
			menu.add("Раскрыть пост").setOnMenuItemClickListener(new OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item) {
					DobroApplication.getApplicationStatic().getHiddenPosts().hidePost(dobroitem.post.getBoardName()+"/"+dobroitem.post.getDisplay_id(),"unhide");
					unhidePost();
					dobroitem.post.setHidden(false);
					return true;
				}
			});
	}
	
	private String getMetadataText(DobroFile.Metadata file_metadata) {
		String metadata = "";
		if(file_metadata.width != null)
			metadata += String.format("Width: %s\n",file_metadata.width);
		if(file_metadata.height != null)
			metadata += String.format("Height: %s\n",file_metadata.height);
		if(file_metadata.type != null)
			metadata += String.format("Type: %s\n",file_metadata.type);
		if(file_metadata.files_count != null)
			metadata += String.format("Files count: %s\n",file_metadata.files_count);
		if(file_metadata.files != null) {
			metadata += "Files:";
			for(String f : file_metadata.files)
				metadata += String.format("\n %s",f);
			metadata += "\n";
		}
		if(file_metadata.title != null)
			metadata += String.format("Title: %s\n",file_metadata.title);
		if(file_metadata.artist != null)
			metadata += String.format("Artist: %s\n",file_metadata.artist);
		if(file_metadata.album != null)
			metadata += String.format("Album: %s\n",file_metadata.album);
		if(file_metadata.length != null)
			metadata += String.format("Length: %s\n",file_metadata.length);
		if(file_metadata.bitrate != null)
			metadata += String.format("Bitrate: %s\n",file_metadata.bitrate);
		if(file_metadata.secured != null)
			metadata += String.format("Secured: %s\n",file_metadata.secured);
		if(file_metadata.lines != null)
			metadata += String.format("Lines: %s\n",file_metadata.lines);
		if(file_metadata.sample_rate != null)
			metadata += String.format("Sample rate: %s\n",file_metadata.sample_rate);
		if(file_metadata.totaltracks != null)
			metadata += String.format("Total tracks: %s\n",file_metadata.totaltracks);
		if(file_metadata.tracknumber != null)
			metadata += String.format("Track number: %s\n",file_metadata.tracknumber);
		return metadata;
	}
	
	private String getThumbnailDate(String thumb_name) {
		try{
			int l = thumb_name.lastIndexOf("/");
			Integer i = Integer.parseInt(thumb_name.substring(l+1, l+11));
			java.util.Date time=new java.util.Date((long)i*1000);
			DateFormat fm = DateFormat.getDateTimeInstance();
			try{
				SimpleDateFormat sdf = (SimpleDateFormat) fm;
				sdf.setTimeZone(TimeZone.getTimeZone("Europe/Moscow"));
				fm = sdf;
			} catch (Exception e) {
			}
			return fm.format(time);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}
