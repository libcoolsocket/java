package com.genonbeta.CoolSocket;

import java.net.*;
import java.io.*;
import org.json.*;

public abstract class CoolJsonCommunication extends CoolCommunication
{
	public CoolJsonCommunication()
	{
	}

	public CoolJsonCommunication(int port)
	{
		super(port);
	}

	public CoolJsonCommunication(String address, int port)
	{
		super(address, port);
	}
	
	@Override
	protected final void onMessage(Socket socket, String message, PrintWriter writer, String clientIp)
	{
		// TODO: Implement this method
		
		try
		{
			JSONObject receivedMessage = new JSONObject(message);
			JSONObject responseJson = new JSONObject();
			
			this.onRequest(socket, receivedMessage, responseJson, clientIp);

			writer.append(responseJson.toString());
			writer.flush();
		}
		catch (JSONException e)
		{
			this.onError(e);
		}
	}
	
	public abstract void onRequest(Socket socket, JSONObject receivedMessage, JSONObject response, String clientIp);
	
	public static abstract class JsonExpandedResponseHandler implements Messenger.ExpandedResponseHandler
	{
		@Override
		public void onMessage(Socket socket, PrintWriter response)
		{
			// TODO: Implement this method
			JSONObject json = new JSONObject();
			
			this.onJsonMessage(socket, json);
			
			response.append(json.toString());
			response.flush();
		}
		
		public abstract void onJsonMessage(Socket socket, JSONObject json);
	}
}
