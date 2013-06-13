package org.anonymous.dobrochan.json;

public class DobroBoard {
	private Integer pages;
	private boolean allow_names;
	private boolean require_thread_file;
	private String description;
	private boolean require_captcha;
	private boolean restrict_read;
	private boolean allow_files;
	private boolean require_post_file;
	private String[] allowed_filetypes;
	private boolean remember_name;
	private boolean require_new_file;
	private boolean allow_OP_moderation;
	private boolean allow_custom_restricts;
	private Integer id;
	private Integer files_max_qty;
	private boolean restrict_trip;
	private Integer delete_thread_post_limit;
	private String title;
	private Integer file_max_res;
	private String __class__;
	private boolean archive;
	private boolean restrict_new_reply;
	private Integer file_max_size;
	private boolean allow_delete_threads;
	private boolean restrict_new_thread;
	private DobroThread[] threads;
	private Integer bump_limit;
	private boolean keep_filenames;
	private String board;

	public Integer getPages() {
		return pages;
	}

	public void setPages(Integer pages) {
		this.pages = pages;
	}

	public boolean isAllow_names() {
		return allow_names;
	}

	public void setAllow_names(boolean allow_names) {
		this.allow_names = allow_names;
	}

	public boolean isRequire_thread_file() {
		return require_thread_file;
	}

	public void setRequire_thread_file(boolean require_thread_file) {
		this.require_thread_file = require_thread_file;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public boolean isRequire_captcha() {
		return require_captcha;
	}

	public void setRequire_captcha(boolean require_captcha) {
		this.require_captcha = require_captcha;
	}

	public boolean isRestrict_read() {
		return restrict_read;
	}

	public void setRestrict_read(boolean restrict_read) {
		this.restrict_read = restrict_read;
	}

	public boolean isAllow_files() {
		return allow_files;
	}

	public void setAllow_files(boolean allow_files) {
		this.allow_files = allow_files;
	}

	public boolean isRequire_post_file() {
		return require_post_file;
	}

	public void setRequire_post_file(boolean require_post_file) {
		this.require_post_file = require_post_file;
	}

	public String[] getAllowed_filetypes() {
		return allowed_filetypes;
	}

	public void setAllowed_filetypes(String[] allowed_filetypes) {
		this.allowed_filetypes = allowed_filetypes;
	}

	public boolean isRemember_name() {
		return remember_name;
	}

	public void setRemember_name(boolean remember_name) {
		this.remember_name = remember_name;
	}

	public boolean isRequire_new_file() {
		return require_new_file;
	}

	public void setRequire_new_file(boolean require_new_file) {
		this.require_new_file = require_new_file;
	}

	public boolean isAllow_OP_moderation() {
		return allow_OP_moderation;
	}

	public void setAllow_OP_moderation(boolean allow_OP_moderation) {
		this.allow_OP_moderation = allow_OP_moderation;
	}

	public boolean isAllow_custom_restricts() {
		return allow_custom_restricts;
	}

	public void setAllow_custom_restricts(boolean allow_custom_restricts) {
		this.allow_custom_restricts = allow_custom_restricts;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Integer getFiles_max_qty() {
		return files_max_qty;
	}

	public void setFiles_max_qty(Integer files_max_qty) {
		this.files_max_qty = files_max_qty;
	}

	public boolean isRestrict_trip() {
		return restrict_trip;
	}

	public void setRestrict_trip(boolean restrict_trip) {
		this.restrict_trip = restrict_trip;
	}

	public Integer getDelete_thread_post_limit() {
		return delete_thread_post_limit;
	}

	public void setDelete_thread_post_limit(Integer delete_thread_post_limit) {
		this.delete_thread_post_limit = delete_thread_post_limit;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public Integer getFile_max_res() {
		return file_max_res;
	}

	public void setFile_max_res(Integer file_max_res) {
		this.file_max_res = file_max_res;
	}

	public String get__class__() {
		return __class__;
	}

	public void set__class__(String __class__) {
		this.__class__ = __class__;
	}

	public boolean isArchive() {
		return archive;
	}

	public void setArchive(boolean archive) {
		this.archive = archive;
	}

	public boolean isRestrict_new_reply() {
		return restrict_new_reply;
	}

	public void setRestrict_new_reply(boolean restrict_new_reply) {
		this.restrict_new_reply = restrict_new_reply;
	}

	public Integer getFile_max_size() {
		return file_max_size;
	}

	public void setFile_max_size(Integer file_max_size) {
		this.file_max_size = file_max_size;
	}

	public boolean isAllow_delete_threads() {
		return allow_delete_threads;
	}

	public void setAllow_delete_threads(boolean allow_delete_threads) {
		this.allow_delete_threads = allow_delete_threads;
	}

	public boolean isRestrict_new_thread() {
		return restrict_new_thread;
	}

	public void setRestrict_new_thread(boolean restrict_new_thread) {
		this.restrict_new_thread = restrict_new_thread;
	}

	public DobroThread[] getThreads() {
		return threads;
	}

	public void setThreads(DobroThread[] threads) {
		this.threads = threads;
	}

	public Integer getBump_limit() {
		return bump_limit;
	}

	public void setBump_limit(Integer bump_limit) {
		this.bump_limit = bump_limit;
	}

	public boolean isKeep_filenames() {
		return keep_filenames;
	}

	public void setKeep_filenames(boolean keep_filenames) {
		this.keep_filenames = keep_filenames;
	}

	public String getBoard() {
		return board;
	}

	public void setBoard(String board) {
		this.board = board;
	}
}
