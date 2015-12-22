package uk.lancs.sharc.smat.model;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Hashtable;
import java.util.List;

/**
 * <p>This class is a model of the EOI entity</p>
 * <p>It can be changed later depending on future work </p>
 *
 * Author: Trien Do
 * Date: Feb 2015
 **/
public class EOIModel {
	private String id;
	private String name;
	private String description;
	private String mediaHTMLCode;//All media of a EOI is presented as a HTML page in a webview
	private String[] mediaOrder;
	
	public EOIModel(String mID, String mName, String mDescription, String mMediaHTMLCode, String mediaOrderString)
	{
		id = mID;
		name = mName;
		description = mDescription;
		mediaHTMLCode = mMediaHTMLCode;
		if (mediaOrderString.equalsIgnoreCase(""))
			this.setMediaOrder(null);
		else
			mediaOrder = mediaOrderString.split(" ");
	}

	public String getHTMLPresentation(List<MediaModel> mediaList)
	{
		String htmlContent = "";
		Hashtable<String, String> mediaIDList = new Hashtable<String, String>();
		if(mediaList != null && mediaList.size() > 0)
		{
			for (MediaModel media : mediaList)
			{
				String strMedia = media.getHTMLPresentation();
				htmlContent += strMedia;
				mediaIDList.put(media.getId(), strMedia);
			}
            if(mediaOrder != null && mediaOrder.length > 0)
            {
                htmlContent = "";//Reorder media
                for(int i = 0; i < mediaOrder.length; i++)
                {
                    if(mediaIDList.get(mediaOrder[i])!=null)
                        htmlContent += mediaIDList.get(mediaOrder[i]);
                }
            }
		}
        this.mediaHTMLCode = htmlContent;
		return htmlContent;
	}

	public String getName() {
        try {
            return URLDecoder.decode(name,"UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
        try {
            return URLDecoder.decode(description,"UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getMediaHTMLCode() {
		return mediaHTMLCode;
	}

	public void setMediaHTMLCode(String mediaHTMLCode) {
		this.mediaHTMLCode = mediaHTMLCode;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String[] getMediaOrder() {
		return mediaOrder;
	}

	public void setMediaOrder(String[] mediaOrder) {
		this.mediaOrder = mediaOrder;
	}

}
