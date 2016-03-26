
import java.util.*;
import com.genonbeta.CoolSocket.*;

public class MessengerTest
{
	public static final ArrayList<String> Sentences = new ArrayList<String>();
	
	public static void main()
	{
		loadSentences();
		
		Runnable runnableInstance = new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					Thread.sleep(4000);
					
					for (String text : Sentences)
					{
						Thread.sleep(1000);
						CoolCommunication.Messenger.send("0.0.0.0", 3000, text, null);
					}
				}
				catch (Exception e)
				{}
			}
		};
		
		new Thread(runnableInstance).start();
	}
	
	public static void loadSentences()
	{
		Sentences.add("This is CoolSocket");
		Sentences.add("CoolSocket doesnt care about other java socket implementations");
		Sentences.add("Because CoolSocket knows he is different");
		Sentences.add("CoolSocket is smart. Be like CoolSocket");
	}
}
