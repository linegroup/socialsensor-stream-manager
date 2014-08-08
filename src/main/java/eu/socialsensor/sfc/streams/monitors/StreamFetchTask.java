package eu.socialsensor.sfc.streams.monitors;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import eu.socialsensor.framework.common.domain.Feed;
import eu.socialsensor.framework.common.domain.Item;
import eu.socialsensor.framework.streams.Stream;
import eu.socialsensor.framework.streams.StreamException;

/**
 * Class for handling a Stream Task that is responsible for retrieving
 * content for the stream it is assigned to
 * 
 * @author ailiakop
 * @email  ailiakop@iti.gr
 */
public class StreamFetchTask implements Runnable {
	
	private final Logger logger = Logger.getLogger(StreamFetchTask.class);
	
	private Stream stream;
	
	private boolean completed = false;
	
	private List<Feed> feeds = new ArrayList<Feed>();
	private List<Item> totalRetrievedItems = new ArrayList<Item>();
	
	public StreamFetchTask(Stream stream) throws Exception {
		this.stream = stream;
		if(!this.stream.setMonitor())
			throw new Exception("Feeds monitor for stream: " + this.stream.getClass() + " cannot be initialized");
	}
	
	public StreamFetchTask(Stream stream,List<Feed> feeds) throws Exception {
		this.stream = stream;
		this.feeds.addAll(feeds);
		
		if(!this.stream.setMonitor()) {
			throw new Exception("Feeds monitor for stream: " + this.stream.getClass() + " cannot be initialized");
		}
	}
	
	/**
	 * Adds the input feeds to search 
	 * for relevant content in the stream
	 * @param feeds
	 */
	public void addFeeds(List<Feed> feeds) {
		this.feeds.addAll(feeds);
	}
	
	/**
	 * Clear the input feeds
	 */
	public void clear() {
		feeds.clear();
	}
	
	/**
	 * Returns the total number of retrieved items
	 * for the associated stream
	 * @return
	 */
	public List<Item> getTotalRetrievedItems() {
		return totalRetrievedItems;
	}
	
	/**
	 * Returns true if the stream task is completed
	 * @return
	 */
	public boolean isCompleted() {
		return completed;
	}
	
	/**
	 * Sets the task in ready mode to retrieve again 
	 * from the stream with the same set of feeds
	 */
	public void restartTask() {
		completed = false;
	}

	/**
	 * Retrieves content using the feeds assigned to the task
	 * making rest calls to stream's API. 
	 */
	@Override
	public void run() {
		try {
			stream.poll(feeds);
			
			//totalRetrievedItems.clear();
			totalRetrievedItems.addAll(stream.getTotalRetrievedItems());
			completed = true;
			
		} catch (StreamException e) {
			completed = true;
			logger.error("ERROR IN STREAM FETCH TASK", e);
		}
		
		
	}
}
