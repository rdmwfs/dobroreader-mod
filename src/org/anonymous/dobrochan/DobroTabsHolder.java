package org.anonymous.dobrochan;

import java.util.LinkedList;
import java.util.List;

import android.text.TextUtils;

public class DobroTabsHolder {
	List<String[]> m_tabs = new LinkedList<String[]>();;

	public static DobroTabsHolder getInstance() {
		DobroApplication app = DobroApplication.getApplicationStatic();
		return app.getTabs();
	}

	public void addTab(String board, String thread, String scroll_to,
			String title, String post) {
		String[] s = { board, thread, scroll_to, title, post };
		for (String[] arr : m_tabs.toArray(new String[0][]))
			if (TextUtils.equals(arr[0], s[0])
					&& TextUtils.equals(arr[1], s[1])
					&& TextUtils.equals(arr[4], s[4]))
				m_tabs.remove(arr);
		m_tabs.add(s);
	}

	public List<String[]> getTabs() {
		return m_tabs;
	}

	public void clear() {
		m_tabs.clear();
	}

	public void remove(String[] tag) {
		m_tabs.remove(tag);
	}

	public void removeLast() {
		if (m_tabs.size() > 0)
			m_tabs.remove(m_tabs.size() - 1);
	}

	public void updateScroll(String board, String thread, String scroll_to) {
		for (String[] arr : m_tabs)
			if (TextUtils.equals(arr[0], board)
					&& TextUtils.equals(arr[1], thread))
				arr[2] = scroll_to;
	}
}
