package org.anonymous.dobrochan;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.anonymous.dobrochan.json.DobroPost;
import org.xml.sax.XMLReader;

import android.content.Context;
import android.graphics.Color;
import android.text.Editable;
import org.anonymous.dobrochan.DobroHtml.TagHandler;
import android.text.Spannable;
import android.text.style.ForegroundColorSpan;

public class DobroHtmlTagHandler implements TagHandler {
	Context m_context = null;
	DobroPost m_post = null;
	boolean first = true;
	int ul_level = 0;
	private static class UlSpan { }
	public DobroHtmlTagHandler(Context context, DobroPost dobropost) {
		m_context = context;
		m_post = dobropost;
	}
	@Override
	public void handleTag(boolean opening, String tag, Editable output,
            XMLReader xmlReader) {
        if(tag.equalsIgnoreCase("span")) {
            processSpan(opening, output, new SpoilerSpan());
        } else if(tag.startsWith("link")) {
        	Matcher m = Pattern.compile("link_(\\w+)_(\\d+)_(\\d+)").matcher(tag);
        	if(!m.find())
        		return;
        	String board = m.group(1);
        	String thread = m.group(2);
        	String post = m.group(3);
        	processSpan(opening, output, new DobroLinkSpan(board, post, m_context, m_post));
        } else if (tag.equalsIgnoreCase("li")) {
            char lastChar = 0;
            if (output.length() > 0)
                lastChar = output.charAt(output.length() - 1);
            if (first) {
                if (lastChar == '\n')
                    output.append(" "+(ul_level==0?"●":" ○")+"  ");
                else
                    output.append("\n "+(ul_level==0?"●":" ○")+"  ");
                first = false;
            } else {
                first = true;
            }
        } else if (tag.equalsIgnoreCase("ul")) {
        	if(opening)
        	{
        		if(output.getSpans(output.length(), output.length(), UlSpan.class).length>0)
        			ul_level=(ul_level+1)%2;
        		else
        			ul_level=0;
        	}
        	processSpan(opening, output, new UlSpan());
        } else if (tag.equals("pre") && !opening) {
        	output.append("\n");
        }
    }

    private void processSpan(boolean opening, Editable output, Object span) {
        int len = output.length();
        if(opening) {
            output.setSpan(span, len, len, Spannable.SPAN_MARK_MARK);
        } else {
            Object obj = getLast(output, span.getClass());
            int where = output.getSpanStart(obj);

            output.removeSpan(obj);
            if (where != len) {
                output.setSpan(span, where, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
	private Object getLast(Editable text, Class kind) {
        Object[] objs = text.getSpans(0, text.length(), kind);

        if (objs.length == 0) {
            return null;
        } else {
            for(int i = objs.length;i>0;i--) {
                if(text.getSpanFlags(objs[i-1]) == Spannable.SPAN_MARK_MARK) {
                    return objs[i-1];
                }
            }
            return null;
        }
    }
}