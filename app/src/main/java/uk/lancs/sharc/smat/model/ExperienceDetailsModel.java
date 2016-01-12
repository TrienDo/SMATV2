package uk.lancs.sharc.smat.model;

import java.util.ArrayList;
import java.util.List;

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

import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Location;

import uk.lancs.sharc.smat.R;
import uk.lancs.sharc.smat.service.ExperienceDatabaseManager;
import uk.lancs.sharc.smat.service.SharcLibrary;

/**
 * <p>This class is a model of the Experience entity</p>
 * <p>It can be changed later depending on future work </p>
 *
 * Author: Trien Do
 * Date: Feb 2015
 **/
public class ExperienceDetailsModel {
	private List<POIModel> allPOIs;
	private List<EOIModel> allEOIs;
	private List<RouteModel> allRoutes;
	private List<ResponseModel> myResponses;
	private ExperienceMetaDataModel metaData;
	private List<Marker> allPoiMarkers;		//PoI Markers
	private List<Object> allTriggerZoneVizs;	//Geo-fences for PoIs
	private Marker startRoute;
	private Marker endRoute;

	private ExperienceDatabaseManager experienceDatabaseManager;

	/**
	 * @param experienceDatabaseManager: help interact with database
	 * @param isFromOnline: true = loading the experience from server -> delete existing data to have the fresh data
	 */

	public ExperienceDetailsModel(ExperienceDatabaseManager experienceDatabaseManager, boolean isFromOnline)
	{
		allPOIs = new ArrayList<POIModel>();
		allEOIs = new ArrayList<EOIModel>();
		allRoutes = new ArrayList<RouteModel>();
		myResponses = new ArrayList<ResponseModel>();
		allPoiMarkers = new ArrayList<Marker>();
		allTriggerZoneVizs = new ArrayList<Object>();
		this.experienceDatabaseManager = experienceDatabaseManager;
	}

	/**
	 * An experience is stored as a json file on Dropbox
	 * This function parse a json file to extract info of an experience and store this info in an SQLite db
	 * @param jsonExperience
	 */
	public void getExperienceFromSnapshotOnCloud(JSONObject jsonExperience) //parse content of an experience from JSON file and download media files
	{
		experienceDatabaseManager.parseJsonAndSaveToDB(jsonExperience);
	}

	public void renderAllPOIs(GoogleMap mMap, SMEPAppVariable mySMEPAppVariable)
	{
		allPOIs = experienceDatabaseManager.getAllPOIs();
		metaData.setPoiCount(allPOIs.size());
		mySMEPAppVariable.setAllPOIs(allPOIs);
		mySMEPAppVariable.setNewExperience(true);
		for(int i = 0; i < allPOIs.size(); i++)
		{
			renderPOIandTriggerZone(mMap, allPOIs.get(i), String.valueOf(i));//i = ID of marker -> when marker is tapped -> show Media of POI with that id
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

		//Check if POI is polygon or polyline to render
		if(curPOI.getPoiViz().size() > 0)
		{
			mMap.addPolyline((new PolylineOptions()
					.width(5)
					.color(SharcLibrary.hex2rgb("#FF0000"))
					.visible(true)
			)).setPoints(curPOI.getPoiViz());
		}
		String firstImage = curPOI.getThumbnailPath();

		if (firstImage.equalsIgnoreCase(""))
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
			shape =   mMap.addCircle(new CircleOptions()
							.center(curPOI.getTriggerZoneCoordinates().get(0))
							.radius(curPOI.getTriggerZoneRadius())
							.strokeWidth(2)
							.strokeColor(Color.parseColor(curPOI.getTriggerZoneColour()))
							.fillColor(SharcLibrary.hex2Argb(alpha, curPOI.getTriggerZoneColour()))
							.visible(true)
			);
		}
		else if(curPOI.getTriggerType().equals("polygon"))
		{
			shape =   mMap.addPolygon(new PolygonOptions()
							.addAll(curPOI.getTriggerZoneCoordinates())
							.strokeWidth(2)
							.strokeColor(Color.parseColor(curPOI.getTriggerZoneColour()))
							.fillColor(SharcLibrary.hex2Argb(alpha, curPOI.getTriggerZoneColour()))
							.visible(true)
			);
		}
		allTriggerZoneVizs.add(shape);
	}

	public void renderAllEOIs()
	{
		allEOIs = experienceDatabaseManager.getAllEOIs();
		metaData.setEoiCount(allEOIs.size());
		for (int i = 0; i < allEOIs.size(); i++)
		{
			List<MediaModel> mediaList = experienceDatabaseManager.getMediaForEntity(allEOIs.get(i).getEoiId(), "EOI");
			String content = allEOIs.get(i).getHTMLPresentation(mediaList);
			allEOIs.get(i).setMediaHTMLCode(content);
		}
	}

	//Return the index of the route which has not been saved (e.g., app crashes)
	//Else - 1 if all routes have been saved
	public int renderAllRoutes(GoogleMap mMap)
	{
		int inSaveRouteIndex = -1;
		allRoutes = experienceDatabaseManager.getAllRoutes();
		metaData.setRouteCount(allRoutes.size());
		String routeInfo = "";
		for (int i = 0; i < allRoutes.size(); i++)
		{
			routeInfo += "<div> - Route name: " + allRoutes.get(i).getName() + " (" +   String.format("%.2f", allRoutes.get(i).getDistance()) + " km). Description: " + allRoutes.get(i).getDescription() +"</div>";
			mMap.addPolyline((new PolylineOptions()
					.width(5)
					.color(SharcLibrary.hex2rgb(allRoutes.get(i).getColour()))
					.visible(true)
			)).setPoints(allRoutes.get(i).getLatLngPath());
			if(allRoutes.get(i).getDirected())
			{
				startRoute = mMap.addMarker(new MarkerOptions()
								.anchor(0.5f, 1.0f)
								.title("START")
								.position(allRoutes.get(i).getLatLngPath().get(0))
								.icon(BitmapDescriptorFactory.fromResource(R.raw.start))
								.visible(true)
				);
				if(allRoutes.get(i).getDescription().equalsIgnoreCase("This route is being recorded"))
				{
					inSaveRouteIndex = i;
				}
				else {
					endRoute = mMap.addMarker(new MarkerOptions()
									.title("END")
									.anchor(0.5f, 0.0f)
									.position(allRoutes.get(i).getLatLngPath().get(allRoutes.get(i).getLatLngPath().size() - 1))
									.icon(BitmapDescriptorFactory.fromResource(R.raw.end))
									.visible(true)
					);
				}
			}
			//metaData.setDifficultLevel(tmpRoute.getDescription());
		}
		metaData.setRouteInfo(routeInfo);
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
    		List<LatLng> path = allRoutes.get(k).getLatLngPath();
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

	public List<String> getPOIHtmlListItems(int index, LatLng currentLocation)//get data to display in the listview of POI Media
	{
		String state = "";
		if(currentLocation == null)
			state = "Sorry, the app cannot identify your current location at the moment";
		else
		{
			if(this.isLocationWithinTrigerzone(currentLocation, index))
				state = "Your current location falls within the trigger zone for this Point of Interest";
			else
				state = "Your current location does not fall within the trigger zone for this Point of Interest";
		}
		if(index >=0 && index < allPOIs.size())
			return allPOIs.get(index).getHtmlListItems(this.experienceDatabaseManager, state);
		else
			return null;
	}

	public List<String> getAllEOIMediaListItems() //get data to display in the listview of EOI Media
	{
		List<String> mediaList = new ArrayList<String>();
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
		List<ResponseModel> responseList = experienceDatabaseManager.getResponsesForTab("EOI");
		for(int i = 0; i< responseList.size(); i++ )
			mediaList.add(responseList.get(i).getHTMLCodeForResponse(false));
		return mediaList;
	}

	public String[] getHTMLCodeForEOI(String id)
	{
		for(int i = 0; i < allEOIs.size(); i++)
		{
			if(allEOIs.get(i).getEoiId().equalsIgnoreCase(id))
			{
				return new String[]{allEOIs.get(i).getName(), allEOIs.get(i).getMediaHTMLCode()};
			}
		}
		return null;
	}

	public void getMediaStatFromDB()
	{
		experienceDatabaseManager.getMediaStat(metaData);
	}


	public void updatePOIThumbnail(POIModel curPOI, int markerIndex)
	{
		//Get icon for marker to present the POI
		Marker tmpMarker = allPoiMarkers.get(markerIndex);
		String firstImage = curPOI.getThumbnailPath();//Only used when adding a new photo media item
		//if(firstImage.contains("http"))//downloaded
		//{
		//	firstImage = firstImage.substring(firstImage.lastIndexOf("/"),firstImage.lastIndexOf("."));
		//	firstImage = SharcLibrary.SHARC_MEDIA_FOLDER + "/" + firstImage + ".jpg";
		//}
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


	public String getExperienceSumaryInfo()
	{
		if(metaData != null) {
			String routeInfo = "";
			if(allRoutes.size() >= 1)
			{
				for(int i = 0; i < allRoutes.size(); i++)
					routeInfo += "<div> - Route name: " + allRoutes.get(i).getName() + " (" +   String.format("%.2f", allRoutes.get(i).getDistance()) + " km). Description: " + allRoutes.get(i).getDescription() +"</div>";
			}
			metaData.setRouteInfo(routeInfo);
			return metaData.getExperienceStats();
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

	public void getMyResponsesFromDatabase()
	{
		myResponses.clear();
		myResponses = experienceDatabaseManager.getMyResponses();
	}

	public String getMyResponseName(int i)
	{
		ResponseModel res = myResponses.get(i);
		String entityType = res.getEntityType().toUpperCase();
		String mediaType = res.getContentType().toUpperCase();

		if(entityType.equalsIgnoreCase(ResponseModel.FOR_POI))
		{
			String enName = getPOINameFromID(res.getEntityId());
			if(enName!=null) {
				if(mediaType.equalsIgnoreCase("text"))
					return ("Added a new " + mediaType + " for POI " + enName + " (" + SharcLibrary.getStringSize(myResponses.get(i).getContent()) + "KB)");
				else if(mediaType.equalsIgnoreCase("image"))
					return ("Added a new " + mediaType + " for POI " + enName + " (" + SharcLibrary.getFilesize(myResponses.get(i).getContent(),true) + "MB)");
				else
					return ("Added a new " + mediaType + " for POI " + enName + " (" + SharcLibrary.getFilesize(myResponses.get(i).getContent(),false) + "MB)");
			}
		}
		else if(entityType.equalsIgnoreCase(ResponseModel.FOR_ROUTE))
		{
			return ("Created a new route named " + myResponses.get(i).getContent());
		}
		else if(entityType.equalsIgnoreCase(ResponseModel.FOR_NEW_POI))
		{
			return ("Created a new POI named " + myResponses.get(i).getContent());
		}
		return "";
	}

	public void deleteMyResponseAt(int index)
	{
		experienceDatabaseManager.deleteMyResponse(myResponses.get(index).getId());
	}

	public void addMyResponse(ResponseModel res)
	{
		myResponses.add(res);
		experienceDatabaseManager.insertResponse(res);
	}

	/**********************************
	 //Getters and setters for the class
	 **********************************/

	public ExperienceMetaDataModel getMetaData() {
		return metaData;
	}

	public void setMetaData(ExperienceMetaDataModel metaData) {
		this.metaData = metaData;
	}

	public void updateMetaData(){
		experienceDatabaseManager.updateExperienceOnceUploadedToServer(metaData);
	}
	public List<ResponseModel> getMyResponses() {
		return myResponses;
	}

	/**********************************
	 //Other getters and setters
	 **********************************/

	public String getPOIName(int index)
	{
		return allPOIs.get(index).getName();
	}

	public String getPOIID(int index)
	{
		if(index >=0 && index < allPOIs.size())
			return allPOIs.get(index).getPoiId();
		else
			return "-1";
	}

	public ResponseModel getMyResponseAt(int index)
	{
		return myResponses.get(index);
	}

	public String getMyResponseContentAt(int index)
	{
		return myResponses.get(index).getHTMLCodeForResponse(true);
	}

	public String getPOINameFromID(String id)
	{
		for (int i = 0; i < allPOIs.size(); i++)
		{
			if(allPOIs.get(i).getPoiId().equalsIgnoreCase(id))
				return allPOIs.get(i).getName();
		}
		return null;
	}

	public String getEOINameFromID(String id)
	{
		for (int i = 0; i < allEOIs.size(); i++)
		{
			if(allEOIs.get(i).getEoiId().equalsIgnoreCase(id))
				return allEOIs.get(i).getName();
		}
		return null;
	}

	public void removeUploadedResponseAt(int index)
	{
		ResponseModel response = myResponses.get(index);
		experienceDatabaseManager.removeUploadedResponse(response.getId());
	}

	//Return index of the new POI
	public int addNewPOI(String id, String name, String description, String coordinate, String triggerZone, String typeList, GoogleMap mMap, SMEPAppVariable mySMEPAppVariable)
	{
		//add to internal memory
		POIModel tmpPOI = new POIModel(id, name, description, coordinate, triggerZone, metaData.getDesignerId(), metaData.getExperienceId(), typeList, "", "", "", 0, 0);
		renderPOIandTriggerZone(mMap, tmpPOI, String.valueOf(allPOIs.size()));//ID of marker -> when marker is tapped -> show Media of POI with that id
		allPOIs.add(tmpPOI);
		metaData.setPoiCount(allPOIs.size());
		mySMEPAppVariable.setAllPOIs(allPOIs);
		mySMEPAppVariable.setNewExperience(true);
		experienceDatabaseManager.savePoi(tmpPOI);
		return allPOIs.size()-1;
	}

	public void addNewRoute(RouteModel inRoute)
	{
		//add to internal memory
		allRoutes.add(inRoute);
		metaData.setRouteCount(allRoutes.size());
		metaData.setRouteLength(metaData.getRouteLength() + inRoute.getDistance() / 1000);
		metaData.setDifficultLevel(inRoute.getDescription());
		experienceDatabaseManager.saveRoute(inRoute);
	}

	public void updateRoute(RouteModel inRoute)
	{
		//add to internal memory
		metaData.setRouteLength(metaData.getRouteLength() + inRoute.getDistance() / 1000);
		metaData.setDifficultLevel(inRoute.getDescription());
		experienceDatabaseManager.updateRoute(inRoute);
	}

	public void updateRoutePath(RouteModel inRoute)
	{
		experienceDatabaseManager.updateRoute(inRoute);
	}

	public void removeRoute(RouteModel inRoute)
	{
		//add to internal memory
		allRoutes.remove(inRoute);
		metaData.setRouteCount(allRoutes.size());
		metaData.setRouteLength(metaData.getRouteLength() - inRoute.getDistance() / 1000);
		metaData.setDifficultLevel("");
		experienceDatabaseManager.deleteRoute(inRoute.getId());
	}

	public void addNewMediaItem(ResponseModel response)
	{
		boolean isMainMedia = experienceDatabaseManager.saveMediaFromResponse(response);
			//update media order for POI
		POIModel poi = this.getPOIFromID(response.getEntityId());
		if (poi != null) {
			if (response.getContentType().equalsIgnoreCase(MediaModel.TYPE_IMAGE)) {
				if(isMainMedia && !poi.getType().equalsIgnoreCase(SharcLibrary.SHARC_POI_ACCESSIBILITY)) {
					poi.setThumbnailPath(response.getContent());
					this.updatePOIThumbnail(poi, this.getPoiIndexFromID(response.getEntityId()));
				}
				this.metaData.setImageCount(this.metaData.getImageCount() + 1);
			} else if (response.getContentType().equalsIgnoreCase(MediaModel.TYPE_TEXT)) {
				this.metaData.setTextCount(this.metaData.getTextCount() + 1);
			} else if (response.getContentType().equalsIgnoreCase(MediaModel.TYPE_AUDIO)) {
				this.metaData.setAudioCount(this.metaData.getAudioCount() + 1);
			} else if (response.getContentType().equalsIgnoreCase(MediaModel.TYPE_VIDEO)) {
				this.metaData.setVideoCount(this.metaData.getVideoCount() + 1);
			}
		}
	}

	public void updatePublicURLForMedia(Long mediaId, String mediaURL)
	{
		experienceDatabaseManager.updateMediaURL(mediaId, mediaURL);
	}

	public POIModel getPOIFromID(String id)
	{
		for (int i = 0; i < allPOIs.size(); i++)
		{
			if(allPOIs.get(i).getPoiId().equalsIgnoreCase(id))
				return allPOIs.get(i);
		}
		return null;
	}

	public int getPoiIndexFromID(String id)
	{
		for (int i = 0; i < allPOIs.size(); i++)
		{
			if(allPOIs.get(i).getPoiId().equalsIgnoreCase(id))
				return i;
		}
		return -1;
	}

	public RouteModel getRouteFromID(String id)
	{
		for (int i = 0; i < allRoutes.size(); i++)
		{
			if(allRoutes.get(i).getRouteId().equalsIgnoreCase(id))
				return allRoutes.get(i);
		}
		return null;
	}

	public MediaModel getMediaFromId(String id)
	{
		return experienceDatabaseManager.getMediaFromId(id);
	}

	public RouteModel getRouteAt(int index)
	{
		return allRoutes.get(index);
	}

	public String getOverallSummary()
	{
		String routeInfo = "";
		if(allRoutes.size() > 0)
		{
			for(int i = 0; i < allRoutes.size(); i++)
				routeInfo += " [Route name: " + allRoutes.get(i).getName() + " (" +   String.format("%.2f", allRoutes.get(i).getDistance()) + " km). " + allRoutes.get(i).getDescription() +"].";
		}
		return "This experience has " + allRoutes.size() + " route(s), " + allEOIs.size() + " EOI(s), and " + allPOIs.size() + " POI(s)." + routeInfo;
	}
}
