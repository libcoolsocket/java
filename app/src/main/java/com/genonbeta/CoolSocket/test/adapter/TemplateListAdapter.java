package com.genonbeta.CoolSocket.test.adapter;

import android.content.*;
import android.database.sqlite.*;
import android.view.*;
import android.widget.*;
import com.genonbeta.CoolSocket.test.*;
import com.genonbeta.CoolSocket.test.database.*;
import java.util.*;

public class TemplateListAdapter extends BaseAdapter
{
	private Context mContext;
	private TemplateListDatabase mDatabase;
	private ArrayList<String> mList = new ArrayList<String>();
	
	public TemplateListAdapter(Context context)
	{
		this.mContext = context;
		this.mDatabase = new TemplateListDatabase(mContext);
	}

	public void update()
	{
		mList.clear();
		mDatabase.getList(mList);
		notifyDataSetChanged();
	}
	
	public TemplateListDatabase getDatabase()
	{
		return this.mDatabase;
	}

	@Override
	public int getCount()
	{
		// TODO: Implement this method
		return mList.size();
	}

	@Override
	public Object getItem(int p1)
	{
		// TODO: Implement this method
		return mList.get(p1);
	}

	@Override
	public long getItemId(int p1)
	{
		// TODO: Implement this method
		return 0;
	}

	@Override
	public View getView(int position, View view, ViewGroup container)
	{
		// TODO: Implement this method
		return getViewAt(LayoutInflater.from(mContext).inflate(R.layout.list, container, false), position);
	}

	public View getViewAt(View view, int position)
	{
		TextView text = (TextView) view.findViewById(R.id.list_text);

		text.setText((String) getItem(position));

		return view;
	}
}
