package com.gm.mqtransfer.provider.facade.service;

import com.gm.mqtransfer.provider.facade.model.ServiceDesc;

/**
 * plugin provider service
 * @author GuoMin
 *
 */
public interface PluginProviderService {

	public ServiceDesc serviceDesc();
	
	public ProviderServiceFactory getServiceFactory();
	
}
