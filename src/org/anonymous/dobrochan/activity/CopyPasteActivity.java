package org.anonymous.dobrochan.activity;

import org.anonymous.dobrochan.DobroApplication;
import org.anonymous.dobrochan.DobroHelper;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import greendroid.app.GDActivity;

public class CopyPasteActivity extends Activity {

	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		DobroHelper.updateCurrentTheme(this);
		DobroHelper.setOrientation(this);
		super.onCreate(savedInstanceState);
		String text = getIntent().getStringExtra("text");
		if(Build.VERSION.SDK_INT < 11) {
			//XXX SDK_INT < 11
			EditText te = new EditText(this);
			te.setGravity(Gravity.TOP);
			getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
			te.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
			te.setText(text);
			setContentView(te);
		} else {
			SharedPreferences prefs = DobroApplication.getApplicationStatic().getDefaultPrefs();
			int font_size = 10;
			try{
				font_size = Integer.parseInt(prefs.getString("message_font_size", "16"));
			} catch (Exception e){}
			if (font_size < 10)
				font_size = 10;
			
			TextView tv = new TextView(this);
			tv.setPadding(10, 5, 10, 5);
			tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, font_size);
			tv.setGravity(Gravity.TOP);
			tv.setFocusable(true);
			tv.setTextIsSelectable(true);
			tv.setText(text);
			tv.setVerticalScrollBarEnabled(true);
			setContentView(tv);
			tv.requestFocus();
		}
	}

}
