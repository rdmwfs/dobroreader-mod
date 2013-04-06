package org.anonymous.dobrochan;

import android.content.Context;
import android.text.Spannable;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class TextViewWithSpoilers extends TextView implements OnClickListener {

	public TextViewWithSpoilers(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
		handleSpoilers();
	}

	public TextViewWithSpoilers(Context context, AttributeSet attrs) {
		super(context, attrs);
		handleSpoilers();
	}

	public TextViewWithSpoilers(Context context) {
		super(context);
		handleSpoilers();
	}
	void handleSpoilers() {
		setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		if(DobroApplication.getApplicationStatic().show_spoilers)
			return;
		try{
			SpoilerSpan[] spans = ((Spannable) getText()).getSpans(0,
					getText().length(), SpoilerSpan.class);
			for(SpoilerSpan span : spans)
				span.onClick(this);
			invalidate();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
