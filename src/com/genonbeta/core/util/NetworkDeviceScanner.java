package com.genonbeta.core.util;

import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class NetworkDeviceScanner
{
	private ArrayList<String> mInterfaces = new ArrayList<String>();
	private ScannerExecutor mExecutor = new ScannerExecutor();
	private ScannerHandler mHandler;
	private Scanner mScanner = new Scanner();
	private boolean mIsBreakRequested = false;
	private boolean mIsLockRequested = false;
	
	public NetworkDeviceScanner()
	{}
	
	public boolean interrupt()
	{
		if (this.mIsBreakRequested == false)
			this.mIsBreakRequested = true;
		else
			return false;
		
		return true;
	}
	
	public boolean isScannerAvaiable()
	{
		return (mInterfaces.size() == 0 && !mIsLockRequested);
	}
	
	private void nextThread()
	{
		if (this.mIsLockRequested)
			return;
		
		if (this.isScannerAvaiable())
		{
			this.mIsBreakRequested = false;
			
			this.setLock(true); // lock scanner
			this.mHandler.onThreadsCompleted();
			this.setLock(false); // release lock
			
			return;
		}

		String ipAddress = mInterfaces.get(0);

		String[] pts = ipAddress.split("\\.");
		String prefixOfIp = pts[0] + "." + pts[1] + "." + pts[2] + ".";

		this.mScanner.updateScanner(prefixOfIp);

		mExecutor.execute(this.mScanner);
		mExecutor.execute(this.mScanner);
		mExecutor.execute(this.mScanner);
		mExecutor.execute(this.mScanner);
		mExecutor.execute(this.mScanner);
		mExecutor.execute(this.mScanner);
	}
	
	public boolean scan(ArrayList<String> interfaces, ScannerHandler handler)
	{
		if (!this.isScannerAvaiable())
			return false;
			
		this.mInterfaces.addAll(interfaces);
		
		this.mHandler = handler;
		this.nextThread();

		return true;
	}
	
	public void setLock(boolean lock)
	{
		mIsLockRequested = lock;
	}

	protected class Scanner implements Runnable
	{
		private String mNetworkInterfacePrefix = "192.168.0.";
		private boolean[] mDevices = new boolean[256];
		private boolean mNotified = false;
		
		public Scanner(String networkInterface)
		{
			this.updateScanner(networkInterface);
		}
		
		public Scanner()
		{}

		public void updateScanner(String newInterface)
		{
			this.mNetworkInterfacePrefix = newInterface;
			this.mDevices = new boolean[256];
			this.mNotified = false;
		}
		
		@Override
		public void run()
		{	
			for (int mPosition = 0; mPosition < mDevices.length; mPosition++)
			{
				if (mDevices[mPosition] == true || mPosition == 0 || NetworkDeviceScanner.this.mIsBreakRequested == true)
					continue;
					
				mDevices[mPosition] = true;

				try
				{
					InetAddress inet = InetAddress.getByName(mNetworkInterfacePrefix + mPosition);

					if (inet.isReachable(300) && NetworkDeviceScanner.this.mHandler != null)
						NetworkDeviceScanner.this.mHandler.onDeviceFound(inet);
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
			
			if (!this.mNotified)
			{
				this.mNotified = true;
				mInterfaces.remove(0);
				NetworkDeviceScanner.this.nextThread();
			}
		}
	}
	
	protected class ScannerExecutor implements Executor
	{
		@Override
		public void execute(Runnable scanner)
		{
			new Thread(scanner).start();
		}	
	}

	public static interface ScannerHandler
	{
		public void onDeviceFound(InetAddress address);
		public void onThreadsCompleted();
	}
}
