package com.genonbeta.CoolSocket.test.adapter;

import android.content.*;
import android.support.v4.util.*;
import android.view.*;
import android.widget.*;
import com.genonbeta.CoolSocket.test.*;
import com.genonbeta.CoolSocket.test.helper.*;
import java.io.*;
import java.util.*;
import java.net.*;

public class PairListAdapter extends BaseAdapter
{
	private Context mContext;
	
	public PairListAdapter(Context context)
	{
		this.mContext = context;
	}	
	
	@Override
	public int getCount()
	{
		// TODO: Implement this method
		return PairListHolder.getList().size();
	}

	@Override
	public Object getItem(int itemId)
	{
		// TODO: Implement this method	
		return PairListHolder.getList().get(itemId);
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
