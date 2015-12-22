package uk.lancs.sharc.smat.model;

import java.util.ArrayList;
import java.util.List;

import uk.lancs.sharc.smat.R;
import uk.lancs.sharc.smat.controller.MainActivity;


import android.app.Activity;
import android.database.DataSetObserver;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;
/**
 * <p>This class instructs how the response list view should be rendered.
 * Each list view item has 3 buttons for Upload/View/Delete + text view summarising the response
 * </p>
 *
 * Author: Trien Do
 * Date: Feb 2015
 **/
public class ResponseListAdapter extends ArrayAdapter<String>
{
	
	private final Activity context;
	private final List<String> responseList;
	public ResponseListAdapter(Activity context, ArrayList<String> responseList)
	{
		super(context, R.layout.media_list_item, responseList);
		this.context = context;
		this.responseList = responseList;
	}
		
	@Override
	public View getView(int position, View view, ViewGroup parent) 
	{
		LayoutInflater inflater = context.getLayoutInflater();
		View rowView= inflater.inflate(R.layout.response_list_item, null, true);
		//Number of like
		TextView txtResponseType = (TextView) rowView.findViewById(R.id.txtResponseType);		
		txtResponseType.setText(responseList.get(position));
		
		//Button
		ImageButton btnUpload = (ImageButton) rowView.findViewById(R.id.btnUploadResponse);
		btnUpload.setTag(position);
		ImageButton btnEdit = (ImageButton) rowView.findViewById(R.id.btnViewResponse);
		btnEdit.setTag(position);
		ImageButton btnDelete = (ImageButton) rowView.findViewById(R.id.btnDeleteResponse);
		btnDelete.setTag(position);
		
		if(position==0)
		{
			//Hide all button, just show overview info
			btnUpload.setVisibility(View.GONE);
			btnEdit.setVisibility(View.GONE);
			btnDelete.setVisibility(View.GONE);
			txtResponseType.setText(Html.fromHtml(responseList.get(position)));
		}
		
		btnUpload.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				int position = (Integer) v.getTag();
				MainActivity activity = (MainActivity)context;
				activity.uploadResponse(position-1);				
			}
		});
		
		btnEdit.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				int position = (Integer) v.getTag();
				MainActivity activity = (MainActivity)context;
				activity.viewResponse(position-1);				
			}
		});
		
		btnDelete.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				int position = (Integer) v.getTag();
				MainActivity activity = (MainActivity)context;
				activity.deleteResponse(position-1);				
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