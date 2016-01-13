package uk.lancs.sharc.smat.service;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.AbstractInputStreamContent;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.About;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.ParentReference;
import com.google.api.services.drive.model.Permission;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;

import uk.lancs.sharc.smat.controller.MainActivity;
import uk.lancs.sharc.smat.model.MediaModel;

/**
 * Created by SHARC on 11/12/2015.
 */
public class GoogleDriveCloud extends CloudManager{
    //Google Drive
    private static final Long MAX_FILE_SIZE = Long.valueOf(5120 * 1024);//in MB
    public static final int REQUEST_ACCOUNT_PICKER = 1000;
    public static final int REQUEST_AUTHORIZATION = 1001;
    public static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static final String[] SCOPES = { DriveScopes.DRIVE_FILE };
    private com.google.api.services.drive.Drive mService;
    private About about;
    private String sharcWebViewLink;
    private String sharcFolderId;
    private GoogleAccountCredential mCredential;

    public GoogleDriveCloud(Activity activity) {
        super(activity);
        setCloudType(CloudManager.TYPE_GOOGLE_DRIVE);
        SharedPreferences settings = activity.getPreferences(Context.MODE_PRIVATE);
        mCredential = GoogleAccountCredential.usingOAuth2(
                activity.getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff())
                .setSelectedAccountName(settings.getString(PREF_ACCOUNT_NAME, null));
    }

    @Override
    public String[] uploadAndShareFile(String fName, Uri fileUri, String textContent, String mediaType) throws Exception {
        //File on Google Drive
        File body = new File();
        body.setTitle(fName);
        String mimeType = "application/octet-stream";
        body.setMimeType(mimeType);
        body.setParents(Arrays.asList(new ParentReference().setId(sharcFolderId)));

        String fileSize = "0";
        java.io.File fileContent = null;
        AbstractInputStreamContent mediaContent = null;
        if(mediaType.equalsIgnoreCase(MediaModel.TYPE_IMAGE))
        {
            //Bitmap bmp = BitmapFactory.decodeFile(fileUri.getLatLngPath());
            Bitmap bmp = MediaStore.Images.Media.getBitmap(activity.getContentResolver(), fileUri);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.JPEG, 70, bos);
            FileOutputStream fos = new FileOutputStream(new java.io.File(SharcLibrary.SHARC_MEDIA_FOLDER + "/sharc.tmp"));
            bos.writeTo(fos);
            fos.flush();
            fos.close();
            fileContent = new java.io.File(SharcLibrary.SHARC_MEDIA_FOLDER + "/sharc.tmp");
            mediaContent = new FileContent(mimeType, fileContent);
        }
        else if(mediaType.equalsIgnoreCase(MediaModel.TYPE_TEXT)){
            ByteArrayInputStream bis = null;
            try {
                bis = new ByteArrayInputStream(textContent.getBytes("UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            mediaContent = new InputStreamContent(null, bis);
        }
        else {
            mediaContent =  new InputStreamContent(null,activity.getContentResolver().openInputStream(fileUri));
        }
        try {
            File file = mService.files().insert(body, mediaContent).execute();
            fileSize = file.getFileSize().toString();
            return new String[]{fileSize, sharcWebViewLink.concat(file.getTitle()), file.getId()};
        } catch (IOException e) {
            e.printStackTrace();//System.out.println("An error occured: " + e);
            throw  e;
        }
    }

    @Override
    public void login(int actionCode) {
        activity.startActivityForResult(mCredential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
    }

    @Override
    public boolean isLoginRemembered() {
        if (mCredential.getSelectedAccountName() == null)
            return false;
        else
            return true;
    }

    @Override
    public void getUserDetail() {
        new GetUserDetailsService(mCredential).execute();
    }

    @Override
    public void logout() {
        mCredential.setSelectedAccountName(null);
    }

    @Override
    public boolean isCloudServiceReady() {
        return isGooglePlayServicesAvailable();
    }

    @Override
    public void setDefaultUser(String user) {
        String accountName = user;
        if (accountName != null) {
            mCredential.setSelectedAccountName(accountName);
            SharedPreferences settings =
                    activity.getPreferences(Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = settings.edit();
            editor.putString(PREF_ACCOUNT_NAME, accountName);
            editor.apply();
        }
    }

    @Override
    public boolean isLoggedin() {
        return isLoginRemembered();
    }

    @Override
    public Long getMaxFileSize() {
        return MAX_FILE_SIZE;
    }

    /**
     * Check that Google Play services APK is installed and up to date. Will
     * launch an error dialog for the user to update Google Play Services if
     * possible.
     * @return true if Google Play Services is available and up to
     *     date on this device; false otherwise.
     */
    private boolean isGooglePlayServicesAvailable() {
        final int connectionStatusCode =
                GooglePlayServicesUtil.isGooglePlayServicesAvailable(activity);
        if (GooglePlayServicesUtil.isUserRecoverableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
            return false;
        } else if (connectionStatusCode != ConnectionResult.SUCCESS ) {
            return false;
        }
        return true;
    }

    /**
     * Display an error dialog showing that Google Play Services is missing
     * or out of date.
     * @param connectionStatusCode code describing the presence (or lack of)
     *     Google Play Services on this device.
     */
    private void showGooglePlayServicesAvailabilityErrorDialog(
            final int connectionStatusCode) {
        Dialog dialog = GooglePlayServicesUtil.getErrorDialog(
                connectionStatusCode,
                activity,
                REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }

    /**
     * <p>This class is another background thread which get user details when logging into SMEP</p>
     **/
    class GetUserDetailsService extends AsyncTask<String, String, String>
    {
        //Before starting background thread Show Progress Dialog
        private ProgressDialog pDialog;
        private Exception mLastError = null;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(activity);
            pDialog.setMessage("Getting user information. Please wait...");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(false);
            pDialog.show();
        }

        public GetUserDetailsService(GoogleAccountCredential credential) {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.drive.Drive.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("Drive API Android Quickstart")
                    .build();
        }

        protected String doInBackground(String... args)
        {
            try
            {
                about = mService.about().get().execute();
                //Create folder if neccessary
                FileList sharcFolder = mService.files().list().setQ("mimeType='application/vnd.google-apps.folder' and trashed=false and title='SHARC20' and 'root' in parents").execute();
                List<File> mfiles = sharcFolder.getItems();
                if(mfiles.size() > 0) {//SHARC20 has been created previously
                    sharcFolderId = mfiles.get(0).getId();
                    sharcWebViewLink = mfiles.get(0).getWebViewLink();
                }
                else {//not created yet
                    //Create new folder
                    File body = new File();
                    body.setTitle("SHARC20");
                    body.setMimeType("application/vnd.google-apps.folder");
                    File folder = mService.files().insert(body).execute();
                    //Set public permission
                    Permission newPermission1 = new Permission();
                    newPermission1.setValue("");
                    newPermission1.setType("anyone");
                    newPermission1.setRole("reader");
                    mService.permissions().insert(folder.getId(), newPermission1).execute();
                    //Get public link
                    File f = mService.files().get(folder.getId()).execute();
                    sharcWebViewLink = f.getWebViewLink();
                    sharcFolderId = f.getId();
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            return null;
        }

        //After completing background task Dismiss the progress dialog
        protected void onPostExecute(String file_url)
        {
            // updating UI from Background Thread
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    pDialog.dismiss();
                    if (about != null) {
                        setUserEmail(mCredential.getSelectedAccountName());
                        setUserName(about.getName());
                        setCloudAccountId(about.getPermissionId());
                    }
                    ((MainActivity) activity).displayUserDetail();
                }
            });
        }
    }
}
