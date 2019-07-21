package com.genonbeta.CoolSocket;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

/**
 * CoolSocket is TCP socket server with a set of features like keep-alive connections, header
 * support, connection management etc. It only uses native libraries and JSON to do its job.
 */
abstract public class CoolSocket
{
	public static final String TAG = CoolSocket.class.getSimpleName();

	public static final int NO_TIMEOUT = -1;

	public static final String HEADER_SEPARATOR = "\nHEADER_END\n";
	public static final String HEADER_ITEM_LENGTH = "length";

	private final ArrayList<CoolSocket.ActiveConnection> mConnections = new ArrayList<>();
	private ExecutorService mExecutor;
	private Thread mServerThread;
	private ServerSocket mServerSocket;
	private SocketAddress mSocketAddress = null;
	private int mSocketTimeout = NO_TIMEOUT; // no timeout
	private int mMaxConnections = 10;
	private ServerRunnable mSocketRunnable = new ServerRunnable();

	/**
	 * Creates a CoolSocket instance.
	 */
	public CoolSocket()
	{
	}

	/**
	 * Creates a CoolSocket instance that will be available to the local machine.
	 * @see CoolSocket#setSocketAddress(SocketAddress)
	 * @param port Port that CoolSocket will run on. A neutral zero would mean any port that is available.
	 */
	public CoolSocket(int port)
	{
		this.mSocketAddress = new InetSocketAddress(port);
	}

	/**
	 * Creates a CoolSocket instance that will be available to an address range.
	 * @see CoolSocket#setSocketAddress(SocketAddress)
	 * @param address IPv4 address for network interface.
	 * @param port Port that CoolSocket will run on. A neutral zero would mean any port that is available.
	 */
	public CoolSocket(String address, int port)
	{
		this.mSocketAddress = new InetSocketAddress(address, port);
	}

	/**
	 * When a client is connected, this method will be called.
	 * @param activeConnection The connection object that represents the client.
	 */
	protected abstract void onConnected(ActiveConnection activeConnection);

	/**
	 * This emulates a connection to a CoolSocket server where the task is carried on the Thread
	 * that called it.
	 * @param handler The handler that will handle the connection.
	 * @param clazz The class that will be used to cast the result object.
	 * @param <T> Any object that the Client will return.
	 * @return
	 */
	public static <T> T connect(final Client.ConnectionHandler handler, Class<T> clazz)
	{
		Client clientInstance = new Client();

		handler.onConnect(clientInstance);

		return clientInstance.getReturn() != null && clazz != null
				? clazz.cast(clientInstance.getReturn())
				: null;
	}

	/**
	 * Connect to a CoolSocket server.
	 * @param handler The handler that will handle the connection.
	 */
	public static void connect(final Client.ConnectionHandler handler)
	{
		new Thread()
		{
			@Override
			public void run()
			{
				super.run();
				connect(handler, null);
			}
		}.start();
	}

	/**
	 * Counts the total connection of a client to the CoolSocket server.
	 * @param address Client address.
	 * @return The total number of connections.
	 */
	public int getConnectionCountByAddress(InetAddress address)
	{
		int returnObject = 0;

		for (ActiveConnection activeConnection : getConnections())
			if (activeConnection.getAddress().equals(address))
				returnObject++;

		return returnObject;
	}

	/**
	 * Returns the active connections to the server. You should copy the members if the task you
	 * need to carry out will take long. The copying process should be synchronized to prevent
	 * errors.
	 * @return The original list instance that holds the connections objects.
	 */
	public synchronized List<ActiveConnection> getConnections()
	{
		return this.mConnections;
	}

	/**
	 * Returns the executor that starts child threads as needed. The child threads are used to allow
	 * simultaneous connections.
	 * @return The executor.
	 */
	public ExecutorService getExecutor()
	{
		if (mExecutor == null)
			mExecutor = Executors.newFixedThreadPool(mMaxConnections);

		return mExecutor;
	}

	/**
	 * This should not be called before the {@link CoolSocket#start()} is called.
	 * If the server was started with random port, this returns the port
	 * assigned to the server.
	 * @return The port that the server is running on.
	 */
	public int getLocalPort()
	{
		return this.getServerSocket().getLocalPort();
	}

	/**
	 * @return The server socket object.
	 */
	protected ServerSocket getServerSocket()
	{
		return this.mServerSocket;
	}

	/**
	 * @return The server thread.
	 */
	protected Thread getServerThread()
	{
		return this.mServerThread;
	}

	/**
	 * This returns the socket address that is used to run the server.
	 * @return Null if nothing was assigned before the original instance
	 * of the socket address.
	 */
	public SocketAddress getSocketAddress()
	{
		return this.mSocketAddress;
	}

	/**
	 * @return The socket runnable.
	 */
	protected ServerRunnable getSocketRunnable()
	{
		return this.mSocketRunnable;
	}

	/**
	 * Returns The timeout defines the time before the server gives up waiting for receiving or sending the
	 * response.
	 * @return The timeout in millisecond. If not defined previously, it might be {@link CoolSocket#NO_TIMEOUT}.
	 */
	public int getSocketTimeout()
	{
		return this.mSocketTimeout;
	}

	/**
	 * This checks whether the server is ready to be started or whatever the task is needed to ensure
	 * that the server is started or can be started.
	 * @return The result of the safety check.
	 */
	public boolean isComponentsReady()
	{
		return this.getServerSocket() != null && this.getServerThread() != null && this.getSocketAddress() != null;
	}

	/**
	 * This checks whether there is a server thread and if it is running
	 * @return False if the thread is not running, true otherwise.
	 */
	public boolean isInterrupted()
	{
		return this.getServerThread() == null ||
				this.getServerThread().isInterrupted();
	}

	/**
	 * This checks if the server thread is alive.
	 * @return True if it is.
	 */
	public boolean isServerAlive()
	{
		return this.getServerThread() != null
				&& this.getServerThread().isAlive();
	}

	/**
	 * When a client is connected, to not block the server thread, we call this method to communicate
	 * with it. The socket is different from a normal socket connection where the data should also
	 * contain a header.
	 * @param socket The socket that is connected to the client.
	 * @return True if the switching process to a child thread is successful.
	 */
	protected boolean respondRequest(final Socket socket)
	{
		if (this.getConnections().size() <= this.mMaxConnections || this.mMaxConnections == 0) {
			final ActiveConnection connectionHandler = new ActiveConnection(socket, CoolSocket.this.mSocketTimeout);

			synchronized (getConnections()) {
				getConnections().add(connectionHandler);
			}

			getExecutor().submit(new Runnable()
			{
				@Override
				public void run()
				{
					try {
						if (CoolSocket.this.mSocketTimeout > NO_TIMEOUT)
							connectionHandler.getSocket().setSoTimeout(CoolSocket.this.mSocketTimeout);
					} catch (SocketException e) {
						e.printStackTrace();
					}

					onConnected(connectionHandler);

					try {
						if (!socket.isClosed()) {
							System.out.println(TAG + ": You should close connections in the end of onConnected(ActiveConnection) method");
							socket.close();
						}
					} catch (IOException e) {
						e.printStackTrace();
					} finally {
						synchronized (getConnections()) {
							getConnections().remove(connectionHandler);
						}
					}
				}
			});
		} else
			return false;

		return true;
	}

	/**
	 * Replaces the executor.
	 * @param executor The executor that will handle starting threads for requests and connections.
	 */
	public void setExecutor(ExecutorService executor)
	{
		mExecutor = executor;
	}

	/**
	 * Defines the maximum connection number that the server is allowed to handle at the same time.
	 * There is always a limit for safety purposes {@link CoolSocket#mMaxConnections}.
	 * @param value A positive number to limit the maximum allowed connections.
	 */
	public void setMaxConnections(int value)
	{
		this.mMaxConnections = value;
	}

	/**
	 * Replaces the socket address that the server will run on.
	 * @param address The address that is used to start the server.
	 */
	public void setSocketAddress(SocketAddress address)
	{
		this.mSocketAddress = address;
	}

	/**
	 * Sets the timeout that limits the time CoolSocket waits for the response delivery. The limit is
	 * between the last time a byte arrives and the current time, not the time request was made.
	 * @see CoolSocket#getSocketTimeout()
	 * @param timeout Time in millisecond.
	 */
	public void setSocketTimeout(int timeout)
	{
		this.mSocketTimeout = timeout;
	}

	/**
	 * Starts the CoolSocket server.
	 * @see CoolSocket#startDelayed(int)
	 * @see CoolSocket#startEnsured(int)
	 * @see CoolSocket#onServerStarted()
	 * @return Returns if the server will be started.
	 */
	public boolean start()
	{
		if (this.getServerSocket() == null || this.getServerSocket().isClosed()) {
			try {
				this.mServerSocket = new ServerSocket();
				this.getServerSocket().bind(this.mSocketAddress);
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
		}

		if (this.getServerThread() == null || Thread.State.TERMINATED.equals(this.getServerThread().getState())) {
			this.mServerThread = new Thread(this.getSocketRunnable());

			this.getServerThread().setDaemon(true);
			this.getServerThread().setName(TAG + " Main Thread");
		} else if (this.getServerThread().isAlive())
			return false;

		this.getServerThread().start();

		return true;
	}

	/**
	 * Starts the CoolSocket server ensuring {@link CoolSocket#stop()} call is successful. This should
	 * only be used when the server is expected to stop or not running.
	 * @see CoolSocket#start()
	 * @see CoolSocket#startEnsured(int)
	 * @see CoolSocket#onServerStarted()
	 * @param timeout The time that it will block the thread the request was made on.
	 * @return False if the server is running and did not stop or failed to start. True
	 * if the server will be started{@link CoolSocket#start()}
	 */
	public boolean startDelayed(int timeout)
	{
		long startTime = System.currentTimeMillis();

		while (this.isServerAlive()) {
			if ((System.currentTimeMillis() - startTime) > timeout)
				// We did not request start but it was already running, so it was rather not
				// requested to stop or the server blocked itself and does not respond
				return false;
		}

		return this.start();
	}

	/**
	 * Starts the server ensuring previous {@link CoolSocket#stop()} call is successful or the server
	 * was already not running and is started after the {@link CoolSocket#startDelayed(int)} call.
	 * @see CoolSocket#start()
	 * @see CoolSocket#startDelayed(int)
	 * @see CoolSocket#onServerStarted()
	 * @param timeout The time that it will block the thread the request was made on.
	 * @return True if the server is started and ready to accept new connections.
	 */
	public boolean startEnsured(int timeout)
	{
		long startTime = System.currentTimeMillis();

		if (!this.startDelayed(timeout))
			return false;

		while (!this.isServerAlive())
			if ((System.currentTimeMillis() - startTime) > timeout)
				return false;

		return true;
	}

	/**
	 * Stops the CoolSocket server.
	 * @return True if the server was running and has been stopped.
	 */
	public boolean stop()
	{
		if (this.isInterrupted())
			return false;

		this.getServerThread().interrupt();

		if (!this.getServerSocket().isClosed()) {
			try {
				this.getServerSocket().close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return true;
	}

	/**
	 * Override this method to be informed when the server is started.
	 */
	public void onServerStarted()
	{
	}

	/**
	 * Override this method to be informed when the server is stopped.
	 */
	public void onServerStopped()
	{
	}

	/**
	 * This method will be called when the connection handling failed or something that was not
	 * expected happened.
	 * @param exception The error that occurred.
	 */
	public void onInternalError(Exception exception)
	{
	}

	/**
	 * This class represents a connection to CoolServer client and server.
	 */
	public static class ActiveConnection
	{
		private Socket mSocket;
		private int mTimeout = NO_TIMEOUT;
		private int mId = getClass().hashCode();

		/**
		 * An instance that uses its own socket. Call {@link ActiveConnection(Socket)} with null
		 * argument if the socket will be provided later.
		 */
		public ActiveConnection()
		{
			this(new Socket());
		}

		/**
		 * An instance that uses its own socket with a timeout. Call {@link ActiveConnection(Socket)}
		 * with null argument if the socket will be provided later.
		 * @param timeout Timeout that will limit the amount of time that the requests to wait for
		 *                another packet to arrive or go.
		 */
		public ActiveConnection(int timeout)
		{
			this(new Socket(), timeout);
		}

		/**
		 * An instance with socket connection to a CoolSocket server.
		 * @param socket The connection to CoolSocket server or client.
		 */
		public ActiveConnection(Socket socket)
		{
			mSocket = socket;
		}

		/**
		 * An instance with timeout and socket connection to a CoolSocket server.
		 * @param socket The connection to CoolSocket server or client.
		 * @param timeout Timeout that will limit the amount of time that the requests to wait for
		 *                another packet to arrive or go.
		 */
		public ActiveConnection(Socket socket, int timeout)
		{
			this(socket);
			setTimeout(timeout);
		}

		/**
		 * Connects to a CoolSocket server.
		 * @param socketAddress The address of CoolSocket server.
		 * @return Returns the instance of this class.
		 * @throws IOException When connection fails.
		 */
		public ActiveConnection connect(SocketAddress socketAddress) throws IOException
		{
			if (getTimeout() != NO_TIMEOUT)
				getSocket().setSoTimeout(getTimeout());

			getSocket().bind(null);
			getSocket().connect(socketAddress);


			return this;
		}

		/**
		 * This ensures the connection is closed before this instance of the class is about to be
		 * destroyed.
		 * @throws Throwable Override to use this feature.
		 * @deprecated For it is not clear that the call for this method will be immediate after it
		 * is out of scope, it is deprecated and might be removed in the future and should not be
		 * relied on.
		 */
		@Override
		@Deprecated
		protected void finalize() throws Throwable
		{
			super.finalize();

			if (getSocket() != null && !getSocket().isClosed()) {
				System.out.println(TAG + ": Connections should be closed before their references are being destroyed");
				getSocket().close();
			}
		}

		/**
		 * This should be called after ensuring that the socket is provided
		 * @return The address that the socket is bound to.
		 */
		public InetAddress getAddress()
		{
			return this.getSocket().getInetAddress();
		}

		/**
		 * This should be called after ensuring that the socket is provided.
		 * @return The readable address the socket is bound to.
		 */
		public String getClientAddress()
		{
			return this.getAddress().getHostAddress();
		}

		/**
		 * A proposed method to determine a connection with a unique id.
		 * @see ActiveConnection#setId(int)
		 * @return The class id defined during creation of this instance of the class.
		 */
		public int getId()
		{
			return mId;
		}

		/**
		 * The socket that is used to communicate
		 * @return Null if no socket was provided or the socket instance.
		 */
		public Socket getSocket()
		{
			return this.mSocket;
		}

		/**
		 * On server side, this is defined with the given server timeout by default. When used on client
		 * side, it is defined by the method associated with it.
		 * @return Timeout in milliseconds.
		 */
		public int getTimeout()
		{
			return mTimeout;
		}

		/**
		 * This uses {@link Object#toString()} method which will return the unique id to this instance of
		 * the class to determine whether they are the same instance.
		 * @param obj An instance of this class is expected and if not then the parent class will handle it.
		 * @return True if the two object is the same.
		 */
		@Override
		public boolean equals(Object obj)
		{
			return obj instanceof ActiveConnection ? obj.toString().equals(toString()) : super.equals(obj);
		}

		/**
		 * When the opposite side calls {@link ActiveConnection#reply(String)}, this method should
		 * be invoked so that the communication occurs. The order of the calls should be in order
		 * with their peers. For instance, when you call {@link ActiveConnection#reply(String)} method,
		 * the other side should already have called this method or vice versa, which means asynchronous
		 * task should not block the thread when the data is being transferred.
		 * @see ActiveConnection#reply(String)
		 * @return The response that is received.
		 * @throws IOException When a socket IO error occurs.
		 * @throws TimeoutException When the amount time exceeded while waiting for another byte to
		 * transfer.
		 * @throws JSONException When the JSON parsing error occurs.
		 */
		public Response receive() throws IOException, TimeoutException, JSONException
		{
			byte[] buffer = new byte[8096];
			int len;
			long calculatedTimeout = getTimeout() != NO_TIMEOUT ? System.currentTimeMillis() + getTimeout() : NO_TIMEOUT;

			InputStream inputStream = getSocket().getInputStream();
			ByteArrayOutputStream headerIndex = new ByteArrayOutputStream();
			ByteArrayOutputStream receivedMessage = new ByteArrayOutputStream();

			Response response = new Response();
			response.remoteAddress = getSocket().getRemoteSocketAddress();

			do {
				if ((len = inputStream.read(buffer)) > 0) {
					if (response.totalLength != -1) {
						receivedMessage.write(buffer, 0, len);
						receivedMessage.flush();
					} else {
						headerIndex.write(buffer, 0, len);
						headerIndex.flush();

						if (headerIndex.toString().contains(HEADER_SEPARATOR)) {
							String headerString = headerIndex.toString();
							int headerEndPoint = headerString.indexOf(HEADER_SEPARATOR);

							JSONObject headerJSON = new JSONObject(headerString.substring(0, headerEndPoint));
							response.totalLength = headerJSON.getLong(HEADER_ITEM_LENGTH);
							response.headerIndex = headerJSON;

							if (headerEndPoint < headerIndex.size())
								// When the bytes are transferred as they come, we might exceed the
								// point where the header ends. If it is the case, then we trim the
								// header and the response accordingly
								receivedMessage.write(headerString.substring(headerEndPoint + (HEADER_SEPARATOR.length())).getBytes());
						}
					}
				}

				if (calculatedTimeout != NO_TIMEOUT && System.currentTimeMillis() > calculatedTimeout)
					throw new TimeoutException("Read timed out!");
			}
			while (response.totalLength != receivedMessage.size() && response.totalLength != 0);

			response.response = receivedMessage.toString();

			return response;
		}

		/**
		 * This will send the given data to the other side while the other side has already called
		 * {@link ActiveConnection#receive()}.
		 * @see ActiveConnection#receive()
		 * @param out The data that should be sent.
		 * @throws IOException When a socket IO error occurs.
		 * @throws TimeoutException When the amount time exceeded while waiting for another byte to
		 * transfer.
		 * @throws JSONException When the JSON parsing error occurs.
		 */
		public void reply(String out) throws TimeoutException, IOException, JSONException
		{
			byte[] outputBytes = out == null ? new byte[0] : out.getBytes();

			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			PrintWriter outputWriter = new PrintWriter(outputStream);

			JSONObject headerJSON = new JSONObject()
					.put(HEADER_ITEM_LENGTH, outputBytes.length);

			outputWriter.write(headerJSON.toString() + HEADER_SEPARATOR);
			outputWriter.flush();

			byte[] buffer = new byte[8096];
			int len;
			long calculatedTimeout = getTimeout() != NO_TIMEOUT ? System.currentTimeMillis() + getTimeout() : NO_TIMEOUT;

			ByteArrayInputStream inputStream = new ByteArrayInputStream(outputBytes);
			DataOutputStream remoteOutputStream = new DataOutputStream(getSocket().getOutputStream());

			remoteOutputStream.write(outputStream.toByteArray());
			remoteOutputStream.flush();

			do {
				if ((len = inputStream.read(buffer)) > 0) {
					remoteOutputStream.write(buffer, 0, len);
					remoteOutputStream.flush();
				}

				if (calculatedTimeout != NO_TIMEOUT && System.currentTimeMillis() > calculatedTimeout)
					throw new TimeoutException("Read timed out!");
			}
			while (len != -1);
		}

		/**
		 * Sets the id for this class.
		 * @param id That you want to change to.
		 */
		public void setId(int id)
		{
			mId = id;
		}

		/**
		 * Sets the timeout
		 * @see ActiveConnection#getSocketTimeout()
		 * @param timeout The timout in milliseconds.
		 */
		public void setTimeout(int timeout)
		{
			this.mTimeout = timeout;
		}

		/**
		 * This class represents the response received from the opposite side that the CoolSocket
		 * is connected to.
		 * @see ActiveConnection#receive()
		 */
		public class Response
		{
			/**
			 * The remote that sent the response.
			 */
			public SocketAddress remoteAddress;
			/**
			 * Header for the response.
			 */
			public JSONObject headerIndex;
			/**
			 * The response.
			 * @see Response#totalLength
			 */
			public String response;
			/**
			 * The length of the response.
			 */
			public long totalLength = -1;

			/**
			 * Creates an instance of this class. This does nothing for all the related
			 * members are public.
			 */
			public Response()
			{
			}
		}
	}

	/**
	 * Handles the running process of the server.
	 */
	private class ServerRunnable implements Runnable
	{
		@Override
		public void run()
		{
			try {
				onServerStarted();

				do {
					Socket request = CoolSocket.this.getServerSocket().accept();

					if (CoolSocket.this.isInterrupted())
						request.close();
					else
						respondRequest(request);
				}
				while (!CoolSocket.this.isInterrupted());
			} catch (IOException e) {
				CoolSocket.this.onInternalError(e);
			} finally {
				onServerStopped();
			}
		}
	}

	/**
	 * This class helps the connection process to a CoolSocket server.
	 */
	public static class Client
	{
		private Object mReturn;

		/**
		 * Connects to a CoolSocket server.
		 * @param socketAddress The server address.
		 * @param operationTimeout The timeout before the operation is declared to have failed.
		 * @return The connection object.
		 * @throws IOException When the connection fails to establish.
		 */
		public ActiveConnection connect(SocketAddress socketAddress, int operationTimeout) throws IOException
		{
			return new ActiveConnection(operationTimeout)
					.connect(socketAddress);
		}

		/**
		 * This emulates the connection to a server.
		 * @param connection The connection object.
		 * @param socketAddress The address that should be connected.
		 * @throws IOException When the connection fails.
		 */
		public void connect(ActiveConnection connection, SocketAddress socketAddress) throws IOException
		{
			connection.connect(socketAddress);
		}

		/**
		 * You can return object when the process exits. This is mostly useful when the same thread
		 * is used for the connection.
		 * @see Client#setReturn(Object)
		 * @return The object given during the connection. Null if nothing was given.
		 */
		public Object getReturn()
		{
			return mReturn;
		}

		/**
		 * This sets the object that should be returned during the connection process.
		 * @see Client#getReturn()
		 * @param returnedObject The object to return.
		 */
		public void setReturn(Object returnedObject)
		{
			this.mReturn = returnedObject;
		}

		/**
		 * A class that emulates a connection to a CoolSocket server.
		 */
		public interface ConnectionHandler
		{
			/**
			 * There is nothing special about this. It just returns a {@link Client} object. But it
			 * is here in the hope that the client will do some special stuff before the it is used
			 * by the target. To do so, you should override this class and do some magic before
			 * calling this method.
			 * @param client The client that is used to connect.
			 */
			void onConnect(Client client);
		}
	}
}