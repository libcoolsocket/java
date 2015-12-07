package com.genonbeta.CoolSocket.test.fragment;

import android.app.*;
import android.content.*;
import android.net.*;
import android.os.*;
import android.support.v4.app.*;
import android.util.*;
import android.view.*;
import android.widget.*;
import android.widget.AdapterView.*;
import com.genonbeta.CoolSocket.test.*;
import com.genonbeta.CoolSocket.test.adapter.*;
import com.genonbeta.CoolSocket.test.dialog.*;
import java.io.*;
import java.util.*;

import android.support.v4.app.ListFragment;

public class TemplateListFragment extends ListFragment implements OnItemClickListener
{
	private ChoiceListener mChoiceListener = new ChoiceListener();
	private TemplateListAdapter mAdapter;
	DialogInterface.OnClickListener mPositive = new DialogInterface.OnClickListener()
	{
		@Override
		public void onClick(DialogInterface dialogInterface, int p2)
		{
			// TODO: Implement this method
			mAdapter.update();
		}	
	};

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		// TODO: Implement this method
		super.onActivityCreated(savedInstanceState);

		mAdapter = new TemplateListAdapter(getActivity());

		setListAdapter(mAdapter);

		getListView().setPadding(15, 0, 15, 0);
		getListView().setOnItemClickListener(this);

		getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
		getListView().setMultiChoiceModeListener(mChoiceListener);
		
		setHasOptionsMenu(true);
	}

	@Override
	public void onItemClick(AdapterView<?> p1, View p2, int p3, long p4)
	{
		// TODO: Implement this method
		Intent result = new Intent();
		result.putExtra(Main.EXTRA_MESSAGE, (String) mAdapter.getItem(p3));

		if (getActivity().getParent() != null)
			getActivity().getParent().setResult(Activity.RESULT_OK, result);
		else
			getActivity().setResult(Activity.RESULT_OK, result);

		getActivity().finish();
	}

	@Override
	public void onResume()
	{
		// TODO: Implement this method
		super.onResume();
		mAdapter.update();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		// TODO: Implement this method
		super.onCreateOptionsMenu(menu, inflater);
		menu.add("Add").setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		// TODO: Implement this method

		if ("Add".equals(item.getTitle()))
		{
			NewTemplateDialog templateAdder = new NewTemplateDialog(getActivity(), mAdapter.getDatabase(), mPositive, null);
			templateAdder.show();

			return true;
		}
		else if ("Remove".equals(item.getTitle()))
		{
			PairListHolder.update();
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	private class ChoiceListener implements AbsListView.MultiChoiceModeListener
	{
		protected HashSet<String> mCheckedList = new HashSet<String>();

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu)
		{
			// TODO: Implement this method
			mode.setTitle("Edit list");
			menu.add("Delete");

			return true;
		}

		@Override
		public boolean onPrepareActionMode(ActionMode p1, Menu p2)
		{
			// TODO: Implement this method
			mCheckedList.clear();
			return true;
		}

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item)
		{
			// TODO: Implement this method
			if ("Delete".equals(item.getTitle()))
			{
				for (String text : mCheckedList)
				{
					mAdapter.getDatabase().delete(text);
				}
			}

			mode.finish();

			return true;
		}
		
		@Override
		public void onDestroyActionMode(ActionMode p1)
		{
			// TODO: Implement this method
			mAdapter.update();
			mCheckedList.clear();
		}

		@Override
		public void onItemCheckedStateChanged(ActionMode mode, int pos, long id, boolean isChecked)
		{
			// TODO: Implement this method

			String text = (String) mAdapter.getItem(pos);

			if (isChecked)
				mCheckedList.add(text);
			else
				mCheckedList.remove(text);

			mode.setSubtitle(getListView().getCheckedItemCount() + " selected");
		}
	}
}
