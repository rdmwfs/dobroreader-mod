/**
 * Copyright (c) 2011 Duy Truong <hduyudh@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Derived from code by The Android Open Source Project
 * Modification is not made available under the Apache 2.0 license
 * The Apache 2.0 license is included for attribution purpose only.
 */

/**
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.anonymous.dobrochan.minoriko;

import java.io.File;

import org.anonymous.dobrochan.ApiWrapper;

import android.content.Context;

/**
 * The actual AsyncTask that will asynchronously download the image.
 */
public class BitmapDownloaderTask {

	public static String getFileName(Context context, String url) {
		File cacheDir = ApiWrapper.getExternalCacheDir(context);
		File dir = new File(cacheDir, "full-sized");
		if (!dir.exists()) {
			if (!dir.mkdir()) {
				dir = cacheDir;
			}
		}
		if (dir == null) { // no SD card
			return "";
		}
		return dir.getPath() + "/" + Integer.toHexString(url.hashCode());
	}
}
