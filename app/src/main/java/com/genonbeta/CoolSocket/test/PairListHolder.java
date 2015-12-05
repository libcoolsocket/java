package com.genonbeta.CoolSocket.test;

import com.genonbeta.CoolSocket.test.helper.*;
import java.net.*;
import java.util.*;

public class PairListHolder
{
	private final static ArrayList<String> mIndex = new ArrayList<String>();
	private final static NetworkDeviceScanner mScanner = new NetworkDeviceScanner();
	private final static ResultHandler mHandler = new ResultHandler();
	
	public static boolean update()
	{
		if (mScanner.isScannerAvaiable())
		{
			ArrayList<String> list = NetworkUtils.getIPAddressList(true);

			for (String ip : list)
			{
				if (!mIndex.contains(ip))
				{
					mIndex.add(ip);
				}
			}

			return mScanner.scan(list, mHandler);
		}

		return false;
	}
	
	public static ArrayList<String> getList()
	{
		return mIndex;
	}

	public static NetworkDeviceScanner getScanner()
	{
		return mScanner;
	}

	private static class ResultHandler implements NetworkDeviceScanner.ScannerHandler
	{
		@Override
		public void onDeviceFound(InetAddress address)
		{
			// TODO: Implement this method
			if (!mIndex.contains(address.getHostAddress()))
				mIndex.add(address.getHostAddress());
		}

		@Override
		public void onThreadsCompleted()
		{
			// TODO: Implement this method
			// this.notifyDataSetChanged();
		}
	}
}
