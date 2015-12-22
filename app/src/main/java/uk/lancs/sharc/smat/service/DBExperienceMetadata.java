package uk.lancs.sharc.smat.service;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * <p>This class helps interact with an SQLite database which stores information
 * about cached experiences </p>
 * <p>Note that all SQLite databases of SMEP are stored locally in the folder
 * "/data/data/uk.lancs.sharc/databases". This folder can't be seen in a File Manage app </p>
 *
 * Author: Trien Do
 * Date: Feb 2015
 **/


public class DBExperienceMetadata extends SQLiteOpenHelper
{
	private static final String DATABASE_NAME = "ExperienceMetadata.db";	//Database file
	private static final int DATABASE_VERSION = 1;
	
	public DBExperienceMetadata(Context context) {
        //Create a new database if it is not existed
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
		
	private static final String TABLE_EXPERIENCES  = "EXPERIENCES";
	// Sql statement for creating the EXPERIENCES table which contains info about all cached experiences
	private static final String TABLE_EXPERIENCES_CREATE = "create table " + TABLE_EXPERIENCES  +
            " (proPath varchar(256) primary key, proName varchar(256) NOT NULL, proDesc text  NOT NULL, proDate varchar(100) NOT NULL, proAuthID varchar(20) NOT NULL, proPublicURL varchar(300), proLocation varchar(20))";

	//when an experience is downloaded, a new row will be added to the table
	// or if the experience has been download before, its corresponding row will be update
	public long insertExperience(String name, String path, String desc, String date, String authID, String publicURL, String location)
	{
		// Gets the data repository in write mode
		SQLiteDatabase db = this.getWritableDatabase();
        //Check if the experience has been downloaded before
		Cursor existingExperience = db.rawQuery("select * from EXPERIENCES where proPath = ?", new String[] {path});
		if (existingExperience.getCount() < 1)//insert new
		{
			// Create a new map of values, where column names are the keys
			ContentValues values = new ContentValues();
			values.put("proPath", path);
			values.put("proName", name);
			values.put("proDesc", desc);
			values.put("proDate", date);
			values.put("proAuthID", authID);
			values.put("proPublicURL", publicURL);
			values.put("proLocation", location);
			// Insert the new row, returning the primary key value of the new row
			return db.insert(TABLE_EXPERIENCES,null,values);
		}
		else //update old
		{
			return updateExperience(name, path, desc, date, authID, publicURL, location);
		}
	}
	
	//update
	public int updateExperience(String name, String path, String desc, String date, String authID, String publicURL, String location)
	{
		SQLiteDatabase db = this.getWritableDatabase();
		// New value for one column
		ContentValues values = new ContentValues();		
		values.put("proName", name);
		values.put("proDesc", desc);
		values.put("proDate", date);
		values.put("proAuthID", authID);
		values.put("proPublicURL", publicURL);
		values.put("proLocation", location);

		// Which row to update, based on the ID
		String selection = " proPath LIKE ?";
		String[] selectionArgs = {path};

		return db.update(
			TABLE_EXPERIENCES,
		    values,
		    selection,
		    selectionArgs);
	}

	//update
	public int updateExperiencePath(String oldPath, String name, String newPath, String desc, String date, String authID, String publicURL, String location)
	{
		SQLiteDatabase db = this.getWritableDatabase();
		// New value for one column
		ContentValues values = new ContentValues();
		values.put("proPath", newPath);
		values.put("proName", name);
		values.put("proDesc", desc);
		values.put("proDate", date);
		values.put("proAuthID", authID);
		values.put("proPublicURL", publicURL);
		values.put("proLocation", location);

		// Which row to update, based on the ID
		String selection = " proPath LIKE ?";
		String[] selectionArgs = {oldPath};

		return db.update(
				TABLE_EXPERIENCES,
				values,
				selection,
				selectionArgs);
	}
	
	//select all experiences to present them as markers on Google Maps
	public Cursor getExperiences()
	{
		// Gets the data repository in write mode
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor results = db.rawQuery("select * from EXPERIENCES order by proName", null);
		Log.d("SQLite database path:", db.getPath());
		return results;
	}

	//delete an experience
	public void deleteExperience(String proPath)
	{
		// Define 'where' part of query.
		SQLiteDatabase db = this.getReadableDatabase();
		String selection = "proPath LIKE ?";
		// Specify arguments in placeholder order.
		String[] selectionArgs = { String.valueOf(proPath) };
		// Issue SQL statement.
		db.delete(TABLE_EXPERIENCES, selection, selectionArgs);
        //need to delete all cached media files -- later
	}	
	
	@Override
	public void onCreate(SQLiteDatabase database) {
		database.execSQL(TABLE_EXPERIENCES_CREATE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.w(DBExperienceMetadata.class.getName(),"Upgrading database from version " + oldVersion + " to " + newVersion + ", which will destroy all old data");
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_EXPERIENCES);
		onCreate(db);
    }
}