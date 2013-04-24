package org.anonymous.dobrochan.json;

public class DobroFile {
	public enum Rating{
		SWF, R15, R18, R18G, ILLEGAL
	}
	public static class Metadata {
		public String type; // Archive | MP3 | PDF | Text
		//Archive
		public String[] files;
		public Integer files_count;
		//MP3
		public String album;
		public String artist;
		public String title;
		public String totaltracks;
		public Float length;
		public Integer sample_rate;
		public String tracknumber;
		public Float bitrate;
		//PDF
		public Boolean secured;
		//Text
		public Integer lines;

		public Integer width;
		public Integer height;

		public Metadata() {
		}
	}
	
	public DobroFile() {
	}

	private transient Rating rat = null;

	public Rating getRat() {
		if (rat == null) {
			rat = Rating.SWF;
			if (rating.equalsIgnoreCase("r-15"))
				rat = Rating.R15;
			else if (rating.equalsIgnoreCase("r-18"))
				rat = Rating.R18;
			else if (rating.equalsIgnoreCase("r-18g"))
				rat = Rating.R18G;
			else if (rating.equalsIgnoreCase("illegal"))
				rat = Rating.ILLEGAL;
		}
		return rat;
	}

	private String src;
	private int thumb_height;
	private int file_id;
	private int thumb_width;
	private String rating;
	private int size;
	private String type;
	private String __class__;
	private String thumb;
	private Metadata metadata;

	public String getSrc() {
		return src;
	}

	public void setSrc(String src) {
		this.src = src;
	}

	public int getThumb_height() {
		return thumb_height;
	}

	public void setThumb_height(int thumb_height) {
		this.thumb_height = thumb_height;
	}

	public int getFile_id() {
		return file_id;
	}

	public void setFile_id(int file_id) {
		this.file_id = file_id;
	}

	public int getThumb_width() {
		return thumb_width;
	}

	public void setThumb_width(int thumb_width) {
		this.thumb_width = thumb_width;
	}

	public String getRating() {
		return rating;
	}

	public void setRating(String rating) {
		this.rating = rating;
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String get__class__() {
		return __class__;
	}

	public void set__class__(String __class__) {
		this.__class__ = __class__;
	}

	public String getThumb() {
		return thumb;
	}

	public void setThumb(String thumb) {
		this.thumb = thumb;
	}

	public Metadata getMetadata() {
		return metadata;
	}

	public void setMetadata(Metadata metadata) {
		this.metadata = metadata;
	}
}
