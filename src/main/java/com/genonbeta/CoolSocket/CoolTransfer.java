package com.genonbeta.CoolSocket;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

abstract public class CoolTransfer<T>
{
	public final static int DELAY_DISABLED = -1;

	private final ArrayList<TransferHandler<T>> mProcess = new ArrayList<>();
	private ExecutorService mExecutor;
	private int mNotifyDelay = CoolTransfer.DELAY_DISABLED;
	private Object mBlockingObject = new Object();

	public abstract Flag onError(TransferHandler<T> handler, Exception error);

	public abstract void onNotify(TransferHandler<T> handler, int percentage);

	public abstract Flag onPrepare(TransferHandler<T> handler);

	public abstract Flag onTaskPrepareSocket(TransferHandler<T> handler);

	public abstract void onTaskEnd(TransferHandler<T> handler);

	public abstract void onDestroy(TransferHandler<T> handler);

	public void onPrepareNext(TransferHandler<T> handler)
	{
	}

	public Flag onTaskOrientateStreams(TransferHandler<T> handler, InputStream inputStream, OutputStream outputStream)
	{
		return Flag.CONTINUE;
	}

	public void onProcessListChanged(ArrayList<TransferHandler<T>> processList, TransferHandler<T> handler, boolean isAdded)
	{
	}

	protected void addProcess(TransferHandler<T> processHandler)
	{
		synchronized (getProcessList()) {
			getProcessList().add(processHandler);
			onProcessListChanged(getProcessList(), processHandler, true);
		}
	}

	public Object getBlockingObject()
	{
		return mBlockingObject;
	}

	public ExecutorService getExecutor()
	{
		if (mExecutor == null)
			mExecutor = Executors.newFixedThreadPool(10);

		return mExecutor;
	}

	public int getNotifyDelay()
	{
		return mNotifyDelay;
	}

	public ArrayList<TransferHandler<T>> getProcessList()
	{
		return mProcess;
	}

	public void setBlockingObject(Object blockingObject)
	{
		mBlockingObject = blockingObject;
	}

	public void setExecutor(ExecutorService executor)
	{
		mExecutor = executor;
	}

	public void setNotifyDelay(int delay)
	{
		mNotifyDelay = delay;
	}

	protected void removeProcess(TransferHandler<T> processHandler)
	{
		synchronized (getProcessList()) {
			getProcessList().remove(processHandler);
			onProcessListChanged(getProcessList(), processHandler, false);
		}
	}

	public enum Flag
	{
		CONTINUE,
		CANCEL_ALL,
		CANCEL_CURRENT
	}

	public enum Status
	{
		INTERRUPTED,
		RUNNING,
		PENDING,
	}

	public static class ParentBuilder<T>
	{
		private Flag mFlag = Flag.CANCEL_ALL;
		private int mPort;
		private long mFileSize;
		private long mSkippedBytes;
		private byte[] mBuffer;
		private Socket mSocket;
		private TransferProgress<T> mProgress;
		private T mExtra;

		public ParentBuilder<T> setBuffer(byte[] buffer)
		{
			mBuffer = buffer;
			return this;
		}

		public byte[] getBuffer()
		{
			return mBuffer;
		}

		public T getExtra()
		{
			return mExtra;
		}

		public long getFileSize()
		{
			return mFileSize;
		}

		public Flag getFlag()
		{
			return mFlag;
		}

		public int getPort()
		{
			return mPort;
		}

		public Socket getSocket()
		{
			return mSocket;
		}

		public long getSkippedBytes()
		{
			return mSkippedBytes;
		}

		public TransferProgress<T> getTransferProgress()
		{
			if (mProgress == null)
				setTransferProgress(new TransferProgress<>());

			return mProgress;
		}

		public ParentBuilder<T> reset()
		{
			mPort = 0;
			mFileSize = 0;
			mSkippedBytes = 0;
			mBuffer = null;
			mSocket = null;
			getTransferProgress().resetCurrentTransferredByte();

			return this;
		}

		public ParentBuilder<T> setExtra(T extra)
		{
			mExtra = extra;
			return this;
		}

		public ParentBuilder<T> setFileSize(long fileSize)
		{
			mFileSize = fileSize;
			return this;
		}

		public void setFlag(Flag flag)
		{
			mFlag = flag;
		}

		public ParentBuilder<T> setPort(int port)
		{
			mPort = port;
			return this;
		}

		public ParentBuilder<T> setSkippedBytes(long skippedBytes)
		{
			mSkippedBytes = skippedBytes;
			return this;
		}

		public ParentBuilder<T> setSocket(Socket socket)
		{
			mSocket = socket;
			return this;
		}

		public ParentBuilder<T> setTransferProgress(TransferProgress<T> transferProgress)
		{
			mProgress = transferProgress;
			return this;
		}

		public ParentBuilder<T> skipBytes(long bytes)
		{
			if (getSkippedBytes() > 0)
				getTransferProgress().decrementTransferredByte(getSkippedBytes());

			setSkippedBytes(bytes);
			getTransferProgress().incrementTransferredByte(bytes);

			return this;
		}
	}

	public abstract static class TransferHandler<T> implements Runnable
	{
		private Status mStatus = Status.PENDING;
		private ParentBuilder<T> mParentBuilder;

		public TransferHandler(ParentBuilder<T> parentBuilder)
		{
			mParentBuilder = parentBuilder;
		}

		protected abstract void onRun();

		public byte[] getBuffer()
		{
			return getParentBuilder().getBuffer();
		}

		public Flag getFlag()
		{
			return getParentBuilder().getFlag();
		}

		public long getFileSize()
		{
			return getParentBuilder().getFileSize();
		}

		public T getExtra()
		{
			return getParentBuilder().getExtra();
		}

		public int getPort()
		{
			return getParentBuilder().getPort();
		}

		public ParentBuilder<T> getParentBuilder()
		{
			return mParentBuilder;
		}

		public long getSkippedBytes()
		{
			return getParentBuilder().getSkippedBytes();
		}

		public Socket getSocket()
		{
			return getParentBuilder().getSocket();
		}

		public Status getStatus()
		{
			return mStatus;
		}

		public TransferProgress<T> getTransferProgress()
		{
			return getParentBuilder().getTransferProgress();
		}

		public void setFlag(Flag flag)
		{
			getParentBuilder().setFlag(flag);
		}

		protected void setSocket(Socket socket)
		{
			getParentBuilder().setSocket(socket);
		}

		public void setStatus(Status status)
		{
			mStatus = status;
		}

		public void setTransferProgress(TransferProgress<T> transferProgress)
		{
			getParentBuilder().setTransferProgress(transferProgress);
		}

		public void skipBytes(long bytes) throws IOException
		{
			getParentBuilder().skipBytes(bytes);
		}

		@Override
		public void run()
		{
			getTransferProgress().resetCurrentTransferredByte();

			setStatus(Status.RUNNING);
			onRun();
			setStatus(Status.INTERRUPTED);
		}
	}

	public static abstract class Receive<T> extends CoolTransfer<T>
	{
		public abstract Flag onTaskPrepareSocket(TransferHandler<T> handler, ServerSocket serverSocket);

		public Handler prepare(Builder<T> builder) {
			return new Handler(builder);
		}

		public Handler receive(Builder<T> builder, boolean currentThread)
		{
			return receive(prepare(builder), currentThread);
		}

		public Handler receive(Handler handler, boolean currentThread)
		{
			if (currentThread)
				handler.run();
			else
				getExecutor().submit(handler);

			return handler;
		}

		public static class Builder<T> extends ParentBuilder<T>
		{
			private int mTimeout = CoolSocket.NO_TIMEOUT;
			private OutputStream mOutputStream;
			private ServerSocket mServerSocket;

			public OutputStream getOutputStream()
			{
				return mOutputStream;
			}

			public ServerSocket getServerSocket()
			{
				return mServerSocket;
			}

			public int getTimeout()
			{
				return mTimeout;
			}

			@Override
			public ParentBuilder<T> reset()
			{
				mOutputStream = null;
				mServerSocket = null;
				mTimeout = CoolSocket.NO_TIMEOUT;
				return super.reset();
			}

			public Builder<T> setOutputStream(OutputStream outputStream)
			{
				mOutputStream = outputStream;
				return this;
			}

			public Builder<T> setOutputStream(File file) throws FileNotFoundException
			{
				return setOutputStream(new FileOutputStream(file));
			}

			public Builder<T> setServerSocket(ServerSocket serverSocket)
			{
				mServerSocket = serverSocket;
				return this;
			}

			public Builder<T> setTimeout(int timeout)
			{
				mTimeout = timeout;
				return this;
			}
		}

		public class Handler extends CoolTransfer.TransferHandler<T>
		{
			public Handler(Builder<T> builder)
			{
				super(builder);
			}

			@Override
			protected void onRun()
			{
				addProcess(this);

				setFlag(onPrepare(this));

				try {
					if (Flag.CONTINUE.equals(getFlag())) {
						if (getServerSocket() == null)
							setServerSocket(new ServerSocket(getPort()));

						if (getTimeout() != CoolSocket.NO_TIMEOUT)
							getServerSocket().setSoTimeout(getTimeout());

						setFlag(onTaskPrepareSocket(this, getServerSocket()));

						if (Flag.CONTINUE.equals(getFlag())) {
							setSocket(getServerSocket().accept());

							if (getTimeout() != CoolSocket.NO_TIMEOUT)
								getSocket().setSoTimeout(getTimeout());

							setFlag(onTaskPrepareSocket(this));

							if (Flag.CONTINUE.equals(getFlag())) {
								InputStream inputStream = getSocket().getInputStream();
								int len = 0;
								long lastRead = System.currentTimeMillis();

								setFlag(onTaskOrientateStreams(this, inputStream, getOutputStream()));

								if (Flag.CONTINUE.equals(getFlag())) {
									while (len != -1) {
										synchronized (getBlockingObject()) {
											if ((len = inputStream.read(getBuffer())) > 0) {
												getOutputStream().write(getBuffer(), 0, len);
												getOutputStream().flush();

												lastRead = System.currentTimeMillis();

												getTransferProgress().incrementTransferredByte(len);
											}
										}

										getTransferProgress().doNotify(Receive.this, this);

										if ((getBuilder().getTimeout() > 0 && (System.currentTimeMillis() - lastRead) > getBuilder().getTimeout())
												|| !Flag.CONTINUE.equals(getFlag())
												|| getTransferProgress().isInterrupted()) {
											System.out.println("CoolTransfer: Timed out... Exiting.");
											break;
										}
									}

									if (Flag.CONTINUE.equals(getFlag())) {
										getTransferProgress().incrementTransferredFileCount();
										onTaskEnd(this);
									}
								}

								getOutputStream().close();
								inputStream.close();
							}
						}
					}
				} catch (Exception e) {
					setFlag(onError(this, e));
				} finally {
					try {
						if (getSocket() != null && !getSocket().isClosed())
							getSocket().close();
					} catch (IOException e) {
						e.printStackTrace();
					}

					try {
						if (getServerSocket() != null && !getServerSocket().isClosed())
							getServerSocket().close();
					} catch (IOException e) {
						e.printStackTrace();
					}

					onDestroy(this);

					if (!Flag.CANCEL_ALL.equals(getFlag()))
						onPrepareNext(this);

					removeProcess(this);
				}
			}

			public Builder<T> getBuilder()
			{
				return (Builder<T>) getParentBuilder();
			}

			public OutputStream getOutputStream()
			{
				return getBuilder().getOutputStream();
			}

			public ServerSocket getServerSocket()
			{
				return getBuilder().getServerSocket();
			}

			public int getTimeout()
			{
				return getBuilder().getTimeout();
			}

			public void setServerSocket(ServerSocket serverSocket)
			{
				getBuilder().setServerSocket(serverSocket);
			}

			@Override
			public void skipBytes(long bytes) throws IOException
			{
				super.skipBytes(bytes);
			}
		}
	}

	public static abstract class Send<T> extends CoolTransfer<T>
	{
		public Handler prepare(Builder<T> builder) {
			return new Handler(builder);
		}

		public Handler send(Builder<T> builder, boolean currentThread)
		{
			return send(prepare(builder), currentThread);
		}

		public Handler send(Handler handler, boolean currentThread)
		{
			if (currentThread)
				handler.run();
			else
				getExecutor().submit(handler);

			return handler;
		}

		public static class Builder<T> extends ParentBuilder<T>
		{
			private InputStream mInputStream;
			private String mServerIp;

			public InputStream getInputStream()
			{
				return mInputStream;
			}

			public String getServerIp()
			{
				return mServerIp;
			}

			@Override
			public ParentBuilder<T> reset()
			{
				mInputStream = null;
				mServerIp = null;
				return super.reset();
			}

			public Builder<T> setInputStream(InputStream inputStream)
			{
				mInputStream = inputStream;
				return this;
			}

			public Builder<T> setInputStream(File file) throws FileNotFoundException
			{
				return setInputStream(new FileInputStream(file));
			}

			public Builder<T> setServerIp(String ipAddress)
			{
				mServerIp = ipAddress;
				return this;
			}
		}

		public class Handler extends CoolTransfer.TransferHandler<T>
		{
			public Handler(Builder<T> builder)
			{
				super(builder);
			}

			public Builder<T> getBuilder()
			{
				return (Builder<T>) getParentBuilder();
			}

			@Override
			protected void onRun()
			{
				addProcess(this);
				setFlag(onPrepare(this));

				try {
					if (Flag.CONTINUE.equals(getFlag())) {
						if (getSocket() == null) {
							setSocket(new Socket());
							getSocket().bind(null);
						}

						getSocket().connect(new InetSocketAddress(getServerIp(), getPort()));
						setFlag(onTaskPrepareSocket(this));

						if (Flag.CONTINUE.equals(getFlag())) {
							OutputStream outputStream = getSocket().getOutputStream();
							int len = 0;

							setFlag(onTaskOrientateStreams(this, getInputStream(), outputStream));

							if (Flag.CONTINUE.equals(getFlag())) {
								while (len != -1) {
									synchronized (getBlockingObject()) {
										if ((len = getInputStream().read(getBuffer())) > 0) {
											outputStream.write(getBuffer(), 0, len);
											outputStream.flush();

											getTransferProgress().incrementTransferredByte(len);
										}
									}

									getTransferProgress().doNotify(Send.this, this);

									if (!Flag.CONTINUE.equals(getFlag())
											|| getTransferProgress().isInterrupted())
										break;
								}

								if (Flag.CONTINUE.equals(getFlag())) {
									getTransferProgress().incrementTransferredFileCount();
									onTaskEnd(this);
								}
							}

							outputStream.close();
							getInputStream().close();
						}
					}
				} catch (Exception e) {
					setFlag(onError(this, e));
				} finally {
					try {
						if (getSocket() != null && !getSocket().isClosed())
							getSocket().close();
					} catch (IOException e) {
						e.printStackTrace();
					}

					onDestroy(this);

					if (!Flag.CANCEL_ALL.equals(getFlag()))
						onPrepareNext(this);

					removeProcess(this);
				}
			}

			public InputStream getInputStream()
			{
				return getBuilder().getInputStream();
			}

			public String getServerIp()
			{
				return getBuilder().getServerIp();
			}

			@Override
			public void skipBytes(long bytes) throws IOException
			{
				super.skipBytes(bytes);
				getInputStream().skip(bytes);
			}
		}
	}

	public static class TransferProgress<T>
	{
		private long mStartTime = System.currentTimeMillis();
		private long mCurrentTransferredByte;
		private long mTransferredByte;
		private long mTotalByte;
		private long mTimeElapsed;
		private long mTimePassed;
		private long mTimeRemaining;
		private long mLastNotified;
		private int mTransferredFileCount;
		private boolean mInterrupted = false;

		public int calculatePercentage(long max, long current)
		{
			return (int) (((float) 100 / max) * current);
		}

		public long decrementTransferredByte(long size)
		{
			mCurrentTransferredByte -= size;
			mTransferredByte -= size;
			return mTransferredByte;
		}

		public int decrementTransferredFileCount()
		{
			mTransferredFileCount--;
			return mTransferredFileCount;
		}

		public boolean doNotify(CoolTransfer<T> transfer, TransferHandler<T> handler)
		{
			if (transfer.getNotifyDelay() != -1 && (System.currentTimeMillis() - getLastNotified()) < transfer.getNotifyDelay())
				return false;

			int percentage = calculatePercentage(getTotalByte(), getTransferredByte());

			setTimeElapsed(System.currentTimeMillis() - getStartTime());

			if (getTotalByte() > 0 && getTransferredByte() > 0) {
				setTimePassed(getTimeElapsed() * getTotalByte() / getTransferredByte());
				setTimeRemaining(getTimePassed() - getTimeElapsed());
			}

			transfer.onNotify(handler, percentage);

			mLastNotified = System.currentTimeMillis();

			return true;
		}

		public long getCurrentTransferredByte()
		{
			return mCurrentTransferredByte;
		}

		public long getLastNotified()
		{
			return mLastNotified;
		}

		public long getStartTime()
		{
			return mStartTime;
		}

		public long getTimeElapsed()
		{
			return mTimeElapsed;
		}

		public long getTimePassed()
		{
			return mTimePassed;
		}

		public long getTimeRemaining()
		{
			return mTimeRemaining;
		}

		public long getTotalByte()
		{
			return mTotalByte;
		}

		public int getTransferredFileCount()
		{
			return mTransferredFileCount;
		}

		public long getTransferredByte()
		{
			return mTransferredByte;
		}

		public long incrementTransferredByte(long size)
		{
			mCurrentTransferredByte += size;
			mTransferredByte += size;
			return mTransferredByte;
		}

		public int incrementTransferredFileCount()
		{
			mTransferredFileCount++;
			return mTransferredFileCount;
		}

		public void interrupt()
		{
			mInterrupted = true;
		}

		public boolean isInterrupted()
		{
			return mInterrupted;
		}

		public void resetCurrentTransferredByte()
		{
			mCurrentTransferredByte = 0;
		}

		public void setTotalByte(long totalByte)
		{
			mTotalByte = totalByte;
		}

		public void setTransferredByte(long transferredByte)
		{
			mTransferredByte = transferredByte;
		}

		public void setTransferredFileCount(int transferredFileCount)
		{
			mTransferredFileCount = transferredFileCount;
		}

		public void setStartTime(long startTime)
		{
			mStartTime = startTime;
		}

		public void setTimeElapsed(long timeElapsed)
		{
			mTimeElapsed = timeElapsed;
		}

		public void setTimePassed(long timePassed)
		{
			mTimePassed = timePassed;
		}

		public void setTimeRemaining(long timeRemaining)
		{
			mTimeRemaining = timeRemaining;
		}
	}
}
