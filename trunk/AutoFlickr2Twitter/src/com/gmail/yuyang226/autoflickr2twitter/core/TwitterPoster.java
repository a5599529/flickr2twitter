/**
 * 
 */
package com.gmail.yuyang226.autoflickr2twitter.core;

import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;

import twitter4j.GeoLocation;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.PropertyConfiguration;
import twitter4j.http.AccessToken;
import twitter4j.http.Authorization;
import twitter4j.http.OAuthAuthorization;
import twitter4j.http.RequestToken;

import com.gmail.yuyang226.autoflickr2twitter.datastore.MyPersistenceManagerFactory;
import com.gmail.yuyang226.autoflickr2twitter.datastore.model.UserConfiguration;

/**
 * @author yayu
 *
 */
public class TwitterPoster {
	private static final Logger log = Logger.getLogger(TwitterPoster.class.getName());
	private Twitter twitter = null;

	/**
	 * 
	 */
	public TwitterPoster() {
		super();
	}
	
	public String requestNewToken() throws TwitterException {
		try {
			twitter = new TwitterFactory().getInstance();
			twitter.setOAuthConsumer(GlobalConfiguration.getInstance().getTwitterConsumerId(), 
					GlobalConfiguration.getInstance().getTwitterConsumerSecret());
			RequestToken requestToken = twitter.getOAuthRequestToken();
			log.info("Open the following URL and grant access to your account:");
			log.info(requestToken.getAuthorizationURL());
			return requestToken.getAuthorizationURL();
		} catch (TwitterException e) {
			twitter = null;
			throw e;
		}
	}
	
	public String readyTwitterAuthorization() throws TwitterException {
		StringBuffer buf = new StringBuffer();
		if (twitter != null) {
			AccessToken accessToken = twitter.getOAuthAccessToken();
			buf.append(" User Id: " + accessToken.getUserId());
			buf.append(" User Screen Name: " + accessToken.getScreenName());
			buf.append(" Access Token: " + accessToken.getToken());
			buf.append(" Token Secret: " + accessToken.getTokenSecret());
			PersistenceManagerFactory pmf = MyPersistenceManagerFactory.getInstance();
			PersistenceManager pm = pmf.getPersistenceManager();

			try {
				/*List<UserConfiguration> users = MyPersistenceManagerFactory.getAllUsers();
				if (users.isEmpty() == false) {
					UserConfiguration user = users.get(0);
					user = pm.getObjectById(UserConfiguration.class, user.getFlickrUserId());
					user.setTwitterUserId(String.valueOf(twitter.verifyCredentials().getId()));
					user.setTwitterUserName(accessToken.getScreenName());
					user.setTwitterAccessToken(accessToken.getToken());
					user.setTwitterTokenSecret(accessToken.getTokenSecret());
				}*/
			} finally {
				pm.close();
			}
		}
		return buf.toString();
	}
	
	public static void updateTwitterStatus(UserConfiguration user, String message, GeoLocation geoLoc) throws TwitterException {
		log.info("Posting message -> " + message + " for " + user);
		// The factory instance is re-useable and thread safe.
		AccessToken accessToken = new AccessToken(user.getTwitterAccessToken(), user.getTwitterTokenSecret()); 
		PropertyConfiguration conf = new PropertyConfiguration(new Properties());
		
		Authorization auth = new OAuthAuthorization(conf, GlobalConfiguration.getInstance().getTwitterConsumerId(), 
				GlobalConfiguration.getInstance().getTwitterConsumerSecret(), accessToken);
	    Twitter twitter = new TwitterFactory().getInstance(auth);
	    Status status = geoLoc == null ? twitter.updateStatus(message) : twitter.updateStatus(message, geoLoc);
	    log.info("Successfully updated the status [" + status.getText() + "] to user @" + user.getTwitterUserName());
	}

}
