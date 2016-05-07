import com.genonbeta.CoolSocket.*;
import com.genonbeta.core.util.*;
import java.io.*;
import java.net.*;
import java.util.*;
import org.json.*;

public class Main implements Serializable
{
	public static void main(String[] args)
	{
		final Cool cool = new Cool();
		final Cool.Messenger msn = new Cool.Messenger();
		
		cool.start();
		cool.setMaxConnections(5);
		
		System.out.println("Main@@: Try to stop server = " + cool.stop()); // stop test
		System.out.println("Main@@: Try to start server = " + cool.startDelayed(5000)); // restart test
		
		msn.send("127.0.0.1", 4000, null, 
			new Cool.JsonResponseHandler()
			{
				@Override
				public void onJsonMessage(Socket socket, CoolCommunication.Messenger.Process process, JSONObject json)
				{
					try
					{
						json.put("Client", "OK");
						
						System.out.println("Main@@: Active Connections = " + cool.getConnections().size());
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
				System.out.println("Server: Connected to " + clientIp);
				System.out.println("Server: Received = " + receivedMessage.toString());
				
				response.put("Server", "OK");
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
			System.out.println("Server: Closing connection of " + client.getAddress().getHostAddress());
		}

		@Override
		protected void onError(Exception exception)
		{
			System.out.println("Server: " + ((this.isInterrupted()) ? "Server stopped" : " Error = " + exception));
		}	
	}
}
