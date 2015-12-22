package uk.lancs.sharc.smat.controller;

import uk.lancs.sharc.smat.model.MapWindowAdapter;
import uk.lancs.sharc.smat.model.MediaListAdapter;
import uk.lancs.sharc.smat.service.CloudManager;
import uk.lancs.sharc.smat.service.ErrorReporter;
import uk.lancs.sharc.smat.service.RestfulManager;
import uk.lancs.sharc.smat.service.SharcLibrary;
import uk.lancs.sharc.smat.R;

import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.content.ActivityNotFoundException;
import android.os.Bundle;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import uk.lancs.sharc.smat.model.POIModel;
import uk.lancs.sharc.smat.model.RouteModel;
import uk.lancs.sharc.smat.service.BackgroundService;
import uk.lancs.sharc.smat.service.DBExperienceDetails;
import uk.lancs.sharc.smat.service.DBExperienceMetadata;
import uk.lancs.sharc.smat.model.ExperienceDetailsModel;
import uk.lancs.sharc.smat.model.ExperienceMetaDataModel;
import uk.lancs.sharc.smat.service.InteractionLog;
import uk.lancs.sharc.smat.model.JSONParser;
import uk.lancs.sharc.smat.model.SMEPAppVariable;
import uk.lancs.sharc.smat.model.ResponseListAdapter;
import uk.lancs.sharc.smat.model.SMEPSettings;
import uk.lancs.sharc.smat.model.ResponseModel;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.media.MediaRecorder;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.dropbox.sync.android.DbxAccountInfo;
import com.dropbox.sync.android.DbxAccountManager;
import com.dropbox.sync.android.DbxDatastore;
import com.dropbox.sync.android.DbxDatastoreManager;
import com.dropbox.sync.android.DbxFile;
import com.dropbox.sync.android.DbxFileSystem;
import com.dropbox.sync.android.DbxPath;
import com.dropbox.sync.android.DbxPrincipal;
import com.dropbox.sync.android.DbxRecord;
import com.dropbox.sync.android.DbxTable;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.jeremyfeinstein.slidingmenu.lib.app.SlidingActivity;

/**
 * <p>This class controls the logic of the app and user interaction</p>
 *
 * Author: Trien Do
 * Date: Feb 2014
 **/
public class MainActivity extends SlidingActivity implements OnMapClickListener {

    //constants
    private static final String TAG = "SMAT_MAIN";
    //private static final String url_all_experiences = "http://wraydisplay.lancs.ac.uk/sharcauthoring/php/getPublicProjects.php"; //RESTful apis
    private static final String url_all_experiences = "http://wraydisplay.lancs.ac.uk/sharcauthoring/php/getUserProjects.php"; //RESTful apis
    private static final String url_mockLocation = "http://wraydisplay.lancs.ac.uk/sharcauthoring/php/getMockLocation.php";
    private static final String url_emailDesigner = "http://wraydisplay.lancs.ac.uk/sharcauthoring/php/emailDesigner.php";
    private static final String url_updateDesignerExperience = "http://wraydisplay.lancs.ac.uk/sharcauthoring/php/updateDesignerExperience.php";
    private static final String url_updatePublishedExperience = "http://wraydisplay.lancs.ac.uk/sharcauthoring/php/publicProjectFromSmat.php";
    private static final int LOCATION_INTERVAL = 1;
    private static final float LOCATION_DISTANCE = 3f;
    private static final float FONT_SIZE = 18f;

    //Google map
    private MapFragment mMapFragment;
    private GoogleMap mMap;		            //Google Maps object
    private Marker currentPosition;         //Mock location -> to simulate a fake current location
    private LatLng initialLocation = null;  //Move to where the current location of the user when starting the app
    private LatLng sessionLocation = null;  //A session of responses for a new location (one or more responses can be added at the same location. They should be treated as all media for a new POI
    private Marker currentPos;              //Real position
    private Circle currentAccuracy;         //accuracy
    private ArrayList<Marker> allExperienceMarkers = new ArrayList<Marker>();
    private ArrayList<Polyline> curRoutePath = new ArrayList<Polyline>();
    private Location lastKnownLocation = null;

    private Button btnResponse;
    private Button btnStartRoute;
    private Button btnStopRoute;

    //Setting
    SMEPSettings smepSettings = new SMEPSettings();
    AlertDialog adYAH;                      //Dialog box to select YAH marker --> need to close this dialog from other place -> global
    private int selectedLocationIcon = 0;   //id of the selected YAH icon
    private ProgressDialog pDialog;         //dialog shows waiting icon when downloading data

    //Experience
    private ArrayList<ExperienceMetaDataModel> allExperienceMetaData = new ArrayList<ExperienceMetaDataModel>();//Store all available experiences (either online or cached)

    public ExperienceDetailsModel getSelectedExperienceDetail() {
        return selectedExperienceDetail;
    }

    private ExperienceDetailsModel selectedExperienceDetail;                                                    //Details of the current experience
    DBExperienceMetadata experienceMetaDB = new DBExperienceMetadata(this);                                     //DB stores metadata of all experiences
    private ArrayList<Integer> nearbyExperiences = new ArrayList<Integer>();                                    //Array of IDs of Experiences within 5 km

    private RouteModel curRoute = null; //For recording a route
    private Marker startRoute;
    private Marker endRoute;
    private Location prevLocation = null;
    private boolean isRecording = false;

    //Tab view - Menu
    private Menu actionbarMenu;	        //tab menu
    int currentTab = 0;			        //which tab is current selected
    int currentPOIIndex = -1;			//id of the current POI displayed in the media pane
    ViewGroup.LayoutParams params;      //Control how to show maps - height = 0 or fill parent
    ListView mediaItemsPresentation;    //List views to render media for tabs POI, EOI, Summary
    ListView responseTab;               //List views to render Response tab
    MediaListAdapter medAdapter;
    //WebView webviewMedia;               //DISPLAY MEDIA
    ResponseListAdapter resAdapter;

    //Direction sensor
    private static SensorManager sensorService; //To get heading of the device
    private Sensor sensor;                      //Manage all sensors
    private float mDeclination;                 //heading of the device

    //Location service
    LocationListener[] mLocationListeners = new LocationListener[] {
            new LocationListener(LocationManager.GPS_PROVIDER),
            new LocationListener(LocationManager.NETWORK_PROVIDER)
    };
    String testingCode;

    //Cloud manager
    CloudManager cloudManager;

    //Dropbox
    private static final String APP_KEY = "nz8hhultn33wlqu";        //app key genereated from Dropbox App
    private static final String APP_SECRET = "4kldjr3m3330ytn";     //app secret genereated from Dropbox App
    private static final int REQUEST_LINK_TO_DBX = 0;
    private DbxAccountManager mDbxAcctMgr;
    DbxAccountInfo dbUser = null;

    //Response
    private static final int CROP_IMAGE = 6666;               //when croping image for publishing experience
    private static final int PICK_FROM_FILE = 7777;                 //when selecting image for publishing experience
    private static final int PICK_FROM_CAMERA = 5555;
    //private ImageView mImageView;                                   //For croping image

    private static final int TAKE_PICTURE = 9999;                   //mark if the user is taking a picture
    private static final int CAPTURE_VIDEO = 8888;                  //mark if the user is taking a video
    private Uri fileUri;                                            //file url to store image/video
    private MediaRecorder myAudioRecorder = null;                          //to record audio
    private static String outputFile = null;                        //path to output file of responses (e.g., photo, video, audio)
    private long startTime = 0L;                                    //Timer showing that voice is being recorded
    private Handler customHandler = new Handler();                  //handle the recording dialog
    long timeInMilliseconds = 0L;
    long updatedTime = 0L;
    private TextView timerValue;                                    //Textview to display recording time

    //Logfile
    InteractionLog smepInteractionLog;

    //Restful
    RestfulManager restfulManager;
    //////////////////////////////////////////////////////////////////////////////
    // INIT - ACTIVITY
    //////////////////////////////////////////////////////////////////////////////
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
        checkAndReportCrash();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //getMenuInflater().inflate(R.menu.main, menu);
        //getMenuInflater().inflate(R.menu.action_bar_button, menu);
        setupListView();
        actionbarMenu = menu;
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        //getActionBar().hide();
        actionBar.setDisplayShowHomeEnabled(false);//Hide home button
        //Show/hide title
        //actionBar.setTitle("[" + getString(R.string.app_name) + " - V" + getString(R.string.app_version) + "]");
        actionBar.setTitle("GPS is not available yet. Please wait...");
        //actionBar.setDisplayShowTitleEnabled(true);
        //Replace the up icon of app with menu icon
        ViewGroup home = (ViewGroup) findViewById(android.R.id.home).getParent();
        // get the first child (up imageview)
        ((ImageView)home.getChildAt(0)).setImageResource(R.drawable.ic_drawer);
        actionBar.setHomeButtonEnabled(true);

        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        ActionBar.Tab tab = actionBar.newTab()
                .setText("MAP")
                .setTabListener(new SharcTabListener(0));
                //.setTabListener(new SharcTabListener(R.id.action_tab_map));

        actionBar.addTab(tab);

        tab = actionBar.newTab()
                .setText("SELECTED \n      POI    ")
                .setTabListener(new SharcTabListener(1));
                //.setTabListener(new SharcTabListener(R.id.action_tab_poi_media));
        actionBar.addTab(tab);

        /*tab = actionBar.newTab()
                .setText("SUMMARY")
                .setTabListener(new SharcTabListener(3));
                //.setTabListener(new SharcTabListener(R.id.action_tab_poi_media));
        actionBar.addTab(tab);*/

        tab = actionBar.newTab()
                .setText("  REVIEW  \n& UPLOAD")
                .setTabListener(new SharcTabListener(2));
                //.setTabListener(new SharcTabListener(R.id.action_tab_poi_media));
        actionBar.addTab(tab);
        return true;
    }

    class SharcTabListener implements ActionBar.TabListener {
        private int tabID;

        public SharcTabListener(int id)
        {
            tabID = id;
        }
        @Override
        public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
            processTab(tabID);
        }

        @Override
        public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {

        }

        @Override
        public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {

        }
    }


    @Override
    public void onDestroy()
    {
        Log.d(TAG, "Stopping service");
        stopService(new Intent(this, BackgroundService.class));
        super.onDestroy();
        if (sensor != null) {
            sensorService.unregisterListener(mySensorEventListener);
        }
        smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.EXIT_APP, "exit");
    }

    public void startBackgroundService()
    {
        //Start tracking service
        Log.e(TAG, "Starting service");
        Intent trackingIntent = new Intent(this, BackgroundService.class);
        startService(trackingIntent);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if(keyCode==KeyEvent.KEYCODE_BACK)
        {
            smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.SELECT_BACK_BUTTON, "from tab " + currentTab);
            displayMapTab();
            return true;
        }
        else if(keyCode==KeyEvent.KEYCODE_HOME)
        {
            getSlidingMenu().showMenu(true);
            //smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.SELECT_HOME_BUTTON, "open sliding menu");
            return true;
        }
        else
            return super.onKeyDown(keyCode, event);
    }

    public void createActionListenerForSlideMenu()
    {
        //SMEP version
        TextView txtVersion = (TextView) findViewById(R.id.txtSetting);
        txtVersion.setText("SMAT Settings (Version " + getString(R.string.app_version) + ")");
        smepSettings.setAppVersion(getString(R.string.app_version));

        //Three buttons for login/out
        ImageButton imgBtnUser = (ImageButton) findViewById(R.id.imgBtnUser);
        imgBtnUser.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                loginOrLogout();
            }
        });

        TextView txtUserName = (TextView) findViewById(R.id.txtUsername);
        txtUserName.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                loginOrLogout();
            }
        });

        TextView txtUseremail = (TextView) findViewById(R.id.txtUseremail);
        txtUseremail.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                loginOrLogout();
            }
        });
        //Done login/out

        Button btnShowHelp = (Button) findViewById(R.id.btnShowHelp);
        btnShowHelp.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showHelp();
            }
        });

        Button btnLoadFile = (Button) findViewById(R.id.btnLoadProject);
        btnLoadFile.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                presentDownloadedExperiences();
            }
        });

        Button btnExploreProject = (Button) findViewById(R.id.btnExploreProject);
        btnExploreProject.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                loadOnlineExperiences();
            }
        });

        Button btnCreateProject = (Button) findViewById(R.id.btnCreateProject);
        btnCreateProject.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                createExperience();
            }
        });

        Button btnSelectYAH = (Button) findViewById(R.id.btnSelectYAH);
        btnSelectYAH.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                selectYAHMarker();
            }
        });

        Switch switchShowGPS = (Switch) findViewById(R.id.switchShowGPS);
        switchShowGPS.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                smepSettings.setIsShowingGPS(isChecked);
                if (!isChecked)
                    getActionBar().setTitle("[" + getString(R.string.app_name) + " - V" + getString(R.string.app_version) + "]");
                else
                {
                    if (lastKnownLocation != null)
                        getActionBar().setTitle("GPS Accuracy: " + String.format("%.1f", lastKnownLocation.getAccuracy()) + " (m)");
                    else
                        getActionBar().setTitle("GPS is not available");
                }
                smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.SHOW_GPS_INFO, String.valueOf(isChecked));
            }
        });

        Switch switchPushMedia = (Switch) findViewById(R.id.switchPushMedia);
        switchPushMedia.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                smepSettings.setPushingMedia(isChecked);
                smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.SELECT_PUSH, String.valueOf(isChecked));
            }
        });

        Switch switchPushMediaAgain = (Switch) findViewById(R.id.switchPushMediaAgain);
        switchPushMediaAgain.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                SMEPAppVariable mySMEPAppVariable = (SMEPAppVariable) getApplicationContext();
                mySMEPAppVariable.setIsPushAgain(isChecked);
                smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.SELECT_PUSH_AGAIN, String.valueOf(isChecked));
            }
        });


        Switch switchSoundNotification = (Switch) findViewById(R.id.switchSoundNotification);
        switchSoundNotification.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                smepSettings.setSoundNotification(isChecked);
                SMEPAppVariable mySMEPAppVariable = (SMEPAppVariable) getApplicationContext();
                mySMEPAppVariable.setSoundNotification(isChecked);
                smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.SELECT_SOUND, String.valueOf(isChecked));
            }
        });

        Switch switchVibrationNotification = (Switch) findViewById(R.id.switchVibrationNotification);
        switchVibrationNotification.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                smepSettings.setVibrationNotification(isChecked);
                SMEPAppVariable mySMEPAppVariable = (SMEPAppVariable) getApplicationContext();
                mySMEPAppVariable.setVibrationNotification(isChecked);
                smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.SELECT_VIBRATION, String.valueOf(isChecked));
            }
        });

        Switch switchMapType = (Switch) findViewById(R.id.switchMapType);
        switchMapType.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                smepSettings.setSatellite(isChecked);
                if(isChecked)
                    mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                else
                    mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.SELECT_SETELLITE, String.valueOf(isChecked));
            }
        });

        Switch switchMapRotate = (Switch) findViewById(R.id.switchMapRotate);
        switchMapRotate.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                smepSettings.setRotating(isChecked);
                if(isChecked)
                    sensorService.registerListener(mySensorEventListener, sensor,SensorManager.SENSOR_DELAY_NORMAL);
                else
                    sensorService.unregisterListener(mySensorEventListener);
                smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.SELECT_ROTATION, String.valueOf(isChecked));
            }
        });

        Switch switchMapCentre = (Switch) findViewById(R.id.switchMapCentre);
        switchMapCentre.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                smepSettings.setYAHCentred(isChecked);
                smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.SELECT_YAH_CENTRED, String.valueOf(isChecked));
            }
        });

        Switch switchTestMode = (Switch) findViewById(R.id.switchTestMode);
        switchTestMode.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                final boolean selection = isChecked;
                smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.SELECT_TEST, String.valueOf(isChecked));
                if(isChecked)
                {
                    AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
                    alert.setTitle("Please enter the testing code:");
                    final EditText testCode = new EditText(MainActivity.this);
                    alert.setView(testCode);

                    alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            testingCode = testCode.getText().toString();
                            smepSettings.setTestMode(selection);
                            //Global variable for the whole application
                            SMEPAppVariable mySMEPAppVariable = (SMEPAppVariable) getApplicationContext();
                            currentPosition.setVisible(selection);
                            mySMEPAppVariable.setTestMode(selection);
                        }
                    });

                    alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            // Canceled.
                            Switch switchTest = (Switch) findViewById(R.id.switchTestMode);
                            switchTest.setSelected(false);
                        }
                    });
                    setDialogFontSizeAndShow(alert, FONT_SIZE);
                    //alert.show();
                }
                else
                {
                    smepSettings.setTestMode(selection);
                    //Global variable for the whole application
                    SMEPAppVariable mySMEPAppVariable = (SMEPAppVariable) getApplicationContext();
                    currentPosition.setVisible(selection);
                    mySMEPAppVariable.setTestMode(selection);
                }
            }
        });

        Switch switchTriggerZone = (Switch) findViewById(R.id.switchTriggerZone);
        switchTriggerZone.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                smepSettings.setShowingTriggers(isChecked);
                smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.SELECT_SHOW_TRIGGER_ZONE, String.valueOf(isChecked));
                if(selectedExperienceDetail != null)
                    selectedExperienceDetail.showTriggerZones(isChecked);
                else
                    Toast.makeText(MainActivity.this, getString(R.string.message_no_experience), Toast.LENGTH_LONG).show();
            }
        });

        Switch switchShowPOI = (Switch) findViewById(R.id.switchShowPOI);
        switchShowPOI.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                smepSettings.setShowingThumbnails(isChecked);
                smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.SELECT_SHOW_POI_THUMBS, String.valueOf(isChecked));
                if(selectedExperienceDetail != null)
                    selectedExperienceDetail.showPOIThumbnails(isChecked);
                else
                    Toast.makeText(MainActivity.this, getString(R.string.message_no_experience), Toast.LENGTH_LONG).show();
            }
        });

        Button btnResetPOIs = (Button) findViewById(R.id.btnResetPOIs);
        btnResetPOIs.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                //Global variable for the whole application
                SMEPAppVariable mySMEPAppVariable = (SMEPAppVariable) getApplicationContext();
                mySMEPAppVariable.setResetPOI(true);
                getSlidingMenu().showContent(false);
                smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.SELECT_RESET_POI, "resetPOI");
            }
        });
    }

    public void showHelp()
    {
        Intent help = new Intent(MainActivity.this, Usermanual.class);
        startActivity(help);
    }

    public void loadOnlineExperiences()
    {
        if(SharcLibrary.isNetworkAvailable(this))
        {
            if(dbUser != null) {
                gotoExperiencesBrowsingMapMode();
                new GetAllOnlineExperiencesThread().execute();
            }
            else
                Toast.makeText(this, "You need to log in to download your experiences. Please do so from the sliding menu", Toast.LENGTH_LONG).show();
        }
        else
            Toast.makeText(this, "No internet/data connection detected. Please check your device", Toast.LENGTH_LONG).show();
    }

    public void loginOrLogout()
    {
        if(mDbxAcctMgr.hasLinkedAccount())
        {
            AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
            alert.setTitle("Authentication")
                    .setMessage("Are you sure that you want to log out?")
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            mDbxAcctMgr.getLinkedAccount().unlink();
                            displayDropboxUser(null);
                            smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.SELECT_LOGOUT, "logout");
                        }
                    })
                    .setNegativeButton("No", null);	//Do nothing on no
            setDialogFontSizeAndShow(alert, FONT_SIZE);
                    //.show();
        }
        else
        {
            if(SharcLibrary.isNetworkAvailable(MainActivity.this))
                mDbxAcctMgr.startLink(MainActivity.this, REQUEST_LINK_TO_DBX);
            else
                Toast.makeText(MainActivity.this, getString(R.string.message_wifiConnection), Toast.LENGTH_LONG).show();

        }
    }

    public void presentDownloadedExperiences()
    {
        if(initialLocation == null)
        {
            Toast.makeText(this, R.string.message_gps, Toast.LENGTH_LONG).show();
            return;
        }
        gotoExperiencesBrowsingMapMode();
        getAllExperienceMetaDataFromLocalDatabase();
        addDBExperienceMarkerListener();
    }

    public void gotoExperiencesBrowsingMapMode()
    {
        getSlidingMenu().showContent(false);
        displayMapTab();
        clearMap(false);
        moveAndZoomToLocation(initialLocation, 10);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(selectedExperienceDetail == null && item.getItemId() != android.R.id.home) {
            Toast.makeText(this, getString(R.string.message_no_experience), Toast.LENGTH_LONG).show();
            return true;
        }
        else
            processTab(item.getItemId());
        return true;
    }

    public void processTab(int tabID)
    {
        switch(tabID)
        {
            case android.R.id.home:
                getSlidingMenu().showMenu(true);
                break;
            case 0:
                displayMapTab();
                break;
            case 1:
                switchToPOIMediaTab("FROM_TAB");
                break;
            case 2:
                displayResponseTab();
                break;
        }
    }

    public void init()
    {
        try
        {
            btnResponse = (Button) findViewById(R.id.btnAddResponse);
            btnStartRoute = (Button) findViewById(R.id.btnStartRoute);
            btnStopRoute = (Button) findViewById(R.id.btnStopRoute);

            setBehindContentView(R.layout.sliding_menu); //https://www.youtube.com/watch?v=vmiUh0RQ7QY --> Sliding menu tutorial
            //getSlidingMenu().setBehindWidth(630);
            DisplayMetrics metrics = getResources().getDisplayMetrics();
            final float menu_width = 315.0f;
            getSlidingMenu().setBehindWidth((int) (metrics.density * menu_width));
            createActionListenerForSlideMenu();
            mMapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.myMap);
            mMap = mMapFragment.getMap();

            if (mMap != null) {
                mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);       	//mMap.control
                //mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setZoomControlsEnabled(true); 	//mMap.setPadding(0, 0, 0, 70);
            }
            mMap.setOnMapClickListener(this);
            mMap.setInfoWindowAdapter(new MapWindowAdapter(this));
            //Make sharc folder
            SharcLibrary.createFolder(SharcLibrary.SHARC_FOLDER);
            //Log folder
            SharcLibrary.createFolder(SharcLibrary.SHARC_LOG_FOLDER);
            //Database folder
            SharcLibrary.createFolder(SharcLibrary.SHARC_DATABASE_FOLDER);
            //Make media folder to store media files
            SharcLibrary.createFolder(SharcLibrary.SHARC_MEDIA_FOLDER);

            //Current mock position
            currentPosition = mMap.addMarker(new MarkerOptions()
                            .position(new LatLng(0,0))
                            .icon(BitmapDescriptorFactory.fromResource(R.raw.location))
                            .anchor(0.5f, 0.5f)
            );
            currentPosition.setVisible(false);

            //Current real position
            selectedLocationIcon = R.raw.yahred24;
            createCurrentLocationMarker();

            setUpLocationService();
            //Start sensor
            sensorService = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            sensor = sensorService.getDefaultSensor(Sensor.TYPE_ORIENTATION);
            if (sensor != null)
                sensorService.registerListener(mySensorEventListener, sensor,SensorManager.SENSOR_DELAY_NORMAL);
            params = mMapFragment.getView().getLayoutParams();
            startBackgroundService();
            checkDropboxLogin();
            smepInteractionLog = new InteractionLog(this, mMap);
            smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.START_APP, smepSettings.getAppVersion());
            //showTermsAndConditions();
        }
        catch (Exception e)
        {
            //Log.e(TAG, "Error: " + e.toString());
            e.printStackTrace();
        }
    }

    public void showTermsAndConditions()
    {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        //alert.setCancelable(false);
        alert.setTitle("Terms and conditions");
        alert.setMessage(getString(R.string.terms_and_conditions));
        alert.setPositiveButton("I agree", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                selectYAHMarker();
                showHelp();
                //smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.ADD_RESPONSE_TEXT,  entity[0] + "#" + entity[1]);
            }
        });
        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                finish();
                //smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.ADD_RESPONSE_TEXT,  entity[0] + "#" + entity[1]);
            }
        });
        setDialogFontSizeAndShow(alert, FONT_SIZE);
    }

    public void downloadOrOpen()
    {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        //alert.setCancelable(false);
        alert.setTitle("What would you like to do?");
        alert.setItems(R.array.starting_session, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        createExperience();
                        break;
                    case 1:
                        presentDownloadedExperiences();
                        break;
                    case 2:
                        loadOnlineExperiences();
                        break;
                }

            }
        });
        alert.show();

        /*alert.setTitle("Starting SMAT");
        alert.setCancelable(false);
        LayoutInflater factory = LayoutInflater.from(this);
        alert.setMessage("What would you like to do now?");
        alert.setPositiveButton("Download an experience to edit", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                loadOnlineExperiences();
                //smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.ADD_RESPONSE_TEXT,  entity[0] + "#" + entity[1]);
            }
        });
        alert.setNeutralButton("Open a local experience to edit", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                presentDownloadedExperiences();
                //smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.ADD_RESPONSE_TEXT,  entity[0] + "#" + entity[1]);
            }
        });

        alert.setNegativeButton("Create a new experience", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                createExperience();
                        //smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.ADD_RESPONSE_TEXT,  entity[0] + "#" + entity[1]);
            }
        });

        //alert.setNegativeButton("Cancel", null);
        setDialogFontSizeAndShow(alert, FONT_SIZE);
        */
    }
    public void createCurrentLocationMarker()//SMEP doesn't use a default YAH marker of Google Maps. currentPos is used for this purpose
    {
        currentPos = mMap.addMarker(new MarkerOptions()
                        .position(initialLocation!=null? initialLocation : new LatLng(54.101519,-2.604666))
                        .icon(BitmapDescriptorFactory
                                .fromResource(selectedLocationIcon))
                                //.anchor(0.5f, 0.71f)
                        .anchor(0.5f, 0.5f)
                        .title("YAH")
        );
        currentPos.showInfoWindow();
        currentAccuracy = mMap.addCircle(new CircleOptions().center(currentPos.getPosition()).radius(10f).strokeColor(Color.argb(100, 93, 188, 210)).strokeWidth(1).fillColor(Color.argb(30, 93, 188, 210)));
    }

    public void clearMap(boolean isExperienceMode)
    {
        //Data
        allExperienceMetaData.clear();
        //Viz
        allExperienceMarkers.clear();
        if(selectedExperienceDetail != null)
            selectedExperienceDetail.clearExperience();
        mMap.clear();
        //Add current location again
        if(initialLocation == null)
            initialLocation = new LatLng(54.101519,-2.604666);
        currentPosition = mMap.addMarker(new MarkerOptions()
                        .position(initialLocation)
                        .icon(BitmapDescriptorFactory.fromResource(R.raw.location))
                        .anchor(0.5f, 0.5f)
        );
        currentPosition.setVisible(smepSettings.isTestMode());
        createCurrentLocationMarker();
        showButtonsOnMap(isExperienceMode);
        if(!isExperienceMode)
            selectedExperienceDetail = null;
        currentPOIIndex = -1;
    }

    public void showButtonsOnMap(boolean show)
    {
        if(show)
        {
            btnStartRoute.setVisibility(View.VISIBLE);
            btnStopRoute.setVisibility(View.VISIBLE);
            btnResponse.setVisibility(View.VISIBLE);
        }
        else
        {
            btnStartRoute.setVisibility(View.GONE);
            btnStopRoute.setVisibility(View.GONE);
            btnResponse.setVisibility(View.GONE);
        }
    }

    public void selectYAHMarker()
    {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        adYAH = alert.create();
        adYAH.setTitle("Please select a You-Are-Here (YAH) marker");
        adYAH.setCancelable(false);
        LayoutInflater factory = LayoutInflater.from(this);
        final View textEntryView = factory.inflate(R.layout.select_yah_dialog, null);
        adYAH.setView(textEntryView);
        adYAH.show();
    }

    public void setBlueYAH(View v)
    {
        setYAHMarker(R.raw.yahblue24, "SmallBlue");
    }

    public void setRedYAH(View v)
    {
        setYAHMarker(R.raw.yahred24, "SmallRed");
    }

    public void setBlueYAHBig(View v)
    {
        setYAHMarker(R.raw.yahblue32, "BigBlue");
    }

    public void setRedYAHBig(View v)
    {
        setYAHMarker(R.raw.yahred32, "BigRed");
    }

    public void setYAHMarker(int iconID, String iconText)
    {
        adYAH.dismiss();
        selectedLocationIcon = iconID;
        currentPos.setIcon(BitmapDescriptorFactory.fromResource(selectedLocationIcon));
        smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.SELECT_YAH, iconText);
        if(selectedExperienceDetail == null) {
            downloadOrOpen();
            getSlidingMenu().showMenu(true);
        }
        else
            getSlidingMenu().toggle();
    }

    //////////////////////////////////////////////////////////////////////////////
    // TABS
    //////////////////////////////////////////////////////////////////////////////
    public void setSelectedTabIcons(int index)
    {
        //for(int i = 0; i < actionbarMenu.size(); i++)
        //    actionbarMenu.getItem(i).setIcon(R.drawable.tab_default);
        //actionbarMenu.getItem(index).setIcon(R.drawable.tab_selected);
        currentTab = index;
        if(index == 2)//Change icon
            btnResponse.setCompoundDrawablesWithIntrinsicBounds(R.drawable.upload, 0, 0, 0);
        else
            btnResponse.setCompoundDrawablesWithIntrinsicBounds(R.drawable.addnew, 0, 0, 0);
        switch (index)
        {
            case 0:
                btnResponse.setText("Add a new POI for your current location");
                break;
            case 1:
                btnResponse.setText("Add new media items for this POI");
                break;
            case 2:
                btnResponse.setText("Upload all authoring actions");
                break;
        }
        getActionBar().setSelectedNavigationItem(index);
    }

    public void displayMapTab()
    {
        setSelectedTabIcons(0);
        //webviewMedia.setVisibility(View.GONE);
        mediaItemsPresentation.setVisibility(View.GONE);
        showMap(true);
        try {
            smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.SELECT_MAP_TAB, mMap.getCameraPosition().target.latitude + " " + mMap.getCameraPosition().target.longitude + "#" + mMap.getCameraPosition().zoom);
        }
        catch (Exception ex){ex.printStackTrace();}
    }

    public void switchToPOIMediaTab(String type)//type = 0: selected by user from tab, 1: selected by user from POI marker, 2: pushed by location service , 3: refresh after adding a media item
    {
        setSelectedTabIcons(1);
        showMap(false);
        mediaItemsPresentation.setVisibility(View.VISIBLE);
        //webviewMedia.setVisibility(View.VISIBLE);
        responseTab.setVisibility(View.GONE);
        if(selectedExperienceDetail == null)
            return;
        if(currentPOIIndex >=0)
        {
            displayMediaItems(selectedExperienceDetail.getPOIHtmlListItems(currentPOIIndex, initialLocation), 0);
            btnResponse.setVisibility(View.VISIBLE);
            smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.SELECT_POI_TAB, type + "#" + selectedExperienceDetail.getPOIName(currentPOIIndex));
        }
        else
        {
            displayMediaItems(new ArrayList<String>(){{add("No media has been pushed/pulled yet");}}, 0);
            smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.SELECT_POI_TAB, type + "#" + "No POI selected");
            btnResponse.setVisibility(View.GONE);
        }
    }

    public void displayMediaTab(int poiID,String type)
    {
        currentPOIIndex = poiID;
        ArrayList<String> mediaList = selectedExperienceDetail.getPOIHtmlListItems(poiID, initialLocation);
        displayMediaItems(mediaList, 0);

        if(smepSettings.isPushingMedia())   //Push -> Go to the POI Media tab
        {
            //switchToPOIMediaTab(type);
        }
        else                                //Pull -> Show notification icon for POI Media tab
        {
            //actionbarMenu.getItem(1).setIcon(R.drawable.tab_new);
            switchToPOIMediaTab(type);
        }
    }


    public void displayEOIMediaTab()
    {
        setSelectedTabIcons(2);
        mediaItemsPresentation.setVisibility(View.VISIBLE);
        //webviewMedia.setVisibility(View.VISIBLE);
        responseTab.setVisibility(View.GONE);
        showMap(false);

        if(selectedExperienceDetail!=null)
        {
            displayMediaItems(selectedExperienceDetail.getAllEOIMediaListItems(), 1);
            smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.SELECT_EOI_TAB, "All EOIs info");
        }
        else
        {
            displayMediaItems(new ArrayList<String>(){{add(getString(R.string.message_no_experience));}}, 1);
            smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.SELECT_EOI_TAB, "No EOIs info");
        }

    }

    //Show info of an EOI when the user clicks on a button in the Point of Interest's media tab
    public void showSelectedEOI(final String EoiID)
    {
        //Work around error "Calling View methods on another thread than the UI thread"
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
                String[] eoiContent = selectedExperienceDetail.getHTMLCodeForEOI(EoiID);
                if (eoiContent != null) {
                    alert.setTitle("Event: " + eoiContent[0]);
                    WebView wv = new WebView(MainActivity.this);
                    SharcLibrary.setupWebView(wv, MainActivity.this);
                    String base = "file://" + SharcLibrary.SHARC_MEDIA_FOLDER + "/";
                    wv.loadDataWithBaseURL(base, eoiContent[1], "text/html", "utf-8", null);
                    alert.setView(wv);
                    //alert.setCancelable(false);
                    alert.setNegativeButton("Close", null);    //Do nothing
                } else
                    alert.setMessage("No information found for this Event of Interest");
                setDialogFontSizeAndShow(alert, FONT_SIZE);
                //alert.show();
                //AlertDialog alertDialog = alert.create();
                //alertDialog.getWindow().setLayout(600, 400); //Controlling width and height.
                //alertDialog.show();
                //smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.SELECT_VIEW_RESPONSE, index + "#" + selectedExperienceDetail.getMyResponseName(index));
            }
        });
    }


    public void displayResponseTab()
    {
        setSelectedTabIcons(2);
        //webviewMedia.setVisibility(View.GONE);
        mediaItemsPresentation.setVisibility(View.GONE);
        responseTab.setVisibility(View.VISIBLE);
        showMap(false);
        ArrayList<String> responseList = new ArrayList<String>();
        if(selectedExperienceDetail!=null) {
            //Briefing
            String intro = "<div>Here you can review and upload your authoring actions for the experience: '" + selectedExperienceDetail.getMetaData().getProName() + "'. You may need to scroll the screen down.</div>";
            //Get summary info
            String summaryInfo = selectedExperienceDetail.getExperienceSumaryInfo();
            //Get
            responseList.addAll(selectedExperienceDetail.getMyResponsesList());
            smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.SELECT_RESPONSE_TAB, TextUtils.join("#", responseList));
            if (SharcLibrary.isNetworkAvailable(this) && dbUser != null) {
                //Create a new experience on Dropbox
                createNewExperienceOnDropbox();
            }
            if (SharcLibrary.isNetworkAvailable(this) && dbUser == null)
                Toast.makeText(this, getString(R.string.message_dropboxConnection), Toast.LENGTH_LONG).show();
            if (!SharcLibrary.isNetworkAvailable(this) && dbUser != null)
                Toast.makeText(this, getString(R.string.message_wifiConnection), Toast.LENGTH_LONG).show();
            //String htmlCode = "You have " + responseList.size() + (responseList.size() > 1 ? " responses" : " response" + " to upload.") ;
            String htmlCode = "<div><b>";
            if (responseList.size() > 0) {
                htmlCode += "Tap the yellow icon to view, blue icon to upload, and red icon to delete an action. You can also tap the 'Upload all authoring actions' button at bottom of the screen to upload all of your authoring actions. Remember that you can edit this experience later using the SLAT editing tool (Web app).";
                btnResponse.setVisibility(View.VISIBLE);
            } else {
                htmlCode += "Currently, there are no authoring actions to review.";
                btnResponse.setVisibility(View.GONE);
            }
            htmlCode += "</b></div>";
            responseList.add(0,intro + summaryInfo + htmlCode);
        }
        else {
            responseList.add(0, getString(R.string.message_no_experience));
            btnResponse.setVisibility(View.GONE);
            smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.SELECT_RESPONSE_TAB, "No experience selected");
        }
        try {
            resAdapter = new ResponseListAdapter(MainActivity.this, responseList);
            ListView mLv = (ListView) findViewById(R.id.responseTab);
            mLv.setAdapter(resAdapter);
        }
        catch(Exception ex)
        {
            Toast.makeText(this, getString(R.string.message_rendering_error), Toast.LENGTH_LONG).show();
            processTab(0);
        }
        //showPublishExperienceDialog();
    }

    public void showMap(boolean isFull)
    {
        ImageButton goCurLocation = (ImageButton) findViewById(R.id.btnCurrentLocation);
        if(isFull)
        {
            if(params != null)
                params.height = params.MATCH_PARENT;
            goCurLocation.setVisibility(View.VISIBLE);
            //params.width = params.MATCH_PARENT;
            if(selectedExperienceDetail == null)
            {
                showButtonsOnMap(false);
            }
            else
            {
                showButtonsOnMap(true);
            }
        }
        else
        {
            if(params != null)
                params.height = 0;
            goCurLocation.setVisibility(View.GONE);
            showButtonsOnMap(false);
        }
        if(params != null)
            mMapFragment.getView().setLayoutParams(params);
    }

    public void displayMediaItems(List<String> mediaList, int type)
    {
        try {
            /*StringBuilder content = new StringBuilder(mediaList.get(0));
            for (int i = 1; i < mediaList.size(); i++)
                content.append("<hr/><h5 style='margin-left:20px;'>[Media item " + i + " of " + (mediaList.size()-1) + "]</h5>" + mediaList.get(i));
            content.append("<blockquote><button style='width:95%;height:50px;font-size:20px;' onclick='Android.goToMapView()' >Go to Map View</button></blockquote>");
            renderMediaContent(content.toString());
            */
            medAdapter = new MediaListAdapter(MainActivity.this, mediaList, type);
            ListView mLv = (ListView) findViewById(R.id.webViewTab);
            mLv.setAdapter(medAdapter);
        }
        catch (Exception e)
        {
            Toast.makeText(this, getString(R.string.message_rendering_error), Toast.LENGTH_LONG).show();
            processTab(0);
        }
    }

    public void setupListView()
    {
       /* webviewMedia = (WebView)findViewById(R.id.webViewTab);
        AndroidWebViewInterface inObj = new AndroidWebViewInterface(MainActivity.this);
        SharcLibrary.setupWebView(webviewMedia, MainActivity.this, inObj);
        renderMediaContent("No data available");*/


        ArrayList<String> data = new ArrayList<String>();
        data.add("No data available");
        medAdapter = new MediaListAdapter(MainActivity.this, data, 0);
        mediaItemsPresentation = (ListView)findViewById(R.id.webViewTab);
        mediaItemsPresentation.setAdapter(medAdapter);

        resAdapter = new ResponseListAdapter(MainActivity.this, data);
        responseTab = (ListView)findViewById(R.id.responseTab);
        responseTab.setAdapter(resAdapter);
    }

    /*public void renderMediaContent(String content)
    {
        String base = "file://" + SharcLibrary.SHARC_MEDIA_FOLDER + "/";
        webviewMedia.loadDataWithBaseURL(base, content, "text/html", "utf-8",null);
    }*/

    //////////////////////////////////////////////////////////////////////////////
    // EXPERIENCE METADATA: ONLINE BROWSING - LOCAL BROWSING - RENDERING
    //////////////////////////////////////////////////////////////////////////////
    public void createExperience()
    {
        if(initialLocation == null)
        {
            Toast.makeText(this, R.string.message_gps, Toast.LENGTH_LONG).show();
            return;
        }
        final AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Create a new experience");
        //alert.setCancelable(false);
        alert.setMessage("Please enter experience name:");
        final EditText etExperienceName = new EditText(this);
        etExperienceName.setInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        alert.setView(etExperienceName);
        alert.setPositiveButton("Create", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String exName = etExperienceName.getText().toString().trim();
                if (exName.equalsIgnoreCase("")) {
                    Toast.makeText(MainActivity.this, "Please enter experience name", Toast.LENGTH_LONG).show();
                    createExperience();
                } else {
                    String exDesc = "This experience was initially authored using " + getString(R.string.app_name) + " - V" + getString(R.string.app_version) + " at " + new Date().toString();
                    ExperienceMetaDataModel selectedExperienceMeta = new ExperienceMetaDataModel(exName, exName + new Date().getTime(), exDesc, new Date().toString(), "SMAT (sharc@gmail.com)", "###SMAT###", initialLocation.latitude + " " + initialLocation.longitude);
                    experienceMetaDB.insertExperience(selectedExperienceMeta.getProName(), selectedExperienceMeta.getProPath(), selectedExperienceMeta.getProDesc(), selectedExperienceMeta.getProDate(), selectedExperienceMeta.getProAuthID(), selectedExperienceMeta.getProPublicURL(), selectedExperienceMeta.getProLocation());
                    selectedExperienceDetail = new ExperienceDetailsModel(new DBExperienceDetails(MainActivity.this, selectedExperienceMeta.getProPath()), true);
                    selectedExperienceDetail.setMetaData(selectedExperienceMeta);
                    getSlidingMenu().toggle();
                    clearMap(true);
                    addPOIMarkerListener();
                    gotoCurrentLocation(null);
                    smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.CREATE_EXPERIENCE, exName);
                    setSelectedTabIcons(0);
                }
            }
        });
        alert.setNegativeButton("Cancel", null);
        setDialogFontSizeAndShow(alert, FONT_SIZE);
        //alert.show();
        //smepInteractionLog.takeScreenShot();
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
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(MainActivity.this);
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
                params.add(new BasicNameValuePair("proAuthID", mDbxAcctMgr.getLinkedAccount().getUserId()));
                // Getting result in form of a JSON string from a Web RESTful
                JSONObject json = jParser.makeHttpRequest(url_all_experiences, "POST", params);

                int success = json.getInt("success");
                if (success == 1)
                {
                    // Get Array of experiences
                    JSONArray sharcFiles = json.getJSONArray("projects");

                    // Loop through all experiences
                    ExperienceMetaDataModel tmpExperience;
                    String logData = "";
                    for (int i = 0; i < sharcFiles.length(); i++)
                    {
                        JSONObject c = sharcFiles.getJSONObject(i);

                        // Storing each json item in variable
                        String name = c.getString("proName");
                        String path = c.getString("proPath");
                        String desc = c.getString("proDesc");
                        if(desc.length() > 0 && desc.charAt(desc.length()-1) != '.')
                            desc.concat(".");
                        String date = c.getString("proDate");
                        String authName = c.getString("userName");
                        String authEmail = c.getString("userEmail");
                        String authID = authName.concat(" (") + authEmail + ")";
                        String publicURL = c.getString("proPublicURL");
                        String location = c.getString("proLocation");
                        String summary = c.getString("proSummary");
                        if (summary == null)
                            summary = "";
                        if(location.length()<6)
                            continue;
                        //tmpExperience = new ExperienceMetaDataModel(name, path, desc, date, authID, authName, authEmail, publicURL, location);
                        tmpExperience = new ExperienceMetaDataModel(name, path, summary + desc, date, authID, publicURL, location);
                        logData += "#" + tmpExperience.getProName();
                        allExperienceMetaData.add(tmpExperience);
                    }
                    smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.VIEW_ONLINE_EXPERIENCES, logData);
                }
                else
                {
                    Toast.makeText(getApplicationContext(), json.getString("message") ,Toast.LENGTH_LONG).show();
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
            runOnUiThread(new Runnable()
            {
                public void run()
                {
                    //Updating parsed JSON data into ListView
                    displayAllExperienceMetaData(true);
                    addOnlineExperienceMarkerListener();
                }
            });
        }
    }

    /*
        This inner class helps
            - Download a snapshot (JSON file) of an experience from Dropbox
            - Download all media files from Dropbox
            - Present the experience
        Note this class needs retrieve information from server so it has to run in background
    */
    class ExperienceDetailsThread extends AsyncTask<String, String, String>
    {
        //Before starting background thread -> Show Progress Dialog
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(MainActivity.this);
            pDialog.setMessage("Loading the experience. Please wait...");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(false);
            pDialog.show();
        }

        protected String doInBackground(String... f_url)
        {
            try
            {
                URL url = new URL(f_url[0]);
                //Read content of the experience in a JSON file on Dropbox
                BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
                String jsonString = "";
                String str;
                while ((str = in.readLine()) != null) {
                    jsonString += str; // str is one line of text; readLine() strips the newline character(s)
                }
                in.close();
                selectedExperienceDetail.getExperienceFromSnapshotOnDropbox(jsonString);
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
            runOnUiThread(new Runnable()
            {
                public void run()
                {
                    //Render the experience
                    presentExperience();
                }
            });
        }
    }

    public void getAllExperienceMetaDataFromLocalDatabase()
    {
        clearMap(false);
        Cursor epxRets = experienceMetaDB.getExperiences();
        String logData = "";
        if(epxRets.getCount()>0)
        {
            ExperienceMetaDataModel tmpExperience;
            epxRets.moveToFirst();
            do
            {
                tmpExperience = new ExperienceMetaDataModel(epxRets.getString(1), epxRets.getString(0), epxRets.getString(2), epxRets.getString(3), epxRets.getString(4), epxRets.getString(5), epxRets.getString(6));
                logData += "#" + tmpExperience.getProName();
                allExperienceMetaData.add(tmpExperience);
            }
            while (epxRets.moveToNext());
        }
        smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.VIEW_CACHED_EXPERIENCES, logData);
        displayAllExperienceMetaData(false);
    }

    public void displayAllExperienceMetaData(boolean isOnline)
    {
        Marker tmpMarker = null;
        nearbyExperiences.clear();
        ArrayList<String> nearbyExperienceName = new ArrayList<String>();
        for (int i = 0; i < allExperienceMetaData.size() ; i++) {
            LatLng exLocation = allExperienceMetaData.get(i).getLocation();
            tmpMarker = mMap.addMarker(new MarkerOptions()
                            .title(String.valueOf(i))
                            .anchor(0, 0)
                            .position(exLocation)
                            .icon(BitmapDescriptorFactory.fromResource(R.raw.experience))
                            .visible(true)
            );
            //all experiences
            nearbyExperienceName.add(allExperienceMetaData.get(i).getProName());
            nearbyExperiences.add(i);//key = index of current list, value = index of marker --> reuse marker event
            //Nearby only
			/*
            if(initialLocation != null)
            {
                //Get near by experience 5000m
                float[] results = new float[1];
                Location.distanceBetween(exLocation.latitude,exLocation.longitude, initialLocation.latitude,initialLocation.longitude, results);
                if(results[0] < 5000)//radius of circle
                {
                    nearbyExperienceName.add(allExperienceMetaData.get(i).getProName());
                    nearbyExperiences.add(i);//key = index of current list, value = index of marker --> reuse marker event
                }
            }
            else
                Toast.makeText(this, R.string.message_gps, Toast.LENGTH_LONG).show();
			*/
        }
        allExperienceMarkers.add(tmpMarker);
        showNearByExperienceList(nearbyExperienceName.toArray(new CharSequence[nearbyExperienceName.size()]), isOnline);
    }

    public void addOnlineExperienceMarkerListener()
    {
        mMap.setOnMarkerClickListener(new OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker arg0) {
                return markerClick(arg0.getTitle(), true);
            }
        });
    }

    public void addDBExperienceMarkerListener()
    {
        mMap.setOnMarkerClickListener(new OnMarkerClickListener()
        {
            @Override
            public boolean onMarkerClick(Marker arg0)
            {
                return markerClick(arg0.getTitle(),false);
            }
        });
    }

    public boolean markerClick(String markerTitle, boolean isOnline)
    {
        if(markerTitle.equalsIgnoreCase("YAH"))//current location marker
        {
            Toast.makeText(getApplicationContext(), "This circle shows your current location", Toast.LENGTH_LONG).show();
            currentPos.showInfoWindow();
            return true;
        }
        final ExperienceMetaDataModel selectedExperienceMeta = allExperienceMetaData.get(Integer.parseInt(markerTitle));
        if(isOnline)
        {
            AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
            alert.setTitle(selectedExperienceMeta.getProName())
                    .setMessage(selectedExperienceMeta.getProDesc() + " This experience is designed by " + selectedExperienceMeta.getProAuthName() + " on " + selectedExperienceMeta.getProDate() + ".")
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton("Download", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            experienceMetaDB.insertExperience(selectedExperienceMeta.getProName(), selectedExperienceMeta.getProPath(), selectedExperienceMeta.getProDesc(), selectedExperienceMeta.getProDate(), selectedExperienceMeta.getProAuthID(), selectedExperienceMeta.getProPublicURL(), selectedExperienceMeta.getProLocation());
                            selectedExperienceDetail = new ExperienceDetailsModel(new DBExperienceDetails(MainActivity.this, selectedExperienceMeta.getProPath()), true);
                            selectedExperienceDetail.setMetaData(selectedExperienceMeta);
                            smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.DOWNLOAD_ONLINE_EXPERIENCE, selectedExperienceMeta.getProName());
                            new ExperienceDetailsThread().execute(selectedExperienceMeta.getProPublicURL());
                            setSelectedTabIcons(0);
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.CANCEL_DOWNLOAD_EXPERIENCE, selectedExperienceMeta.getProName());
                        }
                    });
            setDialogFontSizeAndShow(alert, FONT_SIZE);
                    //.show();
        }
        else
        {
            AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
            alert.setTitle(selectedExperienceMeta.getProName())
                    .setMessage(selectedExperienceMeta.getProDesc() + " This experience is designed by " + selectedExperienceMeta.getProAuthName() + " on " + selectedExperienceMeta.getProDate() + ".")
                    //.setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton("Edit", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            selectedExperienceDetail = new ExperienceDetailsModel(new DBExperienceDetails(MainActivity.this, selectedExperienceMeta.getProPath()), false);
                            selectedExperienceDetail.setMetaData(selectedExperienceMeta);
                            presentExperience();
                            setSelectedTabIcons(0);
                            smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.PLAY_EXPERIENCE, selectedExperienceMeta.getProName());
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.CANCEL_PLAY_EXPERIENCE, selectedExperienceMeta.getProName());
                        }
                    })
                    .setNeutralButton("Delete", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            //Delete db
                            smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.DELETE_EXPERIENCE, selectedExperienceMeta.getProName());

                            //MainActivity.this.deleteDatabase(selectedExperienceMeta.getProPath());
                            deleteSQLiteDB(selectedExperienceMeta.getProPath());
                            //Delete entry
                            experienceMetaDB.deleteExperience(selectedExperienceMeta.getProPath());
                            //Reload map
                            presentDownloadedExperiences();
                        }
                    });
            setDialogFontSizeAndShow(alert, FONT_SIZE);
                    //.show();
        }
        if(currentPos != null)
            currentPos.showInfoWindow();
        return true;
    }

    public void deleteSQLiteDB(String dbName) {
        File dDB = new File(SharcLibrary.SHARC_DATABASE_FOLDER + File.separator + dbName);
        File jDB = new File(SharcLibrary.SHARC_DATABASE_FOLDER + File.separator + dbName + "-journal");
        if (dDB.exists())
            dDB.delete();
        if (jDB.exists())
            jDB.delete();
    }

    public void showNearByExperienceList(final CharSequence[] items, final boolean isOnline)
    {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        //alert.setCancelable(false);
        if(items.length > 0)
        {
            //alert.setTitle("Experiences within 5 km");
            alert.setTitle("Please select an experience");
            alert.setItems(items, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int index) {
                    // Do something with the selection
                    //Toast.makeText(getApplicationContext(), items[item], Toast.LENGTH_LONG).show();
                    markerClick(String.valueOf(nearbyExperiences.get(index)), isOnline);
                }
            });
        }
        else
            alert.setTitle("There are no available experiences");
        //alert.setTitle("There are no experiences within 5 km");
        alert.setNegativeButton("Close to browse on map", null);	//Do nothing on no
        setDialogFontSizeAndShow(alert, FONT_SIZE);
        //alert.show();
    }

    //////////////////////////////////////////////////////////////////////////////
    // EXPERIENCE DETAIL: DOWNLOADING FROM SERVER -
    //////////////////////////////////////////////////////////////////////////////

    public void presentExperience()
    {
        clearMap(true);
        selectedExperienceDetail.renderAllPOIs(mMap, (SMEPAppVariable) getApplicationContext());
        addPOIMarkerListener();
        final int index = selectedExperienceDetail.renderAllRoutes(mMap);
        if(index != -1)//One route has not been saved
        {
            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setTitle("A route has not been saved");
            //alert.setCancelable(false);
            alert.setMessage("Previously, a route has not been saved. Do you want to continue recording this route (you should go back to where you were on the route)?");
            curRoute = selectedExperienceDetail.getRouteAt(index);
            alert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    isRecording = true;
                    LatLng lastPoint = curRoute.getLastPointOfPath();
                    if(lastPoint != null) {
                        prevLocation = new Location("");
                        prevLocation.setLatitude(lastPoint.latitude);
                        prevLocation.setLongitude(lastPoint.longitude);
                    }
                    btnStartRoute.setText("Pause recording route");
                    btnStartRoute.setBackgroundResource(R.drawable.custom_btn_blue);
                    btnStopRoute.setEnabled(true);
                    btnStopRoute.setBackgroundResource(R.drawable.custom_btn_blue);
                    smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.CONTINUE_ROUTE, curRoute.getName());
                }
            });
            alert.setNegativeButton("No", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {

                }
            });
            setDialogFontSizeAndShow(alert, FONT_SIZE);
        }
        selectedExperienceDetail.renderAllEOIs();
        selectedExperienceDetail.getMediaStat();
        //bound for the whole experience
        LatLngBounds bounds = selectedExperienceDetail.getGeographicalBoundary();
        if(bounds != null)
            mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));//50 = margin ~ geofence
        if(mMap.getCameraPosition().zoom > 19)// && allPOIs.size() < 3)
            mMap.animateCamera(CameraUpdateFactory.zoomTo(19), 2000, null);
    }

    public void addPOIMarkerListener()
    {

        mMap.setOnMarkerClickListener(new OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker arg0) {
                if (arg0.getTitle().equalsIgnoreCase("START"))
                    Toast.makeText(getApplicationContext(), "This marker indicates the starting point of the route", Toast.LENGTH_LONG).show();
                else if (arg0.getTitle().equalsIgnoreCase("END"))
                    Toast.makeText(getApplicationContext(), "This marker indicates the end point of the route", Toast.LENGTH_LONG).show();
                else if (arg0.getTitle().equalsIgnoreCase("YAH"))//current location marker
                {
                    Toast.makeText(getApplicationContext(), "This circle shows your current location", Toast.LENGTH_LONG).show();
                } else {
                    pushMediaToUser(Integer.valueOf(arg0.getTitle()));
                    System.out.println("You've tap POI marker:" + arg0.getTitle());
                }
                if(currentPos != null)
                    currentPos.showInfoWindow();
                return true;
            }
        });
    }

    @Override
    public void onMapClick(LatLng arg0) {
        // Click on a trigger zone
        if(selectedExperienceDetail != null)
            pushMediaToUser(selectedExperienceDetail.getTriggerZoneIndexFromLocation(arg0));
    }

    public void pushMediaToUser(int poiID)
    {
        try
        {
            //currentPOIIndex = Integer.valueOf(arg0.getTitle());
            displayMediaTab(poiID, "FROM_MAP_PULL");
            if(smepSettings.isSoundNotification())
            {
                Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
                r.play();
            }
        }
        catch (Exception e) {Log.e(TAG, "Error when playing sound: " + e.getMessage());}
    }

    //////////////////////////////////////////////////////////////////////////////
    // LOCATION CHANGE SERVICE
    //////////////////////////////////////////////////////////////////////////////

    /**
     * <p>This class tracks the current location of the user</p>
     **/
    private class LocationListener implements android.location.LocationListener
    {
        Location mLastLocation;

        public LocationListener(String provider)
        {
            //Log.e(TAG, "LocationListener " + provider);
            mLastLocation = new Location(provider);
        }
        @Override
        public void onLocationChanged(Location location)
        {
            //Log.e(TAG, "onLocationChanged..................................: " + location);
            mLastLocation.set(location);
            updateSMEPWhenLocationChange(location);
        }

        @Override
        public void onProviderDisabled(String provider)
        {
            //Log.e(TAG, "onProviderDisabled: " + provider);
            if(smepSettings.isShowingGPS())
                getActionBar().setTitle("GPS is not available yet");
        }
        @Override
        public void onProviderEnabled(String provider)
        {
            //Log.e(TAG, "onProviderEnabled: " + provider);
        }
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras)
        {
            //Log.e(TAG, "onStatusChanged: " + provider);
        }
    }

    public void updateSMEPWhenLocationChange(Location location) {
        try {
            //Log.d("Mock Location:","Location changed:" + "Test mode: " + isTestMode);
            lastKnownLocation = location;
            GeomagneticField field = new GeomagneticField(
                    (float) location.getLatitude(),
                    (float) location.getLongitude(),
                    (float) location.getAltitude(), System.currentTimeMillis());
            // getDeclination returns degrees
            mDeclination = field.getDeclination();
            LatLng curPos = new LatLng(location.getLatitude(),location.getLongitude());
            currentPos.setPosition(curPos);

            //currentPos.
            currentAccuracy.setCenter(curPos);
            //System.out.println("GPS accuracy:" + location.getAccuracy());
            if(smepSettings.isShowingGPS())
                getActionBar().setTitle("GPS Accuracy: " + String.format("%.1f", location.getAccuracy()) + " (m)");
            currentAccuracy.setRadius(location.getAccuracy());
            initialLocation = new LatLng(location.getLatitude(),location.getLongitude());

            if (smepSettings.isYAHCentred()) {
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentPos.getPosition(), mMap.getCameraPosition().zoom));
            }

            SMEPAppVariable mySMEPAppVariable = (SMEPAppVariable) getApplicationContext();
            if(mySMEPAppVariable.isNewMedia())
            {
                mySMEPAppVariable.setNewMedia(false);
                currentPOIIndex = mySMEPAppVariable.getNewMediaIndex();
                displayMediaTab(currentPOIIndex, "FROM_LOCATION_SERVICE");
            }

            if (smepSettings.isTestMode()) {
                new MockLocationService().execute();
            }

            //Log current location
            smepInteractionLog.addLog(SharcLibrary.locationToLatLng(location), mDbxAcctMgr, InteractionLog.LOCATION_CHANGE, String.valueOf(location.getAccuracy()));
            //recording route
            if(isRecording)
            {
                if(prevLocation == null)
                    prevLocation = location;
                LatLng lastLatLng= SharcLibrary.locationToLatLng(prevLocation);
                LatLng thisLatLng= SharcLibrary.locationToLatLng(location);
                //Only add new line for walking now -> 2m < x < 10m
                float[] results = new float[1];
                Location.distanceBetween(lastLatLng.latitude,lastLatLng.longitude, thisLatLng.latitude,thisLatLng.longitude, results);
                //if(results[0] < 10) {
                if(location.getAccuracy() <= 15 && results[0] >= 5) {
                    curRoute.getPath().add(thisLatLng);
                    curRoutePath.add(mMap.addPolyline(new PolylineOptions().add(lastLatLng).add(thisLatLng).width(5).color(Color.RED)));
                    //Caculate distance
                    curRoute.setDistance(curRoute.getDistance() + results[0]/1000);//get km
                    prevLocation = location;
                    selectedExperienceDetail.updateRoutePath(curRoute);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setUpLocationService()	{
        LocationManager mLocationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        try	{
            mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE, mLocationListeners[1]);
        }
        catch (java.lang.SecurityException ex) {
            Log.i(TAG, "fail to request location update, ignore", ex);
        }
        catch (IllegalArgumentException ex) {
            Log.d(TAG, "network provider does not exist, " + ex.getMessage());
        }
        try {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE, mLocationListeners[0]);
        }
        catch (java.lang.SecurityException ex) {
            Log.i(TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "gps provider does not exist " + ex.getMessage());
        }
    }


    /**
     * <p>This class is another background thread which simulates the fake current location</p>
     **/
    class MockLocationService extends AsyncTask<String, String, String>
    {
        //Before starting background thread Show Progress Dialog
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        protected String doInBackground(String... args)
        {
            try
            {
                JSONParser jParser = new JSONParser();
                // Building Parameters
                List<NameValuePair> params = new ArrayList<NameValuePair>();
                params.add(new BasicNameValuePair("locationID",testingCode));
                // getting JSON string from URL
                JSONObject json = jParser.makeHttpRequest(url_mockLocation, "POST", params);

                // Check your log cat for JSON reponse
                Log.d("Mock Location:", json.toString());
                // Checking for SUCCESS TAG
                int success = json.getInt("success");
                if (success == 1)
                {
                    JSONArray mockLocations = json.getJSONArray("location");
                    // looping through All files
                    for (int i = 0; i < mockLocations.length(); i++)
                    {
                        JSONObject c = mockLocations.getJSONObject(i);
                        double lat = c.getDouble("lat");
                        double lng = c.getDouble("lng");
                        SMEPAppVariable mySMEPAppVariable = (SMEPAppVariable) getApplicationContext();
                        Location mockLoc = new Location("");//provider name is necessary
                        mockLoc.setLatitude(lat);//your coords of course
                        mockLoc.setLongitude(lng);
                        mySMEPAppVariable.setMockLocation(mockLoc);
                        System.out.println("Lat x Lng:" + lat + " x " + lng);
                    }
                }
                else
                {
                    // no file found
                    Toast.makeText(getApplicationContext(), json.getInt("message") ,Toast.LENGTH_LONG).show();
                }
            }
            catch (JSONException e)
            {
                e.printStackTrace();
            }
            return null;
        }

        //After completing background task Dismiss the progress dialog
        protected void onPostExecute(String file_url)
        {
            // updating UI from Background Thread
            runOnUiThread(new Runnable()
            {
                public void run()
                {
                    //Updating parsed JSON data into ListView
                    SMEPAppVariable mySMEPAppVariable = (SMEPAppVariable) getApplicationContext();
                    currentPosition.setPosition(new LatLng(mySMEPAppVariable.getMockLocation().getLatitude(), mySMEPAppVariable.getMockLocation().getLongitude()));
                }
            });
        }
    }

    public void gotoCurrentLocation(View v)
    {
        if(lastKnownLocation == null)
            moveAndZoomToLocation(null, 16);
        else
            moveAndZoomToLocation(initialLocation, 16);
        //Tmp code here for hard binding
        /*DbxDatastore mDatastore = null;
        try {
            //Connect to datastore
            DbxDatastoreManager mDatastoreManager = DbxDatastoreManager.forAccount(mDbxAcctMgr.getLinkedAccount());
            mDatastore  = mDatastoreManager.openDatastore(selectedExperienceDetail.getMetaData().getProPath());
            DbxTable responseTable = mDatastore.getTable("Responses");
            //Get the index of the response
            DbxRecord responseMedia = responseTable.getOrInsert("1430658478889");
            responseMedia.set("entityID", "1430654450268");
            mDatastore.sync();
            //responseMedia.set("type",response.getType()).set("content", response.getContent()).set("entityType", response.getEntityType()).set("entityID", response.getEntityID())
            //        .set("desc", response.getDesc()).set("noOfLike", 0).set("consumerName", response.getConName()).set("consumerEmail", response.getConEmail()).set("status", response.getStatus());

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            if(mDatastore!=null)
                mDatastore.close();
        }
        */

            /*authoringTable = mDatastore.getTable("POIs");
            //Insert a new row to the POI table
            POIModel poi = selectedExperienceDetail.getPOIFromID(response.getId());
            if(poi != null) {
                DbxRecord poiRow = authoringTable.getOrInsert(poi.getID());
                poiRow.set("type", poi.getType()).set("name", poi.getName()).set("latLng", poi.getLocationString()).set("associatedEOI", "")
                        .set("desc", poi.getDesc()).set("mediaOrder", poi.getStringMediaOrder()).set("triggerZone", poi.getTriggerZoneString()).set("associatedRoute", "");
            }
            */


           /* authoringTable = mDatastore.getTable("Routes");
            //Insert a new row to the POI table
            RouteModel route = selectedExperienceDetail.getRouteFromID(response.getId());
            if(route != null) {
                DbxRecord routeRow = authoringTable.getOrInsert(route.getId());
                routeRow.set("name", route.getName()).set("polygon", route.getPathString()).set("associatedEOI", "")
                        .set("desc", route.getDesc()).set("mediaOrder", "").set("colour", route.getColour()).set("associatedPOI", "").set("directed", route.getDirected());
            }*/

            /*authoringTable = mDatastore.getTable("media");
            String publicURL = "";
            //Insert a new row into Media table
            DbxRecord responseMedia = authoringTable.getOrInsert(response.getId());
            responseMedia.set("type", response.getType()).set("name", response.getDesc()).set("attachedTo", response.getEntityType()).set("PoIID", response.getEntityID())
                    .set("desc", "").set("noOfLike", "0").set("content", response.getContent()).set("context", "");
            //Update the POI table
            DbxTable poiTable = mDatastore.getTable("POIs");
            DbxRecord poiRow = poiTable.getOrInsert(response.getEntityID());
            poiRow.set("mediaOrder", selectedExperienceDetail.getPOIFromID(response.getEntityID()).getStringMediaOrder());
            */
    }

    public void moveAndZoomToLocation(LatLng location, int zoomLevel)
    {
        if(location == null)
        {
            Toast.makeText(this, R.string.message_gps, Toast.LENGTH_LONG).show();
        }
        else {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, zoomLevel-1));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(zoomLevel), 2000, null);
        }
    }

    //////////////////////////////////////////////////////////////////////////////
    // DIRECTION SENSOR
    //////////////////////////////////////////////////////////////////////////////
    private SensorEventListener mySensorEventListener = new SensorEventListener(){
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            // angle between the magnetic north direction
            float bearing = (float) (event.values[0] + mDeclination);
            currentPos.setRotation(bearing - mMap.getCameraPosition().bearing);
            if(smepSettings.isRotating())
                updateCamera(bearing);
        }
    };

    private void updateCamera(float bearing) {
        CameraPosition oldPos = mMap.getCameraPosition();
        CameraPosition pos = CameraPosition.builder(oldPos).bearing(bearing).build();
        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(pos));
    }

    //////////////////////////////////////////////////////////////////////////////
    // RESPONSE
    //////////////////////////////////////////////////////////////////////////////
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if(requestCode == TAKE_PICTURE)
        {
            if (resultCode == RESULT_OK) {
                String[] entity = getAssociatedEntity();
                String id = outputFile.substring(outputFile.lastIndexOf(File.separator) + 1, outputFile.lastIndexOf("."));
                ResponseModel res = new ResponseModel(id, "Waiting", "image", "", outputFile, entity[0], entity[1], "", "NEW", "NEW");
                smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.ADD_RESPONSE_IMAGE, entity[0] + "#" + entity[1]);
                //res.setFileUri(fileUri);
                addDescription(res);
            }
        }
        else if(requestCode == CAPTURE_VIDEO)
        {
            if (resultCode == RESULT_OK) {
                String[] entity = getAssociatedEntity();
                String id = outputFile.substring(outputFile.lastIndexOf(File.separator) + 1, outputFile.lastIndexOf("."));
                ResponseModel res = new ResponseModel(id, "Waiting", "video", "", outputFile, entity[0], entity[1], "", "NEW", "NEW");
                smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.ADD_RESPONSE_VIDEO, entity[0] + "#" + entity[1]);
                addDescription(res);
            }
        }
        else if(requestCode == PICK_FROM_CAMERA)
        {
            if (resultCode == RESULT_OK) {
                cropImage(false);
                //smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.ADD_RESPONSE_VIDEO, entity[0] + "#" + entity[1]);
            }
        }
        else if(requestCode == PICK_FROM_FILE)
        {
            if (resultCode == RESULT_OK) {
                fileUri = data.getData();
                cropImage(true);
                //smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.ADD_RESPONSE_VIDEO, entity[0] + "#" + entity[1]);
            }
        }
        else if(requestCode == CROP_IMAGE)
        {
            if (resultCode == RESULT_OK && data != null) {
                String imagePath = data.getExtras().getString("imagePath");
                new UpdatePublishedExperiencesThread().execute(imagePath);
                //smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.ADD_RESPONSE_VIDEO, entity[0] + "#" + entity[1]);
            }
        }
        else if (requestCode == REQUEST_LINK_TO_DBX)
        {
            if (resultCode == RESULT_OK) {
                if(mDbxAcctMgr.hasLinkedAccount())
                {
                    dbUser = null;
                    new GetUserDetailsService().execute();
                }
                displayDropboxUser(dbUser);
            }
            else {
                //... Link failed or was cancelled by the user.
                smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.SELECT_LOGIN, "failed");
                Toast.makeText(this, "Link to Dropbox failed.", Toast.LENGTH_LONG).show();
            }
        }
        else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    /**
     * <p>This class is another background thread which get user details when logging into SMEP</p>
     **/
    class GetUserDetailsService extends AsyncTask<String, String, String>
    {
        //Before starting background thread Show Progress Dialog
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(MainActivity.this);
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
            runOnUiThread(new Runnable()
            {
                public void run()
                {
                    pDialog.dismiss();
                    if(dbUser != null ) {
                        smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.SELECT_LOGIN, mDbxAcctMgr.getLinkedAccount().getUserId());
                    }
                    displayDropboxUser(dbUser);
                }
            });
        }
    }

    public void addResponse(View v)
    {
        if(currentTab == 0) {
            if(selectedExperienceDetail == null)
                Toast.makeText(this, "You need to create or open an experience first", Toast.LENGTH_LONG).show();
            else
                addNewPOI();
        }
        else if(currentTab == 1) {
            if(selectedExperienceDetail == null)
                Toast.makeText(this, "You need to select a POI first", Toast.LENGTH_LONG).show();
            else {
                AlertDialog.Builder alert = new AlertDialog.Builder(this);
                alert.setTitle("Add media items");
                //alert.setCancelable(false);
                LayoutInflater factory = LayoutInflater.from(this);
                final View textEntryView = factory.inflate(R.layout.response_select_type_dialog, null);
                sessionLocation = initialLocation;//start a new session -> all responses will have the same LatLng
                alert.setView(textEntryView);
                alert.setNegativeButton("Close", new DialogInterface.OnClickListener() {//Refresh media tab
                    public void onClick(DialogInterface dialog, int whichButton) {
                        switchToPOIMediaTab("3");
                    }
                });
                //alert.show();
                setDialogFontSizeAndShow(alert, FONT_SIZE);
                smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.OPEN_RESPONSE_DIALOG, String.valueOf(currentTab));
            }
        }
        else
        {
            if(dbUser == null)
            {
                Toast.makeText(this, getString(R.string.message_dropboxConnection), Toast.LENGTH_LONG).show();
                return;
            }
            else if(!SharcLibrary.isNetworkAvailable(this))
            {
                Toast.makeText(this, getString(R.string.message_wifiConnection), Toast.LENGTH_LONG).show();
                return;
            }
            smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.SELECT_UPLOAD_ALL, "Up load all");
            new UploadAllToDropboxThread().execute();
            /*File mPath = new File("/data/data/uk.lancs.sharc.smat/databases");
            if(mPath.exists())
            {
                File[] fList = mPath.listFiles();
                for (File file : fList) {
                    if (file.isFile()) {
                        System.out.println(file.getAbsolutePath());
                    }
                }
            }*/
        }
    }

    public void addTextResponse(View v)
    {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle("Add text");
        //alert.setCancelable(false);
        LayoutInflater factory = LayoutInflater.from(this);
        final View textEntryView = factory.inflate(R.layout.response_add_text_dialog, null);
        alert.setView(textEntryView);

        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                EditText content = (EditText) textEntryView.findViewById(R.id.editTextMediaContentD);
                EditText title = (EditText) textEntryView.findViewById(R.id.editTextTitleD);
                String id = String.valueOf((new Date()).getTime());
                String[] entity = getAssociatedEntity();
                ResponseModel res = new ResponseModel(id, "Waiting", "text", title.getText().toString(), content.getText().toString(), entity[0], entity[1], "", "NEW", "NEW");
                selectedExperienceDetail.addMyResponse(res);
                selectedExperienceDetail.addNewMediaItem(res);
                showResponseDone();
                smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.ADD_RESPONSE_TEXT, entity[0] + "#" + entity[1]);
            }
        });

        alert.setNegativeButton("Cancel", null);
        setDialogFontSizeAndShow(alert, FONT_SIZE);
        //alert.show();
    }

    public String[] getAssociatedEntity()
    {
        String mEntityType = "";
        String mEntityID = "";
        switch (currentTab)
        {
            case 0:
                mEntityType = "NEW";
                mEntityID = sessionLocation.latitude + " " + sessionLocation.longitude;
                break;
            case 1:
                mEntityType = "POI";
                mEntityID = selectedExperienceDetail.getPOIID(currentPOIIndex);
                break;
            case 2:
                mEntityType = "EOI";
                mEntityID = "";
                break;
            case 3:
                mEntityType = "ROUTE";
                mEntityID = "";
                break;
            default:
                break;
        }
        return new String[]{mEntityType, mEntityID};
    }

    public void addPhotoResponse(View v)
    {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        fileUri = getOutputMediaFileUri(TAKE_PICTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
        // start the image capture Intent
        startActivityForResult(intent, TAKE_PICTURE);
    }

    public Uri getOutputMediaFileUri(int type)
    {
        String path = SharcLibrary.SHARC_MEDIA_FOLDER + File.separator + (new Date()).getTime();
        File mediaFile;
        if (type == TAKE_PICTURE)
        {
            mediaFile = new File(path + ".jpg");
        }
        else if (type == CAPTURE_VIDEO)
        {
            mediaFile = new File(path + ".mp4");
        }
        else
        {
            return null;
        }
        outputFile = mediaFile.getAbsolutePath();
        return Uri.fromFile(mediaFile);
    }

    public Uri getOutputMediaFileUriOld(int type)
    {
        // External sdcard location
        File mediaStorageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsoluteFile();
        //Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss",Locale.getDefault()).format(new Date());
        File mediaFile;
        if (type == TAKE_PICTURE)
        {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator + "Camera" + File.separator + "IMG_" + timeStamp + ".jpg");
        }
        else if (type == CAPTURE_VIDEO)
        {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator + "Camera" + File.separator + "VID_" + timeStamp + ".mp4");
        }
        else
        {
            return null;
        }
        outputFile = mediaFile.getAbsolutePath();
        return Uri.fromFile(mediaFile);
    }

    public void addDescription(final ResponseModel curRes)
    {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Please enter title for the media (optional)");
        //alert.setCancelable(false);
        // Set an EditText view to get user input
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        alert.setView(input);
        alert.setPositiveButton("Add", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                curRes.setDesc(input.getText().toString());
                selectedExperienceDetail.addMyResponse(curRes);
                selectedExperienceDetail.addNewMediaItem(curRes);
                smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.ADD_RESPONSE_DESC, curRes.getDesc());
                showResponseDone();
            }
        });

        alert.setNegativeButton("Skip", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
                curRes.setDesc("");
                selectedExperienceDetail.addMyResponse(curRes);
                selectedExperienceDetail.addNewMediaItem(curRes);
                showResponseDone();
            }
        });
        setDialogFontSizeAndShow(alert, FONT_SIZE);
        //alert.show();
        // see http://androidsnippets.com/prompt-user-input-with-an-alertdialog
    }

    public void addAudioResponse(View v)
    {
        final AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Record audio");
        //alert.setCancelable(false);
        LayoutInflater factory = LayoutInflater.from(this);
        final View textEntryView = factory.inflate(R.layout.response_add_audio_dialog, null);
        alert.setView(textEntryView);
        timerValue = (TextView)textEntryView.findViewById(R.id.viewRecording);
        timerValue.setText("00:00:00");
        Button record = (Button)textEntryView.findViewById(R.id.btnAudioRecording);
        record.setText("Start recording");
        //record.setTextColor(Color.BLACK);
        outputFile = null;
        alert.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                if(outputFile == null) {
                    Toast.makeText(MainActivity.this, "Please tap the 'Start recording' button to record an audio clip first", Toast.LENGTH_LONG).show();
                    addAudioResponse(null);
                }
                else {
                    stopRecording();
                    String id = outputFile.substring(outputFile.lastIndexOf(File.separator) + 1, outputFile.lastIndexOf("."));
                    String[] entity = getAssociatedEntity();
                    ResponseModel res = new ResponseModel(id, "Waiting", "audio", "", outputFile, entity[0], entity[1], "", "NEW", "NEW");
                    //selectedExperienceDetail.addNewMediaItem(res);
                    smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.ADD_RESPONSE_AUDIO, entity[0] + "#" + entity[1]);
                    //File audioFile = new File(outputFile);
                    //res.setFileUri(Uri.fromFile(audioFile));
                    addDescription(res);
                }
            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                stopRecording();
            }
        });
        setDialogFontSizeAndShow(alert, FONT_SIZE);
        //alert.show();
    }

    public void recordAudio (View v)
    {
        Button record = (Button)v;//findViewById(R.id.btnAudioRecording);
        if(record.getText().toString().contains("Start"))
        {
            startTime = SystemClock.uptimeMillis();
            customHandler.postDelayed(updateTimerThread, 0);

            String id = String.valueOf((new Date()).getTime());
            outputFile = SharcLibrary.SHARC_MEDIA_FOLDER + File.separator + id + ".mp3";
            myAudioRecorder = new MediaRecorder();
            myAudioRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            myAudioRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            myAudioRecorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);
            myAudioRecorder.setOutputFile(outputFile);
            try {
                myAudioRecorder.prepare();
                myAudioRecorder.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
            record.setText("STOP");
            record.setBackgroundColor(Color.argb(255, 60, 190, 255));
        }
        else if(record.getText().toString().contains("STOP"))
        {
            //timeSwapBuff += timeInMilliseconds;
            customHandler.removeCallbacks(updateTimerThread);

            try {
                myAudioRecorder.stop();
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
            finally
            {
                myAudioRecorder.release();
                myAudioRecorder  = null;
                record.setText("Start recording");
                record.setBackgroundColor(Color.argb(255, 173, 13, 6));
            }
        }
    }

    private void stopRecording()
    {
        if(myAudioRecorder!=null)
        {
            customHandler.removeCallbacks(updateTimerThread);

            try {
                myAudioRecorder.stop();
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
            finally
            {
                myAudioRecorder.release();
                myAudioRecorder  = null;
            }
        }
    }

    private Runnable updateTimerThread = new Runnable() {
        public void run() {

            timeInMilliseconds = SystemClock.uptimeMillis() - startTime;
            //updatedTime = timeSwapBuff + timeInMilliseconds;
            updatedTime =  timeInMilliseconds;

            int secs = (int) (updatedTime / 1000);
            int mins = secs / 60;
            secs = secs % 60;
            int milliseconds = (int) (updatedTime % 1000);
            timerValue.setText("" + String.format("%02d", mins) + ":"
                    + String.format("%02d", secs) + ":"
                    + String.format("%03d", milliseconds));
            customHandler.postDelayed(this, 0);
        }
    };
    public void addVideoResponse(View v)
    {
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        fileUri = getOutputMediaFileUri(CAPTURE_VIDEO);
        // set video quality
        intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri); // set the image file name
        // start the video capture Intent
        startActivityForResult(intent, CAPTURE_VIDEO);
    }

    public void showResponseDone()
    {
        Toast.makeText(this, "Your media item has been associated with the POI. When you have Wi-Fi/Data, please go to the REVIEW AND UPLOAD tab to review and upload it", Toast.LENGTH_LONG).show();
    }

    public void uploadResponse(int index)
    {
        if(dbUser == null)
        {
            Toast.makeText(this, getString(R.string.message_dropboxConnection), Toast.LENGTH_LONG).show();
            return;
        }
        else if(!SharcLibrary.isNetworkAvailable(this))
        {
            Toast.makeText(this, getString(R.string.message_wifiConnection), Toast.LENGTH_LONG).show();
            return;
        }
        smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.SELECT_UPLOAD_RESPONSE, index + "#" + selectedExperienceDetail.getMyResponseName(index));
        new UploadToDropboxThread().execute(String.valueOf(index));
    }

    public void viewResponse(final int index)
    {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("View an authoring action");
        //alert.setCancelable(false);
        LayoutInflater factory = LayoutInflater.from(this);
        final View textEntryView = factory.inflate(R.layout.response_preview_dialog, null);
        alert.setView(textEntryView);
        alert.setNegativeButton("Close", null);	//Do nothing on no
        //alert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
        //    public void onClick(DialogInterface dialog, int which) {
        String responseContent = selectedExperienceDetail.getMyResponseContentAt(index);
        WebView wv = (WebView) textEntryView.findViewById(R.id.webViewContent);
        String base = "file://" + SharcLibrary.SHARC_MEDIA_FOLDER + "/";
        wv.loadDataWithBaseURL(base, responseContent, "text/html", "utf-8", null);
        //    }
        //});
        setDialogFontSizeAndShow(alert, FONT_SIZE);
        //alert.show();
        smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.SELECT_VIEW_RESPONSE, index + "#" + selectedExperienceDetail.getMyResponseName(index));
    }

    public void deleteResponse(final int index)
    {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Delete an authoring action");
        //alert.setCancelable(false);
        if(selectedExperienceDetail.getMyResponseAt(index).getEntityType().equalsIgnoreCase("NEW"))
            alert.setMessage("Are you sure you want to delete this POI and its associated media items?");
        else
            alert.setMessage("Are you sure you want to delete this authoring action?");

        alert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                selectedExperienceDetail.deleteMyResponseAt(index);
                smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.SELECT_DELETE_RESPONSE, index + "#" + selectedExperienceDetail.getMyResponseName(index));
                displayResponseTab();
            }
        });
        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                stopRecording();
            }
        });
        setDialogFontSizeAndShow(alert, FONT_SIZE);
        //alert.show();
    }
    //////////////////////////////////////////////////////////////////////////////
    // MEDIA ITEM - DROPBOX
    //////////////////////////////////////////////////////////////////////////////
    public void checkDropboxLogin()
    {
        mDbxAcctMgr = DbxAccountManager.getInstance(getApplicationContext(), APP_KEY, APP_SECRET);
        if(mDbxAcctMgr.hasLinkedAccount())
        {
            dbUser = mDbxAcctMgr.getLinkedAccount().getAccountInfo();
        }
        displayDropboxUser(dbUser);
    }

    public void displayDropboxUser(DbxAccountInfo dbUser)
    {
        TextView txtUsername = (TextView) findViewById(R.id.txtUsername);
        TextView txtUseremail = (TextView) findViewById(R.id.txtUseremail);
        if(dbUser!=null)
        {
            //Log.d(TAG, dbUser.toString());
            try {
                JSONObject jsonUser = new JSONObject(dbUser.toString());
                JSONObject jsonUserInfo = new JSONObject(jsonUser.getString("rawJson"));
                txtUseremail.setText(jsonUserInfo.getString("email"));
                txtUsername.setText(dbUser.displayName);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        else
        {
            //Prompt to login
            txtUsername.setText("Log in to submit responses");
            txtUseremail.setText("(Dropbox account is required)");
        }
    }

    //This inner class (thread) enable uploading a media file and get public URL
    class UploadToDropboxThread extends AsyncTask<String, String, String>
    {
        //Before starting background thread Show Progress Dialog
        private boolean isError = false;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(MainActivity.this);
            pDialog.setMessage("Uploading response(s). Please wait...");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(false);
            pDialog.show();
        }

        protected String doInBackground(String... args)
        {
            DbxDatastore mDatastore = null;
            try {
                //Connect to datastore
                DbxDatastoreManager mDatastoreManager = DbxDatastoreManager.forAccount(mDbxAcctMgr.getLinkedAccount());
                String proPath = selectedExperienceDetail.getMetaData().getProPath();
                //Else local project
                mDatastore  = mDatastoreManager.openDatastore(proPath);
                //Get the index of the response
                int resIndex = Integer.parseInt(args[0]);
                ResponseModel response = selectedExperienceDetail.getMyResponseAt(resIndex);
                uploadOneResponse(resIndex, response, mDatastore);
                //displayResponseTab();
            }
            catch (Exception e)
            {
                e.printStackTrace();
                isError = true;
            }
            finally
            {
                if(mDatastore != null)
                    mDatastore.close();
            }
            return null;
        }

        //After completing background task Dismiss the progress dialog
        protected void onPostExecute(String file_url)
        {
            // dismiss the dialog after getting all files
            pDialog.dismiss();
            // updating UI from Background Thread
            runOnUiThread(new Runnable()
            {
                public void run()
                {
                    displayResponseTab();//To view new list after delete the submitted response
                    if(isError)
                        Toast.makeText(MainActivity.this, getString(R.string.message_upload_error), Toast.LENGTH_LONG).show();
                    else {
                        Toast.makeText(MainActivity.this, "The authoring action has been uploaded successfully", Toast.LENGTH_LONG).show();
                        if (selectedExperienceDetail.getMyResponses().size() == 0)
                            showPublishExperienceDialog();
                    }
                }
            });
        }
    }

    //This inner class (thread) enable uploading all media files and get public URL
    class UploadAllToDropboxThread extends AsyncTask<String, String, String>
    {
        //Before starting background thread Show Progress Dialog
        private boolean isError = false;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(MainActivity.this);
            pDialog.setMessage("Uploading all authoring actions. Please wait...");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(false);
            pDialog.show();
        }

        protected String doInBackground(String... args)
        {
            DbxDatastore mDatastore = null;
            try {
                //Connect to datastore
                DbxDatastoreManager mDatastoreManager = DbxDatastoreManager.forAccount(mDbxAcctMgr.getLinkedAccount());
                String proPath = selectedExperienceDetail.getMetaData().getProPath();
                //Else local project
                mDatastore  = mDatastoreManager.openDatastore(proPath);
                //Get the index of the response
                for(int index = 0; index < selectedExperienceDetail.getMyResponses().size(); index++) {
                    ResponseModel response = selectedExperienceDetail.getMyResponseAt(index);
                    uploadOneResponse(index, response, mDatastore);
                }
                //displayResponseTab();
            }
            catch (Exception e) {
                e.printStackTrace();
                isError = true;
            }
            finally
            {
                if(mDatastore != null)
                    mDatastore.close();
            }
            return null;
        }

        //After completing background task Dismiss the progress dialog
        protected void onPostExecute(String file_url)
        {
            // dismiss the dialog after getting all files
            pDialog.dismiss();
            // updating UI from Background Thread
            runOnUiThread(new Runnable()
            {
                public void run()
                {
                    displayResponseTab();//To view new list after delete the submitted response
                    if(isError)
                        Toast.makeText(MainActivity.this, getString(R.string.message_upload_error), Toast.LENGTH_LONG).show();
                    else
                    {
                        Toast.makeText(MainActivity.this, "All authoring actions have been uploaded successfully", Toast.LENGTH_LONG).show();
                        showPublishExperienceDialog();
                    }
                }
            });
        }
    }

    public void uploadOneResponse(int index, ResponseModel response, DbxDatastore mDatastore) throws Exception {
        DbxTable authoringTable = null;
        if(response.getEntityType().equalsIgnoreCase("NEW"))//New POI
        {
            authoringTable = mDatastore.getTable("POIs");
            //Insert a new row to the POI table
            POIModel poi = selectedExperienceDetail.getPOIFromID(response.getId());
            if(poi != null) {
                DbxRecord poiRow = authoringTable.getOrInsert(poi.getID());
                poiRow.set("type", poi.getType()).set("name", poi.getName()).set("latLng", poi.getLocationString()).set("associatedEOI", "")
                        .set("desc", poi.getDesc()).set("mediaOrder", poi.getStringMediaOrder()).set("triggerZone", poi.getTriggerZoneString()).set("associatedRoute", "");
            }
            else
                throw new Exception();
        }
        else if(response.getEntityType().equalsIgnoreCase("ROUTE"))//New Route
        {
            authoringTable = mDatastore.getTable("Routes");
            //Insert a new row to the POI table
            RouteModel route = selectedExperienceDetail.getRouteFromID(response.getId());
            if(route != null) {
                DbxRecord routeRow = authoringTable.getOrInsert(route.getId());
                routeRow.set("name", route.getName()).set("polygon", route.getPathString()).set("associatedEOI", "")
                        .set("desc", route.getDesc()).set("mediaOrder", "").set("colour", route.getColour()).set("associatedPOI", "").set("directed", route.getDirected());
            }
            else
                throw new Exception();
        }
        else // New media
        {
            authoringTable = mDatastore.getTable("media");
            String[] ret = new String[]{"0",""};
            String publicURL = "";

            if (response.getType().equalsIgnoreCase("text"))
                ret[0] = String.valueOf(response.getContent().length());
            else if (response.getType().equalsIgnoreCase("image")) {
                //Get file and upload
                ret = uploadAndShareFile(response.getId().concat(".jpg"), response.getFileUri(), true);
                publicURL = ret[1];
            } else if (response.getType().equalsIgnoreCase("video")) {
                //Get file and upload
                ret = uploadAndShareFile(response.getId().concat(".mp4"), response.getFileUri(), false);
                publicURL = ret[1];
            } else if (response.getType().equalsIgnoreCase("audio")) {
                //Get file and upload
                ret = uploadAndShareFile(response.getId().concat(".mp3"), response.getFileUri(), false);
                publicURL = ret[1];
            }

            if(!response.getType().equalsIgnoreCase("text"))
            {
                //Get direct link
                publicURL = SharcLibrary.getDropboxDirectLink(publicURL);
                response.setContent(publicURL);
                //Update URL for media in local
                selectedExperienceDetail.updatePublicURLForMedia(response.getId(), publicURL);
            }

            //Insert a new row into Media table
            DbxRecord responseMedia = authoringTable.getOrInsert(response.getId());
            responseMedia.set("type", response.getType()).set("name", response.getDesc()).set("attachedTo", response.getEntityType()).set("PoIID", response.getEntityID())
                    .set("desc", "").set("noOfLike", "0").set("content", response.getContent()).set("context", ret[0]);
            //Update the POI table
            DbxTable poiTable = mDatastore.getTable("POIs");
            DbxRecord poiRow = poiTable.getOrInsert(response.getEntityID());
            poiRow.set("mediaOrder", selectedExperienceDetail.getPOIFromID(response.getEntityID()).getStringMediaOrder());
        }
        mDatastore.sync();
        selectedExperienceDetail.removeUploadedResponseAt(index);
    }

    public String[] uploadAndShareFile(String fName, Uri fileUri, boolean isImage) throws Exception {
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
            out = mFile.getWriteStream();
            if(isImage)
            {
                //Bitmap bmp = BitmapFactory.decodeFile(fileUri.getPath());
                Bitmap bmp = MediaStore.Images.Media.getBitmap(this.getContentResolver(), fileUri);
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                bmp.compress(Bitmap.CompressFormat.JPEG, 70, bos);
                FileOutputStream fos = new FileOutputStream (new File(SharcLibrary.SHARC_MEDIA_FOLDER + "/sharc.tmp"));
                bos.writeTo(fos);
                fos.flush();
                fos.close();
                fileSize = String.valueOf(bos.size());
                in = new FileInputStream(SharcLibrary.SHARC_MEDIA_FOLDER + "/sharc.tmp");
            }
            else {
                in = (FileInputStream) getContentResolver().openInputStream(fileUri);
                fileSize = String.valueOf(in.available());
            }
            SharcLibrary.copyFile(in, out);
            out.close();
            in.close();
            mFile.close();
            //Share and get public link
            return new String[]{fileSize, dbxFs.fetchShareLink(path, false).toString()};
        }
        catch (Exception e) {
            e.printStackTrace();
            try {
                out.close();
                in.close();
                mFile.close();
                throw e;
            }
            catch (Exception ex)
            {
                throw ex;
            }
        }
    }

    //////////////////////////////////////////////////////////////////////////////
    // AUTHORING - DROPBOX
    //////////////////////////////////////////////////////////////////////////////
    public void createNewExperienceOnDropbox()
    {
        if(selectedExperienceDetail.getMetaData().getProPublicURL().contains("###SMAT###"))//Created by SMAP
        {
            try {
                //Insert two SQL tables
                TextView txtUseremail = (TextView) findViewById(R.id.txtUseremail);
                new UpdateUserExperiencesThread().execute(txtUseremail.getText().toString());
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    /*
        This inner class helps
            - Get information of all available online experiences
            - Present each experience as a marker on Google Maps
            - Add the Click listener even for each marker
        Note this class needs retrieve information from server so it has to run in background
    */
    class UpdateUserExperiencesThread extends AsyncTask<String, String, String>
    {
        //Before starting the background thread -> Show Progress Dialog
        private boolean isError = false;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(MainActivity.this);
            pDialog.setMessage("Creating a new experiences. Please wait...");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(false);
            pDialog.show();
        }

        //Update designer and experience info
        protected String doInBackground(String... args)
        {
            try
            {
                String designerEmail = args[0];
                //Connect to datastore
                DbxDatastoreManager mDatastoreManager = DbxDatastoreManager.forAccount(mDbxAcctMgr.getLinkedAccount());
                //Create
                DbxDatastore mDatastore  = mDatastoreManager.createDatastore();
                //Share
                mDatastore.setRole(DbxPrincipal.PUBLIC, DbxDatastore.Role.EDITOR);
                mDatastore.sync();
                ExperienceMetaDataModel metaData = selectedExperienceDetail.getMetaData();
                String proPath = mDatastore.getId();
                metaData.setProAuthName(dbUser.displayName);
                metaData.setProAuthEmail(designerEmail);
                //.openDatastore(selectedExperienceDetail.getMetaData().getProPath());
                mDatastore.sync();
                mDatastore.close();
                //Rename SQLite DB
                String oldPath = metaData.getProPath();
                File oldDB = new File(SharcLibrary.SHARC_DATABASE_FOLDER + File.separator + oldPath);
                File oldDBJournal = new File(SharcLibrary.SHARC_DATABASE_FOLDER + File.separator + oldPath + "-journal");
                if(oldDB.exists()){
                    File newDB = new File(SharcLibrary.SHARC_DATABASE_FOLDER + File.separator + proPath);
                    oldDB.renameTo(newDB);
                    File newDBJournal = new File(SharcLibrary.SHARC_DATABASE_FOLDER + File.separator + proPath + "-journal");
                    if(oldDBJournal.exists())
                        oldDBJournal.renameTo(newDBJournal);
                    metaData.setProPath(proPath);//Assign Dropbox path to current path for SQLite
                    //Re-assign newDB for the experience
                    selectedExperienceDetail.setExperienceDetailsDB(new DBExperienceDetails(MainActivity.this, metaData.getProPath()));
                    //Update metaDB
                    //experienceMetaDB.updateExperiencePath(oldPath, metaData.getProName(), metaData.getProPath(), metaData.getProDesc(), metaData.getProDate(), metaData.getProAuthName().concat(" (") + metaData.getProAuthEmail() + ")", metaData.getProPublicURL(), metaData.getProLocation());
                }
                JSONParser jParser = new JSONParser();
                List<NameValuePair> params = new ArrayList<NameValuePair>();
                //User info
                params.add(new BasicNameValuePair("auEmail",metaData.getProAuthEmail()));
                params.add(new BasicNameValuePair("auName",metaData.getProAuthName()));
                params.add(new BasicNameValuePair("auID",mDbxAcctMgr.getLinkedAccount().getUserId()));
                //Experience info
                params.add(new BasicNameValuePair("proName",metaData.getProName()));
                params.add(new BasicNameValuePair("proPath", metaData.getProPath()));//because path is local dbname which is different from dropbox datastore, desc temporally used to store datastore id
                params.add(new BasicNameValuePair("proDesc",""));
                params.add(new BasicNameValuePair("proAccess", "0"));
                params.add(new BasicNameValuePair("proAuthID",mDbxAcctMgr.getLinkedAccount().getUserId()));
                //update MySQL data
                JSONObject json = jParser.makeHttpRequest(url_updateDesignerExperience, "POST", params);
                //smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.VIEW_ONLINE_EXPERIENCES, logData);
                if (json.getInt("success") == 1) {
                    metaData.setProPublicURL("");
                    //save to database
                    experienceMetaDB.updateExperiencePath(oldPath, metaData.getProName(), metaData.getProPath(), metaData.getProDesc(), metaData.getProDate(), metaData.getProAuthName().concat(" (") + metaData.getProAuthEmail() + ")", metaData.getProPublicURL(), metaData.getProLocation());
                }
                else
                    isError = true;
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
            runOnUiThread(new Runnable() {
                public void run() {
                    if (isError) {
                        Toast.makeText(MainActivity.this, getString(R.string.message_create_db_error), Toast.LENGTH_LONG).show();
                        processTab(0);
                    }
                }
            });
        }
    }

    public void addNewPOI()
    {
        //smepInteractionLog.takeScreenShot();
        final String latLng = initialLocation != null ? initialLocation.latitude + " " + initialLocation.longitude : null;
        final AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Create a new POI (GPS accuracy for this POI's position is " + String.format("%.0f", lastKnownLocation.getAccuracy()) + "m. You can use SLAT to reposition POI later if necessary)");
        //alert.setCancelable(false);
        final View experienceEntryView = LayoutInflater.from(this).inflate(R.layout.new_poi_dialog, null);
        alert.setView(experienceEntryView);
        /*final NumberPicker np= (NumberPicker) experienceEntryView.findViewById(R.id.numberPickerTriggerzone);
        np.setMaxValue(100);
        np.setMinValue(0);
        np.setValue(70);*/

        alert.setPositiveButton("Create", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                EditText editTextPoiName = (EditText) experienceEntryView.findViewById(R.id.editTextPoiName);
                String poiName = editTextPoiName.getText().toString().trim();
                String triggerzoneSize = "20";
                String poiType = "";
                RadioGroup rgPoiType = (RadioGroup)experienceEntryView.findViewById(R.id.radioPoiType);
                int selectedID = rgPoiType.getCheckedRadioButtonId();
                String selRadioText = ((RadioButton)experienceEntryView.findViewById(selectedID)).getText().toString();
                if(selRadioText.contains("Normal"))
                    poiType = SharcLibrary.SHARC_POI_NORMAL;
                else if(selRadioText.contains("Accessibility"))
                    poiType = SharcLibrary.SHARC_POI_ACCESSIBILITY;

                //else
                //    triggerzoneSize = "" + np.getValue();

                if (poiName.equalsIgnoreCase("")) {
                    Toast.makeText(MainActivity.this, "Please enter POI name", Toast.LENGTH_LONG).show();
                    addNewPOI();
                } else {

                    String id = String.valueOf(new Date().getTime());
                    String triggerZone = "circle 00ff00 " + triggerzoneSize + " " + latLng;
                    //add new POI and set selected POI
                    currentPOIIndex = selectedExperienceDetail.addNewPOI(id, poiName, poiType, "", latLng, "", "", "", triggerZone, mMap, (SMEPAppVariable) getApplicationContext());
                    ResponseModel res = new ResponseModel(id, "Waiting", "text", "", poiName, "NEW", latLng, "", "NEW", "NEW");//Desc is used to store trigger zone
                    selectedExperienceDetail.addMyResponse(res);
                    smepInteractionLog.addLog(latLng, mDbxAcctMgr, InteractionLog.CREATE_POI, poiName);
                    //Go to the POI tab
                    switchToPOIMediaTab("AFTER_NEW_POI");
                    //Open add response dialog
                    addResponse(null);
                }
            }
        });
        alert.setNegativeButton("Cancel", null);
        setDialogFontSizeAndShow(alert, FONT_SIZE);
        //alert.show();
    }

    public void startRecordRoute(View v)
    {
        if(selectedExperienceDetail == null)
        {
            Toast.makeText(this, "You need to create or open an experience first", Toast.LENGTH_LONG).show();
            return;
        }
        Button btnStart = (Button)v;

        if(btnStart.getText().toString().contains("Start"))
        {
            if(initialLocation == null)
            {
                Toast.makeText(this, R.string.message_gps, Toast.LENGTH_LONG).show();
                return;
            }
            else
                showEnterRouteNameDialog();
        }
        else if(btnStart.getText().toString().contains("Pause"))
        {
            isRecording = false;
            btnStart.setText("Resume recording route");
            btnStart.setBackgroundResource(R.drawable.custom_btn_orange);
            smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.PAUSE_ROUTE, curRoute.getName());
        }
        else if(btnStart.getText().toString().contains("Resume"))
        {
            isRecording = true;
            btnStart.setText("Pause recording route");
            btnStart.setBackgroundResource(R.drawable.custom_btn_blue);
            smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.RESUME_ROUTE, curRoute.getName());
        }
    }

    public void showEnterRouteNameDialog()
    {
        final AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Add a new route (GPS accuracy for the starting point is " + String.format("%.0f", lastKnownLocation.getAccuracy()) + "m. You can use SLAT to edit the route later if necessary)");
        final LatLng startPoint = SharcLibrary.locationToLatLng(lastKnownLocation);
        //alert.setCancelable(false);
        alert.setMessage("Please enter route name (You will be asked for more details when you finish the route. Note: if you encounter likely accessibility obstacles on the route please create POIs for these.)");
        final EditText etRouteName = new EditText(this);
        etRouteName.setInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        etRouteName.setText(selectedExperienceDetail.getMetaData().getProName());
        alert.setView(etRouteName);

        alert.setPositiveButton("Create", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String routeName = etRouteName.getText().toString().trim();
                if (routeName.equalsIgnoreCase("")) {
                    Toast.makeText(MainActivity.this, "Please enter Route name", Toast.LENGTH_LONG).show();
                    showEnterRouteNameDialog();
                } else {
                    isRecording = true;
                    btnStartRoute.setText("Pause recording route");
                    btnStartRoute.setBackgroundResource(R.drawable.custom_btn_blue);
                    btnStopRoute.setEnabled(true);
                    btnStopRoute.setBackgroundResource(R.drawable.custom_btn_blue);
                    startRoute = mMap.addMarker(new MarkerOptions()
                                    .anchor(0.5f, 0.5f)
                                    .position(startPoint)
                                    .icon(BitmapDescriptorFactory.fromResource(R.raw.start))
                                    .visible(true)
                    );
                    curRoute = new RouteModel();
                    curRoute.setName(routeName);
                    curRoute.setDesc(getString(R.string.message_route_recording));
                    curRoute.getPath().add(startPoint);
                    prevLocation = lastKnownLocation;
                    selectedExperienceDetail.addNewRoute(curRoute);
                    smepInteractionLog.addLog(startPoint, mDbxAcctMgr, InteractionLog.START_ROUTE, routeName);
                }
            }
        });
        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

            }
        });
        setDialogFontSizeAndShow(alert, FONT_SIZE);
        //alert.show();
    }

    public void stopRecordRoute(View v)
    {
        btnStopRoute.setEnabled(false);
        isRecording = false;
        v.setBackgroundResource(R.drawable.custom_btn_beige);
        btnStartRoute.setText("Start recording route");
        btnStartRoute.setBackgroundResource(R.drawable.custom_btn_orange);
        endRoute = mMap.addMarker(new MarkerOptions()
                        .title("END")
                        .anchor(0.5f, 0.5f)
                        .position(curRoute.getLastPointOfPath())
                        .icon(BitmapDescriptorFactory.fromResource(R.raw.end))
                        .visible(true)
        );
        smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.STOP_ROUTE, curRoute.getName());
        //Show dialog for name input
        showEnterRouteDetailDialog();
    }

    public void showEnterRouteDetailDialog()
    {
        final AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Save this route (" + String.format("%.2f", curRoute.getDistance()) + " km)");
        //alert.setCancelable(false);
        final View routeEntryView = LayoutInflater.from(this).inflate(R.layout.route_save_dialog, null);
        alert.setView(routeEntryView);
        final EditText etRouteName = (EditText) routeEntryView.findViewById(R.id.editTextRouteName);
        etRouteName.setText(curRoute.getName());
        alert.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String routeName = etRouteName.getText().toString().trim();
                if (routeName.equalsIgnoreCase("")) {
                    Toast.makeText(MainActivity.this, "Please enter Route name", Toast.LENGTH_LONG).show();
                    showEnterRouteDetailDialog();
                } else {
                    //Save to SQLite database
                    //Get route description
                    EditText etRouteDesc = (EditText) routeEntryView.findViewById(R.id.editTextRouteDescription);
                    curRoute.setDesc("");
                    String desc = etRouteDesc.getText().toString().trim();
                    if (desc.length() > 0 && desc.charAt(desc.length() - 1) != '.')
                        desc = desc + ".";
                    List<String> routeType = new ArrayList<String>();
                    CheckBox chkWalk = (CheckBox) routeEntryView.findViewById(R.id.checkBoxWalk);
                    if (chkWalk.isChecked())
                        routeType.add("walk");
                    CheckBox chkCycle = (CheckBox) routeEntryView.findViewById(R.id.checkBoxCycle);
                    if (chkCycle.isChecked())
                        routeType.add("cycle");
                    CheckBox chkDrive = (CheckBox) routeEntryView.findViewById(R.id.checkBoxDrive);
                    if (chkDrive.isChecked())
                        routeType.add("drive");
                    if (routeType.size() == 1)
                        desc += " This route is suitable for a " + routeType.get(0);
                    else if (routeType.size() == 2)
                        desc += " This route is suitable for a " + routeType.get(0) + " or " + routeType.get(1);
                    else if (routeType.size() == 3)
                        desc += " This route is suitable for a" + routeType.get(0) + ", " + routeType.get(1) + " or " + routeType.get(2);
                    desc += ".";
                    //Route
                    curRoute.setName(routeName);
                    curRoute.setDesc(desc);
                    selectedExperienceDetail.updateRoute(curRoute);
                    //Response
                    ResponseModel res = new ResponseModel(curRoute.getId(), "Waiting", "text", "", curRoute.getName(), "ROUTE", "", "", "NEW", "NEW");//Desc is used to store trigger zone
                    selectedExperienceDetail.addMyResponse(res);
                    curRoutePath.clear();
                    smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.SAVE_ROUTE, routeName);
                }
            }
        });
        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                startRoute.remove();
                endRoute.remove();
                //Remove path on map
                for (int i = 0; i < curRoutePath.size(); i++)
                    curRoutePath.get(i).remove();
                curRoutePath.clear();
                selectedExperienceDetail.getMetaData().setRouteLength(0);
                selectedExperienceDetail.removeRoute(curRoute);
                //smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.SAVE_ROUTE, routeName);
            }
        });
        setDialogFontSizeAndShow(alert, FONT_SIZE);
        //alert.show();
    }

    public void showPublishExperienceDialog()
    {
        final AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Publish the experience");
        //alert.setCancelable(false);
        alert.setMessage("Would you like to publish the experience now? Note that Dropbox may take several minutes to synchronise uploaded data so please wait a while before using SMEP and SLAT to view the experience!");
        alert.setPositiveButton("Yes, publish now", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                fileUri = null;
                if (dbUser == null) {
                    Toast.makeText(MainActivity.this, getString(R.string.message_dropboxConnection), Toast.LENGTH_LONG).show();
                    return;
                } else if (!SharcLibrary.isNetworkAvailable(MainActivity.this)) {
                    Toast.makeText(MainActivity.this, getString(R.string.message_wifiConnection), Toast.LENGTH_LONG).show();
                    return;
                } else {
                    showSubmitThumbnailOptionWhenPublish();
                }
            }
        });
        alert.setNegativeButton("No, I will publish later", null);
        setDialogFontSizeAndShow(alert, FONT_SIZE);
    }

    public void showSubmitThumbnailOptionWhenPublish()
    {
        final AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Publish the experience");
        //alert.setCancelable(false);
        alert.setMessage("Would you like to include a representative thumbnail image for your experience?");
        alert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                showSelectThumbnailDialog();
            }
        });
        alert.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                new UpdatePublishedExperiencesThread().execute("");
            }
        });
        alert.setNeutralButton("Cancel", null);
        setDialogFontSizeAndShow(alert, FONT_SIZE);
    }

    public void showSelectThumbnailDialog()
    {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        //alert.setCancelable(false);
        alert.setTitle("Add a representative thumbnail image by");
        alert.setItems(R.array.selecting_image, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        fileUri = Uri.fromFile(new File(SharcLibrary.SHARC_MEDIA_FOLDER, "tmp_avatar.jpg"));
                        intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, fileUri);
                        try {
                            intent.putExtra("return-data", true);
                            startActivityForResult(intent, PICK_FROM_CAMERA);
                        } catch (ActivityNotFoundException e) {
                            e.printStackTrace();
                        }
                        break;
                    case 1:
                        Intent intentSelect = new Intent(Intent.ACTION_GET_CONTENT);
                        Uri uri = Uri.parse(SharcLibrary.SHARC_MEDIA_FOLDER);
                        intentSelect.setDataAndType(uri, "image/*");
                        startActivityForResult(Intent.createChooser(intentSelect, "Complete action using"), PICK_FROM_FILE);
                        break;
                }

            }
        });
        alert.show();
    }

    public void cropImage(boolean isFromFile) {
        Intent cropIntent = new Intent(MainActivity.this, CropActivity.class);
        String imagePath = "";
        if(isFromFile)
            imagePath = getImagePath(fileUri);
        else
            imagePath = fileUri.getPath();
        cropIntent.putExtra("from", imagePath);
        cropIntent.putExtra("to", SharcLibrary.SHARC_MEDIA_FOLDER + File.separator + selectedExperienceDetail.getMetaData().getProPath() + ".png");
        startActivityForResult(cropIntent, CROP_IMAGE);
    }

    public String getImagePath(Uri uri){
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        cursor.moveToFirst();
        String document_id = cursor.getString(0);
        document_id = document_id.substring(document_id.lastIndexOf(":")+1);
        cursor.close();

        cursor = getContentResolver().query(
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                null, MediaStore.Images.Media._ID + " = ? ", new String[]{document_id}, null);
        cursor.moveToFirst();
        String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
        cursor.close();

        return path;
    }

    public void setDialogFontSizeAndShow(AlertDialog.Builder alert, final float fontSize)
    {
        AlertDialog alertDialog = alert.create();
        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                AlertDialog alertDialog = (AlertDialog) dialog;
                Button button = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
                //button.setTypeface(Typeface.DEFAULT, Typeface.BOLD | Typeface.ITALIC);
                button.setTextSize(fontSize);
                button = alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE);
                button.setTextSize(fontSize);
                button = alertDialog.getButton(DialogInterface.BUTTON_NEUTRAL);
                button.setTextSize(fontSize);
            }
        });

        alertDialog.show();
    }

    /*
        This inner class helps
            - Update meta data for a published experience
        Note this class needs retrieve information from server so it has to run in background
    */
    class UpdatePublishedExperiencesThread extends AsyncTask<String, String, String>
    {
        //Before starting the background thread -> Show Progress Dialog
        private boolean isError = false;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(MainActivity.this);
            pDialog.setMessage("Publishing the experience. Please wait...");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(false);
            pDialog.show();
        }

        //Update designer and experience info
        protected String doInBackground(String... args)
        {
            DbxFileSystem dbxFs = null;
            DbxFile testFile = null;
            String imagePath = args[0];
            try
            {
                dbxFs = DbxFileSystem.forAccount(mDbxAcctMgr.getLinkedAccount());
                String proPath = selectedExperienceDetail.getMetaData().getProPath();
                DbxPath path = new DbxPath(proPath + ".json");
                if(dbxFs.isFile(path))
                    testFile = dbxFs.open(path);
                else
                    testFile = dbxFs.create(path);
                testFile.writeString(selectedExperienceDetail.getJSON());
                String publicPath = dbxFs.fetchShareLink(path, false).toString();

                String publicRepThumbnail = "";
                if(!imagePath.equalsIgnoreCase(""))
                {
                    File mediaFile = new File(imagePath);
                    publicRepThumbnail = uploadAndShareFile(proPath + ".png",Uri.fromFile(mediaFile), false)[1];//image but compressed already so false
                    publicRepThumbnail = SharcLibrary.getDropboxDirectLink(publicRepThumbnail);
                }
                LatLng proLocation = selectedExperienceDetail.getGeographicalBoundary().getCenter();
                if(proLocation == null)
                    proLocation = new LatLng(0.0, 0.0);

                //Get size
                String exSize = getExperienceSize();


                JSONParser jParser = new JSONParser();
                List<NameValuePair> params = new ArrayList<NameValuePair>();
                //User info
                params.add(new BasicNameValuePair("proPath",proPath));
                params.add(new BasicNameValuePair("proAccess","1#0"));
                params.add(new BasicNameValuePair("proDesc",selectedExperienceDetail.getMetaData().getProDesc()));
                params.add(new BasicNameValuePair("proSummary",selectedExperienceDetail.getOverallSummary()));
                params.add(new BasicNameValuePair("proPublicURL",SharcLibrary.getDropboxDirectLink(publicPath)));
                params.add(new BasicNameValuePair("proLocation",proLocation.latitude + " " + proLocation.longitude));
                params.add(new BasicNameValuePair("proThumbnail",publicRepThumbnail));
                params.add(new BasicNameValuePair("proSize",exSize));
                //update MySQL data
                JSONObject json = jParser.makeHttpRequest(url_updatePublishedExperience, "POST", params);
                if (json.getInt("success") != 1)
                    isError = true;
                smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.SELECT_UPLOAD_ALL, "");
            }
            catch (Exception e) {
                e.printStackTrace();
                isError = true;
            }
            finally {
                if(testFile != null)
                testFile.close();
            }
            return null;
        }

        //After completing background task ->Dismiss the progress dialog
        protected void onPostExecute(String file_url)
        {
            // dismiss the dialog after getting all files
            pDialog.dismiss();
            // updating UI from Background Thread
            runOnUiThread(new Runnable()
            {
                public void run()
                {
                    if(!isError)
                        Toast.makeText(MainActivity.this, "Your experience has been successfully published", Toast.LENGTH_LONG).show();
                    else
                        Toast.makeText(MainActivity.this, "There were some error when publishing your experience. Please try again later", Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    public String getExperienceSize()
    {
        DbxDatastore mDatastore = null;
        try {
            //Connect to datastore
            DbxDatastoreManager mDatastoreManager = DbxDatastoreManager.forAccount(mDbxAcctMgr.getLinkedAccount());
            String proPath = selectedExperienceDetail.getMetaData().getProPath();
            mDatastore = mDatastoreManager.openDatastore(proPath);
            DbxTable mediaTable = mDatastore.getTable("media");
            DbxTable.QueryResult results = mediaTable.query();
            List<DbxRecord> records = results.asList();
            if(records != null) {
                double size = 0;
                String tmpSize = "";
                for(int i = 0; i < records.size(); i++) {
                    tmpSize = records.get(i).getString("context").trim();
                    if (tmpSize == "null" || tmpSize == null || tmpSize.equalsIgnoreCase(""))
                        size += 0;
                    else
                        size += Double.parseDouble(tmpSize);
                }
                size /= 1024*1024;//convert to MB
                if (size == 0)
                    size = 40;
                mDatastore.sync();
                mDatastore.close();
                return String.format("%.2f", size);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            if(mDatastore != null)
            {
                mDatastore.close();
            }
        }
        return "40";
    }

    public  void checkAndReportCrash() {
        final ErrorReporter reporter = ErrorReporter.getInstance();
        reporter.init(this);
        if (reporter.isThereAnyErrorFile()) {
            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setTitle("Send Error Log");
            alert.setMessage("A previous crash was reported. Would you like to send the developer the error log to fix this issue in the future?");
            alert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    reporter.sendReportEmail();
                }
            });
            alert.setNegativeButton("No", null);
            setDialogFontSizeAndShow(alert, FONT_SIZE);
        }
    }
    public CloudManager getCloudManager() {
        return cloudManager;
    }

    public RestfulManager getRestfulManager() {
        return restfulManager;
    }

}