package org.anonymous.dobrochan.sqlite;

import java.util.LinkedList;
import java.util.List;

public class FakeCache implements IHiddenPosts, IThreadsCache,
		IThreadsInfoCache {

	@Override
	public void clearCache() {
	}

	@Override
	public void addThreadInfo(String adress, String data) {
	}

	@Override
	public String getThreadInfo(String adress) {
		return null;
	}

	@Override
	public void deleteThreadInfo(String adress) {
	}

	@Override
	public List<String> getAllThreads() {
		return new LinkedList<String>();
	}

	@Override
	public void addThread(String adress, String data) {
	}

	@Override
	public String getThreadData(String adress) {
		return null;
	}

	@Override
	public void deleteThread(String adress) {
	}

	@Override
	public void hidePost(String adress) {
	}

	@Override
	public void hidePost(String adress, String data) {
	}

	@Override
	public String getComment(String adress) {
		return null;
	}

	@Override
	public void unhidePost(String adress) {
	}

	@Override
	public void clearAll() {
	}

	@Override
	public boolean isFake() {
		return true;
	}

}
