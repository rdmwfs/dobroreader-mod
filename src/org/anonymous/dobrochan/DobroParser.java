package org.anonymous.dobrochan;

import greendroid.util.GDUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.anonymous.dobrochan.json.DobroBoard;
import org.anonymous.dobrochan.json.DobroFile;
import org.anonymous.dobrochan.json.DobroFile.Rating;
import org.anonymous.dobrochan.json.DobroPost;
import org.anonymous.dobrochan.json.DobroSession;
import org.anonymous.dobrochan.json.DobroThread;

import android.content.Context;
import android.graphics.Bitmap;
import android.text.SpannableString;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageLoadingListener;

public class DobroParser extends Object {
	public static DobroParser getInstance() {
		DobroApplication app = DobroApplication.getApplicationStatic();
		return app.getParser();
	}
	
	public DobroParser() {
		gson = new Gson();
	}

	private Gson gson;
	
	public DobroBoard parceBoard(JsonObject obj) {
		if (obj == null)
			return null;
		DobroBoard b = null;
		try{
		JsonObject boards = obj.getAsJsonObject("boards");
		Set<Entry<String, JsonElement>> parts = boards.entrySet();
		for (Entry<String, JsonElement> p : parts) {
			b = gson.fromJson(p.getValue(), DobroBoard.class);
			for(DobroThread t : b.getThreads())
			{
				t.setBoard(b);
				for(DobroPost pst : t.getPosts())
				{
					pst.setThread(t);
					pst.formatText();
				}
				createReflinks(t);
			}
			b.setBoard(p.getKey());
			break;
		}
		} catch(JsonSyntaxException e){
			return null;
		}
		return b;
	}
	
	public void parceError(JsonObject err) {
		try{
			err = err.getAsJsonObject("error");
			DobroApplication app = DobroApplication.getApplicationStatic();
			app.showToast(String.format("Error %s: %s", err.getAsJsonPrimitive("code").getAsString(),
					err.getAsJsonPrimitive("message").getAsString()),2);
		} catch (JsonSyntaxException e){
		}
	}
	
	public DobroSession parceHiddenThreads(JsonObject obj) {
		if (obj == null)
			return null;
		DobroSession sess = gson.fromJson(obj, DobroSession.class);
		if(sess.getThreads() != null)
		try{
		for (DobroThread t : sess.getThreads()) {
			if (!t.getLevel().equals("hidden"))
				continue;
			t.setDisplay_id(t.getThread_id()); //wakaba bug
			t.setThread_id(null);
		}
		} catch (JsonSyntaxException e){
			return null;
		}
		return sess;
	}
	
	public DobroThread[] parceThreads(String dump) {
		if(dump == null)
			return null;
		try{
			return gson.fromJson(dump, DobroThread[].class);
		} catch (Exception e) {
			return null;
		}
	}
	
	public DobroPost[] parcePosts(String dump, String boardname) {
		if(dump == null)
			return null;
		DobroThread empty = new DobroThread();
		empty.setBoardName(boardname);
		DobroPost[] posts = null;
		try{
			posts = gson.fromJson(dump, DobroPost[].class);
		} catch (JsonSyntaxException e) {
			return null;
		}
		for(DobroPost p : posts)
		{
			p.setThread(empty);
			p.formatText();
		}
		return posts;
	}
	
	public DobroPost parcePost(JsonObject post_obj, String board) {
		if (post_obj == null)
			return null;
		DobroPost p = null;
		DobroThread empty = new DobroThread();
		empty.setBoardName(board);
		try{
		p = gson.fromJson(post_obj.toString(), DobroPost.class);
		p.setThread(empty);
		for(DobroFile attach : p.getFiles())
			{
				Context c = DobroApplication.getApplicationStatic();
				boolean urlLoaded = DobroHelper.checkNetworkForPictures(c)
						&& GDUtils.getImageCache(c).get(attach.getThumb()) == null
						&& ! DobroApplication.getApplicationStatic().getNetwork().disc_cache.get(DobroHelper.formatUri(attach.getThumb())).exists();
				Rating rat = attach.getRat();
				if (DobroHelper.checkRating(c, rat))
					urlLoaded = false;
				if(urlLoaded)
				{
					//DobroNetwork.getInstance().addPendingUrl(null, attach.getThumb());
				}
			}
		} catch (JsonSyntaxException e){
			return null;
		}
		p.formatText();
		return p;
	}
	
	public DobroSession parceStarredThreads(JsonObject obj) {
		if (obj == null)
			return null;
		DobroSession sess = null;
		try{
		sess = gson.fromJson(obj, DobroSession.class);
		ArrayList<DobroThread> threads2del = new ArrayList<DobroThread>();
		for (DobroThread t : sess.getThreads()) {
			if (!t.getLevel().equals("bookmarked"))
				continue;
			DobroThread info = DobroNetwork.getInstance().getThreadInfoJson(t.getThread_id());
			if(TextUtils.equals(info.get__class__(),"DeletedThread"))
				threads2del.add(t);
			else {
				t.setDisplay_id(info.getDisplay_id());
				t.setTitle(info.getTitle());
				t.setThreadModified(info.isThreadModified());
				t.setLastModified(info.getLastModified());
				t.setFromCache(info.isFromCache());
				if(t.isThreadModified())
					t.setUnread(1);
			}
		}
		if(threads2del.size() > 0) {
			ArrayList<DobroThread> temp = new ArrayList<DobroThread>(Arrays.asList(sess.getThreads()));
			temp.removeAll(threads2del);
			sess.setThreads(temp.toArray(new DobroThread[temp.size()]));
		}
		} catch (Exception e){
			return null;
		}
		return sess;
	}
	
	public DobroThread parceThread(String dump) {
		if (dump == null)
			return null;
		return parceThread(new JsonParser().parse(dump).getAsJsonObject());
	}
	
	public DobroThread parceThread(JsonObject thread_obj) {
		if (thread_obj == null)
			return null;
		DobroThread t = null;
		try{
		t = gson.fromJson(thread_obj, DobroThread.class);
		if(t.getPosts() != null)
		{
			for(DobroPost p : t.getPosts())
			{
				p.setThread(t);
				p.formatText();
				//p.setFormattedText(new SpannableString(p.getMessage()==null?p.getMessageHtml():p.getMessage()));
			}
			createReflinks(t);
		}
		else
			t.setPosts(new DobroPost[0]);
		} catch (JsonSyntaxException e){
			return null;
		}
		return t;
	}
	
	private void createReflinks(DobroThread t) {
		Map<String, List<String>> m = new HashMap<String, List<String>>();
		for(DobroPost p : t.getPosts())
		{
			Pattern link_pattern = Pattern
					.compile(">>(?:/)?(\\d+)");
			CharSequence message = p.getMessage();
			if(message == null)
				message = p.getFormattedText();
			if(message == null)
				continue;
			Matcher link_matcher = link_pattern.matcher(message);
			while (link_matcher.find()) {
				String ref = link_matcher.group(1);
                List<String> l = m.get(ref);
                if (l == null)
                    m.put(ref, l=new LinkedList<String>());
                l.add(">>"+p.getDisplay_id());
                List<String> p_l = p.getLinks();
                if(p_l == null)
                	p_l = new LinkedList<String>();
                p_l.add(ref);
                p.setLinks(p_l);
			}
		}
		for(DobroPost p : t.getPosts())
		{
			List<String> refs = m.get(p.getDisplay_id());
			if(refs == null)
				continue;
			p.setRefs(refs);
		}
	}
	
	public String composeThread(DobroThread t) {
		return gson.toJson(t);
	}
	
	public String composePosts(DobroPost[] posts) {
		return gson.toJson(posts);
	}
	
	public String composeThreads(DobroThread[] t) {
		return gson.toJson(t);
	}
}
