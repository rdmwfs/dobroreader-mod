package org.anonymous.dobrochan.json;

import org.anonymous.dobrochan.DobroConstants;

import android.text.TextUtils;

import com.google.gson.annotations.SerializedName;

public class DobroThread implements Comparable<DobroThread> {
	private String __class__;
	private boolean autosage;
	private String last_hit;
	private String display_id;
	private String thread_id;
	private String last_modified;
	private Integer posts_count;
	private Integer files_count;
	private int board_id;
	private boolean archived;
	private String title;
	private DobroPost[] posts;
	private transient DobroBoard board;
	private String board_name;
	// only for starred threads
	private String last_viewed;
	private String level;
	private Integer unread;
	private String last_post_id;
	// only from cached thread
	private transient boolean from_cache;
	@SerializedName(DobroConstants.THREAD_MODIFIED)
	private boolean ThreadModified;
	@SerializedName(DobroConstants.LAST_MOD)
	private String LastModifiedHeader;

	public String get__class__() {
		return __class__;
	}

	public void set__class__(String __class__) {
		this.__class__ = __class__;
	}

	public boolean isAutosage() {
		return autosage;
	}

	public void setAutosage(boolean autosage) {
		this.autosage = autosage;
	}

	public String getLastHit() {
		return last_hit;
	}

	public void setLastHit(String last_hit) {
		this.last_hit = last_hit;
	}

	public String getDisplay_id() {
		return display_id;
	}

	public void setDisplay_id(String display_id) {
		this.display_id = display_id;
	}

	public String getThread_id() {
		return thread_id;
	}

	public void setThread_id(String thread_id) {
		this.thread_id = thread_id;
	}

	public String getLastModified() {
		return last_modified;
	}

	public void setLastModified(String last_modified) {
		this.last_modified = last_modified;
	}

	public Integer getPostsCount() {
		return posts_count;
	}

	public void setPostsCount(Integer posts_count) {
		this.posts_count = posts_count;
	}

	public Integer getFilesCount() {
		return files_count;
	}

	public void setFilesCount(Integer files_count) {
		this.files_count = files_count;
	}

	public Integer getBoard_id() {
		return board_id;
	}

	public void setBoard_id(Integer board_id) {
		this.board_id = board_id;
	}

	public boolean isArchived() {
		return archived;
	}

	public void setArchived(boolean archived) {
		this.archived = archived;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public DobroPost[] getPosts() {
		return posts;
	}

	public void setPosts(DobroPost[] posts) {
		this.posts = posts;
	}

	public DobroBoard getBoard() {
		return board;
	}

	public void setBoard(DobroBoard board) {
		this.board = board;
	}

	public String getBoardName() {
		if (DobroConstants.BOARD_ID_TO_NAME.containsKey(this.board_id))
			return DobroConstants.BOARD_ID_TO_NAME.get(board_id);
		if (this.board != null)
			return this.board.getBoard();
		return board_name;
	}

	public void setBoardName(String board_name) {
		this.board_name = board_name;
	}

	public Integer getUnread() {
		return unread;
	}

	public void setUnread(Integer unread) {
		this.unread = unread;
	}

	public String getLastViewed() {
		return last_viewed;
	}

	public void setLastViewed(String last_viewed) {
		this.last_viewed = last_viewed;
	}

	public String getLevel() {
		return level;
	}

	public void setLevel(String level) {
		this.level = level;
	}

	public String getLast_post_id() {
		return last_post_id;
	}

	public void setLast_post_id(String last_post_id) {
		this.last_post_id = last_post_id;
	}

	public boolean isThreadModified() {
		return ThreadModified;
	}

	public void setThreadModified(boolean threadModified) {
		ThreadModified = threadModified;
	}

	public String getLastModifiedHeader() {
		return LastModifiedHeader;
	}

	public void setLastModifiedHeader(String lastModifiedHeader) {
		LastModifiedHeader = lastModifiedHeader;
	}

	@Override
	public int compareTo(DobroThread another) {
		DobroThread t2 = another;
		if (t2.isThreadModified() == this.isThreadModified()) {
			if (!TextUtils.equals(getBoardName(), t2.getBoardName()))
				return getBoardName().compareTo(t2.getBoardName());
			return t2.getDisplay_id().compareTo(getDisplay_id());
		} else if (this.isThreadModified())
			return -1;
		return 1;
	}

	public boolean isFromCache() {
		return from_cache;
	}

	public void setFromCache(boolean from_cache) {
		this.from_cache = from_cache;
	}
}
