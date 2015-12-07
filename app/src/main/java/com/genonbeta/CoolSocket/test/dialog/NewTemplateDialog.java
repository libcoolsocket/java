package com.genonbeta.CoolSocket.test.dialog;

import android.content.*;
import android.support.v7.app.*;
import android.text.*;
import android.view.*;
import android.widget.*;
import com.genonbeta.CoolSocket.test.*;
import android.database.sqlite.*;
import com.genonbeta.CoolSocket.test.database.*;

public class NewTemplateDialog extends AlertDialog.Builder
{
	public DialogInterface.OnClickListener mExtPositive;
	public DialogInterface.OnClickListener mExtNegative;
	
	DialogInterface.OnClickListener mPositive = new DialogInterface.OnClickListener()
	{
		@Override
		public void onClick(DialogInterface dialogInterface, int p2)
		{
			// TODO: Implement this method
			Editable text = mText.getText();
			
			if (!"".equals(text.toString()))
				mDatabase.add(text.toString());
			
			mExtPositive.onClick(dialogInterface, p2);
		}	
	};
	
	private EditText mText;
	private TemplateListDatabase mDatabase;
	
	public NewTemplateDialog(Context context, TemplateListDatabase database, DialogInterface.OnClickListener positive, DialogInterface.OnClickListener negative)
	{
		super(context);
		
		this.mDatabase = database;
		this.mExtPositive = positive;
		this.mExtNegative = negative;
		
		setTitle("Add new template");

		View rootView = LayoutInflater.from(getContext()).inflate(R.layout.layout_new_template, null); 
		mText = (EditText) rootView.findViewById(R.id.layout_new_template_edit_text);
		
		setNegativeButton("Close", mExtNegative);
		setPositiveButton("Add", mPositive);

		setView(rootView);
	}
	
	
}
