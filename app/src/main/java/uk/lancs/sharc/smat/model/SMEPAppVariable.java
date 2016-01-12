package uk.lancs.sharc.smat.model;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.Application;
import android.location.Location;

import com.orm.SugarApp;

/**
 * <p>This class holds global variables which can be accessed from anywhere in SMEP</p>
 * <p>To use this class, just get the Application context
 * SMEPAppVariable mySMEPAppVariable = (SMEPAppVariable) getApplicationContext();
 * Then use its getters and setters
 * </p>
 *
 * Author: Trien Do
 * Date: Feb 2014
 */
public class SMEPAppVariable extends SugarApp {
	private boolean testMode;
	private boolean isNewExperience;
	private List<POIModel> allPOIs;
	private boolean resetPOI;
	private boolean isNewMedia;
	private boolean isSoundNotification;
	private boolean isVibrationNotification;
	private boolean isPushAgain;
	private int	newMediaIndex;
	private Location mockLocation;
	private long timeThreshold;					//Push media again if the user come back after more than this threshold (in minute, default = 5 mis)
	private Activity activity;
	public SMEPAppVariable()
	{
		isNewExperience = false;
		testMode = false;
		resetPOI = false;
		isNewMedia = false;
		isSoundNotification = true;
		isVibrationNotification = true;
		isPushAgain = false;
		newMediaIndex = 0;
		mockLocation = new Location("");
		mockLocation.setLatitude(54.102060001);
		mockLocation.setLongitude(-2.608550001);
		timeThreshold = 5*60*1000; //default = 5 mins * 60 seconds * 1000 milliseconds
		activity = null;
	}

	public Activity getActivity() {
		return activity;
	}

	public void setActivity(Activity activity) {
		this.activity = activity;
	}

	public boolean isTestMode() {
		return testMode;
	}
	public void setTestMode(boolean testMode) {
		this.testMode = testMode;
	}

	public Location getMockLocation() {
		return mockLocation;
	}

	public void setMockLocation(Location mockLocation) {
		this.mockLocation = mockLocation;
	}

	public boolean isResetPOI() {
		return resetPOI;
	}

	public void setResetPOI(boolean resetPOI) {
		this.resetPOI = resetPOI;
	}

	public boolean isNewMedia() {
		return isNewMedia;
	}

	public void setNewMedia(boolean isNewMedia) {
		this.isNewMedia = isNewMedia;
	}

	public int getNewMediaIndex() {
		return newMediaIndex;
	}

	public void setNewMediaIndex(int newMediaIndex) {
		this.newMediaIndex = newMediaIndex;
	}

	public boolean isSoundNotification() {
		return isSoundNotification;
	}

	public void setSoundNotification(boolean mSoundNotification) {
		isSoundNotification = mSoundNotification;
	}

	public boolean isVibrationNotification() {
		return isVibrationNotification;
	}

	public void setVibrationNotification(boolean mVibrationNotification) {
		isVibrationNotification = mVibrationNotification;
	}

	public boolean isNewExperience() {
		return isNewExperience;
	}

	public void setNewExperience(boolean isNewExperience) {
		this.isNewExperience = isNewExperience;
	}

	public List<POIModel> getAllPOIs() {
		return allPOIs;
	}

	public void setAllPOIs(List<POIModel> allPOIs) {
		this.allPOIs = allPOIs;
	}

	public boolean isPushAgain() {
		return isPushAgain;
	}

	public void setIsPushAgain(boolean isPushAgain) {
		this.isPushAgain = isPushAgain;
	}

	public long getTimeThreshold() {
		return timeThreshold;
	}

	public void setTimeThreshold(long timeThreshold) {
		this.timeThreshold = timeThreshold;
	}
}
