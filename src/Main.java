import com.genonbeta.CoolSocket.*;
import java.io.*;
import java.net.*;
import org.json.*;
import com.genonbeta.core.util.*;
import com.genonbeta.CoolSocket.CoolJsonCommunication.JsonResponseHandler.*;
import com.genonbeta.CoolSocket.CoolCommunication.Messenger.*;

public class Main implements Serializable
{
	public static void main(String[] args)
	{
		final Cool cool = new Cool();
		final Cool.Messenger msn = new Cool.Messenger();
		
		System.out.println("\n--- Before we start ---\n");
		
		for (NetworkInterface inetAddress : NetworkUtils.getInterfaces(true, new String[] {"rmnet"}).keySet())
		{
			System.out.println(inetAddress.getName());
		}
		
		System.out.println("\n--- Lets start ---\n");
		
		cool.start();
		cool.setMaxConnections(5);
		
		msn.send("127.0.0.1", 4000, null, 
			new Cool.JsonResponseHandler()
			{
				@Override
				public void onJsonMessage(Socket socket, CoolCommunication.Messenger.Process process, JSONObject json)
				{
					try
					{
						json.put("Client", "OK");
						
						System.out.println("Main: Active Connections = " + cool.getConnections().size());
						System.out.println("Client: Received = " + process.waitForResponse());
					}
					catch (JSONException e)
					{
						this.onError(e);
					}
				}

				@Override
				public void onConfigure(CoolCommunication.Messenger.Process process)
				{
					process.setSocketTimeout(2000);
				}

				@Override
				public void onError(Exception exception)
				{
					System.out.println("Client: Error = " + exception);
				}
			}
		);
		
		TransferTest.main();
		MessengerTest.main();
	}

	public static class Cool extends CoolJsonCommunication
	{
		public Cool()
		{
			super(4000);
			this.setSocketTimeout(2000);
		}
		
		@Override
		public void onJsonMessage(Socket socket, JSONObject receivedMessage, JSONObject response, String clientIp)
		{
			try
			{
				response.put("Server", "OK");
				System.out.println("Server: Received = " + receivedMessage.toString());
			}
			catch (JSONException e)
			{
				e.printStackTrace();
			}
		}

		@Override
		protected void onClosingConnection(CoolSocket.ClientHandler client)
		{
			super.onClosingConnection(client);
			System.out.println("Server: Closing connection of " + client.getSocket().getInetAddress().getHostAddress());
		}

		@Override
		protected void onError(Exception exception)
		{
		}	
	}
}
