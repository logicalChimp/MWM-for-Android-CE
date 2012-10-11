                                                                     
                                                                     
                                                                     
                                             
 /*****************************************************************************
  *  Copyright (c) 2011 Meta Watch Ltd.                                       *
  *  www.MetaWatch.org                                                        *
  *                                                                           *
  =============================================================================
  *                                                                           *
  *  Licensed under the Apache License, Version 2.0 (the "License");          *
  *  you may not use this file except in compliance with the License.         *
  *  You may obtain a copy of the License at                                  *
  *                                                                           *
  *    http://www.apache.org/licenses/LICENSE-2.0                             *
  *                                                                           *
  *  Unless required by applicable law or agreed to in writing, software      *
  *  distributed under the License is distributed on an "AS IS" BASIS,        *
  *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
  *  See the License for the specific language governing permissions and      *
  *  limitations under the License.                                           *
  *                                                                           *
  *****************************************************************************/

 /*****************************************************************************
  * MetaWatch.java                                                            *
  * MetaWatch                                                                 *
  * status activity                                                           *
  *                                                                           *
  *                                                                           *
  *****************************************************************************/

package org.metawatch.manager;

import org.metawatch.communityedition.R;
import org.metawatch.manager.MetaWatchService.GeolocationMode;
import org.metawatch.manager.MetaWatchService.Preferences;
import org.metawatch.manager.MetaWatchService.WeatherProvider;
import org.metawatch.manager.Monitors.LocationData;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.ToggleButton;

public class MetaWatchStatus extends Activity {
	
	public static TextView textView = null;	
	public static ToggleButton toggleButton = null;
	 
	private static Context context = null;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        context = this;                         
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    	
        textView = (TextView) findViewById(R.id.textview);
		toggleButton = (ToggleButton) findViewById(R.id.toggleButton);
		
		toggleButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	if(toggleButton.isChecked())
            		startService();
            	else
            		stopService();
            }
        });
		
		displayStatus(this);
    }
    
    static void displayStatus(Context context) {
    	setButtonState(context);
    	
    	Resources res = context.getResources();
    	textView.setText(res.getString(R.string.app_name_long));
    	textView.append("\n\n");
    	
    	switch (MetaWatchService.connectionState) {
	    	case MetaWatchService.ConnectionState.DISCONNECTED:
	    		Utils.appendColoredText(textView, res.getString(R.string.connection_disconnected).toUpperCase(), Color.RED);
	    		break;
	    	case MetaWatchService.ConnectionState.CONNECTING:
	    		Utils.appendColoredText(textView, res.getString(R.string.connection_connecting).toUpperCase(), Color.YELLOW);
	    		break;
	    	case MetaWatchService.ConnectionState.CONNECTED:
	    		Utils.appendColoredText(textView, res.getString(R.string.connection_connected).toUpperCase(), Color.GREEN);
	    		break;
	    	case MetaWatchService.ConnectionState.DISCONNECTING:
	    		Utils.appendColoredText(textView, res.getString(R.string.connection_disconnecting).toUpperCase(), Color.YELLOW);
	    		break;
    	}
    	textView.append("\n");
    	
    	if (Preferences.weatherProvider != WeatherProvider.DISABLED) {
    		textView.append("\n");
    		if (Monitors.weatherData.error) {
    			Utils.appendColoredText(textView, "ERROR: " , Color.RED);
    			Utils.appendColoredText(textView, Monitors.weatherData.errorString, Color.RED);
    			textView.append("\n");
    		}
    		if (Monitors.weatherData.received) {
    			textView.append(res.getString(R.string.status_weather_last_updated));
    			textView.append("\n  ");
    			textView.append(res.getString(R.string.status_weather_forecast));
    			textView.append("\n    ");
    			printDate(Monitors.weatherData.forecastTimeStamp);
    			textView.append("  ");
    			textView.append(res.getString(R.string.status_weather_observation));
    			textView.append("\n    ");
    			printDate(Monitors.weatherData.timeStamp);
    		}
    		else {
    			textView.append(res.getString(R.string.status_weather_waiting));
    		}
    	}
    	
    	if (Preferences.weatherGeolocationMode != GeolocationMode.MANUAL) {
    		textView.append("\n");
    		if (LocationData.received) {
    			textView.append(res.getString(R.string.status_location_updated));
    			textView.append("\n  ");
    			printDate(LocationData.timeStamp);
    		}
    		else {
    			textView.append(res.getString(R.string.status_location_waiting));
    			textView.append("\n");
    		}
    	}
    	
    	textView.append("\n");
    	if (Utils.isAccessibilityEnabled(context)) {    		
	    	if (MetaWatchAccessibilityService.accessibilityReceived) {
	    		Utils.appendColoredText(textView, res.getString(R.string.status_accessibility_working), Color.GREEN);
	    	}
	    	else {
	    		if(MetaWatch.startupTime==0 || System.currentTimeMillis()-MetaWatch.startupTime<60*1000) {
	    			textView.append(res.getString(R.string.status_accessibility_waiting));
	    		}
	    		else {
	    			Utils.appendColoredText(textView, res.getString(R.string.status_accessibility_failed), Color.RED);
	    		}
	    	}
	    }
    	else {
    		textView.append(res.getString(R.string.status_accessibility_disabled));
    	}
    	textView.append("\n");
    
    	textView.append("\n"+res.getString(R.string.status_message_queue)+" " + Protocol.getQueueLength());
    	textView.append("\n"+res.getString(R.string.status_notification_queue)+" " + Notification.getQueueLength() + "\n");
    	
    	if(Preferences.showNotificationQueue) {
    		textView.append(Notification.dumpQueue());
    	}
    	
    	textView.append("\n\n\nStatus updated at ");
		printDate(System.currentTimeMillis());
    }
    
    private static void printDate(long ticks) {
    	if(ticks==0) {
    		textView.append(context.getResources().getString(R.string.status_loading));
    	}
    	else {
	    	textView.append(Utils.ticksToText(context, ticks));
    	}
    	textView.append("\n");
    }
    
	void startService() {

		Context context = getApplicationContext();
		if(!MetaWatchService.isRunning()) {
			context.bindService(new Intent(MetaWatchStatus.this, 
					MetaWatchService.class), MetaWatch.mConnection, Context.BIND_AUTO_CREATE);
		}
		
        setButtonState(context);

	}
	
    void stopService() {

		Context context = getApplicationContext();
        try {
        	context.stopService(new Intent(this, MetaWatchService.class));
            context.unbindService(MetaWatch.mConnection);            	
        }
        catch(Throwable e) {
        	// The service wasn't running
        	if (Preferences.logging) Log.d(MetaWatch.TAG, e.getMessage());          	
        }

        setButtonState(context);
    }

	private static void setButtonState(Context context) {
		toggleButton.setChecked(MetaWatchService.isRunning());
	}
}