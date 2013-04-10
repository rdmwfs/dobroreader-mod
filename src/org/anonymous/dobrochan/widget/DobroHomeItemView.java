package org.anonymous.dobrochan.widget;

import org.anonymous.dobrochan.DobroHomeItem;
import org.anonymous.dobrochan.clear.R;

import greendroid.widget.AsyncImageView;
import greendroid.widget.itemview.ItemView;
import greendroid.widget.item.Item;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.TextView;

public class DobroHomeItemView extends LinearLayout implements ItemView {

    private TextView mTextView;
    private AsyncImageView mImageView;

    public DobroHomeItemView(Context context) {
        this(context, null);
    }

    public DobroHomeItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void prepareItemView() {
        mTextView = (TextView) findViewById(R.id.gd_text);
        mImageView = (AsyncImageView) findViewById(R.id.gd_drawable);
        mImageView.setImageURI(null);
        mImageView.setScaleType(ScaleType.FIT_START);
        mImageView.setDefaultImageResource(R.drawable.banner);
    }

    public void setObject(Item object) {
        final DobroHomeItem item = (DobroHomeItem) object;
        mTextView.setText(item.text);

        final String uri = item.image_uri;
        if (uri == null) {
            mImageView.setVisibility(View.GONE);
        } else {
            mImageView.setVisibility(View.VISIBLE);
        	mImageView.setUrl(uri);
        }
    }

}
