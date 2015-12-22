package uk.lancs.sharc.smat.service;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import uk.lancs.sharc.smat.model.MediaModel;
import uk.lancs.sharc.smat.model.ResponseModel;


public class DBExperienceDetails extends SQLiteOpenHelper
{
	private static final int DATABASE_VERSION = 1;
	
	public DBExperienceDetails(Context context, String dbName) {
        super(context, SharcLibrary.SHARC_DATABASE_FOLDER + File.separator + dbName, null, DATABASE_VERSION);
		//super(context, dbName, null, DATABASE_VERSION);
    }
	
	
	private static final String TABLE_POI  = "POIS";
	private static final String TABLE_EOI  = "EOIS";
	private static final String TABLE_ROUTE  = "ROUTES";
	private static final String TABLE_MEDIA  = "MEDIA";
	private static final String TABLE_RESPONSE  = "RESPONSES";
	private static final String TABLE_MYRESPONSE  = "MYRESPONSES";
	// Database creation sql statement
	private static final String TABLE_POI_CREATE = "create table " + TABLE_POI  + " (id varchar(15) primary key, name varchar(256), type varchar(256), desc text, latLng varchar(50), mediaOrder text, associatedEOI text, associatedRoute text, triggerZone text)";
	private static final String TABLE_EOI_CREATE = "create table " + TABLE_EOI  + " (id varchar(15) primary key, name varchar(256), desc text, startDate varchar(50), endDate varchar(50), associatedPOI text, associatedRoute text, mediaOrder text)";
	private static final String TABLE_ROUTE_CREATE = "create table " + TABLE_ROUTE  + " (id varchar(15) primary key, name varchar(256), desc text, colour varchar(10), polygon text, associatedPOI text, associatedEOI text, directed varchar(6))";//, mediaOrder text)";
	private static final String TABLE_MEDIA_CREATE = "create table " + TABLE_MEDIA  + " (id varchar(15) primary key, name varchar(256), desc text, attachedTo varchar(10), content varchar(50), context varchar(300), noOfLike int, type varchar(20), EntityID varchar(15))";
	private static final String TABLE_RESPONSE_CREATE = "create table " + TABLE_RESPONSE  +   " (id varchar(15) primary key, status varchar(10), type varchar(10), desc text, content text, entityType varchar(10), entityID varchar(15), noOfLike text, consumerName varchar(300), consumerEmail varchar(300))";
	private static final String TABLE_MYRESPONSE_CREATE = "create table " + TABLE_MYRESPONSE  +   " (id varchar(15) primary key, status varchar(10), type varchar(10), desc text, content text, entityType varchar(10), entityID varchar(15), noOfLike text, consumerName varchar(300), consumerEmail varchar(300))";

	public List<MediaModel> getAllMedia()
	{
		ArrayList<MediaModel> mediaList = new ArrayList<MediaModel>();
		Cursor mediaRet = this.getDataSQL("select * from MEDIA", null);
		if(mediaRet.getCount() > 0)
		{
			mediaRet.moveToFirst();
			do
			{
				MediaModel media = new MediaModel(mediaRet.getString(0), mediaRet.getString(1), mediaRet.getString(2), mediaRet.getString(3), mediaRet.getString(4), mediaRet.getString(5), mediaRet.getString(6), mediaRet.getString(7), mediaRet.getString(8));
				//Get response
				List<ResponseModel> comments = this.getCommentsForEntity(media.getId());
				media.setNoOfComment(String.valueOf(comments.size()));
				mediaList.add(media);
			}
			while (mediaRet.moveToNext());
		}
		return mediaList;
	}

	public List<MediaModel> getMediaForEntity(String ID)
    {
    	ArrayList<MediaModel> mediaList = new ArrayList<MediaModel>();
    	Cursor mediaRet = this.getDataSQL("select * from MEDIA where EntityID = ? ", new String[]{ID});
    	if(mediaRet.getCount() > 0)
    	{
    		mediaRet.moveToFirst();    		
        	do 
        	{
				MediaModel media = new MediaModel(mediaRet.getString(0), mediaRet.getString(1), mediaRet.getString(2), mediaRet.getString(3), mediaRet.getString(4), mediaRet.getString(5), mediaRet.getString(6), mediaRet.getString(7), mediaRet.getString(8));
				//Get response
				List<ResponseModel> comments = this.getCommentsForEntity(media.getId());
				media.setNoOfComment(String.valueOf(comments.size()));
				mediaList.add(media);
			}
        	while (mediaRet.moveToNext());
    	}
    	return mediaList;
    }

	public List<ResponseModel> getCommentsForEntity(String ID)
	{
		ArrayList<ResponseModel> responseList = new ArrayList<ResponseModel>();
		Cursor responseRet = this.getDataSQL("select * from RESPONSES where status = 'Accepted' and entityType <> 'POI' and EntityID = ? order by id", new String[]{ID});
		if(responseRet.getCount() > 0)
		{
			responseRet.moveToFirst();
			do
			{
				ResponseModel response = new ResponseModel(responseRet.getString(0), responseRet.getString(1), responseRet.getString(2), responseRet.getString(3), responseRet.getString(4), responseRet.getString(5), responseRet.getString(6), responseRet.getString(7), responseRet.getString(8), responseRet.getString(9));
				responseList.add(response);
			}
			while (responseRet.moveToNext());
		}
		return responseList;
	}

    public List<ResponseModel> getResponsesForEntity(String ID)
    {
        ArrayList<ResponseModel> responseList = new ArrayList<ResponseModel>();
        Cursor responseRet = this.getDataSQL("select * from RESPONSES where status = 'Accepted' and entityType <> 'media' and EntityID = ? order by id", new String[]{ID});
        if(responseRet.getCount() > 0)
        {
            responseRet.moveToFirst();
            do
            {
                ResponseModel response = new ResponseModel(responseRet.getString(0), responseRet.getString(1), responseRet.getString(2), responseRet.getString(3), responseRet.getString(4), responseRet.getString(5), responseRet.getString(6), responseRet.getString(7), responseRet.getString(8), responseRet.getString(9));
				List<ResponseModel> comments = this.getCommentsForEntity(response.getId());
				response.setNoOfComment(String.valueOf(comments.size()));
                responseList.add(response);
            }
            while (responseRet.moveToNext());
        }
        return responseList;
    }

	public List<ResponseModel> getResponsesForTab(String tabName)//EOI and Summary tabs
	{
		ArrayList<ResponseModel> responseList = new ArrayList<ResponseModel>();
		Cursor responseRet = this.getDataSQL("select * from RESPONSES where status = 'Accepted' and entityType = ? order by id", new String[]{tabName});
		if(responseRet.getCount() > 0)
		{
			responseRet.moveToFirst();
			do
			{
				ResponseModel response = new ResponseModel(responseRet.getString(0), responseRet.getString(1), responseRet.getString(2), responseRet.getString(3), responseRet.getString(4), responseRet.getString(5), responseRet.getString(6), responseRet.getString(7), responseRet.getString(8), responseRet.getString(9));
				List<ResponseModel> comments = this.getCommentsForEntity(response.getId());
				response.setNoOfComment(String.valueOf(comments.size()));
				responseList.add(response);
			}
			while (responseRet.moveToNext());
		}
		return responseList;
	}

	public String getFirstImage(String ID, String[] mediaOrder)
    {
		if(mediaOrder == null || mediaOrder.length == 0)
			return "";
		Hashtable<String, String> mediaList = new Hashtable<String, String>();
    	Cursor mediaRet = this.getDataSQL("select * from MEDIA where EntityID = ? and type = 'image'", new String[]{ID});
    	if(mediaRet.getCount() > 0)
    	{
    		mediaRet.moveToFirst();
        	do 
        	{               
	    		String id = mediaRet.getString(4);//0 = id -> 4 = content
	    		mediaList.put(mediaRet.getString(0), id);
            } 
        	while (mediaRet.moveToNext());
    	}
    	for(int i = 0; i < mediaOrder.length; i++)
    	{
    		if(mediaList.get(mediaOrder[i])!=null)
                return mediaList.get(mediaOrder[i]);
    	}
		//No image from media - try response
		mediaRet = this.getDataSQL("select * from RESPONSES where EntityID = ? and type = 'image' order by id", new String[]{ID});
		if(mediaRet.getCount() > 0)
		{
			mediaRet.moveToFirst();
			return mediaRet.getString(4);
		}
    	return "";
    }
	
	public String[] getEOIFromID(String ID)
    {
    	Cursor eoiRet = this.getDataSQL("select name, desc from EOIs where id = ? ", new String[]{ID});
    	if(eoiRet.getCount() > 0)
    	{
    		eoiRet.moveToFirst();
            String name = "";
            String desc = "";
            try {
                name =  URLDecoder.decode(eoiRet.getString(0), "UTF-8") ;
                desc =  URLDecoder.decode(eoiRet.getString(1), "UTF-8") ;
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
	    	return new String[]{name, desc};
    	}
    	else
    		return null;    	
    }
	
	//insert
	public long insertPOI(String id, String name, String type, String desc, String latLng, String mediaOrder, String associatedEOI, String associatedRoute, String triggerZone)
	{
		// Gets the data repository in write mode
		SQLiteDatabase db = this.getWritableDatabase();
		// Create a new map of values, where column names are the keys
		ContentValues values = new ContentValues();
		values.put("id", id);
		values.put("name", name);
		values.put("type", type);
		values.put("desc", desc);
		values.put("latLng", latLng);
		values.put("mediaOrder", mediaOrder);
		values.put("associatedEOI", associatedEOI);
		values.put("associatedRoute", associatedRoute);
		values.put("triggerZone", triggerZone);
		// Insert the new row, returning the primary key value of the new row
		return db.insert(TABLE_POI,null,values);		 
	}

	public long insertEOI(String id, String name, String desc, String startDate, String endDate, String associatedPOI, String associatedRoute, String mediaOrder)
	{
		// Gets the data repository in write mode
		SQLiteDatabase db = this.getWritableDatabase();

		// Create a new map of values, where column names are the keys
		ContentValues values = new ContentValues();
		values.put("id", id);
		values.put("name", name);
		values.put("desc", desc);
		values.put("startDate", startDate);
		values.put("endDate", endDate);
		values.put("associatedPOI", associatedPOI);
		values.put("associatedRoute", associatedRoute);
		values.put("mediaOrder", mediaOrder);
		// Insert the new row, returning the primary key value of the new row
		return db.insert(TABLE_EOI,null,values);		 
	}
	
	public long insertROUTE(String id, String name, String desc, String colour, String polygon, String associatedPOI, String associatedEOI, String directed)//, String mediaOrder)
	{
		// Gets the data repository in write mode
		SQLiteDatabase db = this.getWritableDatabase();

		// Create a new map of values, where column names are the keys
		ContentValues values = new ContentValues();
		values.put("id", id);
		values.put("name", name);
		values.put("desc", desc);
		values.put("colour", colour);
		values.put("polygon", polygon);
		values.put("associatedPOI", associatedPOI);
		values.put("associatedEOI", associatedEOI);
		values.put("directed", directed);
		//values.put("mediaOrder", mediaOrder);
		// Insert the new row, returning the primary key value of the new row
		return db.insert(TABLE_ROUTE,null,values);		 
	}

	public long updateRoute(String id, String name, String desc, String colour, String polygon, String associatedPOI, String associatedEOI, String directed)
	{
		// Gets the data repository in write mode
		SQLiteDatabase db = this.getWritableDatabase();
		// Create a new map of values, where column names are the keys
		ContentValues values = new ContentValues();
		values.put("name", name);
		values.put("desc", desc);
		values.put("colour", colour);
		values.put("polygon", polygon);
		values.put("associatedPOI", associatedPOI);
		values.put("associatedEOI", associatedEOI);
		values.put("directed", directed);
		String selection = " id LIKE ?";
		String[] selectionArgs = {id};
		return db.update(TABLE_ROUTE, values, selection, selectionArgs);
	}

	public long updateRoutePath(String id, String polygon)
	{
		// Gets the data repository in write mode
		SQLiteDatabase db = this.getWritableDatabase();
		// Create a new map of values, where column names are the keys
		ContentValues values = new ContentValues();
		values.put("polygon", polygon);
		String selection = " id LIKE ?";
		String[] selectionArgs = {id};
		return db.update(TABLE_ROUTE, values, selection, selectionArgs);
	}

	public int deleteRoute(String rID)
	{
		SQLiteDatabase db = this.getWritableDatabase();
		return db.delete(TABLE_ROUTE, "id = ?", new String[]{rID});
	}
	
	public long insertMEDIA(String id, String name, String type, String desc, String attachedTo, String content, String context, int noOfLike, String EntityID)
	{
		// Gets the data repository in write mode
		SQLiteDatabase db = this.getWritableDatabase();

		// Create a new map of values, where column names are the keys
		ContentValues values = new ContentValues();
		values.put("id", id);
		values.put("name", name);
		values.put("type", type);
		values.put("desc", desc);
		values.put("attachedTo", attachedTo );
		values.put("content", content);
		values.put("context", context);
		values.put("noOfLike", noOfLike);
		values.put("EntityID", EntityID);
		// Insert the new row, returning the primary key value of the new row
		return db.insert(TABLE_MEDIA,null,values);		 
	}

	public long updateMediaURL(String mediaId, String mediaURL)
	{
		// Gets the data repository in write mode
		SQLiteDatabase db = this.getWritableDatabase();
		// Create a new map of values, where column names are the keys
		ContentValues values = new ContentValues();
		values.put("content", mediaURL);
		// Which row to update, based on the ID
		String selection = " id LIKE ?";
		String[] selectionArgs = {mediaId};
		return db.update(TABLE_MEDIA,values,selection,selectionArgs);
	}
	
	public long insertRESPONSE(String id, String status, String type, String desc, String entityType, String content, int noOfLike, String entityID, String consumerName, String consumerEmail)
	{
		// Gets the data repository in write mode
		SQLiteDatabase db = this.getWritableDatabase();

		// Create a new map of values, where column names are the keys
		ContentValues values = new ContentValues();
		values.put("id", id);
		values.put("status", status);
		values.put("type", type);
		values.put("desc", desc);
		values.put("entityType", entityType );
		values.put("content", content);
		values.put("noOfLike", noOfLike);
		values.put("entityID", entityID);
		values.put("consumerName", consumerName);
		values.put("consumerEmail", consumerEmail);
		// Insert the new row, returning the primary key value of the new row
		return db.insert(TABLE_RESPONSE,null,values);		 
	}
	
	public long insertMYRESPONSE(String id, String status, String type, String desc, String entityType, String content, String noOfLike, String entityID, String consumerName, String consumerEmail)
	{
		// Gets the data repository in write mode
		SQLiteDatabase db = this.getWritableDatabase();

		// Create a new map of values, where column names are the keys
		ContentValues values = new ContentValues();
		values.put("id", id);
		values.put("status", status);
		values.put("type", type);
		values.put("desc", desc);
		values.put("entityType", entityType );
		values.put("content", content);
		values.put("noOfLike", noOfLike);
		values.put("entityID", entityID);
		values.put("consumerName", consumerName);
		values.put("consumerEmail", consumerEmail);
		// Insert the new row, returning the primary key value of the new row
		return db.insert(TABLE_MYRESPONSE,null,values);		 
	}

	public int deleteMYRESPONSE(String resID)
	{
		SQLiteDatabase db = this.getWritableDatabase();
		return db.delete(TABLE_MYRESPONSE, "id = ?", new String[]{resID});		
	}

	//update
	public long updatePOI(String id, String name, String type, String desc, String latLng, String mediaOrder, String associatedEOI, String associatedRoute, String triggerZone)
	{
		// Gets the data repository in write mode
		SQLiteDatabase db = this.getWritableDatabase();
		// Create a new map of values, where column names are the keys
		ContentValues values = new ContentValues();
		values.put("id", id);
		values.put("name", name);
		values.put("type", type);
		values.put("desc", desc);
		values.put("latLng", latLng);
		values.put("mediaOrder", mediaOrder);
		values.put("associatedEOI", associatedEOI);
		values.put("associatedRoute", associatedRoute);
		values.put("triggerZone", triggerZone);
		// Which row to update, based on the ID
		String selection = " id LIKE ?";
		String[] selectionArgs = {id};
		return db.update(TABLE_POI,values,selection,selectionArgs);
	}

	//select
	public Cursor getDataSQL(String sql, String[] param)
	{
		// Gets the data repository in write mode
		//rawQuery("SELECT id, name FROM people WHERE name = ? AND id = ?", new String[] {"David", "2"});
		SQLiteDatabase db = this.getReadableDatabase();		
		Cursor results = db.rawQuery(sql, param);
		return results;
	}
	
	public void deleteAllDataInTables()
	{
		SQLiteDatabase db = this.getWritableDatabase();
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_EOI);
		db.execSQL(TABLE_EOI_CREATE);
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_ROUTE);
		db.execSQL(TABLE_ROUTE_CREATE);
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_RESPONSE);
		db.execSQL(TABLE_RESPONSE_CREATE);
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_MYRESPONSE);
		db.execSQL(TABLE_MYRESPONSE_CREATE);
		db.delete(TABLE_POI, null, null);
		db.delete(TABLE_EOI, null, null);
		db.delete(TABLE_ROUTE, null, null);
		db.delete(TABLE_MEDIA, null, null);				
		db.delete(TABLE_MYRESPONSE, null, null);
	}
	
	@Override
	public void onCreate(SQLiteDatabase database) {		
		database.execSQL(TABLE_POI_CREATE);
		database.execSQL(TABLE_EOI_CREATE);
		database.execSQL(TABLE_ROUTE_CREATE);
		database.execSQL(TABLE_MEDIA_CREATE);
		database.execSQL(TABLE_RESPONSE_CREATE);
		database.execSQL(TABLE_MYRESPONSE_CREATE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.w(DBExperienceDetails.class.getName(),"Upgrading database from version " + oldVersion + " to " + newVersion + ", which will destroy all old data");
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_POI);
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_EOI);
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_ROUTE);
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_MEDIA);
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_RESPONSE);
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_MYRESPONSE);
		onCreate(db);
  }
}