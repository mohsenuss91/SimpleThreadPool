package org.ooopen.base;

import java.util.LinkedList;

/**
 * This is a simple implementation of thread pool<br/>
 * referenced from <a href="http://blog.csdn.net/ruanruoshi/article/details/657133">csdn blog</a><br/>
 * 
 * @author zmj110@gmail.com
 * 2014-11-11
 */
public class SimpleThreadPool
{
	static final long			IDLE_TIMEOUT	= 60000L;

	private static SimpleThreadPool	threadPool;

	private String				name;
	private int					minsize;
	private int					maxsize;
	private int					nextWorkerId	= 0;
	private LinkedList<Worker>	pool			= new LinkedList<Worker>();
	
	public static SimpleThreadPool getInstance()
	{
		return SimpleThreadPool.getInstance("PThread", 0, 20);
	}
	
	public static SimpleThreadPool getInstance(String name)
	{
		return SimpleThreadPool.getInstance(name, 0, 20);
	}
	
	public static SimpleThreadPool getInstance(String name, int minsize, int maxsize)
	{
		if (threadPool == null)
		{
			doSync(name, minsize, maxsize);
		}
		return threadPool;
	}

	private static synchronized void doSync(String name, int minsize, int maxsize)
	{
		if (threadPool == null)
		{
			threadPool = new SimpleThreadPool(name, minsize, maxsize);
		}
	}

//	private ThreadPool()
//	{
//		this("PThread");
//	}
//
//	private ThreadPool(String name)
//	{
//		this(name, 0, 20);
//	}

	public SimpleThreadPool(String name, int minsize, int maxsize)
	{
		this.name = "PThread";
		this.minsize = minsize;
		this.maxsize = maxsize;
	}

	public synchronized void run(Runnable runner)
	{
		Worker worker;

		if (runner == null)
		{
			throw new NullPointerException();
		}

		// get a worker from free list...
		if (!pool.isEmpty())
		{
			worker = (Worker) pool.removeFirst();
		} else
		{
			// ...no free worker available, create new one...
			worker = new Worker(name + "-" + ++nextWorkerId);
			worker.start();
		}

		// ...and wake up worker to service incoming runner
		worker.wakeup(runner);
	}

	// Notified when a worker has idled timeout
	// @return true if worker should die, false otherwise
	synchronized boolean notifyTimeout(Worker worker)
	{
		if (worker.runner != null)
		{
			return false;
		}
		if (pool.size() > minsize)
		{
			// Remove from free list
			pool.remove(worker);
			return true; // die
		}
		return false; // continue
	}

	// Notified when a worker has finished his work and
	// free to service next runner
	// @return true if worker should die, false otherwise
	synchronized boolean notifyFree(Worker worker)
	{
		if (pool.size() < maxsize)
		{
			// Add to free list
			pool.addLast(worker);
			return false; // continue
		}
		return true; // die
	}

	// The inner class that implement worker thread
	class Worker extends Thread
	{
		Runnable	runner	= null;

		public Worker(String name)
		{
			super(name);
		}

		synchronized void wakeup(Runnable runner)
		{
			this.runner = runner;
			notify();
		}

		public void run()
		{
			for (;;)
			{
				synchronized (this)
				{
					if (runner == null)
					{
						try
						{
							wait(IDLE_TIMEOUT);
						} catch (InterruptedException e)
						{
						}
					}
				}

				// idle timed out, die or put into free list
				if (runner == null)
				{
					if (notifyTimeout(this))
						return;
					else
						continue;
				}

				try
				{
					runner.run();
				} catch (Exception e)
				{
					e.printStackTrace();
				} finally
				{
					runner = null;
					if (notifyFree(this))
						return;
				}
			}
		}
	}
}