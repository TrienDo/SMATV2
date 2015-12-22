package uk.lancs.sharc.smat.model;

import java.util.List;

import uk.lancs.sharc.smat.R;
import uk.lancs.sharc.smat.controller.MainActivity;
import uk.lancs.sharc.smat.service.SharcLibrary;

import android.app.Activity;
import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

/**
 * <p>This class instructs how the media list view should be rendered</p>
 *
 * Author: Trien Do
 * Date: Feb 2015
 **/
public class MediaListAdapter extends ArrayAdapter<String>
{
	private final Activity context;
	private final List<String> mediaList;
	private final int listType;//0 - media, 1 - eoi, 2 - summary info (or route or experience)
	private final ExperienceDetailsModel experienceDetails;
	public MediaListAdapter(Activity context, List<String> mediaList, int mListType)
	{
		super(context, R.layout.media_list_item, mediaList);
		this.context = context;
		this.mediaList = mediaList;
		this.listType = mListType;
		MainActivity activity = (MainActivity)context;
		experienceDetails = activity.getSelectedExperienceDetail();
	}
		
	@Override
	public View getView(int position, View view, ViewGroup parent) 
	{
		LayoutInflater inflater = context.getLayoutInflater();
		View rowView= inflater.inflate(R.layout.media_list_item, null, true);

		String countLine = "<h5 style='margin-left:20px;'>[Media item " + position + " of " + (mediaList.size()-1) + "]</h5>";
		//String countLine = "<h5 style='margin-left:20px;'>[Media item " + position + " of 1]</h5>";
		//Get number of like and comment
		String htmlCode = mediaList.get(position);

		Button btnBack = (Button) rowView.findViewById(R.id.btnBackToMap);

		if(position==0 || listType == 1)
		{
			btnBack.setVisibility(View.GONE);
			countLine = "";
		}
		else
		{
			htmlCode = htmlCode.substring(htmlCode.indexOf("<span"),htmlCode.indexOf("</span>"));////000#id#1111#type#noLike#noComments#therest
			btnBack.setTag(position);
		}

		if(position == mediaList.size() - 1)
			btnBack.setVisibility(View.VISIBLE);
		else
			btnBack.setVisibility(View.GONE);

		if(listType != 0)
			countLine = "";
		//Content in webview
		WebView webviewMedia = (WebView) rowView.findViewById(R.id.webViewMedia);		
		String base = "file://" + SharcLibrary.SHARC_MEDIA_FOLDER + "/";
		if(listType == 0 && position == 0)//eoi
		{
			AndroidWebViewInterface inObj = new AndroidWebViewInterface(context);
			SharcLibrary.setupWebView(webviewMedia, context, inObj);
		} else
			SharcLibrary.setupWebView(webviewMedia, context);
		webviewMedia.loadDataWithBaseURL(base, countLine + mediaList.get(position), "text/html", "utf-8",null);

		btnBack.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				MainActivity activity = (MainActivity)context;
				activity.displayMapTab();
			}
		});
		return rowView;
	}
	@Override
	public void unregisterDataSetObserver(DataSetObserver observer) {
		if (observer != null) {
			super.unregisterDataSetObserver(observer);
		}
	}
}