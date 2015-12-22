package uk.lancs.sharc.smat.model;

import com.orm.SugarRecord;
import com.orm.dsl.Ignore;

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
public class EOIModel extends SugarRecord {
	//@Unique
	private Long mid;
	private Long designerId;
	private Long experienceId;
	private String name;
	private String description;
	private String poiList;
	private String routeList;

	@Ignore
	private String mediaHTMLCode;

	public EOIModel(){
	}

	public EOIModel(Long id, Long designerId, Long experienceId, String name, String description, String poiList, String routeList){
		this.mid = id;
		this.designerId = designerId;
		this.experienceId = experienceId;
		this.name = name;
		this.description = description;
		this.poiList = poiList;
		this.routeList = routeList;
	}

	//All media of a EOI is presented as a HTML page in a webview
	public String getHTMLPresentation(List<MediaModel> mediaList)
	{
		String htmlContent = "";
		if(mediaList != null && mediaList.size() > 0)
		{
			for (MediaModel media : mediaList)
			{
				String strMedia = media.getHTMLPresentation();
				htmlContent += strMedia;
			}
		}
		mediaHTMLCode = htmlContent;
		return htmlContent;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
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

	public Long getId() {
		return mid;
	}

	public void setId(Long id) {
		this.mid = id;
	}
}
