package com.genonbeta.CoolSocket.test.adapter;

import android.content.*;
import android.view.*;
import android.widget.*;
import com.genonbeta.CoolSocket.test.*;
import java.util.*;

public class MessageListAdapter extends BaseAdapter
{
	private Context mContext;
	private ArrayList<String> mList;
	
	public MessageListAdapter(Context context, ArrayList<String> list)
	{
		this.mContext = context;
		this.mList = list;
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
