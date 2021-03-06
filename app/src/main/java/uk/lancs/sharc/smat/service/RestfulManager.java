package uk.lancs.sharc.smat.service;

import android.app.Activity;
import android.app.ProgressDialog;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.StrictMode;
import android.widget.Toast;


import com.google.android.gms.maps.model.LatLng;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import uk.lancs.sharc.smat.controller.MainActivity;
import uk.lancs.sharc.smat.model.ExperienceMetaDataModel;
import uk.lancs.sharc.smat.model.JSONParser;
import uk.lancs.sharc.smat.model.MediaModel;
import uk.lancs.sharc.smat.model.POIModel;
import uk.lancs.sharc.smat.model.ResponseModel;
import uk.lancs.sharc.smat.model.RouteModel;

/**
 * Created by SHARC on 11/12/2015.
 */
public class RestfulManager {
    //RESTful APIs
    public static final String api_path = "http://wraydisplay.lancs.ac.uk/SHARC20/api/v1/";
    //public static final String api_path = "http://148.88.227.222/SHARC20/api/v1/";
    public static final String api_log_in =  api_path + "users";
    public static final String api_experiences = api_path + "experiences";
    public static final String api_get_experience_snapshot = api_path + "experienceSnapshot/";
    public static final String api_media = api_path + "media";
    public static final String api_poi = api_path + "pois";
    public static final String api_route = api_path + "routes";
    public static final String api_experience_publish = api_path + "publishExperience/";

    public static final String STATUS_SUCCESS = "success";

    private Activity activity;
    private CloudManager cloudManager;
    private String userId;
    private String apiKey= "";

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public RestfulManager(Activity activity){
        this.activity = activity;
    }

    public CloudManager getCloudManager() {
        return cloudManager;
    }

    public void setCloudManager(CloudManager cloudManager) {
        this.cloudManager = cloudManager;
    }

    public void getPublishedExperience(){
        new GetAllOnlineExperiencesThread().execute();
    }

    public void downloadExperience(String exprienceId){
        new ExperienceDetailsThread().execute(exprienceId);
    }

    public void createExperienceOnServer(){
        new CreateExperienceOnServerThread().execute();
    }

    public void loginServer(){
        new LoginServerThread().execute();
    }

    public boolean uploadPoi(POIModel poi){
        return uploadEntity(poi.toJson(), api_poi);
    }

    public boolean uploadRoute(RouteModel route){
        //new UploadEntityThread(route.toJson(), api_route, "UPLOAD_ROUTE").execute();
        return uploadEntity(route.toJson(), api_route);
    }

    public boolean uploadMediaFromResponse(ResponseModel response, MediaModel media){
        JSONObject mediaExperience = response.toMediaJson();
        try {
            mediaExperience.put("mainMedia", media.isMainMedia());
            mediaExperience.put("visible", media.isVisible());
            mediaExperience.put("order", media.getOrder());
            return uploadEntity(mediaExperience, api_media);
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean uploadEntity(JSONObject jsonEntity, String apiPath){
        try {
            HttpResponse res = makePostRequest(apiPath, jsonEntity);
            JSONObject json = getJsonFromHttpResponse(res);
            String ret = json.getString("status");
            if (ret.equalsIgnoreCase(RestfulManager.STATUS_SUCCESS))
                return true;
            else
                return false;
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    /*
        This inner class helps
            - Get information of all available online experiences
            - Present each experience as a marker on Google Maps
            - Add the Click listener even for each marker
        Note this class needs retrieve information from server so it has to run in background
    */
    class GetAllOnlineExperiencesThread extends AsyncTask<String, String, String>
    {
        //Before starting the background thread -> Show Progress Dialog
        private ProgressDialog pDialog;
        boolean error = false;
        HttpResponse res;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(activity);
            pDialog.setMessage("Loading available experiences. Please wait...");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(false);
            pDialog.show();
        }

        //Get all available experiences
        protected String doInBackground(String... args)
        {
            try
            {
                res = makeGetRequest(RestfulManager.api_experiences.concat("/" + userId));
            }
            catch (Exception e)
            {
                e.printStackTrace();
                error = true;
            }
            return null;
        }

        //After completing background task ->Dismiss the progress dialog
        protected void onPostExecute(String file_url)
        {
            // dismiss the dialog after getting all files
            pDialog.dismiss();
            // updating UI from Background Thread
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    //Updating parsed JSON data into ListView
                    try
                    {
                        JSONObject json = getJsonFromHttpResponse(res);
                        String ret = null;
                        try {
                            ret = json.getString("status");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        if (ret.equalsIgnoreCase(RestfulManager.STATUS_SUCCESS)) {
                            // Getting result in form of a JSON string from a Web RESTful
                            // Get Array of experiences
                            JSONArray publishedExperiences = json.getJSONArray("data");

                            // Loop through all experiences
                            ExperienceMetaDataModel tmpExperience;
                            String logData = "";
                            for (int i = 0; i < publishedExperiences.length(); i++) {
                                JSONObject objExperience = publishedExperiences.getJSONObject(i);
                                // Storing each json item in variable
                                tmpExperience = new ExperienceMetaDataModel(objExperience);
                                logData += "#" + tmpExperience.getName();
                                ((MainActivity) activity).getAllExperienceMetaData().add(tmpExperience);
                            }
                            ((MainActivity) activity).displayAllExperienceMetaData(true);
                            ((MainActivity) activity).addOnlineExperienceMarkerListener();
                            //smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.VIEW_ONLINE_EXPERIENCES, logData);
                        }
                        else {
                                Toast.makeText(activity, "No experiences found", Toast.LENGTH_LONG).show();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    /*
		This inner class helps
			- Download a snapshot of an experience in form of json object
			- Download all media files from Dropbox
			- Present the experience
		Note this class needs retrieve information from server so it has to run in background
	*/
    class ExperienceDetailsThread extends AsyncTask<String, String, String>
    {
        //Before starting background thread -> Show Progress Dialog
        private ProgressDialog pDialog;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(activity);
            pDialog.setMessage("Loading the experience. Please wait...");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(false);
            pDialog.show();
        }

        protected String doInBackground(String... experienceId)
        {
            try{
                // Building Parameters
                JSONParser jParser = new JSONParser();
                List<NameValuePair> params = new ArrayList<NameValuePair>();
                // Getting result in form of a JSON string from a Web RESTful
                JSONObject json = jParser.makeHttpRequest(RestfulManager.api_get_experience_snapshot.concat(experienceId[0]), "GET", params);
                String ret = json.getString("status");
                if (ret.equalsIgnoreCase(RestfulManager.STATUS_SUCCESS)) {
                    ((MainActivity)activity).getSelectedExperienceDetail().getExperienceFromSnapshotOnCloud(json.getJSONObject("data"));
                    System.out.println("Experience json:" + json.getJSONObject("data"));
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            return null;
        }

        //After completing background task -> Dismiss the progress dialog
        protected void onPostExecute(String file_url)
        {
            // dismiss the dialog after getting all files
            pDialog.dismiss();
            // updating UI from Background Thread
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    //Render the experience
                    ((MainActivity) activity).presentExperience();
                }
            });
        }
    }

    /*
		This inner class helps upload an entity (poi/route/media) to MySQL database on server
	*/
    class UploadEntityThread extends AsyncTask<String, String, String>
    {
        //Before starting the background thread -> Show Progress Dialog
        HttpResponse res;
        private JSONObject jsonEntity;
        private String apiPath;
        private String logInfo;

        public UploadEntityThread(JSONObject jsonEntity, String apiPath, String logInfo){
            this.jsonEntity = jsonEntity;
            this.apiPath = apiPath;
            this.logInfo = logInfo;
        }
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }
        protected String doInBackground(String... args)
        {
            try
            {
                res = makePostRequest(apiPath, jsonEntity);
                //smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.VIEW_ONLINE_EXPERIENCES, logInfo);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            return null;
        }
        //After completing background task ->Dismiss the progress dialog
        protected void onPostExecute(String file_url)
        {
            //pDialog.dismiss();
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    try
                    {
                        JSONObject json = getJsonFromHttpResponse(res);
                        String ret = null;
                        try {
                            ret = json.getString("status");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        if (ret.equalsIgnoreCase(RestfulManager.STATUS_SUCCESS)) {
                            //smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.VIEW_ONLINE_EXPERIENCES, logData);
                        }
                        else {
                            Toast.makeText(activity, json.getString("data"), Toast.LENGTH_LONG).show();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    class LoginServerThread extends AsyncTask<String, String, String>
    {
        //Before starting the background thread -> Show Progress Dialog
        boolean error = false;
        HttpResponse res;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        //Update designer and experience info
        protected String doInBackground(String... args)
        {
            try
            {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("id", SharcLibrary.getIdString(cloudManager.getCloudAccountId()));
                jsonObject.put("username", cloudManager.getUserName());
                jsonObject.put("email", cloudManager.getUserEmail());
                jsonObject.put("cloudType", cloudManager.getCloudType());
                jsonObject.put("cloudAccountId", cloudManager.getCloudAccountId());
                jsonObject.put("location", "");
                res = makePostRequest(api_log_in, jsonObject);
            }
            catch (Exception e)
            {
                e.printStackTrace();
                error = true;
            }
            return null;
        }

        //After completing background task ->Dismiss the progress dialog
        protected void onPostExecute(String file_url)
        {
            // dismiss the dialog after getting all files
            // updating UI from Background Thread
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    try {
                        JSONObject json = getJsonFromHttpResponse(res);
                        String ret = json.getString("status");
                        if (ret.equalsIgnoreCase(RestfulManager.STATUS_SUCCESS)) {
                            JSONObject data = json.getJSONObject("data");
                            apiKey = data.getString("apiKey");
                            userId = data.getString("id");
                        }
                        else
                            error = true;
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    if(error)
                        Toast.makeText(activity, "Error when loging in. Please try again", Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    class CreateExperienceOnServerThread extends AsyncTask<String, String, String>
    {
        //Before starting the background thread -> Show Progress Dialog
        private ProgressDialog pDialog;
        private boolean isError = false;
        ExperienceMetaDataModel experienceMetaDataModel;
        HttpResponse res;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            experienceMetaDataModel = ((MainActivity) activity).getSelectedExperienceDetail().getMetaData();
            pDialog = new ProgressDialog(activity);
            pDialog.setMessage("Creating a new experience on server. Please wait...");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(false);
            pDialog.show();
        }

        //Update designer and experience info
        protected String doInBackground(String... args)
        {
            try
            {
                experienceMetaDataModel.setDesignerId(getUserId());
                JSONObject jsonObject = experienceMetaDataModel.toJSON();

                res = makePostRequest(api_experiences, jsonObject);
            }
            catch (Exception e)
            {
                e.printStackTrace();
                isError = true;
            }
            return null;
        }

        //After completing background task ->Dismiss the progress dialog
        protected void onPostExecute(String file_url)
        {
            // dismiss the dialog after getting all files
            pDialog.dismiss();
            // updating UI from Background Thread
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    try {
                        JSONObject json = getJsonFromHttpResponse(res);
                        if(json == null)
                            isError = true;
                        else{
                            String ret = json.getString("status");
                            if (ret.equalsIgnoreCase(RestfulManager.STATUS_SUCCESS)) {
                                ((MainActivity) activity).getSelectedExperienceDetail().getMetaData().setPublicURL("");//Mark that creating OK
                                ((MainActivity) activity).getSelectedExperienceDetail().updateMetaData();
                            }
                            else
                                isError = true;
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    if(isError)
                        Toast.makeText(activity, "Error when creating the experience on server. Please try again", Toast.LENGTH_LONG).show();
                }
            });
        }
    }


    public void publishExperience(String imagePath){
        new UpdatePublishedExperiencesThread().execute(imagePath);
    }
    /*
        This inner class helps update meta data for a published experience
    */
    class UpdatePublishedExperiencesThread extends AsyncTask<String, String, String>
    {
        //Before starting the background thread -> Show Progress Dialog
        private boolean isError = false;
        private ProgressDialog pDialog;
        HttpResponse res;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(activity);
            pDialog.setMessage("Publishing the experience. Please wait...");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(false);
            pDialog.show();
        }

        //Update designer and experience info
        protected String doInBackground(String... args)
        {
            String imagePath = args[0];
            try
            {
                ExperienceMetaDataModel experienceMetaDataModel = ((MainActivity) activity).getSelectedExperienceDetail().getMetaData();
                if(!imagePath.equalsIgnoreCase(""))
                {
                    File mediaFile = new File(imagePath);
                    String[] ret = cloudManager.uploadAndShareFile(experienceMetaDataModel.getExperienceId() + ".jpg", Uri.fromFile(mediaFile), "", MediaModel.TYPE_IMAGE);
                    experienceMetaDataModel.setThumbnailPath(ret[2] + "###" + ret[1]);
                }
                experienceMetaDataModel.setIsPublished(true);
                LatLng proLocation = ((MainActivity) activity).getSelectedExperienceDetail().getGeographicalBoundary().getCenter();
                if(proLocation == null)
                    proLocation = new LatLng(0.0, 0.0);
                experienceMetaDataModel.setLatLng(proLocation.latitude + " " + proLocation.longitude);
                experienceMetaDataModel.setSummary(((MainActivity) activity).getSelectedExperienceDetail().getOverallSummary(cloudManager.getUserName()));

                res = makePostRequest(api_experience_publish.concat(experienceMetaDataModel.getExperienceId()), experienceMetaDataModel.toJSON());
                //smepInteractionLog.addLog(InteractionLog.SELECT_UPLOAD_ALL, "");
            }
            catch (Exception e) {
                e.printStackTrace();
                isError = true;
            }
            return null;
        }

        //After completing background task ->Dismiss the progress dialog
        protected void onPostExecute(String file_url)
        {
            // dismiss the dialog after getting all files
            pDialog.dismiss();
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    try {
                        JSONObject json = getJsonFromHttpResponse(res);
                        if(json == null)
                            isError = true;
                        else{
                            String ret = json.getString("status");
                            if (ret.equalsIgnoreCase(RestfulManager.STATUS_SUCCESS)) {
                                String size = json.getJSONObject("data").getString("size");
                                size = String.format("%.1f", Float.parseFloat(size));
                                Toast.makeText(activity, "Your experience has been successfully published. The media package is " + size + " MB", Toast.LENGTH_LONG).show();
                            }
                            else
                                isError = true;
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    if (isError)
                        Toast.makeText(activity, "There were some error when publishing your experience. Please try again", Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    public HttpResponse makePostRequest(String path, JSONObject params) throws Exception
    {
        HttpPost httPost = new HttpPost(path);
        httPost.addHeader("apiKey", this.getApiKey());
        StringEntity se = new StringEntity(params.toString());
        httPost.setEntity(se);
        httPost.setHeader("Accept", "application/json");
        httPost.setHeader("Content-type", "application/json");
        return new DefaultHttpClient().execute(httPost);
    }

    public HttpResponse makeGetRequest(String path) throws Exception
    {
        HttpGet httGet = new HttpGet(path);
        httGet.addHeader("apiKey", this.getApiKey());
        httGet.setHeader("Accept", "application/json");
        return new DefaultHttpClient().execute(httGet);
    }

    public JSONObject getJsonFromHttpResponse(HttpResponse response){
        JSONObject finalResult = null;//http://stackoverflow.com/questions/2845599/how-do-i-parse-json-from-a-java-httpresponse
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
            StringBuilder builder = new StringBuilder();
            String line = null;
            line = reader.readLine();
            while (line != null){
                builder.append(line);
                line = reader.readLine();
            }
            //String json = reader.readLine();
            //JSONTokener tokener = new JSONTokener(json);
            JSONTokener tokener = new JSONTokener(builder.toString());
            finalResult = new JSONObject(tokener);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return finalResult;
    }


}
