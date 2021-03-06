package org.metawatch.manager.apps;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.List;

import org.metawatch.manager.Application;
import org.metawatch.manager.FontCache;
import org.metawatch.manager.Idle;
import org.metawatch.manager.MetaWatch;
import org.metawatch.manager.MetaWatchService;
import org.metawatch.manager.MetaWatchService.AppLaunchMode;
import org.metawatch.manager.MetaWatchService.Preferences;
import org.metawatch.manager.Utils;
import org.metawatch.manager.actions.Action;
import org.metawatch.manager.actions.ActionManager;
import org.metawatch.manager.actions.ContainerAction;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Paint.Align;
import android.preference.PreferenceManager;
import android.text.TextPaint;
import android.text.format.DateFormat;
import android.util.Log;

public abstract class ApplicationBase {
	
	public final static int BUTTON_NOT_USED = 0;
	public final static int BUTTON_USED = 1;
	public final static int BUTTON_USED_DONT_UPDATE = 2;
	
	public final static int INACTIVE = 0;
	public final static int ACTIVE_IDLE = 1; // Active as an idle screen
	public final static int ACTIVE_POPUP = 2; // Active as a standalone app
	
	public int appState = INACTIVE;
	
	public static class AppData {
		public String id;
		public String name;
		
		public boolean supportsDigital = false;
		public boolean supportsAnalog = false;
		
		public String getPageSettingName() {
			return id+".app_enabled";
		}
	}
	
	public abstract AppData getInfo();
	public String getId() {
		return getInfo().id;
	}
	public boolean isToggleable() {
		return true;
	}

	public void setPageSetting(Context context, boolean value) {

		AppData info = getInfo();
		final String pageSetting = info.getPageSettingName();
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putBoolean(pageSetting, value);
		editor.commit();

		try {
			Field f = Preferences.class.getDeclaredField(pageSetting);
			f.setBoolean(null, value);
		} catch (Exception e) {
			if (Preferences.logging) Log.e(MetaWatch.TAG, "Error while changing preference attribute", e);
		}
		
	}
	
	// An app should do any required construction on the first call of activate or update
	public abstract void activate(Context context, int watchType);
	public abstract void deactivate(Context context, int watchType);
	
	protected void drawDigitalAppSwitchIcon(Context context, Canvas canvas, boolean preview) {		
		if (Preferences.clockOnAppScreens) {
			String time = DateFormat.getTimeFormat(context).format(new Date());
			
			Paint paint1 = new Paint();
			paint1.setColor(Color.BLACK);
	  
			Paint paint2 = new Paint();
			paint2.setColor(Color.WHITE);
			  
			Paint paintSmall = new TextPaint();
			paintSmall.setColor(Color.BLACK);
			paintSmall.setTextSize(FontCache.instance(context).SmallNumerals.size);
			paintSmall.setTypeface(FontCache.instance(context).SmallNumerals.face);
			paintSmall.setTextAlign(Align.RIGHT);
			  
			int w = (int)paintSmall.measureText(time);
			
			canvas.drawRect(new Rect(93-w,0,96,9), paint2);
			canvas.drawRect(new Rect(94-w,0,96,8), paint1);
			canvas.drawRect(new Rect(95-w,0,96,7), paint2);
			canvas.drawText(time, 96, 6, paintSmall);
			
			if (!preview)
				Utils.setAppClockRefreshAlarm(context);
		}
		else {
			if (appState == ACTIVE_IDLE || preview) { // if preview is true, it's for idle mode
				canvas.drawBitmap(Utils.getBitmap(context, "switch_app.png"), 87, 0, null);
				if (isToggleable()) {
					Bitmap bmp = Utils.getBitmap(context, "app_to_standalone.bmp");
					canvas.drawBitmap(bmp, 79, 0, null);				
				}
			} else if (appState == ACTIVE_POPUP) {
				canvas.drawBitmap(Utils.getBitmap(context, "exit_app.bmp"), 87, 0, null);
				if (isToggleable()) {
					Bitmap bmp = Utils.getBitmap(context, "app_to_idle.bmp");
					canvas.drawBitmap(bmp, 79, 0, null);				
				}
			}
		}
	}
	
	public abstract Bitmap update(Context context, boolean preview, int watchType);
	
	// Returns one of BUTTON_NOT_USED, BUTTON_USED or BUTTON_USED_DONT_UPDATE
	public abstract int buttonPressed(Context context, int id);
	
	public void open(Context context, boolean forcePopup) {
		if(appState != INACTIVE) {
			if (Preferences.logging) Log.d(MetaWatch.TAG, "InternalApp.open(): Ignored, app is not active.");
			return;
		}
		
		int page = Idle.getAppPage(getInfo().id);
		
		// Open the existing Idle app page.
		if (page != -1) {
			Idle.toPage(context, page);
			Idle.toIdle(context);
		
		// Open new app.
		} else {
			if (forcePopup || Preferences.appLaunchMode == AppLaunchMode.POPUP) {
				appState = ACTIVE_POPUP;
				int watchType = MetaWatchService.watchType;
				if (watchType == MetaWatchService.WatchType.DIGITAL) {
					Application.startAppMode(context, this);
					Application.toApp(context); // Will call update() to draw app screen.
				} else if (watchType == MetaWatchService.WatchType.ANALOG) {
					//FIXME
				}
				
			} else if (Preferences.appLaunchMode == AppLaunchMode.APP_PAGE) {
				page = Idle.addAppPage(context, this);
				Idle.toPage(context, page);
				Idle.toIdle(context);
			}
		}
	}
	
	public void setInactive() {
		appState = INACTIVE;
	}
	
	protected List<Action> getMenuActions() {
		return null;
	}
	
	protected void showMenu(final Context context) {
		
		final String appId = getId();
		
		List<Action> actions = getMenuActions();
		if (actions != null) {
			ContainerAction container = new ContainerAction() {

				Action backAction = new Action() {
					@Override
					public String getName() {
						return "-- Back --";
					}

					@Override
					public String bulletIcon() {
						return null;
					}

					@Override
					public int performAction(Context context) {
						AppManager.getApp(appId).open(context, false);
						return BUTTON_USED_DONT_UPDATE;
					}
				};
				
				@Override
				public String getName() {
					return "Menu";
				}
				
				@Override
				public Action getBackAction() {
					return backAction;
				}
				
			};
			
			List<Action> subActions = container.getSubActions();
			subActions.addAll(actions);
			
			ActionManager.displayAction(context, container);
		}
	}
}
