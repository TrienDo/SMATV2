package uk.lancs.sharc.smat.model;
import com.google.android.gms.maps.model.LatLng;
import com.orm.SugarRecord;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * <p>This class stores meta data about an experience</p>
 * <p>It is mainly used to show available online/cached
 * experiences on Google Maps, Summary Info</p>
 *
 * Author: Trien Do
 * Date: Feb 2015
 **/
public class ExperienceMetaDataModel extends SugarRecord {
	private String mid;//note: dont use id because SugarORM already uses this id
	private String name;
	String description;
	String createdDate;
	String lastPublishedDate;
	String designerId;
	boolean isPublished;
	int moderationMode;
	String latLng;
	String summary;
	String snapshotPath;
	String thumbnailPath;
	int size;
	String theme;

	private int textCount = 0;
	private int imageCount = 0;
	private int audioCount = 0;
	private int videoCount = 0;
	private int poiCount = 0;
	private int eoiCount = 0;
	private int routeCount = 0;
	private float routeLength = 0.0f;
	private String difficultLevel = "";
	private String routeInfo = "";

	public ExperienceMetaDataModel(){

	}

	public String getExperienceId() {
		return mid;
	}

	public ExperienceMetaDataModel(String id, String name, String description, String createdDate, String lastPublishedDate, String designerId, boolean isPublished,
								   int moderationMode, String latLng, String summary, String snapshotPath, String thumbnailPath, int size, String theme)
	{
		this.mid = id;
		this.name = name;
		this.description = description;
		this.createdDate = createdDate;
		this.lastPublishedDate = lastPublishedDate;
		this.designerId = designerId;
		this.isPublished = isPublished;
		this.moderationMode = moderationMode;
		this.latLng = latLng;
		this.summary = summary;
		this.snapshotPath = snapshotPath;
		this.thumbnailPath = thumbnailPath;
		this.size = size;
		this.theme = theme;
	}

	public void setDesignerId(String designerId) {
		this.designerId = designerId;
	}

	public String getLastPublishedDate() {
		return lastPublishedDate;
	}

	public String getLatLng() {
		return latLng;
	}

	public boolean isPublished() {
		return isPublished;
	}

	public int getModerationMode() {
		return moderationMode;
	}

	public String getThumbnailPath() {
		return thumbnailPath;
	}

	public String getTheme() {
		return theme;
	}

	public String getSnapshotPath() {
		return snapshotPath;
	}

	public void setExperienceId(String id){
		mid = id;
	}
	public String getSummary() {
		return summary;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}

	public int getSize() {
		return size;
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

	public String getName() {
		return name;
	}

	public String getDescription() {
		return  description;
	}

	public String getCreatedDate() {
		return createdDate;
	}


	public String getDesignerId() {
		return designerId;
	}

	public LatLng getLocation()
	{
		String[] location = latLng.split(" ");
		try {
			return new LatLng(Double.parseDouble(location[0]), Double.parseDouble(location[1]));
		}
		catch (Exception e){
			e.printStackTrace();
			return new LatLng(0,0);
		}
	}

	public String getPublicURL() {
		return snapshotPath;
	}

	public void setPublicURL(String publicURL) {
		snapshotPath = publicURL;
	}

	public int getMediaCount() {
		return textCount + imageCount + audioCount + videoCount;
	}

	public float getRouteLength() {
		return routeLength;
	}

	public void setRouteLength(float routeLength) {
		this.routeLength = routeLength;
	}

	public String getDifficultLevel() {
		return difficultLevel;
	}

	public void setDifficultLevel(String difficultLevel) {
		this.difficultLevel = difficultLevel;
	}

	public String getExperienceStats()
	{
		String htmlCode = "<div><b>The current experience is '" + this.getName()+ "'. It comprises: </b></div>";
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

	public String getRouteInfo() {
		return routeInfo;
	}

	public void setRouteInfo(String routeInfo) {
		this.routeInfo = routeInfo;
	}

	public JSONObject toJSON(){
		JSONObject jsonObject = new JSONObject();
		try {
			jsonObject.put("id", getExperienceId());
			jsonObject.put("name", getName());
			jsonObject.put("description", getDescription());
			jsonObject.put("createdDate", getCreatedDate());
			jsonObject.put("lastPublishedDate", getLastPublishedDate());
			jsonObject.put("designerId", getDesignerId());
			jsonObject.put("isPublished", isPublished());
			jsonObject.put("moderationMode", getModerationMode());
			jsonObject.put("latLng", getLatLng());
			jsonObject.put("summary", getSummary());
			jsonObject.put("snapshotPath", "");
			jsonObject.put("thumbnailPath", getThumbnailPath());
			jsonObject.put("size", getSize());
			jsonObject.put("theme", getTheme());
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return jsonObject;
	}

	public ExperienceMetaDataModel(JSONObject objExperience)
	{
		try {
			this.mid = objExperience.getString("id");
			this.name = objExperience.getString("name");
			this.description = objExperience.getString("description");
			if (description.length() > 0 && description.charAt(description.length() - 1) != '.')
				description.concat(".");
			this.createdDate = objExperience.getString("createdDate");
			this.lastPublishedDate = objExperience.getString("lastPublishedDate");
			this.designerId = objExperience.getString("designerId");
			String pl = objExperience.getString("isPublished");
			if(pl.equalsIgnoreCase("1"))
				this.isPublished = true;
			else
				this.isPublished = false;
			this.moderationMode = objExperience.getInt("moderationMode");
			this.latLng = objExperience.getString("latLng");
			this.summary = objExperience.getString("summary");
			this.snapshotPath = objExperience.getString("snapshotPath");
			this.thumbnailPath = objExperience.getString("thumbnailPath");
			this.size = objExperience.getInt("size");
			this.theme = objExperience.getString("theme");
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
}
