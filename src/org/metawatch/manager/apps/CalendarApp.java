package org.metawatch.manager.apps;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.metawatch.manager.FontCache;
import org.metawatch.manager.MetaWatch;
import org.metawatch.manager.MetaWatchService.Preferences;
import org.metawatch.manager.Monitors;
import org.metawatch.manager.Utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Point;
import android.graphics.Rect;
import android.text.TextPaint;
import android.text.format.DateUtils;
import android.util.Log;

public class CalendarApp extends ApplicationBase {

	public final static String APP_ID = "org.metawatch.manager.apps.CalendarApp";
	
	static AppData appData = new AppData() {{
		id = APP_ID;
		name = "Calendar";
	
		supportsDigital = true;
		supportsAnalog = false;
	}};
	
	@Override
	public AppData getInfo() {
		return appData;
	}

	@Override
	public void activate(Context context, int watchType) {
	}

	@Override
	public void deactivate(Context context, int watchType) {
	}
	
	public int getColumn(int dow)
	{
		switch(dow) {
		case Calendar.MONDAY:
			return 5;
		
		case Calendar.TUESDAY:
			return 18;
			
		case Calendar.WEDNESDAY:
			return 31;
			
		case Calendar.THURSDAY:
			return 44;
			
		case Calendar.FRIDAY:
			return 57;
			
		case Calendar.SATURDAY:
			return 70;
			
		case Calendar.SUNDAY:
			return 83;
		}
		
		return 0;
	}
	
	long lastRefresh = 0;
	private List<Utils.CalendarEntry> calendarEntries = null;
	
	private void refresh(final Context context) {
		Thread thread = new Thread("CalendarApp.refresh") {
			@Override
			public void run() {

				boolean readCalendar = false;
				long time = System.currentTimeMillis();
				if ((time - lastRefresh > 5*DateUtils.MINUTE_IN_MILLIS) || (Monitors.calendarChangedTimestamp > lastRefresh)) {
					readCalendar = true;
					lastRefresh = System.currentTimeMillis();
				}
				
				if (readCalendar) {
					if (Preferences.logging) Log.d(MetaWatch.TAG, "CalendarApp.refresh() start");
					
					Calendar cal = Calendar.getInstance();
					cal.set(Calendar.DAY_OF_MONTH, 1);
					
					long startTime = cal.getTimeInMillis();
					long endTime = startTime + DateUtils.DAY_IN_MILLIS * 32;
					
					calendarEntries = Utils.readCalendar(context, startTime, endTime, false);
										
					if (Preferences.logging) Log.d(MetaWatch.TAG, "CalendarApp.refresh() stop - "+ (calendarEntries == null ? "0" : calendarEntries.size()) + " entries found");   
				}
				
			}
		};
		thread.start();
	}

	@Override
	public Bitmap update(Context context, boolean preview, int watchType) {
		
		refresh(context);
		
		Bitmap bitmap = Bitmap.createBitmap(96, 96, Bitmap.Config.RGB_565);
		Canvas canvas = new Canvas(bitmap);
		canvas.drawColor(Color.WHITE);	
		
		canvas.drawBitmap(Utils.getBitmap(context, "calendar_app_month.png"), 0, 0, null);	
		
		TextPaint paintSmall = new TextPaint();
		paintSmall.setColor(Color.WHITE);
		paintSmall.setTextSize(FontCache.instance(context).Small.size);
		paintSmall.setTypeface(FontCache.instance(context).Small.face);
		paintSmall.setTextAlign(Align.LEFT);
		
		TextPaint paintSmallNumerals = new TextPaint();
		paintSmallNumerals.setColor(Color.BLACK);
		paintSmallNumerals.setTextSize(FontCache.instance(context).SmallNumerals.size);
		paintSmallNumerals.setTypeface(FontCache.instance(context).SmallNumerals.face);
		paintSmallNumerals.setTextAlign(Align.LEFT);
		
		Paint paintBlack = new Paint();
		paintBlack.setColor(Color.BLACK);
		
		Calendar cal = Calendar.getInstance();
		
		SimpleDateFormat df = new SimpleDateFormat("MMMM yyyy");
		canvas.drawText(df.format(cal.getTime()), 4, 8, paintSmall);
		
		final int month = cal.get(Calendar.MONTH);
		final int today = cal.get(Calendar.DAY_OF_MONTH);
		
		int yPos = 18;
		
		Map<Integer,Point> dateCells = new HashMap<Integer,Point>(); 
		
		final int days = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
		for(int day=1; day<=days; ++day) {
			cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), day);
					
			final int dow = cal.get(Calendar.DAY_OF_WEEK);
			final int xPos = getColumn(dow);
			
			dateCells.put(cal.get(Calendar.DAY_OF_MONTH), new Point(xPos,yPos));
		
			if (day==today) {
				canvas.drawBitmap(Utils.getBitmap(context, "calendar_app_today.png"), xPos-4, yPos-4, null);
			}
			
			canvas.drawText(""+day, xPos, yPos+5, paintSmallNumerals);
			
			if (dow==Calendar.SUNDAY)
				yPos += 13;
		}
		
		if (calendarEntries!=null) {
			
			for (Utils.CalendarEntry entry : calendarEntries) {
				cal.setTimeInMillis(entry.startTimestamp);
				
				if( cal.get(Calendar.MONTH) == month ) {
					final int dom = cal.get(Calendar.DAY_OF_MONTH);
					final Point cellLoc = dateCells.get(dom);
					
					if(entry.isAllDay) {
						canvas.drawLine( cellLoc.x-1, cellLoc.y+6, cellLoc.x+9, cellLoc.y+6, paintBlack);
					}
					else {
						// Each horizontal pixel represents 2 hours, giving a span of 7am to 11pm
						final int hour = cal.get(Calendar.HOUR_OF_DAY);						
						int xStart = java.lang.Math.max(0, (hour - 7)/2 );
						int xEnd = java.lang.Math.min(8, xStart + (int)((entry.endTimestamp - entry.startTimestamp) / DateUtils.HOUR_IN_MILLIS * 2 ) );
						
						if( xEnd > xStart )
							canvas.drawRect( new Rect( cellLoc.x+xStart, cellLoc.y+6, cellLoc.x+xEnd, cellLoc.y+9 ), paintBlack);
					}
				}
			}
		}
		
		drawDigitalAppSwitchIcon(context, canvas, preview);
		
		return bitmap;
	}

	@Override
	public int buttonPressed(Context context, int id) {
		return BUTTON_NOT_USED;
	}

}
