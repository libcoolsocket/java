package com.genonbeta.CoolSocket;

import java.net.*;
import java.io.*;
import java.text.*;

public abstract class CoolCommunication extends CoolSocket
{
	public CoolCommunication()
	{
	}

	public CoolCommunication(int port)
	{
		super(port);
	}

	public CoolCommunication(String address, int port)
	{
		super(address, port);
	}

	@Override
	final protected void onPacketReceived(Socket socket)
	{
		// TODO: Implement this method
		try
		{			
			PrintWriter writer = this.getStreamWriter(socket.getOutputStream());
			String message = this.readStreamMessage(socket.getInputStream());
			
			this.onMessage(socket, message, writer, socket.getInetAddress().isAnyLocalAddress() ? "127.0.0.1" : socket.getInetAddress().getHostAddress());
			
			writer.append(CoolSocket.END_SEQUENCE);
			writer.flush();
			
			socket.close();
		}
		catch (IOException e)
		{
			this.onError(e);
		}
	}

	abstract protected void onMessage(Socket socket, String message, PrintWriter writer, String clientIp);
	abstract protected void onError(Exception exception);

	public static class Messenger
	{
		public static String send(String socketHost, int socketPort, String message, ResponseHandler handler)
		{
			return send(new InetSocketAddress(socketHost, socketPort), message, handler);
		}
		
		public static String send(InetSocketAddress address, String message, ResponseHandler handler)
		{
			SenderRunnable runnable = new SenderRunnable(address, message, handler);
			
			new Thread(runnable).start();
			
			return runnable.getResponse();
		}
		
		public static class SenderRunnable implements Runnable
		{
			private SocketAddress mAddress;
			private String mMessage; 
			private String mResponse;
			private ResponseHandler mHandler;
			private int mSocketTimeout = -1;
			
			public SenderRunnable(SocketAddress address, String message, ResponseHandler handler)
			{
				this.mAddress = address;
				this.mMessage = message;
				this.mHandler = handler;
			}
			
			public String getResponse()
			{
				return this.mResponse;
			}
			
			@Override
			public void run()
			{
				// TODO: Implement this method
				if (this.mHandler != null)
					this.mHandler.onConfigure(this);
				
				Socket socket = new Socket();

				try
				{
					socket.bind(null);
					socket.connect(this.mAddress);
					
					if (this.mSocketTimeout != -1)	
						socket.setSoTimeout(this.mSocketTimeout);
					
					PrintWriter writer = getStreamWriter(socket.getOutputStream());

					if (mHandler != null && mHandler instanceof ExpandedResponseHandler)
						((ExpandedResponseHandler) mHandler).onMessage(socket, writer);
					else
						writer.append(this.mMessage);
						
					writer.append(CoolSocket.END_SEQUENCE);

					writer.flush();

					this.mResponse = readStreamMessage(socket.getInputStream());
					
					if (mHandler != null)
						mHandler.onResponseAvaiable(this.mResponse);
				}
				catch (IOException e)
				{
					if (mHandler != null)
						mHandler.onError(e);
				}
				finally
				{
					if (mHandler != null)
						mHandler.onFinal();
				}
			}
			
			public void setSocketTimeout(int timeout)
			{
				this.mSocketTimeout = timeout;
			}
		}
		
		public static interface ResponseHandler
		{
			public void onConfigure(SenderRunnable runnable);
			public void onResponseAvaiable(String response);
			public void onError(Exception exception);
			public void onFinal();
		}
		
		public static interface ExpandedResponseHandler extends ResponseHandler
		{
			public void onMessage(Socket socket, PrintWriter response);
		}
	}
}
