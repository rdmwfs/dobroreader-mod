package org.anonymous.dobrochan.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.TextView;

public class VerticalTextView extends TextView
implements OnClickListener, OnLongClickListener{
   private CachedAsyncImageView mLinkedImageView;
   

   public VerticalTextView(Context context, AttributeSet attrs){
      super(context, attrs);
      final int gravity = getGravity();
         setGravity((gravity&Gravity.HORIZONTAL_GRAVITY_MASK) | Gravity.TOP);
         setLongClickable(true);
         setClickable(true);
         setOnClickListener(this);
         setOnLongClickListener(this);
   }

   @Override
   protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec){
      super.onMeasure(heightMeasureSpec, widthMeasureSpec);
      setMeasuredDimension(getMeasuredHeight(), getMeasuredWidth());
   }

   @Override
   protected void onDraw(Canvas canvas){
      TextPaint textPaint = getPaint(); 
      textPaint.setColor(getCurrentTextColor());
      textPaint.drawableState = getDrawableState();

      canvas.save();

      canvas.translate(0, getHeight());
      canvas.rotate(-90);

//      canvas.translate(getCompoundPaddingLeft()+2, getExtendedPaddingTop());
      getLayout().draw(canvas);
      canvas.restore();
  }

@Override
public void onClick(View v) {
	if(mLinkedImageView != null)
		mLinkedImageView.performClick();
}
public void setLinkedImageView(CachedAsyncImageView view) {
	mLinkedImageView = view;
}

@Override
public boolean onLongClick(View arg0) {
	if(mLinkedImageView != null)
	{
		mLinkedImageView.performLongClick();
		return true;
	}
	return false;
}
}