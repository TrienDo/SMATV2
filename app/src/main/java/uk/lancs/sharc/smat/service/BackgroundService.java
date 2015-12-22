package uk.lancs.sharc.smat.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import uk.lancs.sharc.smat.model.SMEPAppVariable;
import uk.lancs.sharc.smat.model.POIModel;
import com.google.android.gms.maps.model.LatLng;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Vibrator;
import android.util.Log;

/**
 * <p>This class is a background service which enables SMEP to keep tracking the user current location
 * even when the user switch to another app.</p>
 *
 * Author: Trien Do
 * Date: Feb 2014
 */
public class BackgroundService extends Service
{
	private static final String TAG = "SMEP_SERVICE";
	private LocationManager mLocationManager = null;
	private ArrayList<POIModel> allPOIs;
    private Hashtable<Integer,Long> shownLocation;

	private class LocationListener implements android.location.LocationListener
    {
	    Location mCurrentLocation;//Store current location

	    public LocationListener(String provider)
        {
	        Log.e(TAG, "LocationListener provider: " + provider);
	        mCurrentLocation = new Location(provider);
	    }
	    
	    @Override
	    public void onLocationChanged(Location location)
        {
	    	if(location.getAccuracy() > 0)
				return;

			SMEPAppVariable mySMEPAppVariable = (SMEPAppVariable) getApplicationContext();//Get the global settings of SMEP
	    	if(mySMEPAppVariable.isNewExperience())
            {
	    		allPOIs = mySMEPAppVariable.getAllPOIs();
	    		shownLocation.clear();
	    		mySMEPAppVariable.setNewExperience(false);
	    	}

            if(mySMEPAppVariable.isResetPOI())
		    {
		    	//Clear array of pushed media
		    	if(!shownLocation.isEmpty())
		    		shownLocation.clear();
		    	mySMEPAppVariable.setResetPOI(false);
		    }

            if(allPOIs != null)
		    {	
			    if(mySMEPAppVariable.isTestMode())
			    {
			    	mCurrentLocation.set(mySMEPAppVariable.getMockLocation());
			    	findTriggerZone(mySMEPAppVariable.getMockLocation());
			    }
			    else
			    {
			    	if(location.getAccuracy() < 10) {
						mCurrentLocation.set(location);
						findTriggerZone(location);
					}
			    }
		    }
	    }
	    
	    private void findTriggerZone(Location L1)
	    {
			if(allPOIs.size()>0)
			{
				boolean isWithin = false;
				LatLng tmpPoint;
				for (int i = 0; i < allPOIs.size(); i++)
				{
					isWithin = false;
										
					if(allPOIs.get(i).getTriggerType().equalsIgnoreCase("circle"))
					{
						float[] results = new float[1];
						tmpPoint = allPOIs.get(i).getCoordinates().get(0);
						Location.distanceBetween(L1.getLatitude(),L1.getLongitude(), tmpPoint.latitude,tmpPoint.longitude, results);
						if(results[0] < allPOIs.get(i).getRadius())//radius of circle
							isWithin = true;
					}
					else if(allPOIs.get(i).getTriggerType().equalsIgnoreCase("polygon"))
					{						
						List<LatLng> polyPath = allPOIs.get(i).getCoordinates();
						isWithin = SharcLibrary.isCurrentPointInsideRegion(new LatLng(L1.getLatitude(), L1.getLongitude()), polyPath);
					}					
					
					if(isWithin)
					{
                        SMEPAppVariable mySMEPAppVariable = (SMEPAppVariable) getApplicationContext();
                        if(shownLocation.get(i) == null)//If the user has not visited this POI
						{	
							shownLocation.put(i, new Date().getTime());       //push id in the hashmap to record ID and time that a POI is visited
                            pushMediaForPOI(i);

						}
                        else if(mySMEPAppVariable.isPushAgain())//The user has already visited this POI but wants media to be push again when revisiting
                        {
							long period = new Date().getTime() - shownLocation.get(i);//Get milliseconds between last push and current time
							if(period >= mySMEPAppVariable.getTimeThreshold())        //only push again if the user comes back after time threshold - converted to millisecond
                            {
                                shownLocation.put(i,new Date().getTime());
                                pushMediaForPOI(i);
                            }
                        }
					}
				}
			}
	    }
	   
        private void pushMediaForPOI(int i)
        {
            SMEPAppVariable mySMEPAppVariable = (SMEPAppVariable) getApplicationContext();
            mySMEPAppVariable.setNewMedia(true);//Mark that there are new media so Main UI can render them
            mySMEPAppVariable.setNewMediaIndex(i);
            if(mySMEPAppVariable.isVibrationNotification())
            {
                Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                v.vibrate(1000);
            }
            if(mySMEPAppVariable.isSoundNotification())
            {
                try {
                    Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                    Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
                    r.play();
                }
                catch (Exception e) {
                    Log.e(TAG, "Can't play sound: " + e.getLocalizedMessage());
                    e.printStackTrace();
                }
            }
        }
	    @Override
	    public void onProviderDisabled(String provider)
	    {
	        Log.e(TAG, "onProviderDisabled: " + provider);            
	    }
	    @Override
	    public void onProviderEnabled(String provider)
	    {
	        Log.e(TAG, "onProviderEnabled: " + provider);
	    }
	    @Override
	    public void onStatusChanged(String provider, int status, Bundle extras)
	    {
	        Log.e(TAG, "onStatusChanged: " + provider);
	    }
	}
	
	LocationListener[] mLocationListeners = new LocationListener[] {
	        new LocationListener(LocationManager.GPS_PROVIDER),
	        new LocationListener(LocationManager.NETWORK_PROVIDER)
	};
	
	@Override
	public IBinder onBind(Intent arg0){
        return null;
	}
	@Override
	public int onStartCommand(Intent intent, int flags, int startId){
	    Log.e(TAG, "onStartCommand");
	    allPOIs = null;
	    shownLocation = new Hashtable<Integer, Long>();
	    super.onStartCommand(intent, flags, startId);       
	    return START_STICKY;
	}
	@Override
	public void onCreate(){
	    Log.e(TAG, "onCreate");
	    initializeLocationManager();
        final int LOCATION_INTERVAL = 0;
        final float LOCATION_DISTANCE = 0f;
        try {
	        mLocationManager.requestLocationUpdates(
	                LocationManager.NETWORK_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
	                mLocationListeners[1]);
	    } 
		catch (java.lang.SecurityException ex) {
	        Log.i(TAG, "fail to request location update, ignore", ex);
	    } catch (IllegalArgumentException ex) {
	        Log.d(TAG, "network provider does not exist, " + ex.getMessage());
	    }
	    try {
	        mLocationManager.requestLocationUpdates(
	                LocationManager.GPS_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
	                mLocationListeners[0]);
	    } 
		catch (java.lang.SecurityException ex) {
	        Log.i(TAG, "fail to request location update, ignore", ex);
	    } catch (IllegalArgumentException ex) {
	        Log.d(TAG, "gps provider does not exist " + ex.getMessage());
	    }
	}
	@Override
	public void onDestroy()	{
	    super.onDestroy();
	    if (mLocationManager != null) {
	        for (int i = 0; i < mLocationListeners.length; i++) {
	            try {
	                mLocationManager.removeUpdates(mLocationListeners[i]);
	            } catch (Exception ex) {
	                ex.printStackTrace();
	            }
	        }
	    }
	}

	private void initializeLocationManager() {
	    if (mLocationManager == null) {
	        mLocationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
	    }
	}
}