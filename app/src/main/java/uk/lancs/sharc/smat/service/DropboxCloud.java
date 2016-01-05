package uk.lancs.sharc.smat.service;

import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;

import com.dropbox.sync.android.DbxAccountInfo;
import com.dropbox.sync.android.DbxAccountManager;
import com.dropbox.sync.android.DbxFile;
import com.dropbox.sync.android.DbxFileSystem;
import com.dropbox.sync.android.DbxPath;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import uk.lancs.sharc.smat.controller.MainActivity;
import uk.lancs.sharc.smat.model.MediaModel;

/**
 * Created by SHARC on 11/12/2015.
 */
public class DropboxCloud extends CloudManager{
    private static final Long MAX_FILE_SIZE = Long.valueOf(150);//in MB
    public static final int REQUEST_LINK_TO_DROPBOX = 0;
    private static final String APP_KEY = "ggludz9cg3xq1lq";        //app key genereated from Dropbox App
    private static final String APP_SECRET = "9zeykvpdfuwlzo7";     //app secret genereated from Dropbox App
    private DbxAccountManager mDbxAcctMgr;
    DbxAccountInfo dbUser = null;

    public DropboxCloud(Activity activity) {
        super(activity);
        setCloudType(CloudManager.TYPE_DROPBOX);
    }

    @Override
    public void login(int actionCode) {
        mDbxAcctMgr.startLink(activity, actionCode);
    }

    @Override
    public boolean isLoginRemembered() {
        mDbxAcctMgr = DbxAccountManager.getInstance(activity.getApplicationContext(), APP_KEY, APP_SECRET);
        if(mDbxAcctMgr.hasLinkedAccount())
        {
            dbUser = mDbxAcctMgr.getLinkedAccount().getAccountInfo();
            return true;
        }
        return false;
    }

    public void getUserDetail(){
        new GetUserDetailsService().execute();
    }

    @Override
    public void logout() {
        if(mDbxAcctMgr.hasLinkedAccount())
        {
            mDbxAcctMgr.getLinkedAccount().unlink();
        }
    }

    @Override
    public boolean isCloudServiceReady() {
        return true;
    }

    @Override
    public void setDefaultUser(String user) {

    }

    @Override
    public boolean isLoggedin() {
        return mDbxAcctMgr.hasLinkedAccount();
    }

    @Override
    public Long getMaxFileSize() {
        return MAX_FILE_SIZE;
    }

    /**
     * <p>This class is another background thread which get user details when logging into SMEP</p>
     **/
    class GetUserDetailsService extends AsyncTask<String, String, String>
    {
        //Before starting background thread Show Progress Dialog
        private ProgressDialog pDialog;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(activity);
            pDialog.setMessage("Getting user information. Please wait...");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(false);
            pDialog.show();
        }

        protected String doInBackground(String... args)
        {
            try
            {
                while (dbUser == null)
                {
                    if(mDbxAcctMgr.hasLinkedAccount())
                    {
                        dbUser = mDbxAcctMgr.getLinkedAccount().getAccountInfo();
                    }
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
                   if (dbUser != null) {
                       //Log.d(TAG, dbUser.toString());
                       try {
                           JSONObject jsonUser = new JSONObject(dbUser.toString());
                           JSONObject jsonUserInfo = new JSONObject(jsonUser.getString("rawJson"));
                           setUserEmail(jsonUserInfo.getString("email"));
                           setUserName(dbUser.displayName);
                           setCloudAccountId(mDbxAcctMgr.getLinkedAccount().getUserId());
                       } catch (JSONException e) {
                           e.printStackTrace();
                       }
                       //smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.SELECT_LOGIN, mDbxAcctMgr.getLinkedAccount().getUserId());
                   }
                   ((MainActivity) activity).displayUserDetail();
               }
           });
        }
    }

    @Override
    public String[] uploadAndShareFile(String fName, Uri fileUri, String textContent, String mediaType) throws Exception {
        //Dropbox file
        DbxFile mFile = null;
        FileOutputStream out = null;
        FileInputStream in = null;
        try {

            DbxPath path = new DbxPath("/" + fName);
            DbxFileSystem dbxFs = DbxFileSystem.forAccount(mDbxAcctMgr.getLinkedAccount());

            if(dbxFs.isFile(path))
                mFile = dbxFs.open(path);
            else
                mFile = dbxFs.create(path);
            //mFile = dbxFs.create(path);//normally try to open before creating //mFile = dbxFs.open(path);
            String fileSize = "0";
            if(mediaType.equalsIgnoreCase(MediaModel.TYPE_IMAGE))
            {
                //Bitmap bmp = BitmapFactory.decodeFile(fileUri.getLatLngPath());
                Bitmap bmp = MediaStore.Images.Media.getBitmap(activity.getContentResolver(), fileUri);
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                bmp.compress(Bitmap.CompressFormat.JPEG, 70, bos);
                FileOutputStream fos = new FileOutputStream (new File(SharcLibrary.SHARC_MEDIA_FOLDER + "/sharc.tmp"));
                bos.writeTo(fos);
                fos.flush();
                fos.close();
                fileSize = String.valueOf(bos.size());
                in = new FileInputStream(SharcLibrary.SHARC_MEDIA_FOLDER + "/sharc.tmp");
            }
            else if(mediaType.equalsIgnoreCase(MediaModel.TYPE_TEXT)){
                fileSize = String.valueOf(textContent.length());
            }
            else {
                in = (FileInputStream) activity.getContentResolver().openInputStream(fileUri);
                fileSize = String.valueOf(in.available());
            }

            if(mediaType.equalsIgnoreCase(MediaModel.TYPE_TEXT))
                mFile.writeString(textContent);
            else{
                out = mFile.getWriteStream();
                SharcLibrary.copyFile(in, out);
                out.close();
                in.close();
            }
            mFile.close();
            //Share and get public link
            String publicURL = dbxFs.fetchShareLink(path, false).toString();
            publicURL = publicURL.replace("https://www.drop","https://dl.drop");
            publicURL = publicURL.substring(0,publicURL.indexOf("?"));
            return new String[]{fileSize, publicURL};
        }
        catch (Exception e) {
            e.printStackTrace();
            try {
                if (out != null && in != null) {
                    out.close();
                    in.close();
                    mFile.close();
                }
                throw e;
            }
            catch (Exception ex)
            {
                throw ex;
            }
        }
    }
}