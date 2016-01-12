package uk.lancs.sharc.smat.model;

import android.app.Activity;
import android.content.Context;
import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

import java.util.Date;
import java.util.Hashtable;
import java.util.List;

import uk.lancs.sharc.smat.service.SharcLibrary;

/**
 * Created by SHARC on 08/01/2016.
 */
public class GpsContentTriggerSource extends ContentTriggerSource {
    private Location currentLocation;
    private List<POIModel> allPOIs;

    public GpsContentTriggerSource(Location currentLocation, List<POIModel> poiModelList, Context context, Activity activity, Hashtable<Integer, Long> shownLocation){
        super(activity, context, shownLocation);
        this.currentLocation = currentLocation;
        this.allPOIs = poiModelList;
        contentTriggerSourceType = SOURCE_GPS;
    }

    @Override
    public Hashtable<Integer,Long> findSelectedContent() {
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
                    tmpPoint = allPOIs.get(i).getTriggerZoneCoordinates().get(0);
                    Location.distanceBetween(currentLocation.getLatitude(), currentLocation.getLongitude(), tmpPoint.latitude, tmpPoint.longitude, results);
                    if(results[0] < allPOIs.get(i).getTriggerZoneRadius())//radius of circle
                        isWithin = true;
                }
                else if(allPOIs.get(i).getTriggerType().equalsIgnoreCase("polygon"))
                {
                    List<LatLng> polyPath = allPOIs.get(i).getTriggerZoneCoordinates();
                    isWithin = SharcLibrary.isCurrentPointInsideRegion(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()), polyPath);
                }

                if(isWithin)
                {
                    SMEPAppVariable mySMEPAppVariable = (SMEPAppVariable) context.getApplicationContext();
                    if(shownLocation.get(i) == null)//If the user has not visited this POI
                    {
                        shownLocation.put(i, new Date().getTime());       //push id in the hashmap to record ID and time that a POI is visited
                        markSelectedPoi(i);
                    }
                    else if(mySMEPAppVariable.isPushAgain())//The user has already visited this POI but wants media to be push again when revisiting
                    {
                        long period = new Date().getTime() - shownLocation.get(i);//Get milliseconds between last push and current time
                        if(period >= mySMEPAppVariable.getTimeThreshold())        //only push again if the user comes back after time threshold - converted to millisecond
                        {
                            shownLocation.put(i,new Date().getTime());
                            markSelectedPoi(i);
                        }
                    }
                }
            }
        }
        return shownLocation;
    }
}
