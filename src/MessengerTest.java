
import java.util.*;
import com.genonbeta.CoolSocket.*;public class MessengerTest
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
		Sentences.add("Tacizin asikar");
		Sentences.add("Sadece bana mahsus degil yokolma seanslari");
		Sentences.add("Yagmur sakladi gozyaslarimi");
		Sentences.add("Kopya kalplere damladim dam dam");
	}
}
