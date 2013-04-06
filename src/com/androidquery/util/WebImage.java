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

import java.io.InputStream;
import java.lang.reflect.Field;

import org.anonymous.dobrochan.R;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Picture;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebView.PictureListener;
import android.webkit.WebViewClient;
import android.widget.ImageView;

@SuppressWarnings("deprecation")
@SuppressLint("SetJavaScriptEnabled")
public class WebImage extends WebViewClient{

	public WebView wv;
	private String url;
	private boolean zoom;
	private boolean control;
	private int color;
	
	private static String template;
	private static String getSource(Context context){
		
		if(template == null){
		
			try{
				InputStream is = context.getClassLoader().getResourceAsStream("com/androidquery/util/web_image.html");			
				template = new String(AQUtility.toBytes(is));
			}catch(Exception e){
				e.printStackTrace();
			}
			
		}
		
		return template;
		
	}
	
	private static final String PREF_FILE = "WebViewSettings";
	private static final String DOUBLE_TAP_TOAST_COUNT = "double_tap_toast_count";

	private static void fixWebviewTip(Context context){
		SharedPreferences prefs = context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
		if (prefs.getInt(DOUBLE_TAP_TOAST_COUNT, 1) > 0) {
		    prefs.edit().putInt(DOUBLE_TAP_TOAST_COUNT, 0).commit();
		}
	}
	
	public WebImage(WebView wv, boolean zoom, boolean control, int color){
		
		this.wv = wv;
		this.zoom = zoom;
		this.control = control;
		this.color = color;
		
		fixMaxZoom();
	}
	
	private void fixMaxZoom() {
		/*
		try{
			Class<?> webViewClass = wv.getClass();
			Field mZoomManagerField = webViewClass.getDeclaredField("mZoomManager");
			mZoomManagerField.setAccessible(true);
			Object mZoomManagerInstance = mZoomManagerField.get(wv);
			Class<?> zoomManagerClass = Class.forName("android.webkit.ZoomManager");
			Field mDefaultMaxZoomScaleField = zoomManagerClass.getDeclaredField("mDefaultMaxZoomScale");
			mDefaultMaxZoomScaleField.setAccessible(true);
			mDefaultMaxZoomScaleField.set(mZoomManagerInstance, Float.MAX_VALUE);
		} catch (Exception e) {
			e.printStackTrace();
		}
		*/
	}

	public void load(String url){
		this.url = "file://"+url;
		
		if(url.equals(wv.getTag(R.id.TAG_URL))){
			done(wv);
    		return;
    	}
		
		wv.clearView();	
    	wv.setTag(R.id.TAG_URL, url);
    	
    	if(android.os.Build.VERSION.SDK_INT <= 10){
    		wv.setDrawingCacheEnabled(true);
    	}
    	
    	fixWebviewTip(wv.getContext());
		
    	
    	WebSettings ws = wv.getSettings();
    	ws.setSupportZoom(zoom);
    	ws.setBuiltInZoomControls(zoom);
    	
    	if(!control){
    		disableZoomControl(wv);
    	}
    	
    	ws.setJavaScriptEnabled(true);
    	ws.setAllowFileAccess(true);
    	ws.setCacheMode(WebSettings.LOAD_NO_CACHE);
    	wv.setBackgroundColor(color);
    	
		if(wv.getWidth() > 0){
			setup();
		}else{
			delaySetup();
		}
		
		
		
	}
	
	public void reset() {
		this.url = null;
	}
	
	private void delaySetup(){
		Log.i("WebImage","delaySetup");
		wv.setPictureListener(new PictureListener() {
			

			@Override
			public void onNewPicture(WebView view, Picture picture) {
				Log.i("WebImage","onNewPicture");
				wv.setPictureListener(null);
				setup();
			}
			
			
		});
		
		//wv.setInitialScale(100);
		wv.loadData("<html></html>", "text/html", "utf-8");
		wv.setBackgroundColor(color);
		
	}
	
	
    private void setup(){
    	Log.i("WebImage","setup");
    	String source = getSource(wv.getContext());
    	String html = source.replace("@src", url).replace("@color", Integer.toHexString(color));
    	
    	wv.setWebViewClient(this);
    	
    	//wv.setInitialScale(100);
    	wv.loadDataWithBaseURL(null, html, "text/html", "utf-8", null);
    	wv.setBackgroundColor(color);
    	
    }
	
    public void done(WebView view){
    	Log.i("WebImage","done");
		view.setWebViewClient(null);
    }
    
    @Override
	public void onPageFinished(WebView view, String url) {
    	Log.i("WebImage","onPageFinished");
	}
	
	@Override
	public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
		Log.i("WebImage","onReceivedError");
//		done(view);
	}
	
	@Override
	public void onScaleChanged(WebView view, float oldScale, float newScale) {
		Log.i("WebImage","onScaleChanged");
		//AQUtility.debug("onScaleChanged", oldScale + ":" + newScale);
	}
	
	private static void disableZoomControl(WebView wv){
		
		if(android.os.Build.VERSION.SDK_INT < 11) return;
		
		WebSettings ws = wv.getSettings();		
		AQUtility.invokeHandler(ws, "setDisplayZoomControls", false, false, new Class[]{boolean.class}, false);
		
	}
	
	/*
    public static void webImage(WebView wv, String url, final View progress){
    	
    	if(url.equals(wv.getTag(AQuery.TAG_URL))){
    		return;
    	}
    	
    	wv.setTag(AQuery.TAG_URL, url);
    	
    	
    	fixWebviewTip(wv.getContext());
    	
		wv.loadData("<html></html>", "text/html", "utf-8");
		wv.setBackgroundColor(Color.parseColor("#000000"));
		
		//state, "init", "fetch"
		wv.setTag(AQuery.TAG_LAYOUT, "init");
		
		wv.setPictureListener(new PictureListener() {
			
			@Override
			public void onNewPicture(WebView view, Picture picture) {
				
				String url = (String) view.getTag(AQuery.TAG_URL);
				String state = (String) view.getTag(AQuery.TAG_LAYOUT);
				
				AQUtility.debug("pic fin", view.getTag(AQuery.TAG_LAYOUT));
				AQUtility.debug("width", view.getWidth());
				
				//int width = view.getWidth();
				
				if("init".equals(state)){
					view.setTag(AQuery.TAG_LAYOUT, "fetch");
					//lastWidth = width;
					setupWebview(view, url);					
				}else if("fetch".equals(state)){
    				view.setVisibility(View.VISIBLE);
					view.setPictureListener(null);
				}
				
			}
			
			
		});
    		
    	
    	if(progress != null){
    		progress.setVisibility(View.VISIBLE);
    	}
    	
    	
    	wv.setWebViewClient(new WebViewClient(){
    		
    		
    		@Override
    		public void onPageFinished(WebView view, String url) {
    			
    			AQUtility.debug("fin", view.getTag(AQuery.TAG_LAYOUT));
    			
    			if(("fetch".equals(view.getTag(AQuery.TAG_LAYOUT)))){
    				if(progress != null){
    					progress.setVisibility(View.GONE);
    				}
    				view.setWebViewClient(null);
    			}
    			
    		}
    		
    		@Override
    		public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
    			
    			if(progress != null){
    				progress.setVisibility(View.GONE);
    			}
    			
    		}
    	});
    	
    	
    }
    
    private static boolean setupWebview(WebView wv, String url){
    	
    	int width = wv.getWidth();
    	int height = wv.getHeight();
    	
    	wv.setInitialScale(100);
    	
    	if(width <= 0 || height <= 0 || url == null) return false;
    	
    	AQUtility.debug("webview", width + "x" + height);
    	
    	View parent = (View) wv.getParent();
    	AQUtility.debug("parent", parent.getWidth() + "x" + parent.getHeight());
    	
    	String html = getSource(wv.getContext());
    	html = html.replaceAll("@width", width + "").replaceAll("@height", height + "").replaceAll("@src", url);
    	
    	String dim;
    	
    	boolean landscape = width >= height;
    	
    	if(!landscape){
    		dim = "width:" + width + "px;";
    	}else{
    		dim = "height:" + height + "px;";
    	}
    	html = html.replaceAll("@dim", dim).replaceAll("@scape", landscape + "");	
    	
    	//AQUtility.debug(html);
    	
    	WebSettings ws = wv.getSettings();
    	ws.setSupportZoom(true);
    	ws.setBuiltInZoomControls(true);
    	
    	//ws.setDisplayZoomControls(false);
    	
    	ws.setJavaScriptEnabled(true);
    	
    	wv.loadDataWithBaseURL(null, html, "text/html", "utf-8", null);
    	
    	wv.setBackgroundColor(Color.parseColor("#000000"));
    	
    	return true;
    }
	*/
}