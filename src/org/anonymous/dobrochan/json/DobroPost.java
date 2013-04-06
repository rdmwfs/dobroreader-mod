package org.anonymous.dobrochan.json;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.anonymous.dobrochan.DobroApplication;
import org.anonymous.dobrochan.DobroConstants;
import org.anonymous.dobrochan.DobroFormatter;
import org.anonymous.dobrochan.DobroHtml;
import org.anonymous.dobrochan.DobroHtmlTagHandler;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.SpannableString;
import android.text.TextUtils;

public class DobroPost {
	private transient CharSequence formattedText;
	private Context lastContext = null;
	
	private transient String hide_comment = null;
	private transient boolean hidden_by_spells = false;
	private transient DobroThread thread = null;
	private transient String thread_display_id;
	private transient boolean in_thread;
	private transient List<String> refs = new LinkedList<String>();
	private transient List<String> links = new LinkedList<String>();
	private transient int number;
	public int getNumber() {
		return number;
	}
	public void setNumber(int number) {
		this.number = number;
	}
	public List<String> getRefs() {
		if(refs==null)
			refs = new LinkedList<String>();
		return refs;
	}
	public void setRefs(List<String> refs) {
		this.refs = refs;
	}
	public String getBoardName() {
		if(DobroConstants.BOARD_ID_TO_NAME.containsKey(board_id))
			return DobroConstants.BOARD_ID_TO_NAME.get(board_id);
		return thread.getBoardName();
	}
	public DobroThread getThread() {
		return thread;
	}
	public void setThread(DobroThread thread) {
		this.thread = thread;
	}
	public String getThreadDisplay_id() {
		if(this.thread != null)
			return this.thread.getDisplay_id();
		return thread_display_id;
	}
	
	public boolean isIn_thread() {
		return in_thread;
	}
	public void setIn_thread(boolean in_thread) {
		this.in_thread = in_thread;
	}
	public void setThreadDisplay_id(String thread_display_id) {
		this.thread_display_id = thread_display_id;
	}
	//from json
	private String display_id;
	private String post_id;
	private String last_modified;
	private String date;
	private String message;
	private String message_html;
	private String subject;
	private int board_id;
	private String name;
	private String thread_id;
	private String __class__;
	private boolean op;
	private DobroFile[] files;
	public String getDisplay_id() {
		return display_id;
	}
	public void setDisplay_id(String display_id) {
		this.display_id = display_id;
	}
	public String getPost_id() {
		return post_id;
	}
	public void setPost_id(String post_id) {
		this.post_id = post_id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDate() {
		return date;
	}
	public void setDate(String date) {
		this.date = date;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public boolean isOp() {
		return op;
	}
	public void setOp(boolean op) {
		this.op = op;
	}
	public DobroFile[] getFiles() {
		return files;
	}
	public void setFiles(DobroFile[] files) {
		this.files = files;
	}
	public String getSubject() {
		return subject;
	}
	public void setSubject(String subject) {
		this.subject = subject;
	}
	public String getLastModified() {
		return last_modified;
	}
	public void setLastModified(String last_modified) {
		this.last_modified = last_modified;
	}
	public int getBoard_id() {
		return board_id;
	}
	public void setBoard_id(int board_id) {
		this.board_id = board_id;
	}
	public String getThread_id() {
		if(thread != null && thread.getThread_id() != null)
			return thread.getThread_id();
		return thread_id;
	}
	public void setThread_id(String thread_id) {
		this.thread_id = thread_id;
	}
	public String get__class__() {
		return __class__;
	}
	public void set__class__(String __class__) {
		this.__class__ = __class__;
	}
	public CharSequence getFormattedText() {
		return formattedText;
	}
	public void setFormattedText(CharSequence formattedText) {
		this.formattedText = formattedText;
	}
	public void formatText(){
		if(message != null)
			this.formattedText = DobroFormatter.getFormatted(message, getBoardName(), getLastContext(), this);
		else if(message_html != null)
		try {
			String source = message_html;
			source = Pattern.compile("<a href=\"/\\w+?/res/\\d+?.xhtml#i\\d+?\" onmouseover=\"ShowRefPost\\(event,'(\\w+?)', (\\d+?), (\\d+?)\\)\" "+
					"onclick=\"Highlight\\(event, '\\d+?'\\)\">(&gt;&gt;.*?)</a>",
					Pattern.DOTALL).matcher(source).replaceAll("<link_$1_$2_$3>$4</link_$1_$2_$3>");
//			source = Pattern.compile("<span class=\"spoiler\">(.*?)</span>",
//					Pattern.DOTALL).matcher(source).replaceAll("<span>$1</span>");
			source = Pattern.compile("<div class=\"spoiler\">(.*?)</div>",
					Pattern.DOTALL).matcher(source).replaceAll("<br/><span class=\"spoiler\">$1</span><br/>");
			Matcher m = Pattern.compile("<pre>(.*?)</pre>",Pattern.DOTALL).matcher(source);
			while(m.find())
				source = source.replace(m.group(), m.group().replaceAll("\n", "<br/>"));
			final DobroHtmlTagHandler dph = new DobroHtmlTagHandler(getLastContext(), this);
			this.formattedText = DobroHtml.fromHtml(source, null, dph);
			if(this.formattedText.length()>=2)
				this.formattedText = this.formattedText.subSequence(0, this.formattedText.length()-1);
		} catch (OutOfMemoryError e) {
			e.printStackTrace();
			this.formattedText = new SpannableString(message_html);
		}
		else
			this.formattedText = new SpannableString("");
		checkSpells();
		this.message_html = null;
	}
	public Context getLastContext() {
		return lastContext;
	}
	public void setLastContext(Context lastContext) {
		this.lastContext = lastContext;
	}
//	public String getMessageHtml() {
//		return message_html;
//	}
	public void setMessageHtml(String message_html) {
		this.message_html = message_html;
	}
	public List<String> getLinks() {
		return links;
	}
	public void setLinks(List<String> links) {
		this.links = links;
	}
	public void checkSpells() {
		String db_comment = DobroApplication.getApplicationStatic().getHiddenPosts().getComment(getBoardName()+"/"+getDisplay_id());
		if(db_comment!=null)
		{
			if(TextUtils.equals("unhide", db_comment))
				return;
			hidden_by_spells = true;
			hide_comment = "вручную";
			return;
		}
		SharedPreferences prefs = DobroApplication.getApplicationStatic().getDefaultPrefs();
		String spells_str = prefs.getString("spells", "");
		if(TextUtils.isEmpty(spells_str))
			return;
		String[] spells = spells_str.split("\n");
		for(String spell : spells) {
			if(TextUtils.isEmpty(spell))
				continue;
			try{
				Pattern p = Pattern.compile(spell, Pattern.CASE_INSENSITIVE);
				Matcher m = p.matcher(getFormattedText());
				if(m.find()) {
					hidden_by_spells = true;
					hide_comment = spell;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	public boolean isHidden() {
		return hidden_by_spells;
	}
	public void setHidden(boolean state) {
		hidden_by_spells = state;
	}
	public String getHideComment() {
		if(hide_comment == null)
			return "";
		return hide_comment;
	}
	public void setHideComment(String comment) {
		hide_comment = comment;
	}
}
