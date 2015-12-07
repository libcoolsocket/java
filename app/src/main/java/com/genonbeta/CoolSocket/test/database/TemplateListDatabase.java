package com.genonbeta.CoolSocket.test.database;

import android.content.*;
import android.database.*;
import android.database.sqlite.*;
import android.util.*;
import java.util.*;

public class TemplateListDatabase extends SQLiteOpenHelper
{
	public final static String DATABASE_NAME = "TemplateList";
	public final static String TABLE_LIST = "List";
	public final static String COLUMN_MESSAGE = "message";
	
	private Context mContext;
	
	public TemplateListDatabase(Context context)
	{
		super(context, DATABASE_NAME, null, 1);
		this.mContext = context;
	}

	@Override
	public void onCreate(SQLiteDatabase db)
	{
		// TODO: Implement this method
		db.execSQL("CREATE TABLE `" + TABLE_LIST + "` (`" + COLUMN_MESSAGE + "` text NOT NULL)");
	}

	@Override
	public void onUpgrade(SQLiteDatabase p1, int p2, int p3)
	{
		// TODO: Implement this method
	}
	
	public void add(String text)
	{
		ContentValues values = new ContentValues();
		values.put(COLUMN_MESSAGE, text);
		
		SQLiteDatabase db = this.getWritableDatabase();
		db.insert(TABLE_LIST, null, values);
		db.close();
	}
	
	public void getList(ArrayList<String> list)
	{
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.query(TABLE_LIST, new String[] {COLUMN_MESSAGE}, null, null, null, null, null);
		
		if (cursor.moveToFirst())
		{
			int msgColumn = cursor.getColumnIndex(COLUMN_MESSAGE);
			
			do
			{
				list.add(cursor.getString(msgColumn));
			}
			while(cursor.moveToNext());
		}
		
		cursor.close();
		db.close();
	}
	
	public void delete(String text)
	{
		SQLiteDatabase db = this.getWritableDatabase();
		db.delete(TABLE_LIST, COLUMN_MESSAGE + " LIKE ?", new String[] {text});
		db.close();
	}
}
