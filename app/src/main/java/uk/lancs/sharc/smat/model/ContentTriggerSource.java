package uk.lancs.sharc.smat.model;

import android.app.Activity;
import android.content.Context;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Vibrator;
import android.util.Log;

import java.util.Hashtable;

import uk.lancs.sharc.smat.controller.MainActivity;


/**
 * Created by SHARC on 08/01/2016.
 */
public abstract class ContentTriggerSource {
    public static final int SOURCE_GPS = 0;
    public static final int SOURCE_POI_TAP = 1;
    public static final int SOURCE_MAP_TOUCH = 2;
    public static final int SOURCE_BEACON = 3;
    public static final int SOURCE_COMMANDER = 4;

    public static final int RENDER_MODE_POI = 1;
    public static final int RENDER_MODE_MEDIA = 2;

    protected int contentTriggerSourceType;
    private Activity activity;
    protected Context context;
    protected Hashtable<Integer,Long> shownLocation;

    public abstract Hashtable<Integer,Long> findSelectedContent();//to deal with push media again or not when revisiting

    public ContentTriggerSource(Activity activity, Context context, Hashtable<Integer,Long> listOfContentPushedPreviously){
        this.activity = activity;
        this.context = context;
        this.shownLocation = listOfContentPushedPreviously;
    }

    protected void markSelectedPoi(int i)
    {
        SMEPAppVariable mySMEPAppVariable = (SMEPAppVariable) context.getApplicationContext();
        mySMEPAppVariable.setNewMedia(true);//Mark that there are new media so Main UI can render them
        mySMEPAppVariable.setNewMediaIndex(i);
        if(mySMEPAppVariable.isVibrationNotification())
        {
            Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            v.vibrate(1000);
        }
        if(mySMEPAppVariable.isSoundNotification())
        {
            try {
                Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                Ringtone r = RingtoneManager.getRingtone(context.getApplicationContext(), notification);
                r.play();
            }
            catch (Exception e) {
                Log.e("ContentTriggerSource", "Can't play sound: " + e.getLocalizedMessage());
                e.printStackTrace();
            }
        }
    }

    public Hashtable<Integer,Long> renderContent(){
        shownLocation = findSelectedContent();
        ((MainActivity)activity).renderMedia(contentTriggerSourceType, ContentTriggerSource.RENDER_MODE_POI,-1);//as mediaIndex is not required
        return shownLocation;
    }

    /*public Hashtable<Integer,Long> renderContent(){
        shownLocation = findSelectedContent();
        switch(contentTriggerSourceType){
            case ContentTriggerSource.SOURCE_GPS:
                ((MainActivity)activity).renderMedia(contentTriggerSourceType, ContentTriggerSource.RENDER_MODE_POI,-1);//as mediaIndex is not required
                break;
            case ContentTriggerSource.SOURCE_POI_TAP:
                ((MainActivity)activity).renderMedia(contentTriggerSourceType, ContentTriggerSource.RENDER_MODE_POI,-1);//as mediaIndex is not required
                break;
            case ContentTriggerSource.SOURCE_MAP_TOUCH:
                ((MainActivity)activity).renderMedia(contentTriggerSourceType, ContentTriggerSource.RENDER_MODE_POI,-1);//as mediaIndex is not required
                break;
            case ContentTriggerSource.SOURCE_BEACON:
                ((MainActivity)activity).renderMedia(contentTriggerSourceType, ContentTriggerSource.RENDER_MODE_POI,-1);//as mediaIndex is not required
                break;
            case ContentTriggerSource.SOURCE_COMMANDER:
                ((MainActivity)activity).renderMedia(contentTriggerSourceType, ContentTriggerSource.RENDER_MODE_MEDIA, ((CommanderContentTriggerSource) this).getMediaIndex());//as mediaIndex is REQUIRED
                break;
        }
        return shownLocation;
    }*/
}
