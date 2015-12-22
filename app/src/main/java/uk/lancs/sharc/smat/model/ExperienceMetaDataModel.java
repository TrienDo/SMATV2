package uk.lancs.sharc.smat.model;
import com.google.android.gms.maps.model.LatLng;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

/**
 * <p>This class stores meta data about an experience</p>
 * <p>It is mainly used to show available online/cached
 * experiences on Google Maps, Summary Info</p>
 *
 * Author: Trien Do
 * Date: Feb 2015
 **/
public class ExperienceMetaDataModel {
	private String proName = "";
	private String proLocation = "";
	private String proDesc = "";
	private String proDate = "";
	private String proAuthID = "";
	private String proAuthName = "";
	private String proAuthEmail = "";
	private String proPublicURL = "";
	private String proPath;
	
	private int textCount = 0;
	private int imageCount = 0;
	private int audioCount = 0;
	private int videoCount = 0;
	private int poiCount = 0;
	private int eoiCount = 0;
	private int routeCount = 0;
	private float routeLength = 0.0f;
	private String difficultLevel = "Easy";
	private String routeInfo = "";
	public ExperienceMetaDataModel(String name, String path, String desc, String date, String authID, String publicURL, String location)
	{
		proName = name;
		proPath = path;
		proDesc = desc;
        if(proDesc == null || proDesc.equalsIgnoreCase("null"))
            proDesc =  "";
		proDate = date;
		proAuthID = authID;
		int openB = authID.indexOf("(");		
		setProAuthName(authID.substring(0,openB));
		setProAuthEmail(authID.substring(openB + 1, authID.length()-1));;
		proPublicURL = publicURL;		
		proLocation = location;		
	}

	public void setProPublicURL(String proPublicURL) {
		this.proPublicURL = proPublicURL;
	}
	public void setProPath(String proPath) {
		this.proPath = proPath;
	}

	public void setProDesc(String proDesc) {
		this.proDesc = proDesc;
	}

	public int getTextCount() {
		return textCount;
	}

	public void setTextCount(int textCount) {
		this.textCount = textCount;
	}

	public int getImageCount() {
		return imageCount;
	}

	public void setImageCount(int imageCount) {
		this.imageCount = imageCount;
	}

	public int getAudioCount() {
		return audioCount;
	}

	public void setAudioCount(int audioCount) {
		this.audioCount = audioCount;
	}

	public int getVideoCount() {
		return videoCount;
	}

	public void setVideoCount(int videoCount) {
		this.videoCount = videoCount;
	}

	public int getPoiCount() {
		return poiCount;
	}

	public void setPoiCount(int poiCount) {
		this.poiCount = poiCount;
	}

	public int getEoiCount() {
		return eoiCount;
	}

	public void setEoiCount(int eoiCount) {
		this.eoiCount = eoiCount;
	}

	public int getRouteCount() {
		return routeCount;
	}

	public void setRouteCount(int routeCount) {
		this.routeCount = routeCount;
	}

	public String getProName() {
		try {
			return URLDecoder.decode(proName, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return proName;
	}

	public String getProLocation() {
		return proLocation;
	}

	public String getProDesc() {
        try {
            return URLDecoder.decode(proDesc, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return  proDesc;
	}

	public String getProDate() {
		return proDate;
	}

	public String getProAuthID() {
		return proAuthID;
	}

	public String getProPath() {
		return proPath;
	}

	
	public LatLng getLocation()
	{
		String[] latlng = proLocation.split(" ");
		return new LatLng(Double.parseDouble(latlng[0]), Double.parseDouble(latlng[1]));
	}

	public String getProPublicURL() {
		return proPublicURL;
	}

	public int getMediaCount() {
		return textCount + imageCount + audioCount + videoCount;
	}

	public float getRouteLength() {
		return routeLength;
	}

	public void setRouteLength(float routeLength) {
		if(routeLength < 0)
			routeLength = 0;
		this.routeLength = routeLength;
	}

	public String getRouteInfo() {
		return routeInfo;
	}

	public void setRouteInfo(String routeInfo) {
		this.routeInfo = routeInfo;
	}

	public String getDifficultLevel() {
		return difficultLevel;
	}

	public void setDifficultLevel(String difficultLevel) {
		this.difficultLevel = difficultLevel;
	} 
	public String getSumaryInfo()
	{
	  	String htmlCode = "<div><b>This experience comprises: </b></div>";
	  	htmlCode += "<div>" + this.getRouteCount() + (this.getRouteCount()  > 1 ? " routes </div>" : " route </div>") + this.getRouteInfo();
	  	htmlCode += "<div>" + this.getEoiCount() + (this.getEoiCount() > 1 ? " Events of Interest (EOIs). </div>" : " Event of Interest (EOIs). </div>");
	  	htmlCode += "<div>" + this.getPoiCount() + (this.getPoiCount() > 1 ? " Points of Interest (POIs). </div>" : " Point of Interest (POIs). </div>");
	  	htmlCode += "<div>" + this.getMediaCount() + (this.getMediaCount() > 1 ? " media items (" : " media item (")
				+ this.getTextCount() + (this.getTextCount() > 1 ? " texts, " : " text, ")
				+ this.getImageCount() + (this.getImageCount() > 1 ? " photos, " : " photo, ")
				+ this.getAudioCount() + (this.getAudioCount() > 1 ? " audios and " : " audio and ")
				+ this.getVideoCount() + (this.getVideoCount() > 1 ? " videos).</div> " : " video).</div>");
	  	return htmlCode;
	}

	public String getSumaryInfoOld()
	{
		String indentFirst = "style='text-indent: 1em;'";
		String indentSecond = "style='text-indent: 2em;'";
		String indentThird = "style='text-indent: 3em;'";
		String htmlCode = "<div " + indentFirst  + "><b>Experience name: </b></div>";
		htmlCode +=	 "<div  " + indentSecond  + ">" + this.getProName() + "</div>";
		htmlCode += "<div " + indentFirst  + "><b>Created by: </b></div>";
		htmlCode += "<div  " + indentSecond  + ">"  + this.getProAuthID() + "</div>";
		htmlCode += "<div  " + indentFirst  + "><b>Created date: </b></div>";
		htmlCode += "<div  " + indentSecond  + ">"  + this.getProDate() + "</div>";
		htmlCode += "<div  " + indentFirst  + "><b>This experience comprises: </b></div>";
		htmlCode += "<div  " + indentSecond  + "> + " + this.getRouteCount() + " Route </div>";
		htmlCode += "<div  " + indentThird  + "> - Walking distance: " + String.format("%.2f", this.getRouteLength()) + " km </div>";
		htmlCode += "<div  " + indentThird  + "> - Level of difficulty: " + this.getDifficultLevel() + " </div>";
		htmlCode += "<div  " + indentSecond  + "> + " + this.getEoiCount() + " Events of Interest (EOIs) </div>";
		htmlCode += "<div  " + indentSecond  + "> + " + this.getPoiCount() + " Points of Interest (POIs) </div>";
		htmlCode += "<div  " + indentSecond  + "> + Number of media items: " + this.getMediaCount() + "</div>";
		htmlCode += "<div  " + indentThird  + "> - Number of text items: " + this.getTextCount() + "</div>";
		htmlCode += "<div  " + indentThird  + "> - Number of photo items: " + this.getImageCount() + "</div>";
		htmlCode += "<div  " + indentThird  + "> - Number of audio items: " + this.getAudioCount() + "</div>";
		htmlCode += "<div  " + indentThird  + "> - Number of video items: " + this.getVideoCount() + "</div>";
		return htmlCode;
	}

	public String getProAuthName() {
        try {
            return URLDecoder.decode(proAuthName, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return proAuthName;
	}

	public void setProAuthName(String proAuthName) {
		this.proAuthName = proAuthName;
	}

	public String getProAuthEmail() {
		return proAuthEmail;
	}

	public void setProAuthEmail(String proAuthEmail) {
		this.proAuthEmail = proAuthEmail;
	}
}
