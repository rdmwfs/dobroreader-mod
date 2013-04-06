package org.anonymous.dobrochan;

import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.anonymous.dobrochan.json.DobroPost;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.URLSpan;
import android.text.util.Linkify;

public class DobroFormatter {
	static abstract class SpanObjectFactory {
		abstract Object getSpan();
	}

	public static CharSequence getFormatted(String message, String board,
			Context context, DobroPost dobropost) {
		if(message == null || message.length() == 0)
			return "";
		try{
		{
			Pattern p = Pattern
					.compile("http(?:s)?://(?:suigintou.net|dobrochan.(?:ru|com|org))/([a-z]{1,4})/res/(\\d+).xhtml(?:#i(\\d+))?");
			Matcher matcher = p.matcher(message);
			while (matcher.find())
				message = message.replaceFirst(matcher.group(), String.format(
						">>%s/%s",
						matcher.group(1),
						(matcher.group(3) == null) ? matcher.group(2) : matcher
								.group(3)));
		}
		SpannableStringBuilder text = new SpannableStringBuilder(message);
		Linkify.addLinks(text, Linkify.WEB_URLS);
		{
			Pattern p = Pattern.compile("^(\\*){1,2} ", Pattern.MULTILINE);
			Matcher m = p.matcher(text);
			while (m.find()) {
				int st = m.start();
				if (m.group().length() == 2)
					text.replace(st, st + 2, "● ");
				else
					text.replace(st, st + 3, " ○ ");
			}
		}
		{
			// quote
			Pattern pattern = Pattern.compile("^>[^>].*?$", Pattern.MULTILINE);
			Matcher matcher = pattern.matcher(text);
			while (matcher.find()) {
				int pos_start = matcher.start();
				int pos_end = matcher.end();
				text.setSpan(
						new ForegroundColorSpan(Color.parseColor("#789922")),
						pos_start, pos_end, 0);
			}
		}
		{
			// dbl quote
			Pattern pattern = Pattern.compile(
					"^>(\\s*>)+[^(\\d+|[a-z]{1,4}/\\d+)].*?$",
					Pattern.MULTILINE);
			Matcher matcher = pattern.matcher(text);
			while (matcher.find()) {
				int pos_start = matcher.start();
				int pos_end = matcher.end();
				text.setSpan(
						new ForegroundColorSpan(Color.parseColor("#406010")),
						pos_start, pos_end, 0);
			}
		}
		// spoiler
		replaceAll(text, "%%", "%%", new SpanObjectFactory() {
			@Override
			Object getSpan() {
				return new SpoilerSpan();
			}
		});
		// spoiler
		replaceAll(text, "%%", " %%", new SpanObjectFactory() {
			@Override
			Object getSpan() {
				return new SpoilerSpan();
			}
		});
		// bold+italic
		replaceAll(text, "_\\*\\*", "\\*\\*_", new SpanObjectFactory() {
			@Override
			Object getSpan() {
				return new StyleSpan(Typeface.BOLD_ITALIC);
			}
		});
		replaceAll(text, "__\\*", "\\*__", new SpanObjectFactory() {
			@Override
			Object getSpan() {
				return new StyleSpan(Typeface.BOLD_ITALIC);
			}
		});
		// bold
		replaceAll(text, "\\*\\*", "\\*\\*", new SpanObjectFactory() {
			@Override
			Object getSpan() {
				return new StyleSpan(Typeface.BOLD);
			}
		});
		replaceAll(text, "__", "__", new SpanObjectFactory() {
			@Override
			Object getSpan() {
				return new StyleSpan(Typeface.BOLD);
			}
		});
		// italic
		replaceAll(text, "\\*", "\\*", new SpanObjectFactory() {
			@Override
			Object getSpan() {
				return new StyleSpan(Typeface.ITALIC);
			}
		});
		replaceAll(text, "_", "_", new SpanObjectFactory() {
			@Override
			Object getSpan() {
				return new StyleSpan(Typeface.ITALIC);
			}
		});
		// mono
		replaceAll(text, "``", "``", new SpanObjectFactory() {
			@Override
			Object getSpan() {
				return new ForegroundColorSpan(Color.parseColor("#2FA1E7"));
			}
		});
		replaceAll(text, "`", "`", new SpanObjectFactory() {
			@Override
			Object getSpan() {
				return new ForegroundColorSpan(Color.parseColor("#2FA1E7"));
			}
		});
		// spoiler
		replaceAll(text, "^%%\r\n", "\r\n%%$", new SpanObjectFactory() {
			@Override
			Object getSpan() {
				return new SpoilerSpan();
			}
		}, Pattern.DOTALL);
		// mono
		replaceAll(text, "^``\r\n", "\r\n``$", new SpanObjectFactory() {
			@Override
			Object getSpan() {
				return new ForegroundColorSpan(Color.parseColor("#2FA1E7"));
			}
		}, Pattern.DOTALL);

		{
			Vector<Integer> skip_vector = new Vector<Integer>();
			{
				Pattern skip = Pattern
						.compile("^\\s{4,}.*$", Pattern.MULTILINE);
				Matcher skip_matcher = skip.matcher(text);
				while (skip_matcher.find()) {
					skip_vector.add(skip_matcher.start());
					skip_vector.add(skip_matcher.end());
				}
			}

			{
				Pattern link_pattern = Pattern
						.compile(">>(?:/)?([a-z]{1,4}/)?(\\d+)");
				Matcher link_matcher = link_pattern.matcher(text);
				while (link_matcher.find()) {
					boolean cont = false;
					for (int i = 0; i < skip_vector.size(); i += 2)
						if (link_matcher.start() > skip_vector.get(i)
								&& link_matcher.start() < skip_vector
										.get(i + 1))
							cont = true;
					if (cont)
						continue;

					int pos_start = link_matcher.start();
					int pos_end = link_matcher.end();
					text.setSpan(new DobroLinkSpan(
							link_matcher.group(1) == null ? board
									: link_matcher.group(1).replace("/", ""),
							link_matcher.group(2), context, dobropost), pos_start,
							pos_end, 0);
				}
			}

			{
				// ^H
				int delta = 0;
				Pattern pattern = Pattern.compile("(\\^H)+");
				Matcher matcher = pattern.matcher(text);
				while (matcher.find()) {
					boolean cont = false;
					for (int i = 0; i < skip_vector.size(); i += 2)
						if (matcher.start() > skip_vector.get(i)
								&& matcher.start() < skip_vector.get(i + 1))
							cont = true;
					if (cont)
						continue;

					int pos_start = matcher.start() - delta;
					int pos_end = matcher.end() - delta;
					delta += pos_end - pos_start;
					text.setSpan(new StrikethroughSpan(), pos_start
							- (pos_end - pos_start) / 2, pos_start, 0);
					text.delete(pos_start, pos_end);
				}
			}
		}
		{
			Vector<Integer> skip_vector = new Vector<Integer>();
			{
				Pattern skip = Pattern
						.compile("^\\s{4,}.*$", Pattern.MULTILINE);
				Matcher skip_matcher = skip.matcher(text);
				while (skip_matcher.find()) {
					skip_vector.add(skip_matcher.start());
					skip_vector.add(skip_matcher.end());
				}
			}

			int delta = 0;
			String reverce = new StringBuffer(text.toString()).reverse()
					.toString();
			Pattern wordBounds = Pattern.compile("[^ ]+");
			Matcher wordMatcher = wordBounds.matcher(reverce);
			Pattern pattern = Pattern.compile("(\\^W)+");
			Matcher matcher = pattern.matcher(text);
			while (matcher.find()) {
				boolean cont = false;
				for (int i = 0; i < skip_vector.size(); i += 2)
					if (matcher.start() > skip_vector.get(i)
							&& matcher.start() < skip_vector.get(i + 1))
						cont = true;
				if (cont)
					continue;
				int pos_start = matcher.start() - delta;
				int pos_end = matcher.end() - delta;
				int wordsCount = (pos_end - pos_start) / 2;
				int word_start = reverce.length() - matcher.start();
				wordMatcher.reset();
				for (int i = 0; i < wordsCount; i++) {
					if (wordMatcher.find(word_start)) {
						text.setSpan(new StrikethroughSpan(), reverce.length()
								- wordMatcher.end() - delta, reverce.length()
								- wordMatcher.start() - delta, 0);
						word_start = wordMatcher.end();
					}
				}
				delta += pos_end - pos_start;
				text.delete(pos_start, pos_end);
			}
		}
		return text.subSequence(0, text.length());
		} catch (Exception e)
		{
			e.printStackTrace();
			return message;
		}
	}

	static void replaceAll(SpannableStringBuilder text, String begin,
			String end, SpanObjectFactory factory) {
		replaceAll(text, begin, end, factory, 0);
	}

	static void replaceAll(SpannableStringBuilder text, String begin,
			String end, SpanObjectFactory factory, int flag) {
		int delta = 0;
		Vector<Integer> skip_vector = new Vector<Integer>();
		{
			Pattern skip = Pattern.compile("^\\s{4,}.*$", Pattern.MULTILINE);
			Matcher skip_matcher = skip.matcher(text);
			while (skip_matcher.find()) {
				skip_vector.add(skip_matcher.start());
				skip_vector.add(skip_matcher.end());
			}
		}
		Pattern pattern = Pattern.compile(String.format(
				flag == 0 ? "(%s(\\S.*?)%s)" : "(%s(\\S.*?)%s)", begin, end),
				Pattern.MULTILINE | flag);
		Matcher matcher = pattern.matcher(text);
		while (matcher.find()) {
			if (flag == 0) {
				boolean cont = false;
				for (int i = 0; i < skip_vector.size(); i += 2)
					if (matcher.start(1) > skip_vector.get(i)
							&& matcher.start(1) < skip_vector.get(i + 1))
						cont = true;
				URLSpan[] spans = text.getSpans(matcher.start(1), matcher.end(1), URLSpan.class);
				if(spans != null && spans.length > 0 && begin.contains("_"))
					cont = true;
				if (cont)
					continue;
			}
			if (matcher.group(2).startsWith(begin.replace("\\", ""))
					&& matcher.group(2).length() <= begin.replace("\\", "")
							.length())
				continue;
			int pos_start = matcher.start(1) - delta;
			int pos_end = matcher.end(1) - delta;
			SpannableString rep = new SpannableString(matcher.group(2));
			rep.setSpan(factory.getSpan(), 0, rep.length(), 0);
			Linkify.addLinks(rep, Linkify.WEB_URLS);
			text.replace(pos_start, pos_end, rep);
			delta += matcher.group(1).length() - matcher.group(2).length();
		}
	}
}
