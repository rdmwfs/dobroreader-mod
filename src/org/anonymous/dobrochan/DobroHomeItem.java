package org.anonymous.dobrochan;

import greendroid.widget.item.TextItem;
import greendroid.widget.itemview.ItemView;

import android.content.Context;
import android.view.ViewGroup;

public class DobroHomeItem extends TextItem {
	public String image_uri;
    public DobroHomeItem() {
        this(null);
    }

    public DobroHomeItem(String text) {
        this(text, null);
    }

    public DobroHomeItem(String text, String uri) {
        super(text);
        this.image_uri = uri;
    }

    @Override
    public ItemView newView(Context context, ViewGroup parent) {
        return createCellFromXml(context, R.layout.home_item_view, parent);
    }

}
