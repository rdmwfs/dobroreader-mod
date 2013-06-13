/*
 * Copyright 2011 - AndroidQuery.com (tinyeeliu@gmail.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.androidquery.util;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.Method;

import android.util.Log;

/**
 * Utility methods. Warning: Methods might changed in future versions.
 * 
 */

public class AQUtility {

	private static boolean debug = false;
	private static Object wait;

	public static void setDebug(boolean debug) {
		AQUtility.debug = debug;
	}

	public static void debugWait(long time) {

		if (!debug)
			return;

		if (wait == null)
			wait = new Object();

		synchronized (wait) {

			try {
				wait.wait(time);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

	}

	public static void debugNotify() {

		if (!debug || wait == null)
			return;

		synchronized (wait) {
			wait.notifyAll();
		}

	}

	public static void debug(Object msg) {
		if (debug) {
			Log.w("AQuery", msg + "");
		}
	}

	public static void debug(Object msg, Object msg2) {
		if (debug) {
			Log.w("AQuery", msg + ":" + msg2);
		}
	}

	public static void debug(Throwable e) {
		if (debug) {
			String trace = Log.getStackTraceString(e);
			Log.w("AQuery", trace);
		}
	}

	public static void report(Throwable e) {

		if (e == null)
			return;

		try {

			debug(e);

			if (eh != null) {
				eh.uncaughtException(Thread.currentThread(), e);
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}

	}

	private static UncaughtExceptionHandler eh;

	public static void setExceptionHandler(UncaughtExceptionHandler handler) {
		eh = handler;
	}

	public static Object invokeHandler(Object handler, String callback,
			boolean fallback, boolean report, Class<?>[] cls, Object... params) {

		return invokeHandler(handler, callback, fallback, report, cls, null,
				params);

	}

	public static Object invokeHandler(Object handler, String callback,
			boolean fallback, boolean report, Class<?>[] cls, Class<?>[] cls2,
			Object... params) {
		try {
			return invokeMethod(handler, callback, fallback, cls, cls2, params);
		} catch (Exception e) {
			if (report) {
				AQUtility.report(e);
			} else {
				AQUtility.debug(e);
			}
			return null;
		}
	}

	private static Object invokeMethod(Object handler, String callback,
			boolean fallback, Class<?>[] cls, Class<?>[] cls2, Object... params)
			throws Exception {

		if (handler == null || callback == null)
			return null;

		Method method = null;

		try {
			if (cls == null)
				cls = new Class[0];
			method = handler.getClass().getMethod(callback, cls);
			return method.invoke(handler, params);
		} catch (NoSuchMethodException e) {
			// AQUtility.debug(e.getMessage());
		}

		try {
			if (fallback) {

				if (cls2 == null) {
					method = handler.getClass().getMethod(callback);
					return method.invoke(handler);
				} else {
					method = handler.getClass().getMethod(callback, cls2);
					return method.invoke(handler, params);
				}

			}
		} catch (NoSuchMethodException e) {
		}

		return null;

	}

	private static final int IO_BUFFER_SIZE = 1024;

	public static void copy(InputStream in, OutputStream out)
			throws IOException {
		byte[] b = new byte[IO_BUFFER_SIZE];
		int read;
		while ((read = in.read(b)) != -1) {
			out.write(b, 0, read);
		}
	}

	public static byte[] toBytes(InputStream is) {

		byte[] result = null;

		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		try {
			copy(is, baos);
			result = baos.toByteArray();
		} catch (IOException e) {
			AQUtility.report(e);
		}

		close(is);

		return result;

	}

	public static void close(Closeable c) {
		try {
			if (c != null) {
				c.close();
			}
		} catch (Exception e) {
		}
	}
}