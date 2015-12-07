package com.genonbeta.CoolSocket.test;

import android.os.*;
import android.support.v4.app.*;
import android.support.v7.app.*;
import com.genonbeta.CoolSocket.test.fragment.*;

public class TemplateListActivity extends AppCompatActivity
{
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		// TODO: Implement this method
		super.onCreate(savedInstanceState);

		Fragment oldFragment = getSupportFragmentManager().findFragmentByTag("main");

		if (oldFragment == null || oldFragment.isDetached())
		{
			FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
			Fragment fragment = (TemplateListFragment) Fragment.instantiate(this, TemplateListFragment.class.getName(), savedInstanceState);

			ft.add(android.R.id.content, fragment, "main");
			ft.commit();
		}
	}
}
