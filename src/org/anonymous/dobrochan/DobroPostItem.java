package org.anonymous.dobrochan;

import java.io.IOException;

import org.anonymous.dobrochan.json.DobroPost;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.ViewGroup;
import greendroid.widget.item.Item;
import greendroid.widget.itemview.ItemView;

public class DobroPostItem extends Item {
	public DobroPost post;
	public DobroPostItem(DobroPost p) {
		super();
		this.post = p;
	}
	@Override
	public ItemView newView(Context context, ViewGroup parent) {
		return createCellFromXml(context, R.layout.post_item_view, parent);
	}

	@Override
	public void inflate(Resources r, XmlPullParser parser, AttributeSet attrs)
			throws XmlPullParserException, IOException {
		super.inflate(r, parser, attrs);
	}

}
