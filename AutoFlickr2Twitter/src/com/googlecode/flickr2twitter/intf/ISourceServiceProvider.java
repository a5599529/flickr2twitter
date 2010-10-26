/**
 * 
 */
package com.googlecode.flickr2twitter.intf;

import java.util.List;

import com.googlecode.flickr2twitter.datastore.model.GlobalServiceConfiguration;
import com.googlecode.flickr2twitter.datastore.model.UserSourceServiceConfig;

/**
 * @author Toby Yu(yuyang226@gmail.com)
 *
 */
public interface ISourceServiceProvider<T> extends IServiceAuthorizer, IServiceProvider{
	
	
	/**
	 * Get the list of latest items
	 * @return
	 * @throws Exception
	 */
	public List<T> getLatestItems(GlobalServiceConfiguration globalConfig, UserSourceServiceConfig sourceService) throws Exception;

}