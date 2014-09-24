package eu.socialsensor.sfc.storages;

import java.io.IOException;

import org.apache.log4j.Logger;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import eu.socialsensor.framework.Configuration;
import eu.socialsensor.framework.common.domain.Item;
import eu.socialsensor.framework.common.domain.MediaItem;
import eu.socialsensor.framework.common.domain.WebPage;

/**
 * Class for storing items to redis store
 * @author manosetro
 * @email  manosetro@iti.gr
 *
 */
public class RedisStorage implements Storage {

	private static String HOST = "redis.host";
	private static String WEBPAGES_CHANNEL = "redis.webpages.channel";
	private static String MEDIA_CHANNEL = "redis.media.channel";
	private static String ITEMS_CHANNEL = "redis.items.channel";
	
	private Logger  logger = Logger.getLogger(RedisStorage.class);
	
	private Jedis publisherJedis;
	private String host;
	
	private String itemsChannel = null;
	private String webPagesChannel = null;
	private String mediaItemsChannel = null;
	
	private long items = 0, mItems = 0, wPages = 0;
	
	private String storageName = "Redis";
	
	public RedisStorage(Configuration config) {
		this.host = config.getParameter(RedisStorage.HOST);
		this.itemsChannel = config.getParameter(RedisStorage.ITEMS_CHANNEL);
		this.webPagesChannel = config.getParameter(RedisStorage.WEBPAGES_CHANNEL);
		this.mediaItemsChannel = config.getParameter(RedisStorage.MEDIA_CHANNEL);
		
	}
	
	@Override
	public boolean open() {
		try {
			JedisPoolConfig poolConfig = new JedisPoolConfig();
			JedisPool jedisPool = new JedisPool(poolConfig, host, 6379, 0);
		
			this.publisherJedis = jedisPool.getResource();
			return true;
		}
		catch(Exception e) {
			logger.error("Error during opening.", e);
			return false;
		}
	}

	@Override
	public void store(Item item) throws IOException {
		
		if(item == null)
			return;
		
		if(item.isOriginal()) { 	
			if(itemsChannel != null) {
				items++;
				synchronized(publisherJedis) {
					publisherJedis.publish(itemsChannel, item.toJSONString());
				}
			}
		
			if(mediaItemsChannel != null) {
				for(MediaItem mediaItem : item.getMediaItems()) {
					mItems++;
					synchronized(publisherJedis) {
						publisherJedis.publish(mediaItemsChannel, mediaItem.toJSONString());
					}
				}
			}
		
			if(webPagesChannel != null) {
				for(WebPage webPage : item.getWebPages()) {
					wPages++;
					synchronized(publisherJedis) {
						publisherJedis.publish(webPagesChannel, webPage.toJSONString());
					}
				}
			}
		}
	}
	
	@Override
	public void update(Item update) throws IOException {
		// Not supported.
	}
	
	@Override
	public boolean delete(String id) throws IOException {
		// Not supported.
		return false;
	}
	
	

	@Override
	public void updateTimeslot() {
		// Not supported.
	}

	@Override
	public void close() {
		publisherJedis.disconnect();
	}
	
	@Override
	public boolean checkStatus() {
		try {
			logger.info("Redis sent " + items + " items, " + mItems + " media items and " 
					+ wPages + " web pages!");

			boolean connected;
			synchronized(publisherJedis) {
				publisherJedis.info();
				connected = publisherJedis.isConnected();
			}
			if(!connected) {
				connected = reconnect();
			}
			return connected;
		}
		catch(Exception e) {
			e.printStackTrace();
			logger.error(e);
			return reconnect();
		}
	}
	
	private boolean reconnect() {
		synchronized(publisherJedis) {
			try {
				if(publisherJedis != null) {
					publisherJedis.disconnect();
				}
			}
			catch(Exception e) { 
				logger.error(e);
			}
		}
		try {
			synchronized(publisherJedis) {
				JedisPoolConfig poolConfig = new JedisPoolConfig();
        		JedisPool jedisPool = new JedisPool(poolConfig, host, 6379, 0);
        	
        		this.publisherJedis = jedisPool.getResource();
        		publisherJedis.info();
        		return publisherJedis.isConnected();
			}
		}
		catch(Exception e) {
			logger.error(e);
			return false;
		}

	}
	
	@Override
	public boolean deleteItemsOlderThan(long dateThreshold) throws IOException{
		// Not supported
		return true;
	}
	
	@Override
	public String getStorageName(){
		return this.storageName;
	}

	public static void main(String...args) {
		System.out.println("Check Redis Storage");
		
		Configuration config = new Configuration();
		config.setParameter(RedisStorage.HOST, "160.40.51.18");
		config.setParameter(RedisStorage.ITEMS_CHANNEL, "items");
		config.setParameter(RedisStorage.WEBPAGES_CHANNEL, "webpages");
		config.setParameter(RedisStorage.MEDIA_CHANNEL, "media");
		RedisStorage redis = new RedisStorage(config);
		redis.open();
		
		redis.checkStatus();
		
	}
}
