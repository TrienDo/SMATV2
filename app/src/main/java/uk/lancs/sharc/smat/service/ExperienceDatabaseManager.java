package uk.lancs.sharc.smat.service;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import uk.lancs.sharc.smat.model.EOIModel;
import uk.lancs.sharc.smat.model.ExperienceMetaDataModel;
import uk.lancs.sharc.smat.model.MediaModel;
import uk.lancs.sharc.smat.model.POIModel;
import uk.lancs.sharc.smat.model.ResponseModel;
import uk.lancs.sharc.smat.model.RouteModel;


public class ExperienceDatabaseManager
{
	private String experienceId;
	public ExperienceDatabaseManager() {
        experienceId = "-1";
    }

    public void setSelectedExperience(String experienceId){
        this.experienceId = experienceId;
    }

    //select all experiences to present them as markers on Google Maps
    public List<ExperienceMetaDataModel> getExperiences()
    {
        return ExperienceMetaDataModel.listAll(ExperienceMetaDataModel.class);
    }

    public void parseJsonAndSaveToDB(JSONObject jsonExperience) //parse content of an experience from JSON file and download media files
    {
        //parse entities such as POIs, EOIs
        try
        {
            //Gets all links to download media files
            JSONArray jsonEntityList;

            jsonEntityList = jsonExperience.getJSONArray("allPois");
            for(int i = 0; i < jsonEntityList.length(); i++){
                JSONObject jsonEntity = jsonEntityList.getJSONObject(i);
                JSONObject jsonEntityDesigner = jsonEntity.getJSONObject("poiDesigner");
                this.insertPOI(jsonEntity.getString("id"),jsonEntityDesigner.getString("name"), jsonEntity.getString("description"), jsonEntityDesigner.getString("coordinate"),
                        jsonEntityDesigner.getString("triggerZone"),jsonEntityDesigner.getString("designerId"), jsonEntity.getString("experienceId"), jsonEntity.getString("typeList"),
                        jsonEntity.getString("eoiList"), jsonEntity.getString("routeList"), jsonEntity.getString("thumbnail"), jsonEntity.getInt("mediaCount"), jsonEntity.getInt("responseCount"));
            }

            jsonEntityList = jsonExperience.getJSONArray("allEois");
            for(int i = 0; i < jsonEntityList.length(); i++){
                JSONObject jsonEntity = jsonEntityList.getJSONObject(i);
                JSONObject jsonEntityDesigner = jsonEntity.getJSONObject("eoiDesigner");
                this.insertEOI(jsonEntity.getString("id"), jsonEntityDesigner.getString("designerId"), jsonEntity.getString("experienceId"), jsonEntityDesigner.getString("name"),
                        jsonEntityDesigner.getString("description"), jsonEntity.getString("poiList"), jsonEntity.getString("routeList"));
            }

            jsonEntityList = jsonExperience.getJSONArray("allRoutes");
            for(int i = 0; i < jsonEntityList.length(); i++){
                JSONObject jsonEntity = jsonEntityList.getJSONObject(i);
                JSONObject jsonEntityDesigner = jsonEntity.getJSONObject("routeDesigner");
                this.insertROUTE(jsonEntity.getString("id"), jsonEntityDesigner.getString("designerId"), jsonEntity.getString("experienceId"), jsonEntityDesigner.getString("name"),
                        jsonEntity.getString("description"), jsonEntityDesigner.getInt("directed") == 1 ? true : false, jsonEntityDesigner.getString("colour"), jsonEntityDesigner.getString("path"),
                        jsonEntity.getString("poiList"), jsonEntity.getString("eoiList"));
            }

            jsonEntityList = jsonExperience.getJSONArray("allMedia");
            for(int i = 0; i < jsonEntityList.length(); i++){
                JSONObject jsonEntity = jsonEntityList.getJSONObject(i);
                JSONObject jsonEntityDesigner = jsonEntity.getJSONObject("mediaDesigner");

                this.insertMEDIA(jsonEntity.getString("id"), jsonEntityDesigner.getString("designerId"), jsonEntity.getString("experienceId"), jsonEntityDesigner.getString("contentType"),
                        jsonEntityDesigner.getString("content"), jsonEntity.getString("context"), jsonEntityDesigner.getString("name"),
                        jsonEntity.getString("caption"), jsonEntity.getString("entityType"), jsonEntity.getString("entityId"),
                        jsonEntityDesigner.getInt("size"), jsonEntity.getInt("mainMedia") == 1 ? true: false, jsonEntity.getInt("visible") == 1 ? true : false, jsonEntity.getInt("order"), jsonEntity.getInt("commentCount"));
                //Download media
                donwloadMediaFile(jsonEntityDesigner.getString("content"));
            }

            jsonEntityList = jsonExperience.getJSONArray("allResponses");
            for(int i = 0; i < jsonEntityList.length(); i++){
                JSONObject jsonEntity = jsonEntityList.getJSONObject(i);

                this.insertResponse(jsonEntity.getString("id"), jsonEntity.getString("experienceId"), jsonEntity.getString("userId"), jsonEntity.getString("contentType"),
                        jsonEntity.getString("content"), jsonEntity.getString("description"), jsonEntity.getString("entityType"), jsonEntity.getString("entityId"),
                        jsonEntity.getString("status"), jsonEntity.getInt("size"), jsonEntity.getString("submittedDate"));
                //Download media
                donwloadMediaFile(jsonEntity.getString("content"));
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

    }

    public void donwloadMediaFile(String onlinePath){
        String mName = onlinePath.substring(onlinePath.lastIndexOf("/"));
        String localPath = SharcLibrary.SHARC_MEDIA_FOLDER + mName;

        HttpURLConnection inConection = null;
        File localFile = new File(localPath);
        if(!localFile.exists()) {
            try {
                URL inUrl = new URL(onlinePath);
                inConection = (HttpURLConnection) inUrl.openConnection();
                inConection.connect();
                // download the file
                InputStream mInput = new BufferedInputStream(inUrl.openStream(), 8192);
                OutputStream mOutput = new FileOutputStream(localPath);
                System.out.println(" .Downloading:" + onlinePath);
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
                if(inConection != null)
                    inConection.disconnect();
            } finally {
                if(inConection != null)
                    inConection.disconnect();
            }
        }
    }
    public void getMediaStat(ExperienceMetaDataModel metaData)
    {
        //Cursor statRet = this.getDataSQL("select type, count(*) as total from MEDIA group by type", null);
        //Can optimise later
        List<MediaModel> mediaList = MediaModel.find(MediaModel.class, "experience_Id = ? and content_Type = ?", this.experienceId, MediaModel.TYPE_TEXT);
        metaData.setTextCount(mediaList.size());
        mediaList = MediaModel.find(MediaModel.class, "experience_Id = ? and content_Type = ?", this.experienceId, MediaModel.TYPE_IMAGE);
        metaData.setImageCount(mediaList.size());
        mediaList = MediaModel.find(MediaModel.class, "experience_Id = ? and content_Type = ?", this.experienceId, MediaModel.TYPE_AUDIO);
        metaData.setAudioCount(mediaList.size());
        mediaList = MediaModel.find(MediaModel.class, "experience_Id = ? and content_Type = ?", this.experienceId, MediaModel.TYPE_VIDEO);
        metaData.setVideoCount(mediaList.size());
    }

    public List<MediaModel> getMediaForEntity(String entityId, String entityType)
    {
        return MediaModel.find(MediaModel.class, "experience_Id = ? and entity_Id = ? and entity_Type = ?", new String[]{this.experienceId, entityId, entityType}, "", "media_Order", "");
    }

    public MediaModel getMediaFromId(String id)
    {
        List<MediaModel> media = MediaModel.find(MediaModel.class, "experience_Id = ? and mid = ? ", this.experienceId, id);
        if(media.size() > 0)
            return media.get(0);
        else
            return  null;
    }

	public List<ResponseModel> getCommentsForEntity(String entityId)
	{
		return ResponseModel.find(ResponseModel.class, "experience_Id = ? and entity_Id = ? and entity_Type = 'media' and status = ?",
                this.experienceId, entityId, ResponseModel.STATUS_ACCEPTED);
	}

    public List<ResponseModel> getResponsesForEntity(String entityId, String entityType)
    {
        return ResponseModel.find(ResponseModel.class, "experience_Id = ? and entity_Id = ? and entity_Type = ? and status = ?",
                this.experienceId, entityId, entityType, ResponseModel.STATUS_ACCEPTED);
    }

    public List<ResponseModel> getMyResponses()
    {
        return ResponseModel.find(ResponseModel.class, "experience_Id = ? and status = ?",
                this.experienceId, ResponseModel.STATUS_FOR_UPLOAD);
    }

    public List<POIModel> getAllPOIs()
    {
        return POIModel.find(POIModel.class, "experience_Id = ?", this.experienceId);
    }

    public List<EOIModel> getAllEOIs()
    {
        return EOIModel.find(EOIModel.class, "experience_Id = ?", this.experienceId);
    }

    public List<RouteModel> getAllRoutes()
    {
        return RouteModel.find(RouteModel.class, "experience_Id = ?", this.experienceId);
    }

	public List<ResponseModel> getResponsesForTab(String tabName)//EOI for Event tab and ROUTE for Summary tab
	{
		List<ResponseModel> responseList = ResponseModel.find(ResponseModel.class, "experience_Id = ? and entity_Type = ? and status = ?",
            this.experienceId, tabName, ResponseModel.STATUS_ACCEPTED);
		for (int i = 0; i < responseList.size(); i++) {
			List<ResponseModel> comments = this.getCommentsForEntity(responseList.get(i).getId().toString());
            //responseList.get(i).setNoOfComment(String.valueOf(comments.size()));
		}
		return responseList;
	}

	/*
	public String getRepresentativePhoto(String id)
    {
        List<MediaModel> mainMedia =  MediaModel.find(MediaModel.class, "experienceID = ? and entityId = ? and entityType = ? and mainMedia = 1",
                this.experienceId, id, "POI");
        if(mainMedia != null && mainMedia.size() > 0)
            return mainMedia.get(0).getContent();
        else
            return "";
    }
    */


	public String[] getEOIFromID(String eoiId)
    {
        List<EOIModel> objEoi =  EOIModel.find(EOIModel.class, "experience_Id = ? and mid = ?", this.experienceId, eoiId);
        if(objEoi != null && objEoi.size() > 0){
            return new String[]{objEoi.get(0).getName(), objEoi.get(0).getDescription()};
    	}
    	else
    		return null;
    }


	//insert
	public void insertPOI(String id, String name, String description, String coordinate, String triggerZone, String designerId, String experienceId, String typeList, String eoiList, String routeList, String thumbnailPath, int mediaCount, int responseCount)
	{
		POIModel objPOI = new POIModel(id, name, description, coordinate, triggerZone, designerId, experienceId, typeList, eoiList, routeList, thumbnailPath, mediaCount, responseCount);
		objPOI.save();
	}
	
	public void insertEOI(String id, String designerId, String experienceId, String name, String description, String poiList, String routeList)
	{
		EOIModel objEOI = new EOIModel(id, designerId, experienceId, name, description, poiList, routeList);
        objEOI.save();
	}
	
	public void insertROUTE(String id, String designerId, String experienceId, String name, String description,boolean directed, String colour, String path, String poiList, String eoiList)
	{
        RouteModel objRoute = new RouteModel(id, designerId, experienceId, name, description, directed, colour, path, poiList, eoiList);
        objRoute.save();
	}
	
	public void insertMEDIA(String id, String designerId, String experienceId, String contentType, String content, String context, String name, String caption,
                                      String entityType, String entityID, int size, boolean mainMedia, boolean visible, int order, int commentCount)
	{
        MediaModel objMedia = new MediaModel(id, designerId, experienceId, contentType, content, context, name, caption, entityType, entityID, size, mainMedia, visible, order, commentCount);
        objMedia.save();
	}
	
	public void insertResponse(String mid, String experienceId, String userId, String contentType, String content, String description,
                               String entityType, String entityId, String status, int size, String submittedDate)
	{
        ResponseModel objResponse = new ResponseModel(mid, experienceId, userId, contentType, content, description, entityType, entityId, status, size, submittedDate);
        objResponse.save();
	}

    public void insertResponse(ResponseModel responseModel)
    {
        responseModel.save();
    }

	public void deleteMyResponse(Long resID)
	{
        ResponseModel res = ResponseModel.findById(ResponseModel.class, resID);
        if(res != null){
            //Delete media items if it is a POI
            if(res.getEntityType().equalsIgnoreCase(ResponseModel.FOR_NEW_POI))
            {
                ResponseModel.deleteAll(ResponseModel.class, "experience_Id = ? and entity_Id = ?", experienceId, res.getResponseId());
            }
            res.delete();
        }
	}

    public void removeUploadedResponse(Long resID)
    {
        ResponseModel res = ResponseModel.findById(ResponseModel.class, resID);
        if(res != null)
            res.delete();
    }

	
	public void addOrUpdateExperience(ExperienceMetaDataModel experienceMetaDataModel){
        List<ExperienceMetaDataModel> tmp = ExperienceMetaDataModel.find(ExperienceMetaDataModel.class, "mid = ?", experienceMetaDataModel.getExperienceId());
        if(tmp.size() > 0) {//already there -> delete all data
            this.deleteExperience(experienceMetaDataModel.getExperienceId());
            tmp.get(0).delete();
        }
        experienceMetaDataModel.save();
    }

    public void updateExperienceOnceUploadedToServer(ExperienceMetaDataModel experienceMetaDataModel){
        List<ExperienceMetaDataModel> tmp = ExperienceMetaDataModel.find(ExperienceMetaDataModel.class, "mid = ?", experienceMetaDataModel.getExperienceId());
        if(tmp.size() > 0) {//already there -> delete all data
            tmp.get(0).setPublicURL("");
            tmp.get(0).save();
        }
        //Update designerID for all records?
    }

    public boolean addNewExperience(ExperienceMetaDataModel experienceMetaDataModel){
        List<ExperienceMetaDataModel> tmp = ExperienceMetaDataModel.find(ExperienceMetaDataModel.class, "name like ?", experienceMetaDataModel.getName());
        if(tmp.size() > 0) {//already there -> delete all data
            return false;
        }
        experienceMetaDataModel.save();
        return true;
    }

    public void deleteExperience(String experienceId)
	{
		POIModel.deleteAll(POIModel.class,"experience_Id = ?", experienceId);
        EOIModel.deleteAll(EOIModel.class,"experience_Id = ?", experienceId);
        RouteModel.deleteAll(RouteModel.class, "experience_Id = ?", experienceId);
        MediaModel.deleteAll(MediaModel.class, "experience_Id = ?", experienceId);
        ResponseModel.deleteAll(ResponseModel.class, "experience_Id = ?", experienceId);
        ExperienceMetaDataModel.deleteAll(ExperienceMetaDataModel.class, "mid = ?", experienceId);
	}

    public void updateMediaURL(Long mediaId, String mediaURL){
        MediaModel mediaModel = MediaModel.findById(MediaModel.class,mediaId);
        if(mediaModel != null) {
            mediaModel.setContent(mediaURL);
            mediaModel.save();
        }
    }

    public void savePoi(POIModel poiModel){
        poiModel.save();
    }

    public void saveRoute(RouteModel routeModel){
        routeModel.save();
    }

    public void updateRoute(RouteModel routeModel){
        RouteModel tmpRoute = RouteModel.findById(RouteModel.class, routeModel.getId());
        if(tmpRoute != null){
            tmpRoute.setDescription(routeModel.getDescription());
            tmpRoute.setPath(routeModel.getPathString());
            tmpRoute.setRouteId(routeModel.getRouteId());
            tmpRoute.save();
        }
    }

    public void deleteRoute(Long id){
        RouteModel routeModel = RouteModel.findById(RouteModel.class, id);
        routeModel.delete();
    }

    //return true if this media is the main media
    public boolean saveMediaFromResponse(ResponseModel responseModel){
        //Identify media order, main media or not
        List<MediaModel> tmp = this.getMediaForEntity(responseModel.getEntityId(), responseModel.getEntityType());
        //Check if there is any image before
        boolean hasImage = false;
        int order = 0;
        if(tmp.size() > 0) {
            order = tmp.get(tmp.size() - 1).getOrder() + 1;//max order + 1
            for (int i = 0; i < tmp.size(); i++)
                if(tmp.get(i).getContentType().equalsIgnoreCase(MediaModel.TYPE_IMAGE))
                {
                    hasImage = true;
                    break;
                }
        }
        boolean mainMedia = false;//if this is the first media (order = 0) and this is a photo then make this media main media
        if(!hasImage && responseModel.getContentType().equalsIgnoreCase(MediaModel.TYPE_IMAGE)) {
            mainMedia = true;
            //Update the POI's thumbnail
            List<POIModel> poiModel = POIModel.find(POIModel.class, "mid = ?", responseModel.getEntityId());
            if(poiModel.size() > 0) {
                poiModel.get(0).setThumbnailPath(responseModel.getContent());
                poiModel.get(0).save();
            }
        }
        MediaModel mediaModel = new MediaModel(responseModel.getResponseId(), "",responseModel.getExperienceId(), responseModel.getContentType(),
                responseModel.getContent(),  "", responseModel.getDescription(), responseModel.getDescription(), responseModel.getEntityType(),
                responseModel.getEntityId(),responseModel.getSize(), mainMedia,true,order,0);
        mediaModel.save();
        return mainMedia;
    }
}