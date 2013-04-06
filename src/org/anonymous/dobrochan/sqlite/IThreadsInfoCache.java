package org.anonymous.dobrochan.sqlite;

import java.util.List;

public interface IThreadsInfoCache {

	public abstract void addThreadInfo(String adress, String data);

	public abstract String getThreadInfo(String adress);

	public abstract void deleteThreadInfo(String adress);

	public abstract void clearCache();

	public abstract List<String> getAllThreads();

}