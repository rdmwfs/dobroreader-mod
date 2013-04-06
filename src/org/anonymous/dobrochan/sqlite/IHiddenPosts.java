package org.anonymous.dobrochan.sqlite;

public interface IHiddenPosts {

	public abstract void hidePost(String adress);

	public abstract void hidePost(String adress, String data);

	public abstract String getComment(String adress);

	public abstract void unhidePost(String adress);

	public abstract void clearAll();

}