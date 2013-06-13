package org.anonymous.dobrochan;

import greendroid.app.ActionBarActivity;

import org.anonymous.dobrochan.activity.DobroPostActivity;
import org.anonymous.dobrochan.activity.IPostsList;
import org.anonymous.dobrochan.json.DobroPost;

import android.content.Context;
import android.content.Intent;
import android.text.style.ClickableSpan;
import android.view.View;

public class DobroLinkSpan extends ClickableSpan {
	CharSequence board;
	CharSequence post;
	CharSequence thread = null;
	Context context;
	DobroPost parent;

	public DobroLinkSpan(CharSequence board, CharSequence thread,
			CharSequence post, Context context, DobroPost parent) {
		super();
		this.board = board;
		this.post = post;
		this.context = context;
		this.parent = parent;
		this.thread = thread;
	}

	public DobroLinkSpan(CharSequence board, CharSequence post,
			Context context, DobroPost parent) {
		super();
		this.board = board;
		this.post = post;
		this.context = context;
		this.parent = parent;
	}

	@Override
	public void onClick(View widget) {
		if (parent != null && parent.getLastContext() != null) {
			context = parent.getLastContext();
			if (this.board == null)
				this.board = parent.getBoardName();
		}
		try {
			IPostsList activity = (IPostsList) context;
			if (activity != null) {
				if (activity.openLink(post))
					return;
			}
		} catch (ClassCastException e) {
		}
		Intent i = new Intent(context, DobroPostActivity.class);
		i.putExtra(DobroConstants.BOARD, board);
		i.putExtra(DobroConstants.POST, post);
		i.putExtra(ActionBarActivity.GD_ACTION_BAR_TITLE,
				String.format(">>%s/%s", board, post));
		context.startActivity(i);
	}

}