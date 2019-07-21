package com.genonbeta.CoolSocket;

import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeoutException;

public class Main
{
	public static final int PORT_SERVER = 53535;

	@Test
	public void main()
	{
		final TestServer testServer = new TestServer();

		log(Main.class, "Server started ?? %s", testServer.startEnsured(5000));

		CoolSocket.connect(client -> {
			while (!testServer.isServerAlive()) {
				// wait until the server is up.
			}

			try {
				CoolSocket.ActiveConnection activeConnection = client.connect(new InetSocketAddress(PORT_SERVER), CoolSocket.NO_TIMEOUT);

				{
					log(this.getClass(), "Receive");
					CoolSocket.ActiveConnection.Response response = activeConnection.receive();
					log(this.getClass(), response);
				}

				log(this.getClass(), "Send");
				activeConnection.reply("Oh, hi Server!");

				log(this.getClass(), "Send");
				activeConnection.reply("I was wondering if you can prove you work!");

				{
					log(this.getClass(), "Receive");
					CoolSocket.ActiveConnection.Response response = activeConnection.receive();
					log(this.getClass(), response);
				}

				activeConnection.getSocket().close();
			} catch (IOException | TimeoutException e) {
				e.printStackTrace();
			}
		});

		while (testServer.isServerAlive()) {
			// the server has not shut down
		}

		log(this.getClass(), "Exited");
	}

	public static class TestServer extends CoolSocket
	{
		public TestServer()
		{
			super(PORT_SERVER);
		}

		@Override
		public void onServerStarted()
		{
			super.onServerStarted();
			log(this.getClass(), String.format("Server started on port %d", getLocalPort()));
		}

		@Override
		public void onServerStopped()
		{
			super.onServerStopped();
			log(this.getClass(), "Stopped");
		}

		@Override
		protected void onConnected(ActiveConnection activeConnection)
		{
			try {
				log(this.getClass(), "Receive");
				activeConnection.reply("Hey, this is Server. How can I help?");

				{
					log(this.getClass(), "Receive");
					ActiveConnection.Response response = activeConnection.receive();
					log(this.getClass(), response);
				}

				{
					log(this.getClass(), "Receive");
					ActiveConnection.Response response = activeConnection.receive();
					log(this.getClass(), response);
				}

				log(this.getClass(), "Send");
				activeConnection.reply("I do work and if you are reading this, then this is " +
						"the proof that you are looking for.");

				activeConnection.getSocket().close();
			} catch (IOException | TimeoutException e) {
				e.printStackTrace();
			}
		}
	}

	public static void log(Class clazz, CoolSocket.ActiveConnection.Response receivedResponse)
	{
		log(clazz, "%s - UnixTime: %d, TotalLength: %d; HeaderIndex: %s",
				receivedResponse.response, System.currentTimeMillis(), receivedResponse.totalLength,
				receivedResponse.headerIndex.toString());
	}

	public static void log(Class clazz, String print)
	{
		log(clazz, print, (Object[]) null);
	}

	public static void log(Class clazz, String print, Object... formatted)
	{
		StringBuilder stringBuilder = new StringBuilder()
				.append(clazz.getSimpleName())
				.append("(): ")
				.append(print);

		System.out.println(print == null
				? stringBuilder.toString()
				: String.format(stringBuilder.toString(), formatted));
	}
}
