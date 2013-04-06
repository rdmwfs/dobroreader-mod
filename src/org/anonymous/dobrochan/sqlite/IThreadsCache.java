package org.anonymous.dobrochan.sqlite;

public interface IThreadsCache {

	public abstract void addThread(String adress, String data);

	public abstract String getThreadData(String adress);

	public abstract void deleteThread(String adress);

	public abstract void clearCache();

	public abstract boolean isFake();

}