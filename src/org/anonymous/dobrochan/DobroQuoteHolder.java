package org.anonymous.dobrochan;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.anonymous.dobrochan.activity.DobroNewPostActivity.NewPostAttachment;

public class DobroQuoteHolder extends Object {
	private String quote = "";
	private List<NewPostAttachment> images = new LinkedList<NewPostAttachment>();

	public static DobroQuoteHolder getInstance() {
		DobroApplication app = DobroApplication.getApplicationStatic();
		return app.getQuoter();
	}

	public String getAndClearQuote() {
		String result = quote.substring(0);
		quote = "";
		return result;
	}

	public void addQuote(String text) {
		if (quote.length() != 0)
			quote += "\n";
		quote += text.replace("\n", "\n>");
	}

	public void setText(String text) {
		quote = text.substring(0);
	}

	public void setPictures(List<NewPostAttachment> images) {
		this.images.clear();
		this.images.addAll(images);
	}

	public List<NewPostAttachment> getAndClearImages() {
		List<NewPostAttachment> temp = new ArrayList<NewPostAttachment>(
				images.size());
		temp.addAll(images);
		this.images.clear();
		return temp;
	}
}
