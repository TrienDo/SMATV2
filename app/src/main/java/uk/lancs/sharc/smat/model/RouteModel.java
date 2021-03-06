package uk.lancs.sharc.smat.model;

import android.location.Location;

import java.util.ArrayList;
import java.util.List;

import com.google.android.gms.maps.model.LatLng;
import com.orm.SugarRecord;
import com.orm.dsl.Ignore;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * <p>This class is a reduced model of the route entity</p>
 * <p>It can be changed later depending on future work </p>
 *
 * Author: Trien Do
 * Date: Feb 2015
 */

public class RouteModel extends SugarRecord {
	//@Unique
	private String mid;
	private String designerId;
	private String experienceId;
	private String name;
	private String description;
	private boolean directed;
	private String colour;
	private String path;
	private String poiList;
	private String eoiList;

	@Ignore
	List<LatLng> latLngPath;
	public RouteModel(){
		latLngPath = new ArrayList<LatLng>();
	}

	public RouteModel(String id, String designerId, String experienceId, String name, String description, boolean directed, String colour, String path, String poiList, String eoiList){
		this.mid = id;
		this.designerId = designerId;
		this.experienceId = experienceId;
		this.name = name;
		this.description = description;
		this.directed = directed;
		this.colour = colour;
		this.path = path;
		this.poiList = poiList;
		this.eoiList = eoiList;
		latLngPath = new ArrayList<LatLng>();
		parseLatLngPathFromPathString();
	}

	public List<LatLng> getLatLngPath() {
		if(latLngPath.size() == 0 ){
			parseLatLngPathFromPathString();
		}
		return latLngPath;
	}

	private void parseLatLngPathFromPathString(){
		if (!path.equalsIgnoreCase("")) {
			String[] latLngInfo = this.path.split(" ");
			if (latLngInfo.length >= 2) {
				int i = 0;
				while (i < latLngInfo.length) {
					latLngPath.add(new LatLng(Float.parseFloat(latLngInfo[i]), Float.parseFloat(latLngInfo[i + 1])));
					i += 2;
				}
			}
		}
	}


	public void setPath(String path) {
		this.path = path;
	}

	public String getPathString(){
		return  path;
	}
	public float getDistance() {
		float distance = 0.0f;
		float[] results = new float[1];
		List<LatLng> routePath = this.getLatLngPath();
		for (int i=1; i < routePath.size(); i++)
		{
			Location.distanceBetween(routePath.get(i - 1).latitude, routePath.get(i - 1).longitude, routePath.get(i).latitude, routePath.get(i).longitude, results);
			distance += results[0];
		}
		return distance / 1000 ;//km
	}
	public void setDistance(float distance) {
		//this.distance = distance;
	}

	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}

	public void setRouteId(String id) {
		this.mid = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getColour() {
		return colour;
	}

	public void setColour(String colour) {
		this.colour = colour;
	}

	public boolean getDirected() {
		return directed;
	}

	public void setDirected(boolean directed) {
		this.directed = directed;
	}

	public String getRouteId() {
		return mid;
	}

	public String getDesignerId() {
		return designerId;
	}

	public void setDesignerId(String designerId) {
		this.designerId = designerId;
	}

	public String getExperienceId() {
		return experienceId;
	}

	public void setExperienceId(String experienceId) {
		this.experienceId = experienceId;
	}

	public boolean isDirected() {
		return directed;
	}

	public String getPoiList() {
		return poiList;
	}

	public void setPoiList(String poiList) {
		this.poiList = poiList;
	}

	public String getEoiList() {
		return eoiList;
	}

	public void setEoiList(String eoiList) {
		this.eoiList = eoiList;
	}

	public JSONObject toJson(){
		JSONObject routeExperience = new JSONObject();
		try {
			//Media designer first
			JSONObject routeDesigner = new JSONObject();
			routeDesigner.put("id", this.mid);
			routeDesigner.put("name", this.name);
			routeDesigner.put("directed", this.directed);
			routeDesigner.put("colour", this.colour);
			routeDesigner.put("path", this.path);
			routeDesigner.put("designerId", this.designerId);

			routeExperience.put("id", this.mid);
			routeExperience.put("routeDesigner", routeDesigner);
			routeExperience.put("experienceId", this.experienceId);
			routeExperience.put("description", this.description);
			routeExperience.put("eoiList", this.eoiList);
			routeExperience.put("poiList", this.poiList);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return routeExperience;
	}
}
