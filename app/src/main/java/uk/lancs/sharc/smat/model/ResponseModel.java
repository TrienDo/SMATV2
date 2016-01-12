package uk.lancs.sharc.smat.model;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Date;

import uk.lancs.sharc.smat.service.SharcLibrary;
import android.net.Uri;

import com.orm.SugarRecord;
import com.orm.dsl.Ignore;

/**
 * <p>This class is a model of the response entity</p>
 *
 * Author: Trien Do
 * Date: Feb 2015
 */

public class ResponseModel extends SugarRecord {
	private String mid;
	private String experienceId;
	private String userId;
	private String contentType;
	private String content;
	private String description;
	private String entityType;
	private String entityId;//should be Long but need this Id to store LatLng of new POI
	private String status;
	private int size;
	private String submittedDate;
	private String fileId;

	@Ignore
	private Uri fileUri;

	public static final String FOR_POI = "POI";
	public static final String FOR_EOI = "EOI";
	public static final String FOR_ROUTE = "ROUTE";
	public static final String FOR_NEW_POI = "NEW";
	public static final String FOR_MEDIA = "MEDIA";
	public static final String FOR_RESPONSE = "RESPONSES";

	public static final String STATUS_ACCEPTED = "accepted";
	public static final String STATUS_FOR_UPLOAD = "uploading";

	public ResponseModel(){

	}

	public ResponseModel(String mid, String experienceId, String userId, String contentType, String content, String description,
						 String entityType, String entityId, String status, int size, String submittedDate)
	{
		this.mid = mid;
		this.experienceId = experienceId;
		this.userId = userId;
		this.contentType = contentType;//(Text/Image/Audio/Video)
		this.content = content; // Content (Path to media)
		this.description = description;
		this.entityType = entityType;
		this.entityId = entityId;
		this.status = status;
		this.size = size;
		this.submittedDate = submittedDate;
	}

	public String getEntityId() {
		return entityId;
	}

	public void setEntityID(String entityId) {
		this.entityId = entityId;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String desc) {
		this.description = desc;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public int getNoOfLike() {
		return 0;
	}

	public int getNoOfComment() {
		return 0;
	}

	public String getEntityType() {
		return entityType;
	}

	public void setEntityType(String entityType) {
		this.entityType = entityType;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getMyId () {
		return mid;
	}

	public void setMyId(String id) {
		this.mid = id;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String type) {
		this.contentType = type;
	}

	public Uri getFileUri() {
		//return fileUri;
		File mediaFile = new File(this.getContent());
		return Uri.fromFile(mediaFile);
	}

	public void setFileUri(Uri fileUri) {
		this.fileUri = fileUri;
	}

	public String getSubmittedDate(){
		return submittedDate;
	}

	public String getUserId(){
		return  userId;
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public void setEntityId(String entityId) {
		this.entityId = entityId;
	}

	public String getResponseId() {
		return mid;
	}

	public void setMid(String mid) {
		this.mid = mid;
	}

	public String getExperienceId() {
		return experienceId;
	}

	public void setExperienceId(String experienceId) {
		this.experienceId = experienceId;
	}

	public void setSubmittedDate(String submittedDate) {
		this.submittedDate = submittedDate;
	}

	public String getFileId() {
		return fileId;
	}

	public void setFileId(String fileId) {
		this.fileId = fileId;
	}

	public String getHTMLCodeForResponse(boolean isLocal)//Two types of responses: local added by the current user, online submitted by other users
	{
		String responseHeader = "<div style='background-color:#AAEEFF;'><p style='margin-left:30px;font-weight:bold;'> You performed this action ";
		if(isLocal)
			responseHeader += "you on " + this.submittedDate + "</p>";
		else
			responseHeader += this.userId + " on " + this.submittedDate + "</p>";
		return responseHeader + SharcLibrary.getHTMLCodeForMedia(this.getId().toString(),"Responses", this.getNoOfLike(), this.getNoOfComment(), this.getContentType(),
				this.getContent(), this.getDescription(), isLocal) + "</div>";
	}
}
