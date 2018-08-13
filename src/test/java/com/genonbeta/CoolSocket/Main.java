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

		log("Server started ?= %s", testServer.startEnsured(5000));

		CoolSocket.connect(client -> {
			if (!testServer.isServerAlive())
				return;

			try {
				CoolSocket.ActiveConnection activeConnection = client.connect(new InetSocketAddress(PORT_SERVER), CoolSocket.NO_TIMEOUT);

				activeConnection.reply("Hi server");

				CoolSocket.ActiveConnection.Response response = activeConnection.receive();

				log("Server says: %s; with total character of: %d; header of: %s",
						response.response,
						response.totalLength,
						response.headerIndex.toString());

				activeConnection.getSocket().close();
			} catch (IOException | TimeoutException e) {
				e.printStackTrace();
			}
		});

		while (testServer.isServerAlive()) {
			// the server has not shut down
		}

		log("Program exited");
	}

	public static class TestServer extends CoolSocket
	{
		private int connectionRemains = 10;

		public TestServer()
		{
			super(PORT_SERVER);
		}

		@Override
		public void onServerStarted()
		{
			super.onServerStarted();
			log(String.format("Server started on port %d", getLocalPort()));
		}

		@Override
		public void onServerStopped()
		{
			super.onServerStopped();
			log("Server stopped");
		}

		@Override
		protected void onConnected(ActiveConnection activeConnection)
		{
			connectionRemains--;

			try {
				ActiveConnection.Response response = activeConnection.receive();

				log("Client says: %s; with total character of: %d; header of: %s",
						response.response,
						response.totalLength,
						response.headerIndex.toString());

				activeConnection.reply(String.format("Thanks, remaining server shutdown step is %s", connectionRemains));

				activeConnection.getSocket().close();
			} catch (IOException | TimeoutException e) {
				e.printStackTrace();
			}

			if (connectionRemains == 0)
				stop();
		}
	}

	public static void log(String print)
	{
		log(print, (Object[]) null);
	}

	public static void log(String print, Object... formatted)
	{
		StringBuilder stringBuilder = new StringBuilder()
				.append(Main.class.getName())
				.append("(): ")
				.append(print);

		System.out.println(print == null
				? stringBuilder.toString()
				: String.format(stringBuilder.toString(), formatted));
	}
}
