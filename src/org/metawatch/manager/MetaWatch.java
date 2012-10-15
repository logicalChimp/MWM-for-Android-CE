                                                                     
                                                                     
                                                                     
                                             
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
  * Main activity with tab container                                          *
  *                                                                           *
  *                                                                           *
  *****************************************************************************/


package org.metawatch.manager;

import java.io.IOException;
import java.io.InputStream;

import org.metawatch.communityedition.R;
import org.metawatch.manager.MetaWatchService.Preferences;
import org.metawatch.manager.apps.AppManager;

import android.app.AlertDialog;
import android.app.TabActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.webkit.WebView;
import android.widget.TabHost;

import com.bugsense.trace.BugSenseHandler;

public class MetaWatch extends TabActivity {
   
	public static final String TAG = "MetaWatch";
	
    public static Messenger mService = null;
	    
    public static long startupTime = 0;
    
    private static Context context = null;
        
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = this;
        
        // If you want to use BugSense for your fork, register with them
        // and place your API key in /assets/bugsense.txt
        // (This prevents me receiving reports of crashes from forked versions
        // which is somewhat confusing!)      
        try {
			InputStream inputStream = getAssets().open("bugsense.txt");
			String key = Utils.ReadInputStream(inputStream);
			key=key.trim();
			if (Preferences.logging) Log.d(MetaWatch.TAG, "BugSense enabled");
			BugSenseHandler.setup(this, key);
		} catch (IOException e) {
			if (Preferences.logging) Log.d(MetaWatch.TAG, "No BugSense keyfile found");
		}
        
		MetaWatchService.loadPreferences(this);
		AppManager.initApps(this);
        
        startupTime = System.currentTimeMillis();
        
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        final Resources res = getResources();
        final TabHost tabHost = getTabHost();

        tabHost.addTab(tabHost.newTabSpec("tab1")
        		.setIndicator("destroy")
                .setIndicator(res.getString(R.string.ui_tab_status),res.getDrawable(R.drawable.ic_tab_status))
                .setContent(new Intent(this, MetaWatchStatus.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)));

        tabHost.addTab(tabHost.newTabSpec("tab2")
                .setIndicator(res.getString(R.string.ui_tab_preferences),res.getDrawable(R.drawable.ic_tab_settings))
                .setContent(new Intent(this, Settings.class)));
        
        tabHost.addTab(tabHost.newTabSpec("tab3")
        		.setIndicator("destroy")
                .setIndicator(res.getString(R.string.ui_tab_widgets),res.getDrawable(R.drawable.ic_tab_widgets))
                .setContent(new Intent(this, WidgetSetup.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)));
        
        tabHost.addTab(tabHost.newTabSpec("tab4")
                .setIndicator(res.getString(R.string.ui_tab_tests),res.getDrawable(R.drawable.ic_tab_test))
                .setContent(new Intent(this, Test.class)));
		
		if (Preferences.watchMacAddress == "") {
			// Show the watch discovery screen on first start
			startActivity(new Intent(getApplicationContext(), DeviceSelection.class));
		}
		
		Protocol.configureMode();
		
		MetaWatchService.autoStartService(context);
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	
    	context = getApplicationContext();
		context.bindService(new Intent(MetaWatch.this, 
				MetaWatchService.class), mConnection, 0);
    	
    	MetaWatchStatus.displayStatus(this);
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.menu, menu);
	    return true;
	}
	
	@Override
	protected void onStart() {
		super.onStart();

	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
	    case R.id.about:
	    	showAbout();
	        return true;
	    case R.id.exit:	        
	    	exit();
	        return true;
	    default:
	        return super.onOptionsItemSelected(item);
	    }
	}
        
    void exit() {
    	stopService(new Intent(this, MetaWatchService.class));
    	MetaWatchService.setPreviousConnectionState(context, false);
    	System.exit(0);
    }
    
    void showAbout() {
    	
    	WebView webView = new WebView(this);
		String html = "<html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" /><title>About</title></head><body><center>" + 
						"<img src=\"banner.jpg\" width=\"100%\">" +
						"<p>Version " + Utils.getVersion(this) + ".</p>" +
						"<b>MetaWatch Community Team</b><br>" +
						"Joakim Andersson<br>Chris Boyle<br>Garth Bushell<br>Prash D<br>Matthias Gruenewald<br>"+
						"Richard Munn<br>Craig Oliver<br>Didi Pfeifle<br>Thierry Schork<br>Kyle Schroeder<br>"+
						"Chris Sewell<br>Geoff Willingham<br>Dobie Wollert<p>"+
						"<b>Translation Team</b><br>"+
						"Miguel Branco<br>Didi Pfeifle<br>Geurt Pieter Maassen van den Brink<br>Thierry Schork<br>"+
						"Kamosan Software<p>"+
						"<p>&copy; Copyright 2011-2012 Meta Watch Ltd.</p>" +
						"</center></body></html>";
        webView.loadDataWithBaseURL("file:///android_asset/", html, "text/html", "utf-8", null);
        
        new AlertDialog.Builder(this).setView(webView).setCancelable(true).setPositiveButton("OK", new DialogInterface.OnClickListener() {			
			//@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		}).show();        
    }
    
    /**
     * Handler of incoming messages from service.
     */
    static class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MetaWatchService.Msg.UPDATE_STATUS:
                    MetaWatchStatus.displayStatus(context);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
     
    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    
    final static IncomingHandler mIncomingHandler = new IncomingHandler();
    
    final static Messenger mMessenger = new Messenger(new IncomingHandler());

    /**
     * Class for interacting with the main interface of the service.
     */
    public static ServiceConnection mConnection = new ServiceConnection() {
    	   	
        public void onServiceConnected(ComponentName className,
                IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  We are communicating with our
            // service through an IDL interface, so get a client-side
            // representation of that from the raw service object.
            mService = new Messenger(service);

            // We want to monitor the service for as long as we are
            // connected to it.
            try {
                Message msg = Message.obtain(null,
                        MetaWatchService.Msg.REGISTER_CLIENT);
                msg.replyTo = mMessenger;
                mService.send(msg);
            } catch (RemoteException e) {

            }
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mService = null;
        }
    };
  
}
