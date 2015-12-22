package uk.lancs.sharc.smat.service;

import android.app.Activity;
import android.app.ProgressDialog;
import android.location.Location;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import uk.lancs.sharc.smat.controller.MainActivity;
import uk.lancs.sharc.smat.model.ExperienceMetaDataModel;
import uk.lancs.sharc.smat.model.JSONParser;
import uk.lancs.sharc.smat.model.ResponseModel;
import uk.lancs.sharc.smat.model.SMEPAppVariable;

/**
 * Created by SHARC on 11/12/2015.
 */
public class RestfulManager {
    //RESTful APIs
    public static final String api_path = "http://wraydisplay.lancs.ac.uk/SHARC20/api/v1/";
    //public static final String api_path = "http://148.88.227.222/SHARC20/api/v1/";
    public static final String api_get_all_published_experiences = api_path + "experiences";
    public static final String api_get_experience_snapshot = api_path + "experienceSnapshot/";
    public static final String api_get_mock_location = api_path + "locations/";
    public static final String api_submit_response = api_path + "responses";
    public static final String api_update_consumer_experience = api_path + "consumerExperience";

    public static final String STATUS_SUCCESS = "success";

    private Activity activity;
    private CloudManager cloudManager;
    private Long userId;
    private String apiKey;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
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

    public void downloadExperience(Long exprienceId){
        new ExperienceDetailsThread().execute(exprienceId.toString());
    }

    public void updateUserExperience(Long experienceId){
        new UpdateConsumersExperiencesThread().execute(experienceId.toString());
    }

    public void submitResponse(ResponseModel res){
        new SubmitResponseThread(res).execute();
    }

    public void startMockLocationService(String locationId){
        new MockLocationService().execute(locationId);
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
                // Building Parameters
                JSONParser jParser = new JSONParser();
                List<NameValuePair> params = new ArrayList<NameValuePair>();
                // Getting result in form of a JSON string from a Web RESTful
                JSONObject json = jParser.makeHttpRequest(RestfulManager.api_get_all_published_experiences, "GET", params);

                String ret = json.getString("status");
                if (ret.equalsIgnoreCase(RestfulManager.STATUS_SUCCESS))
                {
                    // Get Array of experiences
                    JSONArray publishedExperiences = json.getJSONArray("data");

                    // Loop through all experiences
                    ExperienceMetaDataModel tmpExperience;
                    String logData = "";
                    for (int i = 0; i < publishedExperiences.length(); i++)
                    {
                        JSONObject objExperience = publishedExperiences.getJSONObject(i);
                        // Storing each json item in variable
                        int id = objExperience.getInt("id");
                        String name = objExperience.getString("name");
                        String description = objExperience.getString("description");
                        if(description.length() > 0 && description.charAt(description.length()-1) != '.')
                            description.concat(".");
                        String createdDate = objExperience.getString("createdDate");
                        String lastPublishedDate = objExperience.getString("lastPublishedDate");
                        int designerId = objExperience.getInt("designerId");
                        boolean isPublished = true;
                        int moderationMode = objExperience.getInt("moderationMode");
                        String latLng = objExperience.getString("latLng");
                        String summary = objExperience.getString("summary");
                        String snapshotPath = objExperience.getString("snapshotPath");
                        String thumbnailPath = objExperience.getString("thumbnailPath");
                        int size = objExperience.getInt("size");
                        String theme = objExperience.getString("theme");
                        tmpExperience = new ExperienceMetaDataModel(id, name, description, createdDate, lastPublishedDate, designerId, isPublished,
                                moderationMode, latLng, summary, snapshotPath, thumbnailPath, size, theme);

                        logData += "#" + tmpExperience.getProName();
                        ((MainActivity) activity).getAllExperienceMetaData().add(tmpExperience);
                    }
                    //smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.VIEW_ONLINE_EXPERIENCES, logData);
                }
                else
                {
                    Toast.makeText(activity, "No experiences found", Toast.LENGTH_LONG).show();
                }
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
            // dismiss the dialog after getting all files
            pDialog.dismiss();
            // updating UI from Background Thread
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    //Updating parsed JSON data into ListView
                    ((MainActivity)activity).displayAllExperienceMetaData(true);
                    ((MainActivity)activity).addOnlineExperienceMarkerListener();
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
		This inner class helps
			- Submit info about which users consume which experiences
		Note this class needs retrieve information from server so it has to run in background
	*/
    class UpdateConsumersExperiencesThread extends AsyncTask<String, String, String>
    {
        //Before starting the background thread -> Show Progress Dialog
        private ProgressDialog pDialog;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(activity);
            pDialog.setMessage("Tracking consumer vs. experiences. Please wait...");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(false);
            pDialog.show();
        }

        //Update designer and experience info
        protected String doInBackground(String... args)
        {
            try
            {
                JSONParser jParser = new JSONParser();
                List<NameValuePair> params = new ArrayList<NameValuePair>();
                //User info
                String experienceId = args[0];
                params.add(new BasicNameValuePair("experienceId", experienceId));
                params.add(new BasicNameValuePair("cloudAccountId", cloudManager.getCloudAccountId()));
                params.add(new BasicNameValuePair("username", cloudManager.getUserName()));
                params.add(new BasicNameValuePair("useremail", cloudManager.getUserEmail()));
                params.add(new BasicNameValuePair("cloudType",cloudManager.getCloudType()));
                //update MySQL data
                JSONObject json = jParser.makeHttpRequest(RestfulManager.api_update_consumer_experience, "POST", params);
                String ret = json.getString("status");
                if (ret.equalsIgnoreCase(RestfulManager.STATUS_SUCCESS)) {
                    JSONObject objUser = json.getJSONObject("data");
                    setUserId(Long.valueOf(objUser.getString("id")));
                    setApiKey(objUser.getString("apiKey"));
                    ((MainActivity)activity).getRestfulManager().setUserId(Long.valueOf(objUser.getString("id")));
                }
                ((MainActivity)activity).getSelectedExperienceDetail().setIsUpdatedConsumerExperience(true);
                //smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.VIEW_ONLINE_EXPERIENCES, logData);
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
            // dismiss the dialog after getting all files
            pDialog.dismiss();
            // updating UI from Background Thread
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    //
                }
            });
        }
    }


    /*
		This inner class helps
			- Submit a response to MySQL database
		Note this class needs retrieve information from server so it has to run in background
	*/
    class SubmitResponseThread extends AsyncTask<String, String, String>
    {
        //Before starting the background thread -> Show Progress Dialog
        //private ProgressDialog pDialog;
        private ResponseModel response;

        public SubmitResponseThread(ResponseModel response){
            this.response = response;
        }
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            /*pDialog = new ProgressDialog(activity);
            pDialog.setMessage("Tracking consumer vs. experiences. Please wait...");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(false);
            pDialog.show();
            */
        }

        //Update designer and experience info
        protected String doInBackground(String... args)
        {
            try
            {
                JSONParser jParser = new JSONParser();
                List<NameValuePair> params = new ArrayList<NameValuePair>();
                //Response info
                params.add(new BasicNameValuePair("apiKey", getApiKey()));
                params.add(new BasicNameValuePair("userId", getUserId().toString()));

                params.add(new BasicNameValuePair("id", response.getMid()));
                params.add(new BasicNameValuePair("experienceId", response.getExperienceId().toString()));

                params.add(new BasicNameValuePair("contentType", response.getContentType()));
                params.add(new BasicNameValuePair("content", response.getContent()));
                params.add(new BasicNameValuePair("description", response.getDescription()));

                params.add(new BasicNameValuePair("entityType", response.getEntityType()));
                params.add(new BasicNameValuePair("entityId", response.getEntityId()));
                params.add(new BasicNameValuePair("status", response.getStatus()));

                params.add(new BasicNameValuePair("size", "" + response.getSize()));
                params.add(new BasicNameValuePair("submittedDate", response.getSubmittedDate()));
                //insert MySQL data
                JSONObject json = jParser.makeHttpRequest(RestfulManager.api_submit_response, "POST", params);
                String ret = json.getString("status");
                if (ret.equalsIgnoreCase(RestfulManager.STATUS_SUCCESS)) {
                    //((MainActivity)activity).getSelectedExperienceDetail().setIsUpdatedConsumerExperience(true);
                    //Delete response here
                }

                //smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.VIEW_ONLINE_EXPERIENCES, logData);
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
            // dismiss the dialog after getting all files
            //pDialog.dismiss();
            // updating UI from Background Thread
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    //
                }
            });
        }
    }
}
