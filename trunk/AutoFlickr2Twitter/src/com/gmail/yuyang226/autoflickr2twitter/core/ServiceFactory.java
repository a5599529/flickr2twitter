/**
 * 
 */
package com.gmail.yuyang226.autoflickr2twitter.core;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;



import com.gmail.yuyang226.autoflickr2twitter.impl.flickr.SourceServiceProviderFlickr;
import com.gmail.yuyang226.autoflickr2twitter.intf.ISourceServiceProvider;
import com.gmail.yuyang226.autoflickr2twitter.model.IItem;

/**
 * @author Toby Yu(yuyang226@gmail.com)
 *
 */
public class ServiceFactory {
	private static final Map<String, ISourceServiceProvider<IItem>> SOURCE_PROVIDERS;
	
	static {
		Map<String, ISourceServiceProvider<IItem>> data = new HashMap<String, ISourceServiceProvider<IItem>>(2);
		try {
			data.put(SourceServiceProviderFlickr.ID, new SourceServiceProviderFlickr());
		} catch (Exception e) {
			Logger.getLogger(ServiceFactory.class.getName()).throwing(ServiceFactory.class.getName(), "<init>", e);
		}
		SOURCE_PROVIDERS = data;
	}

	/**
	 * 
	 */
	public ServiceFactory() {
		super();
	}
	
	public static ISourceServiceProvider<IItem> getSourceServiceProvider(String sourceServiceProviderId) throws Exception {
		return SOURCE_PROVIDERS.get(sourceServiceProviderId);
	}

}