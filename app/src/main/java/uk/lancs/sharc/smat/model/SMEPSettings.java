package uk.lancs.sharc.smat.model;
/**
 * <p>This class holds settings of SMEP and provide setters/getters for these
 * configurations (e.g., selected view mode of Goolge maps, sound notification)</p>
 * <p>Users can change these configurations from state buttons of the sliding menu</p>
 *
 * Author: Trien Do
 * Date: Feb 2014
 */
public class SMEPSettings {
	private boolean isSatellite;//Map view mode
	private boolean isShowingTriggers;//Trigger view mode
	private boolean isShowingThumbnails;//Trigger view mode
	private boolean isRotating; //Rotate the map or not	
	private boolean isTestMode; //Rotate the map or not
	private boolean isPushingMedia;//Push mode
	private boolean isShowingGPS;//Show GPS info on title bar
	private boolean isSoundNotification;
	private boolean isVibrationNotification;
	private boolean isYAHCentred;
	private String 	appVersion;
	
	public SMEPSettings()
	{
		isSatellite = false;//Map view mode
		isShowingTriggers = true;//Trigger view mode
		isShowingThumbnails = true;//Trigger view mode
		isRotating = false; //Rotate the map or not	
		isTestMode = false; //Rotate the map or not
		isPushingMedia = false;
		isSoundNotification = true;
		isVibrationNotification = true;
		setYAHCentred(false);
		appVersion = "1.0";
		isShowingGPS = true;
	}

	public boolean isShowingGPS() {
		return isShowingGPS;
	}

	public void setIsShowingGPS(boolean isShowingGPS) {
		this.isShowingGPS = isShowingGPS;
	}

	public boolean isSatellite() {
		return isSatellite;
	}

	public void setSatellite(boolean isSatellite) {
		this.isSatellite = isSatellite;
	}

	public boolean isShowingTriggers() {
		return isShowingTriggers;
	}

	public void setShowingTriggers(boolean isShowingTriggers) {
		this.isShowingTriggers = isShowingTriggers;
	}

	public boolean isShowingThumbnails() {
		return isShowingThumbnails;
	}

	public void setShowingThumbnails(boolean isShowingThumbnails) {
		this.isShowingThumbnails = isShowingThumbnails;
	}

	public boolean isRotating() {
		return isRotating;
	}

	public void setRotating(boolean isRotating) {
		this.isRotating = isRotating;
	}

	public boolean isTestMode() {
		return isTestMode;
	}

	public void setTestMode(boolean isTestMode) {
		this.isTestMode = isTestMode;
	}

	public boolean isPushingMedia() {
		return isPushingMedia;
	}

	public void setPushingMedia(boolean isPushingMedia) {
		this.isPushingMedia = isPushingMedia;
	}

	public boolean isSoundNotification() {
		return isSoundNotification;
	}

	public void setSoundNotification(boolean isSoundNotification) {
		this.isSoundNotification = isSoundNotification;
	}

	public boolean isVibrationNotification() {
		return isVibrationNotification;
	}

	public void setVibrationNotification(boolean isVibrationNotification) {
		this.isVibrationNotification = isVibrationNotification;
	}

	public boolean isYAHCentred() {
		return isYAHCentred;
	}

	public void setYAHCentred(boolean isYAHCentred) {
		this.isYAHCentred = isYAHCentred;
	}

	public String getAppVersion() {
		return appVersion;
	}

	public void setAppVersion(String appVersion) {
		this.appVersion = appVersion;
	}
}
