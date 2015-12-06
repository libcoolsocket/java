package com.genonbeta.CoolSocket.test;

import android.content.*;
import android.os.*;
import android.support.v7.app.*;
import android.text.*;
import android.util.*;
import android.view.*;
import android.widget.*;
import com.genonbeta.CoolSocket.*;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.xml.transform.*;
import com.genonbeta.CoolSocket.test.adapter.*;

public class Main extends AppCompatActivity
{
	public static final String ACTION_UPDATE = "com.genonbeta.CoolSocket.test.ACTION_UODATE";
	public static final String EXTRA_MESSAGE = "extraMessage";
	public static final String EXTRA_PEER_ADDRESS = "extraPeerAddress";

	public static final int REQUEST_CHOOSE_PEER = 15;

	private Receiver mReceiver = new Receiver();
	private MessageSenderHandler mSenderHandler = new MessageSenderHandler();
	private Cool mCool = new Cool();
	private IntentFilter mFilter = new IntentFilter();

	private EditText mEditText;
	private EditText mEditTextServer;
	private EditText mEditTextPort;
	private ListView mListView;

	private ArrayList<String> mList = new ArrayList<String>();
	private MessageListAdapter mAdapter;

	public void addMessage(String message)
	{
		if (message.length() < 1)
			return;	

		mList.add(message);
		mAdapter.notifyDataSetChanged();

		mListView.smoothScrollToPosition(mAdapter.getCount());
	}

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
		
		mFilter.addAction(ACTION_UPDATE);

		mCool.start();

		mEditText = (EditText) findViewById(R.id.mainEditText);
		mEditTextServer = (EditText) findViewById(R.id.mainServer);
		mEditTextPort = (EditText) findViewById(R.id.mainPort);

		mListView = (ListView) findViewById(R.id.mainListView);
		mAdapter = new MessageListAdapter(this, mList);

		mEditText.setOnKeyListener(
			new View.OnKeyListener()
			{
				@Override
				public boolean onKey(View p1, int p2, KeyEvent p3)
				{
					// TODO: Implement this method
					if (KeyEvent.KEYCODE_ENTER == p2)
					{
						Editable edit = mEditText.getText();

						try 
						{
							final String server = mEditTextServer.getText().toString();
							final int port = Integer.parseInt(mEditTextPort.getText().toString());

							Cool.Messenger.send(server, port, edit.toString(), Main.this.mSenderHandler);

							edit.clear();
						}
						catch (Exception e)
						{
							Toast.makeText(Main.this, "Text couldn't be send (" + e.getMessage() + ")", Toast.LENGTH_SHORT).show();
						}

						return true;
					}
					return false;
				}
			}
		);

		mListView.setAdapter(mAdapter);

		Toast.makeText(Main.this, "Server is ready", Toast.LENGTH_SHORT).show();
    }

	@Override
	protected void onStart()
	{
		// TODO: Implement this method
		super.onStart();
		PairListHolder.update();
	}

	@Override
	protected void onResume()
	{
		// TODO: Implement this method
		super.onResume();
		registerReceiver(mReceiver, mFilter);
	}

	@Override
	protected void onPause()
	{
		// TODO: Implement this method
		super.onPause();
		unregisterReceiver(mReceiver);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		// TODO: Implement this method
		super.onSaveInstanceState(outState);

		outState.putString("server", mEditTextServer.getText().toString());
		outState.putString("port", mEditTextPort.getText().toString());
		outState.putString("text", mEditText.getText().toString());
		outState.putStringArrayList("list", mList);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState)
	{
		// TODO: Implement this method
		super.onRestoreInstanceState(savedInstanceState);

		mEditTextServer.getText().clear();
		mEditTextPort.getText().clear();
		mEditText.getText().clear();
		mList.clear();

		mEditTextServer.getText().append(savedInstanceState.getString("server", "0.0.0.0"));
		mEditTextPort.getText().append(savedInstanceState.getString("port", "3000"));
		mEditText.getText().append(savedInstanceState.getString("text", ""));
		mList.addAll(savedInstanceState.getStringArrayList("list"));

		mAdapter.notifyDataSetChanged();

		mListView.smoothScrollToPosition(mList.size());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// TODO: Implement this method
		menu.add("Pair finder");
		menu.add("About").setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		// TODO: Implement this method
		if ("About".equals(item.getTitle()))
		{
			AlertDialog.Builder dialog = new AlertDialog.Builder(this);

			dialog.setTitle("About CoolSocket");
			dialog.setMessage("This application is developed to test Genonbeta CoolSocket API\n\nCoolSocket provides socket communation api");

			dialog.show();
		}
		else if ("Pair finder".equals(item.getTitle()))
		{
			startActivityForResult(new Intent(Main.this, PairFinder.class), REQUEST_CHOOSE_PEER);
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		// TODO: Implement this method
		super.onActivityResult(requestCode, resultCode, data);
		
		Log.d("TTTT", requestCode + " " + resultCode + " " + (data == null));
		
		if (data == null)
			return;

		if (resultCode == RESULT_OK)
			switch (requestCode)
			{
				case REQUEST_CHOOSE_PEER:
					if (data.hasExtra(EXTRA_PEER_ADDRESS))
					{
						mEditTextServer.getText().clear();
						mEditTextServer.getText().append(data.getStringExtra(EXTRA_PEER_ADDRESS));
					}
					break;
			}
	}

	@Override
	protected void onDestroy()
	{
		// TODO: Implement this method
		super.onDestroy();
		mCool.stop();
	}

	private class Cool extends CoolCommunication
	{
		public Cool()
		{
			super(3000);
		}

		@Override
		protected void onMessage(Socket socket, String message, PrintWriter writer, String clientIp)
		{
			// TODO: Implement this method
			if (message.length() > 0)
				sendBroadcast(new Intent(ACTION_UPDATE).putExtra(EXTRA_MESSAGE, "<" + clientIp + "> " + message));
		}

		@Override
		protected void onError(Exception exception)
		{
			// TODO: Implement this method
			Toast.makeText(Main.this, "An server error occurred (" + exception.getMessage() + ")", Toast.LENGTH_SHORT).show();
		}
	}

	public class Receiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			// TODO: Implement this method
			if (intent != null && ACTION_UPDATE.equals(intent.getAction()) && intent.hasExtra(EXTRA_MESSAGE))
			{
				Main.this.addMessage(intent.getStringExtra(EXTRA_MESSAGE));
			}
		}
	}

	private class MessageSenderHandler implements CoolCommunication.Messenger.ResponseHandler
	{
		@Override
		public void onConfigure(CoolCommunication.Messenger.SenderRunnable runnable)
		{
			// TODO: Implement this method
		}

		@Override
		public void onResponseAvaiable(String response)
		{
			// TODO: Implement this method
			if (response != null && !response.equals(""))
				Main.this.sendBroadcast(new Intent(ACTION_UPDATE).putExtra(EXTRA_MESSAGE, "Server.response: " + response));
		}

		@Override
		public void onError(Exception exception)
		{
			// TODO: Implement this method
		}

		@Override
		public void onFinal()
		{
			// TODO: Implement this method
		}
	}
}
