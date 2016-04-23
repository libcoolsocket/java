import com.genonbeta.CoolSocket.*;
import java.io.*;
import java.net.*;

public class TransferTest
{
	private static MySender sender = new MySender();
	private static MyReceiver receiver = new MyReceiver();
	
	public static void main()
	{	
		receiver.setNotifyDelay(1000);
		sender.setNotifyDelay(1000);
		
		try
		{
			Thread.sleep(1000);
			
			System.out.println("\n--- CoolTransfer Test ---\n");

			File sF = new File("/storage/extSdCard/Videos/Music & Others/WILLY WILLIAM - Ego.mp4"); // file to send
			File rF = new File("/storage/extSdCard/receive.mp4"); // file to receive

			if (rF.isFile())
				rF.delete();
			
			rF.createNewFile();

			receiver.receive(3333, rF, sF.length(), new byte[8192], 10000, null);
			sender.send("0.0.0.0", 3333, sF, new byte[8192], null);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public static class MyReceiver extends CoolTransfer.Receive
	{
		@Override
		public boolean onStart(int port, File file, Object extra)
		{
			// by returning false you can force the thread to exit directly.
			return true;
		}

		@Override
		public boolean onBreakRequest(int port, File file, Object extra)
		{
			// if you return true, thread will be considered as cancelled otherwise only thing that can stop concurrent process will be ThreadHandler.requestBreak()
			return super.onBreakRequest(port, file, extra);
		}

		@Override
		public void onError(int port, File file, Exception error, Object extra)
		{
			System.out.println("Receiver: error = " + error.getMessage());
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
		public boolean onStart(String serverIp, int port, File file, Object extra)
		{
			// by returning false you can force the thread to exit directly.
			return true;
		}
		
		@Override
		public boolean onBreakRequest(String serverIp, int port, File file, Object extra)
		{
			// if you return true, thread will be considered as cancelled otherwise only thing that can stop concurrent process will be ThreadHandler.requestBreak()
			return super.onBreakRequest(serverIp, port, file, extra);
		}
		
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
