package org.anonymous.dobrochan;

import android.graphics.Color;
import android.text.TextPaint;
import android.text.style.CharacterStyle;
import android.text.style.UpdateAppearance;
import android.view.View;

public class SpoilerSpan extends CharacterStyle implements UpdateAppearance {
	boolean clickable = false;
	boolean clicked = true;

	public SpoilerSpan() {
		super();
		clickable = !DobroApplication.getApplicationStatic().show_spoilers;
		if (clickable) {
			clicked = false;
		}
	}

	@Override
	public void updateDrawState(TextPaint arg0) {
		arg0.setColor(Color.GRAY);
		if (!clicked)
			arg0.bgColor = Color.GRAY;
	}

	public void onClick(View arg0) {
		if (clickable)
			clicked = !clicked;
	}

}
