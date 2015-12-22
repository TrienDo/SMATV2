package uk.lancs.sharc.smat.model;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.LatLngBounds.Builder;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Location;

import uk.lancs.sharc.smat.R;
import uk.lancs.sharc.smat.controller.MainActivity;
import uk.lancs.sharc.smat.service.SharcLibrary;
import uk.lancs.sharc.smat.service.DBExperienceDetails;
/**
 * <p>This class is a model of the Experience entity</p>
 * <p>It can be changed later depending on future work </p>
 *
 * Author: Trien Do
 * Date: Feb 2015
 **/
public class ExperienceDetailsModel {
	private ArrayList<POIModel> allPOIs;
	private ArrayList<EOIModel> allEOIs;
	private ArrayList<RouteModel> allRoutes;
	private ArrayList<ResponseModel> myResponses;
	private DBExperienceDetails experienceDetailsDB; 
	private ExperienceMetaDataModel metaData;
	private ArrayList<Marker> allPoiMarkers;		//PoI Markers
	private ArrayList<Object> allTriggerZoneVizs;	//Geo-fences for PoIs
	private Marker startRoute;
	private Marker endRoute;
	
	public ExperienceDetailsModel(DBExperienceDetails mExperienceDetailsDB, boolean isFromOnline)
	{
		allPOIs = new ArrayList<POIModel>();
		allEOIs = new ArrayList<EOIModel>();
		allRoutes = new ArrayList<RouteModel>();
		myResponses = new ArrayList<ResponseModel>();

		allPoiMarkers = new ArrayList<Marker>();
		allTriggerZoneVizs = new ArrayList<Object>();
        //From online --> delete all data of the experience if already downloaded before
        //Else just  create a new database = automatically done
        experienceDetailsDB = mExperienceDetailsDB;
		if (isFromOnline)
			experienceDetailsDB.deleteAllDataInTables();		
	}

	public void setExperienceDetailsDB(DBExperienceDetails mExperienceDetailsDB)
	{
		experienceDetailsDB = mExperienceDetailsDB;
	}

	public ArrayList<String> getPOIHtmlListItems(int index, LatLng currentLocation)//get data to display in the listview of POI Media
	{
		String state = "";
		if(currentLocation == null)
			state = "The app cannot identify your current location at the moment";
		else
		{
			if(this.isLocationWithinTrigerzone(currentLocation, index))
				state = "Your current location falls within the trigger zone for this Point of Interest";
			else
				state = "Your current location does not fall within the trigger zone for this Point of Interest";
		}
		if(index >=0 && index < allPOIs.size())
			return allPOIs.get(index).getHtmlListItems(experienceDetailsDB, state);
		else
            return null;
	}
	
	public String getPOIName(int index)
	{
		return allPOIs.get(index).getName();
	}
	
	public String getPOIID(int index)
	{
		if(index >=0 && index < allPOIs.size())
			return allPOIs.get(index).getID();
		else
			return "";
	}

	public List<String> getAllEOIMediaListItems() //get data to display in the listview of EOI Media
	{
		ArrayList<String> mediaList = new ArrayList<String>();
		mediaList.add("<h3>This experience comprises " + allEOIs.size() + " Events of Interest </h3>");
	  	for (int i = 0; i < allEOIs.size(); i++)
	  	{
		  		String htmlCode = "<p><b> " + (i+1) + ". " + allEOIs.get(i).getName() + "</b></p>";
		  		htmlCode += "<blockquote> " + allEOIs.get(i).getDescription() + "</blockquote>";
		  		htmlCode += allEOIs.get(i).getMediaHTMLCode();
		  		mediaList.add(htmlCode);
	  	}
		//get responses for EOI tab
		//Get responses
		List<ResponseModel> responseList = experienceDetailsDB.getResponsesForTab("EOI");
		for(int i = 0; i< responseList.size(); i++ )
			mediaList.add(responseList.get(i).getHTMLCodeForResponse(false));
	  	return mediaList;
	}
	
	public void getExperienceFromSnapshotOnDropbox(String jsonString) //parse content of an experience from JSON file and download media files
	{
		//Gets all links to download media files
	    ArrayList<String> allMediaLinks = SharcLibrary.extractLinksFromText(jsonString);
        for(int m = 0; m < allMediaLinks.size(); m++)
        {	           	 	
        	HttpURLConnection inConection = null;
			// Output stream
			String mName =allMediaLinks.get(m);
			mName = mName.substring(mName.lastIndexOf("/"));
			String localPath = SharcLibrary.SHARC_MEDIA_FOLDER + mName;
			File localFile = new File(localPath);
			if(!localFile.exists()) {
				try {
					URL inUrl = new URL(allMediaLinks.get(m));
					inConection = (HttpURLConnection) inUrl.openConnection();
					inConection.connect();
					// download the file
					InputStream mInput = new BufferedInputStream(inUrl.openStream(), 8192);
					OutputStream mOutput = new FileOutputStream(localPath);
					System.out.println(String.valueOf(m) + " .Downloading:" + mName);
					byte mData[] = new byte[1024];
					int count;
					while ((count = mInput.read(mData)) != -1) {
						// writing data to file
						mOutput.write(mData, 0, count);
					}
					// flushing output
					mOutput.flush();
					mOutput.close();
					mInput.close();
					inConection.disconnect();
				} catch (IOException e) {
					e.printStackTrace();
					inConection.disconnect();
					continue;
				} finally {
					inConection.disconnect();
				}
			}
        }
        //parse entities such as POIs, EOIs
        try 
		{
	        JSONObject json = new JSONObject(jsonString);
	        JSONArray jObject = json.getJSONArray("rows");           
	        for (int i = 0; i < jObject.length(); i++) 
	        {
                //Storing each json object in a variable
                JSONObject c = jObject.getJSONObject(i);
	            String name = c.getString("tid");               //name of the table
	            JSONObject tmpData = c.getJSONObject("data");
	            String mediaOrder = "";
            	try
            	{
            		mediaOrder = tmpData.getString("mediaOrder");	            	
            	}
            	catch(JSONException je)
				{
					mediaOrder = "";
					//je.printStackTrace();
				}
            	
	            if(name.equalsIgnoreCase("POIs"))
	            {	            	
	            	experienceDetailsDB.insertPOI(c.getString("rowid"),tmpData.getString("name"), tmpData.getString("type"), tmpData.getString("desc"), tmpData.getString("latLng"), mediaOrder,tmpData.getString("associatedEOI"), tmpData.getString("associatedRoute"), tmpData.getString("triggerZone"));
	            }
	            else if(name.equalsIgnoreCase("EOIs"))
	            {	            	
	            	experienceDetailsDB.insertEOI(c.getString("rowid"),tmpData.getString("name"), tmpData.getString("desc"), tmpData.getString("startDate"), tmpData.getString("endDate"), tmpData.getString("associatedPOI"), tmpData.getString("associatedRoute"), mediaOrder);
	            }
	            else if(name.equalsIgnoreCase("Routes"))
	            {
	            	experienceDetailsDB.insertROUTE(c.getString("rowid"),tmpData.getString("name"), tmpData.getString("desc"), tmpData.getString("colour"), tmpData.getString("polygon"), tmpData.getString("associatedPOI"), tmpData.getString("associatedEOI"),tmpData.getString("directed"));//, tmpData.getString("mediaOrder"));
	            }
	            else if(name.equalsIgnoreCase("Media"))
	            {
	            	experienceDetailsDB.insertMEDIA(c.getString("rowid"),tmpData.getString("name"), tmpData.getString("type"), tmpData.getString("desc"), tmpData.getString("attachedTo"), tmpData.getString("content"), tmpData.getString("context"), 0, tmpData.getString("PoIID"));
					//experienceDetailsDB.insertMEDIA(c.getString("rowid"),tmpData.getString("name"), tmpData.getString("type"), tmpData.getString("desc"), tmpData.getString("attachedTo"), tmpData.getString("content"), tmpData.getString("context"), tmpData.getInt("noOfLike"), tmpData.getString("PoIID"));
	            }                                                
	            else if(name.equalsIgnoreCase("Responses"))
	            {	
	            	//experienceDetailsDB.insertRESPONSE(c.getString("rowid"), tmpData.getString("status"), tmpData.getString("type"), tmpData.getString("desc"), tmpData.getString("entityType"), tmpData.getString("content"), tmpData.getInt("noOfLike"),tmpData.getString("entityID"), tmpData.getString("consumerName"), tmpData.getString("consumerEmail"));
	            	JSONObject objCount = tmpData.getJSONObject("noOfLike");
	            	experienceDetailsDB.insertRESPONSE(c.getString("rowid"), tmpData.getString("status"), tmpData.getString("type"), tmpData.getString("desc"), tmpData.getString("entityType"), tmpData.getString("content"), objCount.getInt("I") ,tmpData.getString("entityID"), tmpData.getString("consumerName"), tmpData.getString("consumerEmail"));
	            }
	        }
		}
        catch(Exception e)
   	 	{
   	 		e.printStackTrace();
   	 	}
	}

	public void renderAllPOIs(GoogleMap mMap, SMEPAppVariable mySMEPAppVariable)
    {
    	Cursor poiRet = experienceDetailsDB.getDataSQL("select * from POIs", null);
    	POIModel tmpTZ = null;
    	if(poiRet.getCount() > 0)
    	{    		
    		int i = 0;
    		poiRet.moveToFirst();
        	do 
        	{               
				tmpTZ = new POIModel(poiRet.getString(0), poiRet.getString(1),  poiRet.getString(2),poiRet.getString(3), poiRet.getString(4), poiRet.getString(5), poiRet.getString(6), poiRet.getString(8));
                renderPOIandTriggerZone(mMap, tmpTZ, String.valueOf(i));//i = ID of marker -> when marker is tapped -> show Media of POI with that id
                allPOIs.add(tmpTZ);                
                i++;
            } 
        	while (poiRet.moveToNext());
        	metaData.setPoiCount(allPOIs.size());
        	mySMEPAppVariable.setAllPOIs(allPOIs);
        	mySMEPAppVariable.setNewExperience(true);
    	}
    }

	public void renderPOIandTriggerZone(GoogleMap mMap, POIModel curPOI, String markerID)
	{
		Object shape = null;
		//Get icon for marker to present the POI
		Marker tmpMarker = mMap.addMarker(new MarkerOptions()
						.title(markerID)
						.anchor(0.5f, 0.5f)
						.position(curPOI.getLocation())
						.visible(true)
		);
		String firstImage = experienceDetailsDB.getFirstImage(curPOI.getID(), curPOI.getMediaOrder());//Can be local or download
		if(firstImage.contains("http"))//downloaded
		{
			firstImage = firstImage.substring(firstImage.lastIndexOf("/"),firstImage.lastIndexOf("."));
			//Fix error for Wray experience --> SLAT used wrong id for media files --> Remove this part when finished the user study
			if(firstImage.equalsIgnoreCase("1430659956899"))
				firstImage = "1430659185520";
			if(firstImage.equalsIgnoreCase("1430654450271"))
				firstImage = "1430652819056";
			if(firstImage.equalsIgnoreCase("1430654456145"))
				firstImage = "1430652521030";
			if(firstImage.equalsIgnoreCase("1430654453336"))
				firstImage = "1430652391709";
			//Done error
			firstImage = SharcLibrary.SHARC_MEDIA_FOLDER + "/" + firstImage + ".jpg";
		}

		if(firstImage.equalsIgnoreCase(""))
			tmpMarker.setIcon(BitmapDescriptorFactory.fromResource(R.raw.poi));
		else
		{
			Bitmap bitmap = SharcLibrary.getThumbnail(firstImage);
			if(bitmap != null)	//First available image
				tmpMarker.setIcon(BitmapDescriptorFactory.fromBitmap(bitmap));
			else
				tmpMarker.setIcon(BitmapDescriptorFactory.fromResource(R.raw.poi));
		}
		if(curPOI.getType().equalsIgnoreCase(SharcLibrary.SHARC_POI_ACCESSIBILITY))
			tmpMarker.setIcon(BitmapDescriptorFactory.fromResource(R.raw.access));

		allPoiMarkers.add(tmpMarker);

		//Draw trigger zone
		int alpha = 47; //Set alpha fill colour
		if(curPOI.getTriggerType().equalsIgnoreCase("circle"))
		{
			shape = mMap.addCircle(new CircleOptions()
							.center(curPOI.getCoordinates().get(0))
							.radius(curPOI.getRadius())
							.strokeWidth(2)
							.strokeColor(Color.parseColor(curPOI.getColour()))
							.fillColor(SharcLibrary.hex2Argb(alpha, curPOI.getColour()))
							.visible(true)
			);
		}
		else if(curPOI.getTriggerType().equals("polygon"))
		{
			shape =   mMap.addPolygon(new PolygonOptions()
							.addAll(curPOI.getCoordinates())
							.strokeWidth(2)
							.strokeColor(Color.parseColor(curPOI.getColour()))
							.fillColor(SharcLibrary.hex2Argb(alpha, curPOI.getColour()))
							.visible(true)
			);
		}
		allTriggerZoneVizs.add(shape);
	}

	public void updatePOIThumbnail(POIModel curPOI, int markerIndex)
	{
		//Get icon for marker to present the POI
		Marker tmpMarker = allPoiMarkers.get(markerIndex);
		String firstImage = experienceDetailsDB.getFirstImage(curPOI.getID(), curPOI.getMediaOrder());//Can be local or download
		if(firstImage.contains("http"))//downloaded
		{
			firstImage = firstImage.substring(firstImage.lastIndexOf("/"),firstImage.lastIndexOf("."));
			firstImage = SharcLibrary.SHARC_MEDIA_FOLDER + "/" + firstImage + ".jpg";
		}
		if(firstImage.equalsIgnoreCase(""))
			tmpMarker.setIcon(BitmapDescriptorFactory.fromResource(R.raw.poi));
		else
		{
			Bitmap bitmap = SharcLibrary.getThumbnail(firstImage);
			if(bitmap != null)	//First available image		      	
				tmpMarker.setIcon(BitmapDescriptorFactory.fromBitmap(bitmap));
			else
				tmpMarker.setIcon(BitmapDescriptorFactory.fromResource(R.raw.poi));
		}
	}
	//Return the index of the route which has not been saved (e.g., app crashes)
	//Else - 1 if all routes have been saved
    public int renderAllRoutes(GoogleMap mMap)
    {
    	int inSaveRouteIndex = -1;
		Cursor routeRet = experienceDetailsDB.getDataSQL("select * from ROUTES", null);

    	if(routeRet.getCount() > 0)
    	{
    		routeRet.moveToFirst();
			int index = 0;
        	do
        	{               
        		ArrayList<LatLng> path = new ArrayList<LatLng>();
				if(routeRet.getString(4).trim().length() <=0)
					continue;
        		String[] pathInfo = routeRet.getString(4).trim().split(" ");
        		int k = 0;        		
    			while (k < pathInfo.length)
    			{
    				path.add(new LatLng(Float.parseFloat(pathInfo[k]), Float.parseFloat(pathInfo[k+1])));
    				k+=2;
    			}
    			//Calculate distance
    			float distance = 0.0f;
				float[] results = new float[1];
				for (int i=1; i < path.size(); i++)
				{
					Location.distanceBetween(path.get(i-1).latitude,path.get(i-1).longitude, path.get(i).latitude,path.get(i).longitude, results);
					distance += results[0];
				}
				//Distance / 1000 to get km
    			RouteModel tmpRoute = new RouteModel(routeRet.getString(0), routeRet.getString(1), routeRet.getString(2), routeRet.getString(3), distance/1000, routeRet.getString(7));
    			tmpRoute.setPath(path);
    			allRoutes.add(tmpRoute);
    			//metaData.setRouteLength(metaData.getRouteLength() + distance / 1000);//get km
    			mMap.addPolyline((new PolylineOptions()
	    		    .width(5)
	    		    .color(SharcLibrary.hex2rgb("#" + tmpRoute.getColour()))
	    		    .visible(true)
	    		    )).setPoints(path);
    			if(routeRet.getString(7).equalsIgnoreCase("true"))
    			{
    				startRoute = mMap.addMarker(new MarkerOptions()
									.anchor(0.5f, 0.5f)
									.title("START")
									.position(path.get(0))
									.icon(BitmapDescriptorFactory.fromResource(R.raw.start))
									.visible(true)
					);
					if(tmpRoute.getDesc().equalsIgnoreCase("This route is being recorded"))
					{
						inSaveRouteIndex = index;
					}
					else {
						endRoute = mMap.addMarker(new MarkerOptions()
										.title("END")
										.anchor(0.5f, 0.5f)
										.position(path.get(path.size() - 1))
										.icon(BitmapDescriptorFactory.fromResource(R.raw.end))
										.visible(true)
						);
					}
    			}
				index ++;
				metaData.setDifficultLevel(tmpRoute.getDesc());
            }
        	while (routeRet.moveToNext());
        	metaData.setRouteCount(allRoutes.size());
    	}
		return inSaveRouteIndex;
    }
    
    public LatLngBounds getGeographicalBoundary()//Get boundary of an experience to move and zoom to suitable location on maps
    {
    	boolean empty = true;
    	Builder boundsBuilder = new LatLngBounds.Builder();
    	//All POI
    	int i;
    	for (i = 0; i < allPOIs.size(); i++)
    		boundsBuilder.include(allPOIs.get(i).getLocation());
    	if(i>0)
    		empty = false;
    	//All routes
    	for(int k = 0; k < allRoutes.size(); k++)
    	{
    		ArrayList<LatLng> path = allRoutes.get(k).getPath();
    		for (i = 0; i < path.size(); i++)
        		boundsBuilder.include(path.get(i));
    		if(i>0)
        		empty = false;
    	}
    	
    	if(!empty)
    		return boundsBuilder.build();
    	else
    		return null;
    }
    
    public void renderAllEOIs()
    {
    	Cursor eoiRet = experienceDetailsDB.getDataSQL("select * from EOIs", null);
    	if(eoiRet.getCount() > 0)
    	{
    		eoiRet.moveToFirst();
        	do 
        	{               
	    		String id = eoiRet.getString(0);
	    		String name= eoiRet.getString(1);
	    		String desc = eoiRet.getString(2);
	    		String mediaOrder = eoiRet.getString(7);
	    		EOIModel tmpEOI = new EOIModel(id, name, desc, "", mediaOrder);
				List<MediaModel> mediaList = experienceDetailsDB.getMediaForEntity(id);
	    		String content = tmpEOI.getHTMLPresentation(mediaList);
	    		tmpEOI.setMediaHTMLCode(content);
	    		allEOIs.add(tmpEOI);
            } 
        	while (eoiRet.moveToNext());
        	metaData.setEoiCount(allEOIs.size());
    	}
    }

	public String[] getHTMLCodeForEOI(String id)
	{
		for(int i = 0; i < allEOIs.size(); i++)
		{
			if(allEOIs.get(i).getId().equalsIgnoreCase(id))
			{
				return new String[]{allEOIs.get(i).getName(), allEOIs.get(i).getMediaHTMLCode()};
			}
		}
		return null;
	}

    public void getMediaStat()
    {    	
    	Cursor statRet = experienceDetailsDB.getDataSQL("select type, count(*) as total from MEDIA group by type", null);
    	if(statRet.getCount() > 0)
    	{
    		String type = "";
            statRet.moveToFirst();
        	do 
        	{               
        		type = statRet.getString(0);
        		if(type.equalsIgnoreCase("text"))
        			metaData.setTextCount(statRet.getInt(1));	
        		else if(type.equalsIgnoreCase("image"))
        			metaData.setImageCount(statRet.getInt(1));
        		else if(type.equalsIgnoreCase("audio"))
        			metaData.setAudioCount(statRet.getInt(1));
        		else if(type.equalsIgnoreCase("video"))
        			metaData.setVideoCount(statRet.getInt(1));
            }
        	while (statRet.moveToNext());        	
    	}
    }  
    
    public List<String> getSumaryInfo()
    {
		ArrayList<String> mediaList = new ArrayList<String>();
		if(metaData!=null) {
			//Update route info
			String routeInfo = "";
			if(allRoutes.size() >= 1)
			{
				for(int i = 0; i < allRoutes.size(); i++)
					routeInfo += "<div> - Route name: " + allRoutes.get(i).getName() + " (" +   String.format("%.2f", allRoutes.get(i).getDistance()) + " km). " + allRoutes.get(i).getDesc() +"</div>";
			}
			metaData.setRouteInfo(routeInfo);
			mediaList.add(metaData.getSumaryInfo());
		}
    	else
			mediaList.add("No experience loaded yet");
		//Get responses
		List<ResponseModel> responseList = experienceDetailsDB.getResponsesForTab("ROUTE");
		for(int i = 0; i< responseList.size(); i++ )
			mediaList.add(responseList.get(i).getHTMLCodeForResponse(false));
		return mediaList;
    }

	public String getExperienceSumaryInfo()
	{
		if(metaData != null) {
			String routeInfo = "";
			if(allRoutes.size() >= 1)
			{
				for(int i = 0; i < allRoutes.size(); i++)
					routeInfo += "<div> - Route name: " + allRoutes.get(i).getName() + " (" +   String.format("%.2f", allRoutes.get(i).getDistance()) + " km). Description: " + allRoutes.get(i).getDesc() +"</div>";
			}
			metaData.setRouteInfo(routeInfo);
			return metaData.getSumaryInfo();
		}
		else
			return "You have not yet created or opened any experience";
	}
    public void showTriggerZones(boolean visible)
    {
    	for(int i = 0; i < allTriggerZoneVizs.size(); i++)
	   {
		   if(allTriggerZoneVizs.get(i) instanceof Circle)
		   {
			   Circle tmp = (Circle)allTriggerZoneVizs.get(i);
			   tmp.setVisible(visible);
		   }
		   else if(allTriggerZoneVizs.get(i) instanceof Polygon)
		   {
			   Polygon tmp = (Polygon)allTriggerZoneVizs.get(i);
			   tmp.setVisible(visible);
		   }
	   }	    
    }
    
    public void showPOIThumbnails(boolean visible)
    {
    	for(int i = 0; i < allPoiMarkers.size(); i++)
    	{
    		allPoiMarkers.get(i).setVisible(visible);
    	}
    }
    
    public int getTriggerZoneIndexFromLocation(LatLng mLocation)//get ID of the trigger zone touched by users
    {
    	if(allTriggerZoneVizs.size()>0)
		{
			LatLng tmpPoint;
			for (int i = 0; i < allTriggerZoneVizs.size(); i++)
			{
				/*if(allTriggerZoneVizs.get(i).getClass().equals(Circle.class))
				{
					float[] results = new float[1];
					Circle tmpZone = (Circle)allTriggerZoneVizs.get(i);
					tmpPoint = tmpZone.getCenter();
					Location.distanceBetween(mLocation.latitude, mLocation.longitude, tmpPoint.latitude,tmpPoint.longitude, results);
					if(results[0] < tmpZone.getRadius())//radius of circle
						return i;
				}
				else if(allTriggerZoneVizs.get(i).getClass().equals(Polygon.class))
				{						
					List<LatLng> polyPath = ((Polygon)allTriggerZoneVizs.get(i)).getPoints();
					if(SharcLibrary.isCurrentPointInsideRegion(new LatLng(mLocation.latitude, mLocation.longitude), polyPath))
						return i;
				}*/
				if(isLocationWithinTrigerzone(mLocation, i))
					return i;
			}
		}
    	return -1;
    }

	public boolean isLocationWithinTrigerzone(LatLng mLocation, int poiIndex)
	{
		if(poiIndex < 0 || poiIndex > allTriggerZoneVizs.size()-1)
			return false;
		if(allTriggerZoneVizs.get(poiIndex).getClass().equals(Circle.class))
		{
			float[] results = new float[1];
			Circle tmpZone = (Circle)allTriggerZoneVizs.get(poiIndex);
			LatLng tmpPoint = tmpZone.getCenter();
			Location.distanceBetween(mLocation.latitude, mLocation.longitude, tmpPoint.latitude,tmpPoint.longitude, results);
			if(results[0] < tmpZone.getRadius())//radius of circle
				return true;
		}
		else if(allTriggerZoneVizs.get(poiIndex).getClass().equals(Polygon.class))
		{
			List<LatLng> polyPath = ((Polygon)allTriggerZoneVizs.get(poiIndex)).getPoints();
			if(SharcLibrary.isCurrentPointInsideRegion(new LatLng(mLocation.latitude, mLocation.longitude), polyPath))
				return true;
		}
		return false;
	}
    
	public void clearExperience()
	{
		allPOIs.clear();
		allEOIs.clear();
		allRoutes.clear();
		allPoiMarkers.clear();
		allTriggerZoneVizs.clear();		
	}

	public ExperienceMetaDataModel getMetaData() {
		return metaData;
	}

	public void setMetaData(ExperienceMetaDataModel metaData) {
		this.metaData = metaData;
	}
	
	public String getPOINameFromID(String id)
	{
		for (int i = 0; i < allPOIs.size(); i++)
		{
			if(allPOIs.get(i).getID().equalsIgnoreCase(id))
				return allPOIs.get(i).getName();
		}
		return null;
	}
	
	public String getEOINameFromID(String id)
	{
		for (int i = 0; i < allEOIs.size(); i++)
		{
			if(allEOIs.get(i).getId().equalsIgnoreCase(id))
				return allEOIs.get(i).getName();
		}
		return null;
	}
	
	public ArrayList<String> getMyResponsesList()
	{
		getMyResponsesFromDatabase();
		ArrayList<String> resList = new ArrayList<String>();
		for (int i = 0; i < myResponses.size(); i++)
		{	
			resList.add(this.getMyResponseName(i));
		}
		return resList;
	}

	public ArrayList<ResponseModel> getMyResponses() {
		return myResponses;
	}

	public void getMyResponsesFromDatabase()
    {
        myResponses.clear();
        Cursor resRet = experienceDetailsDB.getDataSQL("select * from MYRESPONSES", null);
        if(resRet.getCount() > 0)
        {
            resRet.moveToFirst();
            do
            {
                myResponses.add(new ResponseModel(resRet.getString(0), resRet.getString(1), resRet.getString(2), resRet.getString(3), resRet.getString(4), resRet.getString(5),
                        resRet.getString(6), resRet.getString(7), resRet.getString(8), resRet.getString(9)));
            }
            while (resRet.moveToNext());
        }
    }

	public List<ResponseModel> getCommentsForEntity(String ID)
	{
		return experienceDetailsDB.getCommentsForEntity(ID);
	}

    public String getMyResponseName(int i)
	{
		if(myResponses.get(i).getEntityType().equalsIgnoreCase("POI"))
		{
			String enName = getPOINameFromID(myResponses.get(i).getEntityID());
			if(enName!=null) {
				if(myResponses.get(i).getType().equalsIgnoreCase("text"))
					return ("Added a new " + myResponses.get(i).getType() + " for POI " + enName+ " (" + SharcLibrary.getStringSize(myResponses.get(i).getContent()) + "KB)");
				else if(myResponses.get(i).getType().equalsIgnoreCase("image"))
					return ("Added a new " + myResponses.get(i).getType() + " for POI " + enName + " (" + SharcLibrary.getFilesize(myResponses.get(i).getContent(),true) + "MB)");
				else
					return ("Added a new " + myResponses.get(i).getType() + " for POI " + enName + " (" + SharcLibrary.getFilesize(myResponses.get(i).getContent(),false) + "MB)");
			}
		}
		else if(myResponses.get(i).getEntityType().equalsIgnoreCase("EOI"))
		{
			String enName = getEOINameFromID(myResponses.get(i).getEntityID());
			if(enName!=null)
				return ("Response for EOI named " + enName);
			else
				return ("Response for all EOIs");
		}
		else if(myResponses.get(i).getEntityType().equalsIgnoreCase("ROUTE"))
		{
			return ("Created a new route named " + myResponses.get(i).getContent());
		}
		else if(myResponses.get(i).getEntityType().equalsIgnoreCase("NEW"))
		{
			return ("Created a new POI named " + myResponses.get(i).getContent());
		}
		else if(myResponses.get(i).getEntityType().equalsIgnoreCase("MEDIA"))
		{
			return ("Comment on a media item");
		}
		else if(myResponses.get(i).getEntityType().equalsIgnoreCase("RESPONSES"))
		{
			return ("Comment on a response");
		}
		return "";
	}
	
	public ResponseModel getMyResponseAt(int index)
	{
		return myResponses.get(index);
	}
	
	public String getMyResponseContentAt(int index)
	{
		return myResponses.get(index).getHTMLCodeForResponse(true);
	}

	public void removeUploadedResponseAt(int index)
	{
		ResponseModel response = myResponses.get(index);
		experienceDetailsDB.deleteMYRESPONSE(response.getId());
	}

	public void deleteMyResponseAt(int index)
	{
		ResponseModel response = myResponses.get(index);
		experienceDetailsDB.deleteMYRESPONSE(response.getId());
		//update media order for POI - remove the ID of the media
		POIModel poi = this.getPOIFromID(response.getEntityID());
		if(poi != null)
		{
			String mediaOrder = poi.getStringMediaOrder();
			if(mediaOrder.equalsIgnoreCase(response.getId()))//The only media - delete -> blank
				mediaOrder = "";
			else
			{
				//for example have 3 items:"a b c" -> can be a - b - c  => remove a will need a space after a, remove c needs delete space before c, b -> remove either space
				//Trick: add space at the end "a b c ": -> always replace the "item " (item and a space)
				//If has space at the end (remove b) -> remove last space
				mediaOrder += " ";
				mediaOrder = mediaOrder.replace(response.getId() + " ", "");//remove
				mediaOrder = mediaOrder.trim();//remove last space
			}

			poi.setMediaOrderFromString(mediaOrder);
			experienceDetailsDB.updatePOI(poi.getID(), poi.getName(), poi.getType(), poi.getDesc(),poi.getLocationString(), poi.getStringMediaOrder(), "", "", poi.getTriggerZoneString());
		}
		//Delete media items if it is a POI
		if(response.getEntityType().equalsIgnoreCase("NEW"))
		{
			List<MediaModel> mediaItems = experienceDetailsDB.getMediaForEntity(response.getId());
			for(int i = 0; i < mediaItems.size(); i++)
				experienceDetailsDB.deleteMYRESPONSE(mediaItems.get(i).getId());
		}
	}
	
	public void addMyResponse(ResponseModel res)
	{
		myResponses.add(res);
		experienceDetailsDB.insertMYRESPONSE(res.getId(), res.getStatus(), res.getType(), res.getDesc(), res.getEntityType(), res.getContent(), res.getNoOfLike(), res.getEntityID(), res.getConName(), res.getConEmail());
	}

	//Return index of the new POI
	public int addNewPOI(String id, String name, String type, String desc, String latLng, String mediaOrder, String associatedEOI, String associatedRoute, String triggerZone, GoogleMap mMap, SMEPAppVariable mySMEPAppVariable)
	{
		//add to internal memory
		POIModel tmpPOI = new POIModel(id, name, type, desc, latLng, mediaOrder,associatedEOI, triggerZone);
		renderPOIandTriggerZone(mMap, tmpPOI, String.valueOf(allPOIs.size()));//ID of marker -> when marker is tapped -> show Media of POI with that id
		allPOIs.add(tmpPOI);
		metaData.setPoiCount(allPOIs.size());
		mySMEPAppVariable.setAllPOIs(allPOIs);
		mySMEPAppVariable.setNewExperience(true);
		experienceDetailsDB.insertPOI(id, name, type, desc, latLng, mediaOrder, associatedEOI, associatedRoute, triggerZone);
		return allPOIs.size()-1;
	}

	public void addNewRoute(RouteModel inRoute)
	{
		//add to internal memory
		allRoutes.add(inRoute);
		metaData.setRouteCount(allRoutes.size());
		metaData.setRouteLength(metaData.getRouteLength() + inRoute.getDistance() / 1000);
		metaData.setDifficultLevel(inRoute.getDesc());
		experienceDetailsDB.insertROUTE(inRoute.getId(), inRoute.getName(), inRoute.getDesc(), inRoute.getColour(), inRoute.getPathString(), "", "", inRoute.getDirected());
	}

	public void updateRoute(RouteModel inRoute)
	{
		//add to internal memory
		metaData.setRouteLength(metaData.getRouteLength() + inRoute.getDistance() / 1000);
		metaData.setDifficultLevel(inRoute.getDesc());
		experienceDetailsDB.updateRoute(inRoute.getId(), inRoute.getName(), inRoute.getDesc(), inRoute.getColour(), inRoute.getPathString(), "", "", inRoute.getDirected());
	}

	public void updateRoutePath(RouteModel inRoute)
	{
		experienceDetailsDB.updateRoutePath(inRoute.getId(), inRoute.getPathString());
	}

	public void removeRoute(RouteModel inRoute)
	{
		//add to internal memory
		allRoutes.remove(inRoute);
		metaData.setRouteCount(allRoutes.size());
		metaData.setRouteLength(metaData.getRouteLength() - inRoute.getDistance() / 1000);
		metaData.setDifficultLevel("");
		experienceDetailsDB.deleteRoute(inRoute.getId());
	}

	public void addNewMediaItem(ResponseModel response)
	{
		experienceDetailsDB.insertMEDIA(response.getId(), response.getDesc(), response.getType(), "", response.getEntityType(), response.getContent(), "", 0, response.getEntityID());
		//update media order for POI
		POIModel poi = this.getPOIFromID(response.getEntityID());
		if(poi != null)
		{
			String mediaOrder = poi.getStringMediaOrder();
			if(mediaOrder.equalsIgnoreCase(""))
				mediaOrder = response.getId();
			else
				mediaOrder += " " + response.getId();
			poi.setMediaOrderFromString(mediaOrder);
			experienceDetailsDB.updatePOI(poi.getID(), poi.getName(), poi.getType(), poi.getDesc(),poi.getLocationString(), poi.getStringMediaOrder(), "", "", poi.getTriggerZoneString());
			if(response.getType().equalsIgnoreCase("image") && !poi.getType().equalsIgnoreCase(SharcLibrary.SHARC_POI_ACCESSIBILITY)) {
				this.updatePOIThumbnail(poi, this.getPoiIndexFromID(response.getEntityID()));
				this.metaData.setImageCount(this.metaData.getImageCount() + 1);
			}
			else if(response.getType().equalsIgnoreCase("text")) {
				this.metaData.setTextCount(this.metaData.getTextCount() + 1);
			}
			else if(response.getType().equalsIgnoreCase("audio")) {
				this.metaData.setAudioCount(this.metaData.getAudioCount() + 1);
			}
			else if(response.getType().equalsIgnoreCase("video")) {
				this.metaData.setVideoCount(this.metaData.getVideoCount() + 1);
			}
		}
	}

	public void updatePublicURLForMedia(String mediaId, String mediaURL)
	{
		experienceDetailsDB.updateMediaURL(mediaId, mediaURL);
	}

	public POIModel getPOIFromID(String id)
	{
		for (int i = 0; i < allPOIs.size(); i++)
		{
			if(allPOIs.get(i).getID().equalsIgnoreCase(id))
				return allPOIs.get(i);
		}
		return null;
	}

	public int getPoiIndexFromID(String id)
	{
		for (int i = 0; i < allPOIs.size(); i++)
		{
			if(allPOIs.get(i).getID().equalsIgnoreCase(id))
				return i;
		}
		return -1;
	}

	public RouteModel getRouteFromID(String id)
	{
		for (int i = 0; i < allRoutes.size(); i++)
		{
			if(allRoutes.get(i).getId().equalsIgnoreCase(id))
				return allRoutes.get(i);
		}
		return null;
	}

	public RouteModel getRouteAt(int index)
	{
		return allRoutes.get(index);
	}

	public String getJSON()
	{
		String jsonString = "{\"rows\": [";
		//Add all POI
		int i;
		for(i = 0; i < allPOIs.size(); i++)
		{
			jsonString += "{\"tid\": \"POIs\", \"data\": {\"associatedEOI\": \"\", \"triggerZone\": \"" +  allPOIs.get(i).getTriggerZoneString() + "\", \"latLng\": \"" +  allPOIs.get(i).getLocationString()
					+ "\", \"mediaOrder\": \"" + allPOIs.get(i).getStringMediaOrder() + "\", \"name\": \"" + allPOIs.get(i).getName() + "\", \"associatedRoute\": \"\", \"type\": \"" + allPOIs.get(i).getType() + "\", \"desc\": \"\"}, \"rowid\": \"" + allPOIs.get(i).getID() + "\"},";
		}
		//Add all Media
		List<MediaModel> allMedia = experienceDetailsDB.getAllMedia();
		for(i = 0; i < allMedia.size(); i++)
		{
			jsonString += "{\"tid\": \"media\", \"data\": {\"name\": \"" + allMedia.get(i).getName() + "\", \"attachedTo\": \"POI\", \"content\": \"" + allMedia.get(i).getContent()
					+ "\", \"context\": \"\", \"noOfLike\": {\"I\": \"0\"}, \"type\": \"" + allMedia.get(i).getType() + "\", \"PoIID\": \"" + allMedia.get(i).getEntityID() + "\", \"desc\": \"\"}, \"rowid\": \"" + allMedia.get(i).getId() + "\"},";
		}
		//Add all route
		for(i = 0; i < allRoutes.size(); i++)
		{
			jsonString += "{\"tid\": \"Routes\", \"data\": {\"directed\": \"" + allRoutes.get(i).getDirected() + "\", \"associatedEOI\": \"\", \"polygon\": \"" + allRoutes.get(i).getPathString() + "\", \"associatedPOI\": \"\", \"colour\": \"ff0000\", \"name\": \""
					+ allRoutes.get(i).getName() + "\", \"desc\": \"" + allRoutes.get(i).getDesc() + "\"}, \"rowid\": \"" + allRoutes.get(i).getId() + "\"}, ";
		}
		if(jsonString.charAt(jsonString.length()-1)== ',')
			jsonString = jsonString.substring(0,jsonString.length() - 1);

		jsonString += "]}";
		return jsonString;
	}

	public String getOverallSummary()
	{
		String routeInfo = "";
		if(allRoutes.size() > 0)
		{
			for(int i = 0; i < allRoutes.size(); i++)
				routeInfo += " [Route name: " + allRoutes.get(i).getName() + " (" +   String.format("%.2f", allRoutes.get(i).getDistance()) + " km). " + allRoutes.get(i).getDesc() +"].";
		}
		return "This experience has " + allRoutes.size() + " route(s), " + allEOIs.size() + " EOI(s), and " + allPOIs.size() + " POI(s)." + routeInfo;
	}
}
