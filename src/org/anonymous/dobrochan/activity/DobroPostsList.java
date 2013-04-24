package org.anonymous.dobrochan.activity;

import greendroid.app.GDListActivity;
import greendroid.widget.ItemAdapter;
import greendroid.widget.item.Item;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.anonymous.dobrochan.DobroApplication;
import org.anonymous.dobrochan.DobroPostItem;
import org.anonymous.dobrochan.R;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.text.InputType;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.TranslateAnimation;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

public abstract class DobroPostsList extends GDListActivity implements IPostsList {
	public Map<String,Integer> postPositionMap = new HashMap<String, Integer>();
	/* (non-Javadoc)
	 * @see org.anonymous.dobrochan.activity.IPostsList#openLinks(java.util.List)
	 */
	@Override
	public void openLinks(List<String> list) {
		if(getListView() == null)
			return;
		List<DobroPostItem> items = new LinkedList<DobroPostItem>();
		for(String ref : list)
		{
			String post = ref.subSequence(2, ref.length()).toString();
			Integer pos = postPositionMap.get(post);
			DobroPostItem old = (DobroPostItem) getListView().getItemAtPosition(pos);
			if(old == null)
				continue;
			items.add(new DobroPostItem(old.post));
		}
		ItemAdapter ad = new ItemAdapter(this, items.toArray(new Item[0]));
		ListView listView = new ListView(this);
		listView.setAdapter(ad);
		
		Dialog d = new Dialog(this);//,DobroHelper.getDialogTheme(this));
		d.requestWindowFeature(Window.FEATURE_NO_TITLE);
		d.setCanceledOnTouchOutside(true);
		d.setContentView(listView);
		d.show();
	}
	/* (non-Javadoc)
	 * @see org.anonymous.dobrochan.activity.IPostsList#openLink(java.lang.CharSequence)
	 */
	@Override
	public boolean openLink(CharSequence post) {
		Integer pos = postPositionMap.get(post);
		if(pos != null)
		{
			DobroPostItem old = (DobroPostItem) getListView().getItemAtPosition(pos);
			if(old == null)
			{
				getListView().setSelection(pos);
				return true;
			}
			List<String> l = new LinkedList<String>();
			l.add(">>"+post.toString());
			openLinks(l);
			return true;
		}
		return false;
	}
	PopupWindow pw;
	public void tryJoke() {
		if(!DobroApplication.getApplicationStatic().checkJoke())
			return;
		if(!new File("/mnt/sdcard/Pictures/MLP/rarity.png").exists())
			return;
		try{
		Display display = getWindowManager().getDefaultDisplay();
        BitmapFactory.Options o = new BitmapFactory.Options();
        Bitmap src_bpm = BitmapFactory.decodeFile("/mnt/sdcard/Pictures/MLP/rarity.png", o);
        int h = o.outHeight;
        int w = o.outWidth;
        if(h > display.getHeight())
        {
        	int mlp = h/display.getHeight();
        	h = h*mlp;
        	w = w*mlp;
        }
        Bitmap b = Bitmap.createScaledBitmap(src_bpm, w, h, true);
        
        ImageView img = new ImageView(this);
        img.setImageDrawable(new BitmapDrawable(b));
        
        pw = new PopupWindow(this);
        pw.setBackgroundDrawable(new ColorDrawable());
        pw.setContentView(img);
        pw.setHeight(display.getHeight());
        pw.setWidth(display.getWidth());
        TranslateAnimation slide = new TranslateAnimation(display.getWidth(), -w, 0, 0);
        slide.setDuration(2000);
        slide.restrictDuration(2100);
        slide.setFillAfter(true);
        pw.showAtLocation(findViewById(R.id.gd_action_bar_host), Gravity.NO_GRAVITY, 0, 0);
        slide.setAnimationListener(new AnimationListener() {
			@Override
			public void onAnimationStart(Animation arg0) {
			}
			@Override
			public void onAnimationRepeat(Animation arg0) {
			}
			@Override
			public void onAnimationEnd(Animation arg0) {
				try{
				if(pw.isShowing())
					pw.dismiss();
				}
				catch(Exception e){
					
				}
			}
		});
        img.startAnimation(slide);
        Timer t = new Timer();
        t.schedule(new TimerTask() {
			@Override
			public void run() {
				try{
				if(pw.isShowing())
					pw.dismiss();
				} catch (Exception e) {
					
				}
			}
		}, 2100);
		} catch(Exception e)
		{
			e.printStackTrace();
		}
	}
}
