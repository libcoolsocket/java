package com.genonbeta.CoolSocket;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.ArrayList;

abstract public class CoolSocket
{
	public static final String END_SEQUENCE = "\n();;";
	
	private Thread mMainThread;
	private ServerSocket mServer;
	private SocketAddress mSocketAddress = null;
	private boolean isStopped = false;
	private SocketRunnable mRunnable = new SocketRunnable();
	private int mSocketTimeout = -1; // no timeout
	private int mMaxConnections = 0; // no limit
	private ArrayList<ClientHandler> mConnections = new ArrayList<ClientHandler>();
	
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
	
	protected void onClosingConnection(ClientHandler client)
	{}
	
	abstract protected void onError(Exception exception);
	abstract protected void onPacketReceived(Socket socket);
	
	public ArrayList<ClientHandler> getConnections()
	{
		return this.mConnections;
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
	
	private boolean respondRequest(Socket socket)
	{
		if (this.getConnections().size() < this.mMaxConnections || this.mMaxConnections == 0)
		{	
			ClientHandler clientRunnable = new ClientHandler(socket);
			Thread selfThread = new Thread(clientRunnable);

			this.getConnections().add(clientRunnable);

			selfThread.start();
		}
		else
			return false;

		return true;
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
				this.mServer.bind(this.mSocketAddress);
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
	
	public void setMaxConnections(int value)
	{
		this.mMaxConnections = value;
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
	
	protected class ClientHandler implements Runnable
	{
		private Socket mSocket;
		
		public ClientHandler(Socket socket)
		{
			this.mSocket = socket;
		}
		
		@Override
		public void run()
		{
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
			
			CoolSocket.this.onClosingConnection(this);
			CoolSocket.this.getConnections().remove(this);
		}
		
		public Socket getSocket()
		{
			return this.mSocket;
		}
	}
	
	private class SocketRunnable implements Runnable
	{
		@Override
		public void run()
		{
			try
			{
				do
				{
					Socket request = CoolSocket.this.mServer.accept();
					
					if (CoolSocket.this.isStopped)
						request.close();
					else
						respondRequest(request);
				}
				while (!CoolSocket.this.isStopped);
			}
			catch (IOException e)
			{
				CoolSocket.this.onError(e);
			}
		}
	}
}
