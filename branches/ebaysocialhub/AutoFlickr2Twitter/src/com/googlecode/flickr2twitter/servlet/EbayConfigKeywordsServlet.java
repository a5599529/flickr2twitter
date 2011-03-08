/*
 * Created on Feb 19, 2011
 */

package com.googlecode.flickr2twitter.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.googlecode.flickr2twitter.datastore.MyPersistenceManagerFactory;
import com.googlecode.flickr2twitter.datastore.model.User;
import com.googlecode.flickr2twitter.datastore.model.UserSourceServiceConfig;
import com.googlecode.flickr2twitter.impl.ebay.FindItemsDAO;
import com.googlecode.flickr2twitter.impl.ebay.SourceServiceProviderEbayKeywords;
import com.googlecode.flickr2twitter.impl.ebay.SourceServiceProviderEbayKeywordsSandbox;
import com.googlecode.flickr2twitter.org.apache.commons.lang3.StringUtils;

/**
 * @author Emac Shen (shen.bin.1983@gmail.com)
 */
public class EbayConfigKeywordsServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	public static final String PARA_KEYWORDS = "keywords";

	public static final String PARA_SEARCH_KEYWORDS = "search_keywords";
	public static final String PARA_SEARCH_PRICE_LOW = "search_price_low";
	public static final String PARA_SEARCH_PRICE_HIGH = "search_price_high";
	public static final String PARA_SEARCH_MAX_NOTIFICATION = "search_max_notification";
	
	private static final String PREFIX_PRODUCTION = "http://shop.ebay.com/";
	private static final String PREFIX_SANDBOX = "http://shop.sandbox.ebay.com/";
	private static final String PREFIX_URL = "i.html?_nkw=Nikon+D700&_in_kw=1&_ex_kw=&_sacat=See-All-Categories&_okw=";
	private static final String SUFFIX_URL = "&_ftrt=901&_ftrv=1&_sabdlo=&_sabdhi=&_samilow=&_samihi=&_sadis=200&_fpos=Zip+code&_fsct=&LH_SALE_CURRENCY=0&_sop=10&_dmd=1&_ipg=50";

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * javax.servlet.http.HttpServlet#service(javax.servlet.http.HttpServletRequest
	 * , javax.servlet.http.HttpServletResponse)
	 */
	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		User user = (User) req.getSession().getAttribute(
				UserAccountServlet.PARA_SESSION_USER);

		if (user == null) {
			req.getSession().setAttribute("message", "Please Login first!");
			resp.sendRedirect("/index.jsp");
			return;
		}

		String userEmail = user.getUserId().getEmail();
		String keywords = req.getParameter(PARA_KEYWORDS);
		String minPrice = req.getParameter(PARA_SEARCH_PRICE_LOW);
		String maxPrice = req.getParameter(PARA_SEARCH_PRICE_HIGH);
		boolean isSandbox = Boolean.valueOf(req
				.getParameter(EbayConfigServlet.PARA_SANDBOX));

		UserSourceServiceConfig serviceConfig = new UserSourceServiceConfig();
		// TODO store the keywords in user id
		serviceConfig.setServiceUserId(keywords);
		serviceConfig.setServiceAccessToken(keywords);

		String userDisplayName = keywords;
		serviceConfig.setServiceUserName(userDisplayName);
		serviceConfig
				.setServiceProviderId(isSandbox ? SourceServiceProviderEbayKeywordsSandbox.ID
						: SourceServiceProviderEbayKeywords.ID);
		serviceConfig.setUserEmail(userEmail);

		if (StringUtils.isNotBlank(minPrice)) {
			serviceConfig.addAddtionalParameter(SourceServiceProviderEbayKeywords.KEY_MIN_PRICE, minPrice);
		}
		if (StringUtils.isNotBlank(maxPrice)) {
			serviceConfig.addAddtionalParameter(SourceServiceProviderEbayKeywords.KEY_MAX_PRICE, maxPrice);
		}
		// http://shop.ebay.com/i.html?_trkparms=65%253A12%257C66%253A2%257C39%253A1%257C72%253A4831&rt=nc&_nkw=nikon+d700&_sticky=1&_trksid=p3286.c0.m14&_sop=10&_sc=1
		// http://shop.sandbox.ebay.com/i.html?_trkparms=65%253A1%257C66%253A2%257C39%253A1&rt=nc&_nkw=android+mini+collectible&_ipg=&_sc=1&_sticky=1&_trksid=p3286.c0.m14&_sop=10&_sc=1
		/*
		 * URL url = new FindItemsDAO().buildSearchItemsUrl(false, keywords); if
		 * (url != null) { serviceConfig.setUserSiteUrl(url.toString()); }
		 */
		String userSiteUrl = buildEbayUserSearchUrl(isSandbox, keywords, minPrice, maxPrice);

		serviceConfig.setUserSiteUrl(userSiteUrl);

		MyPersistenceManagerFactory.addSourceServiceApp(userEmail,
				serviceConfig);

		resp.sendRedirect("/user_admin.jsp");
	}
	
	public String buildEbayUserSearchUrl(boolean isSandbox, String keywords, String minPrice, String maxPrice) {
		FindItemsDAO findItemsDao = new FindItemsDAO();
		String userSiteUrl = PREFIX_URL + findItemsDao.urlEncode(keywords) + "&_oexkw=&_adv=1";
		boolean hasMinPrice = StringUtils.isNotBlank(minPrice);
		boolean hasMaxPrice = StringUtils.isNotBlank(maxPrice);
		minPrice = hasMinPrice ? minPrice : "";
		maxPrice = hasMaxPrice ? maxPrice : "";
		
		if (hasMinPrice || hasMaxPrice) {
			userSiteUrl += "&_mPrRngCbx=1";
		}
		userSiteUrl += "&_udlo=" + minPrice + "&_udhi=" + maxPrice + SUFFIX_URL;

		if (isSandbox) {
			userSiteUrl = PREFIX_PRODUCTION + userSiteUrl;
		} else {
			userSiteUrl = PREFIX_SANDBOX + userSiteUrl;
		}
		return userSiteUrl;
	}

}

