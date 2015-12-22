package com.genonbeta.CoolSocket.test.fragment;

import android.app.*;
import android.content.*;
import android.os.*;
import android.support.v4.app.*;
import android.view.*;
import android.widget.*;
import android.widget.AdapterView.*;
import com.genonbeta.CoolSocket.test.*;
import com.genonbeta.CoolSocket.test.adapter.*;

import android.support.v4.app.ListFragment;

public class PairListFragment extends ListFragment implements OnItemClickListener
{
	private PairListAdapter mAdapter;
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		// TODO: Implement this method
		super.onActivityCreated(savedInstanceState);
		
		mAdapter = new PairListAdapter(getActivity());
		setListAdapter(mAdapter);
		
		PairListHolder.update();
		
		getListView().setPadding(15, 0, 15, 0);
		getListView().setOnItemClickListener(this);
		
		setHasOptionsMenu(true);
	}
	
	@Override
	public void onItemClick(AdapterView<?> p1, View p2, int p3, long p4)
	{
		// TODO: Implement this method
		Intent result = new Intent();
		result.putExtra(Main.EXTRA_PEER_ADDRESS, (String) mAdapter.getItem(p3));
		
		if (getActivity().getParent() != null)
			getActivity().getParent().setResult(Activity.RESULT_OK, result);
		else
			getActivity().setResult(Activity.RESULT_OK, result);
	
		getActivity().finish();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		// TODO: Implement this method
		super.onCreateOptionsMenu(menu, inflater);
		
		menu.add("Refresh").setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		menu.add("Scan").setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		// TODO: Implement this method
		
		if ("Refresh".equals(item.getTitle()))
		{
			if (PairListHolder.getScanner().isScannerAvaiable())
				mAdapter.notifyDataSetChanged();
			
			return true;
		}
		else if ("Scan".equals(item.getTitle()))
		{
			PairListHolder.update();
			return true;
		}
		
		return super.onOptionsItemSelected(item);
	}
}
