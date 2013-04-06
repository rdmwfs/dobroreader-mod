package org.anonymous.dobrochan.activity;

import java.util.List;

public interface IPostsList {

	public abstract void openLinks(List<String> list);

	public abstract boolean openLink(CharSequence post);

}