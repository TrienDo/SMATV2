package uk.lancs.sharc.smat.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;

import com.dropbox.sync.android.DbxAccountManager;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.TextUtils;
import android.view.View;

import uk.lancs.sharc.smat.service.SharcLibrary;

/**
 * <p>InteractionLog helps log key user interactions in SMEP </p>
 * <p>It provides the addLog method to log interactions</p>
 *
 * Author: Trien Do
 * Date: May 2015
 */

public class InteractionLog {
	private OutputStreamWriter oswLogWriter; 			//Store log file in the Sharc folder of the External Storage of the device
	private OutputStreamWriter oswBugLogWriter; 			//Store log file in the Sharc folder of the External Storage of the device
	private String deviceID;							//An unique ID for each device = Build.SERIAL;
	private Hashtable<String, String> actionNames; 		//A map of ActionID - Action name (human readable format)
	private Activity activity;
	private GoogleMap mMap;
	
	public InteractionLog(Activity activity, GoogleMap mMap)
	{
		actionNames = new Hashtable<String, String>();
		createActionNameHashtable();					//Fill in the map of ActionID - Action name
		
		//Get log file ready for writing log
		//Logfile path = Sharc/smepLog.csv
		//E.g., in Nexus 7: root/storage/emulated/0/Sharc/smepLog.csv 
		FileOutputStream fos = null;
		//FileOutputStream fosException = null;
		try {
			fos = new FileOutputStream(SharcLibrary.SHARC_LOG_FOLDER + File.separator + "smatLog" + SharcLibrary.getReadableTimeStamp() + ".csv", true);		//true = append
			//fosException = new FileOutputStream(SharcLibrary.SHARC_LOG_FOLDER + File.separator + "smatBugLog" + SharcLibrary.getReadableTimeStamp() + ".csv", true);		//true = append
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		oswLogWriter = new OutputStreamWriter(fos);
		//oswBugLogWriter = new OutputStreamWriter(fosException);
		//Get device id
		deviceID = Build.SERIAL;
		this.activity = activity;
		this.mMap = mMap;
	}
	
	public void addLog(LatLng location, DbxAccountManager userInfo, String actionID, String actionData)
	{
		//Add a log line to the log file
		//A log line format: DateAndTime,LatLng,DeviceID,UserID,ActionID,ActionName,ActionData
		// - DateAndTime: Local time e.g., Thu May 07 13:44:29 BST 2015
		// - LatLng: LAT LNG (space to separate them
		// - UserID: = Dropbox account ID if the user logs into SMEP with their dropbox account else = anonymous
		// - ActionID and ActionName: see the table below
		// - ActionData: depends on the ActionID		
		//Example of a log line: Thu May 07 13:44:29 BST 2015,54.00594448 -2.78566378,0a282ca7,387643271,02,SELECT_YAH,SmallRed
		try {
			ArrayList<String> logData = new ArrayList<String>();
			String timeStamp = (new Date()).toString();

			logData.add(timeStamp);

			if (location == null)
				logData.add("undefined");
			else
				logData.add(location.latitude + " " + location.longitude);

			logData.add(deviceID);

			if (userInfo == null || !userInfo.hasLinkedAccount())
				logData.add("anonymous");
			else
				logData.add(userInfo.getLinkedAccount().getUserId());


			logData.add(actionID);
			logData.add(actionNames.get(actionID));
			logData.add(actionData);

			try {
				oswLogWriter.append(TextUtils.join(",", logData.toArray()));//Separate fields by comma
				oswLogWriter.append(System.getProperty("line.separator"));
				oswLogWriter.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		catch(Exception e){e.printStackTrace();}
		//takeScreenShot();
	}

	public void addLog(String location, DbxAccountManager userInfo, String actionID, String actionData)
	{
		//Add a log line to the log file
		//A log line format: DateAndTime,LatLng,DeviceID,UserID,ActionID,ActionName,ActionData
		// - DateAndTime: Local time e.g., Thu May 07 13:44:29 BST 2015
		// - LatLng: LAT LNG (space to separate them
		// - UserID: = Dropbox account ID if the user logs into SMEP with their dropbox account else = anonymous
		// - ActionID and ActionName: see the table below
		// - ActionData: depends on the ActionID
		//Example of a log line: Thu May 07 13:44:29 BST 2015,54.00594448 -2.78566378,0a282ca7,387643271,02,SELECT_YAH,SmallRed

		ArrayList<String> logData = new ArrayList<String>();
		String timeStamp = (new Date()).toString();

		logData.add(timeStamp);

		if(location == null)
			logData.add("undefined");
		else
			logData.add(location);

		logData.add(deviceID);

		if(userInfo == null || !userInfo.hasLinkedAccount())
			logData.add("anonymous");
		else
			logData.add(userInfo.getLinkedAccount().getUserId());


		logData.add(actionID);
		logData.add(actionNames.get(actionID));
		logData.add(actionData);

		try {
			oswLogWriter.append(TextUtils.join(",", logData.toArray()));//Separate fields by comma
			oswLogWriter.append(System.getProperty("line.separator"));
			oswLogWriter.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		//takeScreenShot();
	}

	public void addBugLogNotUsed(){
		//write log to file
		int pid = android.os.Process.myPid();
		String appProcessID = String.valueOf(pid);
		try {
			String command = String.format("logcat -d -v threadtime *:*");
			Process process = Runtime.getRuntime().exec(command);

			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			StringBuilder result = new StringBuilder();
			String currentLine = null;

			while ((currentLine = reader.readLine()) != null) {
				if (currentLine != null && currentLine.contains(appProcessID)) {
					result.append(currentLine);
					result.append(System.getProperty("line.separator"));
				}
			}
			oswBugLogWriter.append(result.toString());
			oswBugLogWriter.flush();
			//oswBugLogWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}


		//clear the log
		try {
			Runtime.getRuntime().exec("logcat -c");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void takeScreenShot()
	{
		Bitmap mainBitmap;
		try {
			String mPath = SharcLibrary.SHARC_FOLDER + "/screenshots/ss_" + SharcLibrary.getReadableTimeStamp() + ".png";
			View v1 = this.activity.getWindow().getDecorView().getRootView();
			v1.setDrawingCacheEnabled(true);
			mainBitmap = Bitmap.createBitmap(v1.getDrawingCache());
			v1.setDrawingCacheEnabled(false);
			OutputStream fout = null;
			File imageFile = new File(mPath);
			fout = new FileOutputStream(imageFile);
			mainBitmap.compress(Bitmap.CompressFormat.JPEG, 90, fout);
			fout.flush();
			fout.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		GoogleMap.SnapshotReadyCallback callback = new GoogleMap.SnapshotReadyCallback()
		{
			@Override
			public void onSnapshotReady(Bitmap snapshot)
			{
				// TODO Auto-generated method stub
				Bitmap mapBitmap = snapshot;

				OutputStream fout = null;

				String mPath = SharcLibrary.SHARC_FOLDER + "/screenshots/ss_" + SharcLibrary.getReadableTimeStamp() + ".png";

				try
				{
					File imageFile = new File(mPath);
					fout = new FileOutputStream(imageFile);
					// Write the string to the file
					mapBitmap.compress(Bitmap.CompressFormat.JPEG, 90, fout);
					fout.flush();
					fout.close();
				}
				catch (FileNotFoundException e)
				{
					e.printStackTrace();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		};
		mMap.snapshot(callback);
	}

	public void takeCombinedScreenShot()
	{
		Bitmap frameBitmap = null;
		try {
			View v1 = this.activity.getWindow().getDecorView().getRootView();
			v1.setDrawingCacheEnabled(true);
			frameBitmap = Bitmap.createBitmap(v1.getDrawingCache());
			v1.setDrawingCacheEnabled(false);
		} catch (Exception e) {
			e.printStackTrace();
		}
		final Bitmap mainBitmap = frameBitmap;

		GoogleMap.SnapshotReadyCallback callback = new GoogleMap.SnapshotReadyCallback()
		{
			@Override
			public void onSnapshotReady(Bitmap snapshot)
			{
				Bitmap outBitmap = Bitmap.createBitmap(mainBitmap.getWidth(),mainBitmap.getHeight(), Bitmap.Config.ARGB_8888);
				Bitmap mapBitmap = snapshot;
				Canvas c = new Canvas(outBitmap);
				c.drawBitmap(mainBitmap, new Matrix(), null);
				c.drawBitmap(mapBitmap, new Matrix(), null);

				OutputStream fout = null;
				String mPath = SharcLibrary.SHARC_FOLDER + "/screenshots/ss_" + SharcLibrary.getReadableTimeStamp() + ".png";
				try
				{
					File imageFile = new File(mPath);
					fout = new FileOutputStream(imageFile);
					// Write the string to the file
					outBitmap.compress(Bitmap.CompressFormat.JPEG, 90, fout);
					fout.flush();
					fout.close();
				}
				catch (FileNotFoundException e)
				{
					e.printStackTrace();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		};
		mMap.snapshot(callback);
	}
	public static final String START_APP 						= "00";
	public static final String EXIT_APP 						= "01";
	public static final String SELECT_YAH 						= "02"; //Sliding menu: Set You-Are-Here (YAH) marker
	public static final String SELECT_LOGIN 					= "03"; //Sliding menu:
	public static final String SELECT_LOGOUT 					= "04"; //Sliding menu:
	public static final String VIEW_CACHED_EXPERIENCES 			= "05"; //Sliding menu: Play a downloaded experience 
	public static final String VIEW_ONLINE_EXPERIENCES 			= "06"; //Sliding menu: Download experiences	
	
	public static final String DOWNLOAD_ONLINE_EXPERIENCE		= "07"; //A dialog shows when an online experience marker on map is selected: Download 
	public static final String CANCEL_DOWNLOAD_EXPERIENCE 		= "08"; //A dialog shows when an online experience marker on map is selected: Cancel
	public static final String PLAY_EXPERIENCE 					= "09"; //A dialog shows when a cached experience marker on map is selected: Play
	public static final String CANCEL_PLAY_EXPERIENCE 			= "10"; //A dialog shows when a cached experience marker on map is selected: Cancel
	public static final String DELETE_EXPERIENCE 				= "11"; //A dialog shows when a cached experience marker on map is selected: Delete
	
	public static final String SELECT_PUSH		 				= "12"; //Sliding menu: Push Media
	public static final String SELECT_SOUND		 				= "13"; //Sliding menu: Sound Notification
	public static final String SELECT_VIBRATION	 				= "14"; //Sliding menu: Vibration Notification
	public static final String SELECT_SETELLITE	 				= "15"; //Sliding menu: Satellite Maps
	public static final String SELECT_ROTATION	 				= "16"; //Sliding menu: Auto-roate Maps
	public static final String SELECT_YAH_CENTRED 				= "17"; //Sliding menu: Keep YAH marker centred on Maps
	public static final String SELECT_TEST		 				= "18"; //Sliding menu: Test Mode
	public static final String SELECT_SHOW_TRIGGER_ZONE 		= "19"; //Sliding menu: Show Trigger Zones
	public static final String SELECT_SHOW_POI_THUMBS 			= "20"; //Sliding menu: Show POIs
	public static final String SELECT_RESET_POI	 				= "21"; //Sliding menu: Reset POIs

	
	public static final String SELECT_MAP_TAB	 				= "22"; //Tab view: Map View
	public static final String SELECT_POI_TAB	 				= "23"; //Tab view: POI Media
	public static final String SELECT_EOI_TAB	 				= "24"; //Tab view: EOI Media
	public static final String SELECT_SUMMARY_TAB 				= "25"; //Tab view: Summary Info
	public static final String SELECT_RESPONSE_TAB 				= "26"; //Tab view: My Response
	
	public static final String OPEN_RESPONSE_DIALOG				= "27"; //Button: Add response with 1 OF 4 value: 0 - NEW (new POI), 1 - POI, 2 - EOI, 3 - ROUTE -> Just open, not added yet
	public static final String ADD_RESPONSE_TEXT 				= "28"; //Dialog Add response -> Button: Add Text -> Save
	public static final String ADD_RESPONSE_IMAGE				= "29"; //Dialog Add response -> Button: Add Image -> Save
	public static final String ADD_RESPONSE_AUDIO				= "30"; //Dialog Add response -> Button: Add Audio -> Save
	public static final String ADD_RESPONSE_VIDEO				= "31"; //Dialog Add response -> Button: Add Video -> Save
	public static final String ADD_RESPONSE_DESC				= "32"; //Dialog Add response -> Button: Add Video -> Save
	
	public static final String SELECT_UPLOAD_RESPONSE			= "33"; //Response Tab -> Button: Upload
	public static final String SELECT_VIEW_RESPONSE				= "34"; //Response Tab -> Button: View
	public static final String SELECT_DELETE_RESPONSE			= "35"; //Response Tab -> Button: Delete
	
	public static final String SELECT_BACK_BUTTON				= "36"; //Button Back of Android
	public static final String CREATE_EXPERIENCE				= "37"; //Dialog Create a new experience -> Button: Create
	public static final String START_ROUTE						= "38"; //Map view -> Button: Start recording route
	public static final String PAUSE_ROUTE						= "39"; //Map view -> Button: Pause recording route
	public static final String RESUME_ROUTE						= "40"; //Map view -> Button: Resume recording route
	public static final String STOP_ROUTE						= "41"; //Map view -> Button: Stop
	public static final String SAVE_ROUTE						= "42"; //Dialog Save a new route -> Button: Save
	public static final String CREATE_POI						= "43"; //Map View -> Button Add a new POI for your current location -> Dialog Create a new experience -> Button: Create

	public static final String SELECT_PUSH_AGAIN				= "44"; //Sliding menu: Push media when revisiting POIs
	public static final String SELECT_UPLOAD_ALL				= "45"; //Tab Review and Upload -> Button Upload all authoring action
	public static final String CONTINUE_ROUTE					= "46"; //Continue a route when the app crashes
	public static final String LOCATION_CHANGE					= "47"; //LatLng of current location
	public static final String SHOW_GPS_INFO					= "48"; //Show GPS in title bar

	private void createActionNameHashtable()
	{
		actionNames.put("00", "START_APP");
		actionNames.put("01", "EXIT_APP");
		actionNames.put("02", "SELECT_YAH");
		actionNames.put("03", "SELECT_LOGIN");
		actionNames.put("04", "SELECT_LOGOUT");
		actionNames.put("05", "VIEW_CACHED_EXPERIENCES");
		actionNames.put("06", "VIEW_ONLINE_EXPERIENCES");
		
		actionNames.put("07", "DOWNLOAD_ONLINE_EXPERIENCE");
		actionNames.put("08", "CANCEL_DOWNLOAD_EXPERIENCE");
		actionNames.put("09", "PLAY_EXPERIENCE");
		actionNames.put("10", "CANCEL_PLAY_EXPERIENCE");
		actionNames.put("11", "DELETE_EXPERIENCE");
		
		actionNames.put("12", "SELECT_PUSH");
		actionNames.put("13", "SELECT_SOUND");
		actionNames.put("14", "SELECT_VIBRATION");
		actionNames.put("15", "SELECT_SETELLITE");
		actionNames.put("16", "SELECT_ROTATION");
		actionNames.put("17", "SELECT_YAH_CENTRED");
		actionNames.put("18", "SELECT_TEST");
		actionNames.put("19", "SELECT_TRIGGER_ZONE");
		actionNames.put("20", "SELECT_POI_THUMBS");
		actionNames.put("21", "SELECT_RESET_POI");
		
		actionNames.put("22", "SELECT_MAP_TAB");
		actionNames.put("23", "SELECT_POI_TAB");
		actionNames.put("24", "SELECT_EOI_TAB");
		actionNames.put("25", "SELECT_SUMMARY_TAB");
		actionNames.put("26", "SELECT_RESPONSE_TAB");
		
		actionNames.put("27", "OPEN_RESPONSE_DIALOG");
		actionNames.put("28", "ADD_RESPONSE_TEXT");
		actionNames.put("29", "ADD_RESPONSE_IMAGE");
		actionNames.put("30", "ADD_RESPONSE_AUDIO");
		actionNames.put("31", "ADD_RESPONSE_VIDEO");
		actionNames.put("32", "ADD_RESPONSE_DESC");
		
		actionNames.put("33", "SELECT_UPLOAD_RESPONSE");
		actionNames.put("34", "SELECT_VIEW_RESPONSE");
		actionNames.put("35", "SELECT_DELETE_RESPONSE");
		
		actionNames.put("36", "SELECT_BACK_BUTTON");

		actionNames.put("37", "CREATE_EXPERIENCE");
		actionNames.put("38", "START_ROUTE");
		actionNames.put("39", "PAUSE_ROUTE");
		actionNames.put("40", "RESUME_ROUTE");
		actionNames.put("41", "STOP_ROUTE");
		actionNames.put("42", "SAVE_ROUTE");
		actionNames.put("43", "CREATE_POI");

		actionNames.put("44", "SELECT_PUSH_AGAIN");
		actionNames.put("45", "SELECT_UPLOAD_ALL");
		actionNames.put("46", "CONTINUE_ROUTE");
		actionNames.put("47", "LOCATION_CHANGE");
		actionNames.put("48", "SHOW_GPS_INFO");
	}
}
