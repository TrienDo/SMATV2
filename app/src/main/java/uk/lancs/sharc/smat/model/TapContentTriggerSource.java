package uk.lancs.sharc.smat.model;

import android.app.Activity;
import android.content.Context;

import java.util.Hashtable;

/**
 * Created by SHARC on 08/01/2016.
 */
public class TapContentTriggerSource extends ContentTriggerSource {
    private int selectedPoi;
    public TapContentTriggerSource(int poiIndex, Context context, Activity activity, int tapType){
        super(activity, context,  null);
        selectedPoi = poiIndex;
        contentTriggerSourceType = tapType;
    }

    @Override
    public Hashtable<Integer,Long> findSelectedContent() {
        markSelectedPoi(selectedPoi);
        return shownLocation;
    }
}
