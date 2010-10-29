/**
 * 
 */
package com.googlecode.flickr2twitter.datastore;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Query;

import com.google.appengine.api.datastore.Email;
import com.google.appengine.api.datastore.Key;
import com.googlecode.flickr2twitter.core.GlobalDefaultConfiguration;
import com.googlecode.flickr2twitter.datastore.model.GlobalServiceConfiguration;
import com.googlecode.flickr2twitter.datastore.model.GlobalSourceApplicationService;
import com.googlecode.flickr2twitter.datastore.model.GlobalTargetApplicationService;
import com.googlecode.flickr2twitter.datastore.model.User;
import com.googlecode.flickr2twitter.datastore.model.UserSourceServiceConfig;
import com.googlecode.flickr2twitter.datastore.model.UserTargetServiceConfig;

/**
 * @author Toby Yu(yuyang226@gmail.com)
 * 
 */
public final class MyPersistenceManagerFactory {
	private static final PersistenceManagerFactory pmfInstance = JDOHelper
			.getPersistenceManagerFactory("transactions-optional");
	private static final Logger log = Logger
			.getLogger(MyPersistenceManagerFactory.class.getName());

	public static enum Permission {
		ADMIN, NORMAL
	}

	/**
	 * 
	 */
	private MyPersistenceManagerFactory() {
		super();
	}

	public static PersistenceManagerFactory getInstance() {
		return pmfInstance;
	}

	public static GlobalSourceApplicationService getGlobalSourceAppService(
			String providerId) {
		PersistenceManagerFactory pmf = MyPersistenceManagerFactory
				.getInstance();
		PersistenceManager pm = pmf.getPersistenceManager();
		try {
			Query query = pm.newQuery(GlobalSourceApplicationService.class);
			query.setFilter("providerId == id");
			query.declareParameters("String id");
			List<?> data = (List<?>) query.execute(providerId);
			if (data != null && data.isEmpty() == false)
				return (GlobalSourceApplicationService) data.get(0);
		} catch (Exception e) {
			log.warning(e.toString());
		} finally {
			pm.close();
		}
		return null;
	}

	public static GlobalTargetApplicationService getGlobalTargetAppService(
			String providerId) {
		PersistenceManagerFactory pmf = MyPersistenceManagerFactory
				.getInstance();
		PersistenceManager pm = pmf.getPersistenceManager();
		try {
			Query query = pm.newQuery(GlobalTargetApplicationService.class);
			query.setFilter("providerId == id");
			query.declareParameters("String id");
			List<?> data = (List<?>) query.execute(providerId);
			if (data != null && data.isEmpty() == false)
				return (GlobalTargetApplicationService) data.get(0);
		} catch (Exception e) {
			log.warning(e.toString());
		} finally {
			pm.close();
		}
		return null;
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}

	public static User createNewUser(String userEmail, String password,
			String screenName) {
		return createNewUser(userEmail, password, screenName, Permission.NORMAL);
	}

	public static UserSourceServiceConfig addSourceServiceApp(String userEmail,
			UserSourceServiceConfig srcService) {
		return addSourceServiceApp(getUser(userEmail), srcService);
	}

	public static UserSourceServiceConfig addSourceServiceApp(User user,
			UserSourceServiceConfig srcService) {
		if (user == null) {
			throw new IllegalArgumentException("Invalid user: " + user);
		}
		List<UserSourceServiceConfig> services = getUserSourceServices(user);
		int index = services.indexOf(srcService);
		if (index >= 0) {
			return services.get(index);
		}

		PersistenceManagerFactory pmf = MyPersistenceManagerFactory
				.getInstance();
		PersistenceManager pm = pmf.getPersistenceManager();
		try {
			user.addSourceService(srcService);
			pm.makePersistent(user);
			return srcService;
		} finally {
			pm.close();
		}
	}

	public static UserTargetServiceConfig addTargetServiceApp(String userEmail,
			UserTargetServiceConfig targetService) {
		return addTargetServiceApp(getUser(userEmail), targetService);
	}

	public static UserTargetServiceConfig addTargetServiceApp(User user,
			UserTargetServiceConfig targetService) {
		if (user == null) {
			throw new IllegalArgumentException("Invalid user: " + user);
		}
		List<UserTargetServiceConfig> services = getUserTargetServices(user);
		int index = services.indexOf(targetService);
		if (index >= 0) {
			return services.get(index);
		}

		PersistenceManagerFactory pmf = MyPersistenceManagerFactory
				.getInstance();
		PersistenceManager pm = pmf.getPersistenceManager();
		try {
			user.addTargetService(targetService);
			pm.makePersistent(user);
			return targetService;
		} finally {
			pm.close();
		}
	}

	public static User createNewUser(String userEmail, String password,
			String screenName, Permission permission) {
		User user = getUser(userEmail);
		if (user != null) {
			log.warning("User already exist: " + userEmail);
			return user;
		}
		PersistenceManagerFactory pmf = MyPersistenceManagerFactory
				.getInstance();
		PersistenceManager pm = pmf.getPersistenceManager();
		try {
			String encryptionPassword = MessageDigestUtil
					.getSHAPassword(password);
			user = new User(new Email(userEmail), encryptionPassword,
					screenName);
			user.setPermission(permission.name());
			pm.makePersistent(user);
			return user;
		} catch (NoSuchAlgorithmException e) {
			log.warning("Got NoSuchAlgorithmException. Unable to create user!"
					+ e.getCause());
			return null;
		} finally {
			pm.close();
		}
	}

	public static User updateUserPassword(Key key, String newPassword) {
		PersistenceManagerFactory pmf = MyPersistenceManagerFactory
				.getInstance();
		PersistenceManager pm = pmf.getPersistenceManager();
		try {
			User user = pm.getObjectById(User.class, key);
			String passwordE = MessageDigestUtil.getSHAPassword(newPassword);
			user.setPassword(passwordE);
			return user;
		} catch (NoSuchAlgorithmException e) {
			log.warning("Got NoSuchAlgorithmException. Unable to change password!"
					+ e.getCause());
			return null;
		} finally {
			pm.close();
		}
	}

	public static User updateUserDisplayName(Key key, String newDisplayName) {
		PersistenceManagerFactory pmf = MyPersistenceManagerFactory
				.getInstance();
		PersistenceManager pm = pmf.getPersistenceManager();
		try {
			User user = pm.getObjectById(User.class, key);
			user.setScreenName(newDisplayName);
			return user;
		} finally {
			pm.close();
		}
	}

	public static User getUser(String userEmail) {
		PersistenceManagerFactory pmf = MyPersistenceManagerFactory
				.getInstance();
		PersistenceManager pm = pmf.getPersistenceManager();
		try {
			Query query = pm.newQuery(User.class);
			query.setFilter("userId == userEmailAddress");
			query.declareParameters("String userEmailAddress");
			List<?> data = (List<?>) query.execute(userEmail);
			if (data != null && data.isEmpty() == false)
				return (User) data.get(0);
		} finally {
			pm.close();
		}
		return null;
	}
	
	public static User getLoginUser(String userEmail, String password) {
		return getLoginUser(userEmail, password, false);
	}
	
	public static User getOpenIdLoginUser(String userEmail) {
		return getLoginUser(userEmail, null, true);
	}

	public static User getLoginUser(String userEmail, String password, boolean openId) {
		PersistenceManagerFactory pmf = MyPersistenceManagerFactory
				.getInstance();
		PersistenceManager pm = pmf.getPersistenceManager();
		try {
			List<?> data = null;
			if (openId == true) {
				Query query = pm.newQuery(User.class);
				query.setFilter("userId == userEmailAddress");
				query.declareParameters("String userEmailAddress");
				data = (List<?>) query.execute(userEmail);
			} else {
				String encryptionPassword = MessageDigestUtil
				.getSHAPassword(password);
				Query query = pm.newQuery(User.class);
				query.setFilter("userId == userEmailAddress && password == userPassword");
				query.declareParameters("String userEmailAddress, String userPassword");
				data = (List<?>) query.execute(userEmail,
						encryptionPassword);
			}
			
			if (data != null && data.isEmpty() == false) {
				User u = (User) data.get(0);
				log.log(Level.INFO, u.toString());
				return u;
			}
		} catch (NoSuchAlgorithmException e) {
			log.warning("Unable to login because of NoSuchAlgorithmException. WTF???->"
					+ e.getMessage());
		} finally {
			pm.close();
		}
		return null;
	}

	public static List<User> getAllUsers() {
		List<User> users = new ArrayList<User>();
		PersistenceManagerFactory pmf = MyPersistenceManagerFactory
				.getInstance();
		PersistenceManager pm = pmf.getPersistenceManager();
		try {
			Query query = pm.newQuery(User.class);
			List<?> results = (List<?>) query.execute();
			for (Object obj : results) {
				User user = (User) obj;
				users.add(user);
				user.getSourceServices();
				user.getTargetServices();
			}
		} finally {
			pm.close();
		}
		for (User user : users) {
			getUserSourceServices(user);
			getUserTargetServices(user);
		}
		return users;
	}

	public static List<UserSourceServiceConfig> getUserSourceServices(
			String userEmail) {
		return getUserSourceServices(getUser(userEmail));
	}

	public static List<UserSourceServiceConfig> getUserSourceServices(User user) {
		if (user == null) {
			throw new IllegalArgumentException("Invalid user: " + user);
		}
		PersistenceManagerFactory pmf = MyPersistenceManagerFactory
				.getInstance();
		PersistenceManager pm = pmf.getPersistenceManager();
		try {
			Query query = pm.newQuery(UserSourceServiceConfig.class);
			query.setFilter("userEmail == userEmailAddress");
			query.declareParameters("String userEmailAddress");
			List<?> data = (List<?>) query.execute(user.getUserId().getEmail());
			if (data != null) {
				for (Object o : data) {
					if (user.getSourceServices().contains(o) == false) {
						user.addSourceService((UserSourceServiceConfig) o);
					}
				}
			}
		} finally {
			pm.close();
		}
		return user.getSourceServices();
	}

	public static List<UserTargetServiceConfig> getUserTargetServices(
			String userEmail) {
		return getUserTargetServices(getUser(userEmail));
	}

	public static List<UserTargetServiceConfig> getUserTargetServices(User user) {
		if (user == null) {
			throw new IllegalArgumentException("Invalid user: " + user);
		}
		PersistenceManagerFactory pmf = MyPersistenceManagerFactory
				.getInstance();
		PersistenceManager pm = pmf.getPersistenceManager();
		try {
			Query query = pm.newQuery(UserTargetServiceConfig.class);
			query.setFilter("userEmail == userEmailAddress");
			query.declareParameters("String userEmailAddress");
			List<?> data = (List<?>) query.execute(user.getUserId().getEmail());
			if (data != null) {
				for (Object o : data) {
					if (user.getTargetServices().contains(o) == false) {
						user.addTargetService((UserTargetServiceConfig) o);
					}
				}
			}
		} finally {
			pm.close();
		}
		return user.getTargetServices();
	}

	public static GlobalServiceConfiguration getGlobalConfiguration() {
		PersistenceManagerFactory pmf = MyPersistenceManagerFactory
				.getInstance();
		PersistenceManager pm = pmf.getPersistenceManager();
		GlobalServiceConfiguration conf = null;
		try {
			Query query = pm.newQuery(GlobalServiceConfiguration.class);
			List<?> results = (List<?>) query.execute();
			if (results.isEmpty() == false) {
				conf = (GlobalServiceConfiguration) results.get(0);
			} else {
				conf = new GlobalServiceConfiguration();
				conf.setKey("1");
				conf.setMinUploadTime(GlobalDefaultConfiguration.getInstance()
						.getInterval());
				pm.makePersistent(conf);
			}
		} finally {
			pm.close();
		}
		return conf;
	}

}