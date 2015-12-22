package uk.lancs.sharc.smat.model;

import android.location.Location;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Date;

import com.google.android.gms.maps.model.LatLng;
/**
 * <p>This class is a reduced model of the route entity</p>
 * <p>It can be changed later depending on future work </p>
 *
 * Author: Trien Do
 * Date: Feb 2015
 */

public class RouteModel {
	private String id;
	private String name;    
	private String desc;
	private String colour;	
	private ArrayList<LatLng> path; 
	private float distance;			//the length of the route --> summary info
	private String directed;
	//The below attributes are not used now but may be used in the future
    //this.mediaOrder = new Array();
    //this.associatedMedia = new Array();       
    //this.associatedPOI = mSelectedPOIs;    
    //is.associatedEOI = mSelectedEOIs;



	public RouteModel()
	{
		this.id = String.valueOf(new Date().getTime());
		this.colour = "FF0000";
		this.name = "";
		this.desc = "";
		this.path = new ArrayList<LatLng>();
		this.directed = "true";
		this.distance = 0;
	}

	public RouteModel(String id, String name, String desc, String colour, float distance, String directed)
	{
		this.id = id;
		this.colour = colour;
		this.name = name;
		this.desc = desc;
		this.path = new ArrayList<LatLng>();
		this.directed = directed;
		this.distance = distance;
	}

	public String getDirected()
	{
		return directed;
	}

	public void setDirected(String directed) {
		this.directed = directed;
	}

	public String getPathString()
	{
		if (path.size() <= 0)
			return "";
		//float distance = 0.0f;
		//float[] results = new float[1];
		String pathString = path.get(0).latitude + " " + path.get(0).longitude;
		for (int i=1; i < path.size(); i++)
		{
			pathString += " " + path.get(i).latitude + " " + path.get(i).longitude;
			//Location.distanceBetween(path.get(i - 1).latitude, path.get(i - 1).longitude, path.get(i).latitude, path.get(i).longitude, results);
			//distance += results[0];
		}
		//this.setDistance(distance/1000);//Get km
		return pathString;
	}
	public ArrayList<LatLng> getPath() {
		return path;
	}

	public LatLng getLastPointOfPath()
	{
		if(path.size() > 0)
			return path.get(path.size()-1);
		else
			return null;
	}
	public void setPath(ArrayList<LatLng> path) {
		this.path = path;
	}
	public float getDistance() {
		return distance;
	}
	public void setDistance(float distance) {
		this.distance = distance;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		try {
			return URLDecoder.decode(name, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDesc() {
		try {
			return URLDecoder.decode(desc, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	public String getColour() {
		return colour;
	}

	public void setColour(String colour) {
		this.colour = colour;
	}
}
