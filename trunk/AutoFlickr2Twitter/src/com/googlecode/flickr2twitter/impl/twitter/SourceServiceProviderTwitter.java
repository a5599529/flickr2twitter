/**
 * 
 */
package com.googlecode.flickr2twitter.impl.twitter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;
import java.util.logging.Logger;

import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.conf.PropertyConfiguration;
import twitter4j.http.AccessToken;
import twitter4j.http.Authorization;
import twitter4j.http.OAuthAuthorization;

import com.googlecode.flickr2twitter.core.GlobalDefaultConfiguration;
import com.googlecode.flickr2twitter.core.ServiceRunner;
import com.googlecode.flickr2twitter.datastore.MyPersistenceManagerFactory;
import com.googlecode.flickr2twitter.datastore.model.GlobalServiceConfiguration;
import com.googlecode.flickr2twitter.datastore.model.GlobalSourceApplicationService;
import com.googlecode.flickr2twitter.datastore.model.UserSourceServiceConfig;
import com.googlecode.flickr2twitter.impl.twitter.model.TwitterItem;
import com.googlecode.flickr2twitter.intf.ISourceServiceProvider;
import com.googlecode.flickr2twitter.model.IItem;

/**
 * @author Toby Yu(yuyang226@gmail.com)
 *
 */
public class SourceServiceProviderTwitter extends AbstractServiceProviderTwitter
<GlobalSourceApplicationService, UserSourceServiceConfig> 
implements ISourceServiceProvider<IItem> {
	private static final Logger log = Logger.getLogger(SourceServiceProviderTwitter.class
			.getName());
	/**
	 * 
	 */
	public SourceServiceProviderTwitter() {
		super();
	}

	/* (non-Javadoc)
	 * @see com.googlecode.flickr2twitter.intf.ISourceServiceProvider#getLatestItems(com.googlecode.flickr2twitter.datastore.model.GlobalServiceConfiguration, com.googlecode.flickr2twitter.datastore.model.GlobalSourceApplicationService, com.googlecode.flickr2twitter.datastore.model.UserSourceServiceConfig, long)
	 */
	@Override
	public List<IItem> getLatestItems(GlobalServiceConfiguration globalConfig,
			GlobalSourceApplicationService globalSvcConfig,
			UserSourceServiceConfig sourceService, long currentTime)
			throws Exception {
		if (sourceService.getAdditionalParameters() == null
				|| sourceService.getAdditionalParameters().containsKey(KEY_TOKEN_SECRET) == false) {
			throw new IllegalArgumentException("Invalid data -> " + sourceService.getAdditionalParameters());
		}
		final List<IItem> items = new ArrayList<IItem>();
		// The factory instance is re-useable and thread safe.
		AccessToken accessToken = new AccessToken(
				sourceService.getServiceAccessToken(),
				sourceService.getAdditionalParameters().get(KEY_TOKEN_SECRET));
		PropertyConfiguration conf = new PropertyConfiguration(new Properties());

		Authorization auth = new OAuthAuthorization(conf,
				globalSvcConfig.getSourceAppApiKey(),
				globalSvcConfig.getSourceAppSecret(), accessToken);
		Twitter twitter = new TwitterFactory().getInstance(auth);
		
		Calendar cstTime = Calendar.getInstance(TimeZone.getTimeZone(ServiceRunner.TIMEZONE_UTC));
		cstTime.setTimeInMillis(currentTime);
		log.info("Converted current time: " + cstTime.getTime());
		Calendar past = Calendar.getInstance(TimeZone.getTimeZone(ServiceRunner.TIMEZONE_UTC));
		long newTime = cstTime.getTime().getTime() - globalConfig.getMinUploadTime();
		past.setTimeInMillis(newTime);
		
		ResponseList<Status> statuses = twitter.getUserTimeline();
		log.info("Trying to find latest tweets from user " + sourceService.getServiceUserName()
				+ " after " + past.getTime().toString() + " from "
				+ statuses.size() + " tweets");
		
		for (Status status : statuses) {
			TwitterItem item = new TwitterItem(status);
			log.fine("processing tweet: " + item.getTitle()
					+ ", date uploaded: " + status.getCreatedAt());
			if (status.getCreatedAt().after(past.getTime())) {
				log.info(item.getTitle() 
						+ ", date uploaded: " + status.getCreatedAt()
						+ ", GEO: " + item.getGeoData());
				items.add(item);
			}
		}
		return items;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.flickr2twitter.intf.IServiceProvider#createDefaultGlobalApplicationConfig()
	 */
	@Override
	public GlobalSourceApplicationService createDefaultGlobalApplicationConfig() {
		GlobalSourceApplicationService result = new GlobalSourceApplicationService();
		result.setAppName(DISPLAY_NAME);
		result.setProviderId(ID);
		result.setDescription("The world's leading online micro-blog service");
		result.setSourceAppApiKey(GlobalDefaultConfiguration.getInstance()
				.getTwitterConsumerId());
		result.setSourceAppSecret(GlobalDefaultConfiguration
				.getInstance().getTwitterConsumerSecret());
		result.setAuthPagePath("source" + CALLBACK_URL);
		result.setImagePath(null); // TODO set the default image path
		return result;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.flickr2twitter.impl.twitter.AbstractServiceProviderTwitter#getGlobalApplicationConfig()
	 */
	@Override
	protected GlobalSourceApplicationService getGlobalApplicationConfig() {
		return MyPersistenceManagerFactory.getGlobalSourceAppService(ID);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.flickr2twitter.impl.twitter.AbstractServiceProviderTwitter#getUserServiceConfig(java.lang.String)
	 */
	@Override
	protected List<UserSourceServiceConfig> getUserServiceConfigs(String userEmail) {
		return MyPersistenceManagerFactory.getUserSourceServices(userEmail);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.flickr2twitter.impl.twitter.AbstractServiceProviderTwitter#createNewUserServiceConfig()
	 */
	@Override
	protected UserSourceServiceConfig createNewUserServiceConfig() {
		return new UserSourceServiceConfig();
	}

}