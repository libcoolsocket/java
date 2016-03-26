import com.genonbeta.CoolSocket.*;
import java.io.*;
import java.net.*;

public class TransferTest
{
	private static MySender s = new MySender();
	private static MyReceiver r = new MyReceiver();
	
	public static void main()
	{	
		try
		{
			Thread.sleep(1000);
			
			System.out.println("\n--- CoolTransfer Test ---\n");

			File sF = new File("/home/veli/fileToSend");
			File rF = new File("/home/veli/fileToReceive");

			if (rF.isFile())
				rF.delete();
			
			rF.createNewFile();

			r.receive(3333, rF, sF.length(), new byte[1024], 0, null);
			
			Thread.sleep(1000);
			
			s.send("0.0.0.0", 3333, sF, new byte [4096], null);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public static class MyReceiver extends CoolTransfer.Receive
	{
		@Override
		public void onError(int port, File file, Exception error, Object extra)
		{
			System.out.println("Receiver: error");
		}

		@Override
		public void onNotify(Socket socket, int port, File file, int percent, Object extra)
		{
			System.out.println("Receiver: received %" + percent);
		}

		@Override
		public void onTransferCompleted(int port, File file, Object extra)
		{
			System.out.println("Receiver: completed");
		}

		@Override
		public void onSocketReady(ServerSocket socket, int port, File file, Object extra)
		{
			System.out.println("Receiver: ready ; file = " + file + "; port = " + port + "; ");
		}
	}

	public static class MySender extends CoolTransfer.Send
	{
		@Override
		public void onError(String serverIp, int port, File file, Exception error, Object e)
		{
			System.out.println("Sender: error (" + error + ")");
		}

		@Override
		public void onNotify(Socket socket, String serverIp, int port, File file, int percent, Object extra)
		{
		}

		@Override
		public void onTransferCompleted(String serverIp, int port, File file, Object extra)
		{
			System.out.println("Sender: completed");
		}

		@Override
		public void onSocketReady(Socket socket, String serverIp, int port, File file, Object extra)
		{
			System.out.println("Sender: ready ; file = " + file + "; serverIp = " + serverIp + "; port = " + port + "; ");
		}
	}
}
