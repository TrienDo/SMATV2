package uk.lancs.sharc.smat.model;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import uk.lancs.sharc.smat.service.DBExperienceDetails;

import com.google.android.gms.maps.model.LatLng;

/**
 * <p>This class is a model of the POI entity</p>
 * <p>It can be changed later depending on future work </p>
 *
 * Author: Trien Do
 * Date: Feb 2015
 **/

public class POIModel {	
	
	private String id;
	private String name;
	private String desc;
	private LatLng location;
	private String LocationString;
	private String triggerZoneString;
	private String[] mediaOrder;
	private String[] relatedEOIs;
	private String triggerType;
	private String type;
	private String colour;
	private float radius;		                                //For circle trigger zone
	private ArrayList<LatLng> coors= new ArrayList<LatLng>();
	//private ArrayList<String> responseList;                     //Each list item is htmlCode of a response
	
	public POIModel(String mID, String mName, String mType, String mDesc, String locationString,String mediaOrderString, String relatedEOIString, String triggerZoneString)
	{
		this.LocationString = locationString;
		this.triggerZoneString = triggerZoneString;
		String[] locationInfo = locationString.split(" ");      //locationString is in the format:Lat[space]Lng
		String[] triggerInfo = triggerZoneString.split(" ");
		this.id = mID;
		this.name = mName;
		this.desc = mDesc;
		this.location = new LatLng(Double.parseDouble(locationInfo[0]), Double.parseDouble(locationInfo[1]));
		if (mediaOrderString.equalsIgnoreCase(""))
			this.mediaOrder = null;
		else
			this.mediaOrder = mediaOrderString.split(" ");
		this.type = mType;
		this.triggerType = triggerInfo[0];
		this.colour	= "#" + triggerInfo[1];
		if(this.triggerType.equalsIgnoreCase("circle")) //triggerZoneString of circle: type[space]colour[space]radius[space]lat[space]lng
		{
			radius = Float.parseFloat(triggerInfo[2]);
			coors.add(new LatLng(Float.parseFloat(triggerInfo[3]), Float.parseFloat(triggerInfo[4])));
		}
		else //triggerZoneString of polygon: type[space]colour[space]lat1[space]lng1---latN[space]lngN
		{
			radius = 0;
			int k = 2;
			while (k < triggerInfo.length)
			{
				coors.add(new LatLng(Float.parseFloat(triggerInfo[k]), Float.parseFloat(triggerInfo[k+1])));
				k+=2;
			}
		}
		//responseList = null;
		if(relatedEOIString.equalsIgnoreCase(""))
			relatedEOIs = null;
		else
			relatedEOIs = relatedEOIString.split(" ");
	}	 
	
	public String getID()
	{
		return id;
	}
	
	public String getName()
	{
        try {
            return  URLDecoder.decode(name,"UTF-8") ;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return name;
	}
	
	public String getDesc()
	{
        try {
            return  URLDecoder.decode(desc,"UTF-8") ;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return desc;
	}
	public String getLocationString() {
		return LocationString;
	}

	public void setLocationString(String locationString) {
		LocationString = locationString;
	}
	public String getColour()
	{
		return colour;
	}
	
	public ArrayList<LatLng> getCoordinates()
	{
		return coors;
	}
		
	public float getRadius()
	{
		return radius;
	}

	public String getType() {
		return type;
	}

	public String getTriggerType() {
		return triggerType;
	}

	public LatLng getLocation() {
		return location;
	}

	public String[] getMediaOrder() {
		return mediaOrder;
	}

	public String getTriggerZoneString() {
		return triggerZoneString;
	}

	public void setTriggerZoneString(String triggerZoneString) {
		this.triggerZoneString = triggerZoneString;
	}
	/*public ArrayList<String> getResponseList() {
		return responseList;
	}

	public void setResponseList(ArrayList<String> responseList) {
		this.responseList = responseList;
	}*/

	public ArrayList<String> getHtmlListItems(DBExperienceDetails db, String state)  //List of EOIs - Media - Responses. All EOIs is a list item, each media and response is a separated item
	{		
		ArrayList<String> strHtmlListItems = new ArrayList<String>();
		//Header = EOI info
		String header = "";
		//header = "<h3>" + this.getName() + "</h3><div style='color:gray;'>["  + state + "]</div><br/>";
		header = "<h3>" + this.getName() + "</h3>";
		int totalMedia = this.getMediaOrder() == null ? 0 : this.getMediaOrder().length;
		//Get media
		List<MediaModel> mediaList = db.getMediaForEntity(this.id);
		//Get responses
		List<ResponseModel> responseList = db.getResponsesForEntity(this.id);
		//Get EOIs
		if(relatedEOIs!=null)
		{
			if(relatedEOIs.length > 1)
				header += "<div> This Point of Interest (POI) has " + relatedEOIs.length + " related events</div>";
			else
				header += "<div> This Point of Interest (POI) has " + relatedEOIs.length + " related event</div>";

			for (int i = 0; i < relatedEOIs.length; i++)			
			{
				String[] eoiInfo = db.getEOIFromID(relatedEOIs[i]);
				//header += "<blockquote><button style='width:95%;height:40px;' onclick='alert(\""+ eoiInfo[1] + " Please go to Events of OI Media tab for more information!\")'>" + eoiInfo[0] + "</button></blockquote>";
				//Call a function bound in the AndroidWebViewInterface when an EOI button is clicked
				header += "<blockquote><button style='width:95%;height:40px;font-size:20px;' onclick='Android.showEOIInfo(\"" + relatedEOIs[i]   + "\")' >" + eoiInfo[0] + "</button></blockquote>";
			}
		}
		else
			header += "<div> This Point of Interest (POI) does not have any related events</div>";
		header += "<div>You have associated " +  totalMedia + " following media " + (totalMedia > 1 ? "items" : "item") + " with this POI";
		//header += "<div>You have associated 1 following media item with this POI";
		if(responseList !=null && responseList.size() > 0)
			header += " and " + responseList.size() + (responseList.size() > 1 ? " media items submitted in responses.</div>" : " media item submitted in a response.</div>") ;
		else
			header += ".</div>";
		//Add overview info
		strHtmlListItems.add(header);
		//Add media list
		strHtmlListItems.addAll(this.getHTMLMediaListItems(mediaList));
        //Add response list
		strHtmlListItems.addAll(this.getHTMLResponseListItems(responseList));
		return strHtmlListItems ; //EOI + Media + Response
	}

	public List<String> getHTMLMediaListItems(List<MediaModel> mediaList)
	{
		Hashtable<String, String> mediaIDList = new Hashtable<String, String>();
		ArrayList<String> htmlMediaArray = new ArrayList<String>();
		if(mediaList != null && mediaList.size() > 0)
		{
            for (MediaModel media : mediaList)
            {
                String strMedia = media.getHTMLPresentation();
				htmlMediaArray.add(strMedia);
				mediaIDList.put(media.getId(), strMedia);
			}
		}
		if(mediaOrder != null && mediaOrder.length > 0)
		{
			htmlMediaArray.clear();//Reorder media
			for(int i = 0; i < mediaOrder.length; i++)
			{
				if(mediaIDList.get(mediaOrder[i])!=null)
					htmlMediaArray.add(mediaIDList.get(mediaOrder[i]));
			}
		}
		return htmlMediaArray;
	}

    public List<String> getHTMLResponseListItems(List<ResponseModel> responseList)
    {
        ArrayList<String> htmlResponseArray = new ArrayList<String>();
        if(responseList != null && responseList.size() > 0)
        {
            for (ResponseModel response : responseList)
            {
                String strResponse = response.getHTMLCodeForResponse(false);
                htmlResponseArray.add(strResponse);
            }
        }
        return htmlResponseArray;
    }

	public String getStringMediaOrder()
	{
		String strBuilder = "";
		if(this.mediaOrder == null)
			return "";
		for (int i = 0; i < this.mediaOrder.length; i++) {
			strBuilder +=" " + this.mediaOrder[i];
		}
		return strBuilder.substring(1);//remove the first space
	}

	public void setMediaOrderFromString(String mediaOrderString)
	{
		if (mediaOrderString.equalsIgnoreCase(""))
			this.mediaOrder = null;
		else
			this.mediaOrder = mediaOrderString.split(" ");
	}
}
