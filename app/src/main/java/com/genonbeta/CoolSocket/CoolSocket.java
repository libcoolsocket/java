package com.genonbeta.CoolSocket;

import java.net.*;
import java.io.*;

abstract public class CoolSocket
{
	public static final String END_SEQUENCE = "\n();;";
	
	private SocketAddress mSocketAddress = null;
	private boolean isStopped = false;
	private Thread mMainThread;
	private SocketRunnable mRunnable = new SocketRunnable();
	private ServerSocket mServer;
	private int mSocketTimeout = -1;
	
	public CoolSocket()
	{
	}
	
	public CoolSocket(int port)
	{
		this.mSocketAddress = new InetSocketAddress(port);
	}
	
	public CoolSocket(String address, int port)
	{
		this.mSocketAddress = new InetSocketAddress(address, port);
	}
	
	public int getLocalPort()
	{
		return this.mServer.getLocalPort();
	}
	
	public Thread getServerThread()
	{
		return this.mMainThread;
	}
	
	public static PrintWriter getStreamWriter(OutputStream outputStream)
	{
		return new PrintWriter(new BufferedOutputStream(outputStream));
	}
	
	public boolean isStopped()
	{
		return this.isStopped;
	}
	
	public static ByteArrayOutputStream readStream(InputStream inputStreamIns) throws IOException
	{
		BufferedInputStream inputStream = new BufferedInputStream(inputStreamIns);
		ByteArrayOutputStream inputStreamResult = new ByteArrayOutputStream();

		byte[] buffer = new byte[8096];
		int len = 0;
		
		do
		{
			if ((len = inputStream.read(buffer)) > 0)
				inputStreamResult.write(buffer, 0, len);
		}
		while (!inputStreamResult.toString().endsWith(END_SEQUENCE));
		
		return inputStreamResult;
	}

	public static String readStreamMessage(InputStream inputStream) throws IOException
	{
		return readStreamMessage(readStream(inputStream));
	}

	public static String readStreamMessage(ByteArrayOutputStream outputStream)
	{
		String message = outputStream.toString();

		return message.substring(0, message.length() - END_SEQUENCE.length());
	}
	
	public void setSocketAddress(SocketAddress address)
	{
		this.mSocketAddress = address;
	}
	
	public void setSocketTimeout(int timeout)
	{
		this.mSocketTimeout = timeout;
	}

	public boolean start()
	{
		
		this.isStopped = false;
		
		if (this.mServer == null || this.mServer.isClosed())
		{
			try
			{
				this.mServer = new ServerSocket();
				this.mServer.bind(mSocketAddress);
			}
			catch (IOException e)
			{
				e.printStackTrace();
				return false;
			}
		}

		if (this.mMainThread == null || Thread.State.TERMINATED.equals(this.mMainThread.getState()))
		{
			this.mMainThread = new Thread(this.mRunnable);

			this.mMainThread.setDaemon(true);
			this.mMainThread.setName("CoolSocket Main Thread");
		}
		else if (this.mMainThread.isAlive() || this.mMainThread.isInterrupted())
			return false;

		this.mMainThread.start();	

		return true;
	}

	public void stop()
	{
		this.isStopped = true;
		
		if (!this.mServer.isClosed())
		{
			try
			{
				this.mServer.close();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}

	abstract protected void onPacketReceived(Socket socket);
	
	private class ClientHandler implements Runnable
	{
		private Socket mSocket;
		
		public ClientHandler(Socket socket)
		{
			this.mSocket = socket;
		}
		
		@Override
		public void run()
		{
			// TODO: Implement this method
			try
			{
				if (CoolSocket.this.mSocketTimeout != -1)
					this.mSocket.setSoTimeout(CoolSocket.this.mSocketTimeout);
			}
			catch (SocketException e)
			{
				e.printStackTrace();
			}
			
			CoolSocket.this.onPacketReceived(this.mSocket);
		}
	}
	
	private class SocketRunnable implements Runnable
	{
		@Override
		public void run()
		{
			// TODO: Implement this method
			try
			{
				do
				{
					Socket request = mServer.accept();
					
					if (CoolSocket.this.isStopped)
						request.close();
					else
						new Thread(new ClientHandler(request)).start();
				}
				while (!CoolSocket.this.isStopped);
			}
			catch (IOException e)
			{
				stop();
			}
		}
	}
}
