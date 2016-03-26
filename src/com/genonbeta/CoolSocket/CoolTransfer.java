package com.genonbeta.CoolSocket;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.nio.channels.*;

public class CoolTransfer
{
	public abstract static class Send
	{
		public abstract void onError(String serverIp, int port, File file, Exception error, Object extra);
		public abstract void onNotify(Socket socket, String serverIp, int port, File file, int percent, Object extra);
		public abstract void onTransferCompleted(String serverIp, int port, File file, Object extra);
		public abstract void onSocketReady(Socket socket, String serverIp, int port, File file, Object extra);

		public void send(final String serverIp, final int port, final File file, final byte[] bufferSize, final Object extra)
		{
			Runnable runnable = new Runnable()
			{
				@Override
				public void run()
				{
					try
					{
						Socket socket = new Socket();
						
						socket.bind(null);
						socket.connect(new InetSocketAddress(serverIp, port));
						
						Send.this.onSocketReady(socket, serverIp, port, file, extra);

						FileInputStream inputStream = new FileInputStream(file);
						OutputStream outputStream = socket.getOutputStream();
						
						int len;
						int progressPercent = -1;

						while ((len = inputStream.read(bufferSize)) > 0)
						{
							outputStream.write(bufferSize, 0, len);
							outputStream.flush();
							
							int currentPercent = (int)(((float) 100 / file.length()) * inputStream.getChannel().position());
							
							if (currentPercent > progressPercent)
							{
								Send.this.onNotify(socket, serverIp, port, file, currentPercent, extra);
								progressPercent = currentPercent;
							}
						}

						outputStream.close();
						inputStream.close();			
						socket.close();
						
						Send.this.onTransferCompleted(serverIp, port, file, extra);
					}
					catch (IOException e)
					{
						Send.this.onError(serverIp, port, file, e, extra);
					}
				}
			};

			new Thread(runnable).start();
		}
	}

	public abstract static class Receive
	{
		public abstract void onError(int port, File file, Exception error, Object extra);
		public abstract void onNotify(Socket socket, int port, File file, int percent, Object extra);
		public abstract void onTransferCompleted(int port, File file, Object extra);
		public abstract void onSocketReady(ServerSocket socket, int port, File file, Object extra);

		public void receive(final int port, final File file, final long fileSize, final byte[] bufferSize, final int timeOut, final Object extra)
		{
			Runnable runnable = new Runnable()
			{
				@Override
				public void run()
				{
					try
					{
						final ServerSocket serverSocket = new ServerSocket(port);

						Receive.this.onSocketReady(serverSocket, port, file, extra);
						
						Socket socket = serverSocket.accept();
						InputStream inputStream = socket.getInputStream();
						FileOutputStream outputStream = new FileOutputStream(file);
						
						int len;
						long lastRead = System.currentTimeMillis();
						int progressPercent = -1;
						
						while (file.length() != fileSize)
						{
							if ((len = inputStream.read(bufferSize)) > 0)
							{
								outputStream.write(bufferSize, 0, len);
								outputStream.flush();
								
								lastRead = System.currentTimeMillis();
							}
							
							if (timeOut > 0 && (System.currentTimeMillis() - lastRead) > timeOut)
							{
								Receive.this.onError(port, file, new TimeoutException("Standing time has been exceeded"), extra);
								break;
							}
							
							int currentPercent = (int)(((float) 100 / fileSize) * outputStream.getChannel().position());
							
							if (currentPercent > progressPercent)
							{
								Receive.this.onNotify(socket, port, file, currentPercent, extra);
								progressPercent = currentPercent;
							}
						}
						
						outputStream.close();
						inputStream.close();			
						socket.close();
						serverSocket.close();
						
						if (file.length() != fileSize)
							Receive.this.onError(port, file, new NotYetBoundException(), extra);
						
						Receive.this.onTransferCompleted(port, file, extra);
					}
					catch (IOException e)
					{
						Receive.this.onError(port, file, e, extra);
					}
				}
			};
			
			new Thread(runnable).start();
		}
	}
}
