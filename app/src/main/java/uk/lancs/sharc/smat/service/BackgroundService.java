package uk.lancs.sharc.smat.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;

import uk.lancs.sharc.smat.controller.MainActivity;
import uk.lancs.sharc.smat.model.ContentTriggerSource;
import uk.lancs.sharc.smat.model.GpsContentTriggerSource;
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
	private static final String TAG = "SMAT_SERVICE";
	private LocationManager mLocationManager = null;
	private List<POIModel> allPOIs;
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

			SMEPAppVariable mySMEPAppVariable = (SMEPAppVariable) getApplicationContext();//Get the global settings of SMAT
			//Update screen
			((MainActivity)mySMEPAppVariable.getActivity()).updateSMEPWhenLocationChange(location);
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
				if(location.getAccuracy() < 100) {
					mCurrentLocation.set(location);
					//ContentTriggerSource contentTriggerSource = new GpsContentTriggerSource(location, allPOIs, getApplicationContext(),mySMEPAppVariable.getActivity(), shownLocation);
					//shownLocation = contentTriggerSource.renderContent();
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