package com.gm.mqtransfer.facade.service.plugin;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.alipay.sofa.ark.spi.registry.ServiceFilter;
import com.alipay.sofa.ark.spi.registry.ServiceMetadata;
import com.alipay.sofa.ark.spi.registry.ServiceReference;
import com.alipay.sofa.ark.spi.service.ArkInject;
import com.alipay.sofa.ark.spi.service.plugin.PluginManagerService;
import com.alipay.sofa.ark.spi.service.registry.RegistryService;
import com.gm.mqtransfer.provider.facade.model.ClusterInfo;
import com.gm.mqtransfer.provider.facade.model.ServiceDesc;
import com.gm.mqtransfer.provider.facade.service.PluginProviderService;

@Component
public class PluginProviderManagerService{
	
	private final Logger logger = LoggerFactory.getLogger(PluginProviderManagerService.class);
	
	@ArkInject
	private RegistryService registryService;
	@ArkInject
	private PluginManagerService pluginManagerService;
	
	private Map<String, PluginProviderService> providerServiceMap = new ConcurrentHashMap<>();
	
	private Set<ServiceDesc> serviceList = new HashSet<>();
	
	/**
	 * 查找所有Plugin提供的服务
	 */
	@PostConstruct
	public void doInit() {
		logger.info("init plugin provider manager service...");
		List<ServiceReference<PluginProviderService>> referenceList = registryService.referenceServices(new ServiceFilter<PluginProviderService>() {
			@Override
			public boolean match(ServiceReference serviceReference) {
				 ServiceMetadata serviceMetadata = serviceReference.getServiceMetadata();
				 return serviceMetadata.getInterfaceClass().equals(PluginProviderService.class);
			}
		});
		for (ServiceReference<PluginProviderService> reference : referenceList) {
			ServiceMetadata serviceMetadata = reference.getServiceMetadata();
			PluginProviderService pluginProviderService = reference.getService();
			String serviceProviderDesc = serviceMetadata.getServiceProvider().getServiceProviderDesc();
			ServiceDesc desc = pluginProviderService.serviceDesc();
			String key = desc.getMqType() + "#" + desc.getMqVersion();
			providerServiceMap.put(key.toLowerCase(), pluginProviderService);
			serviceList.add(desc);
			logger.info("provider service-->{}[UniqueId:{}],{}[version:{}]", serviceProviderDesc, serviceMetadata.getUniqueId(), desc.getMqType(), desc.getMqVersion());
		}
		logger.info("plugin-->" + pluginManagerService.getAllPluginNames());
	}
	
	public PluginProviderService getPluginProviderService(String pluginServiceId) {
		return providerServiceMap.get(pluginServiceId);
	}
	public PluginProviderService getOnePluginProviderService(ClusterInfo cluster) {
		List<String> pluginServiceIdList = cluster.fetchClientKeyList();
		if (pluginServiceIdList == null || pluginServiceIdList.isEmpty()) {
			return null;
		}
		PluginProviderService pluginProviderService = null;
		for (String pluginServiceId : pluginServiceIdList) {
			pluginProviderService = providerServiceMap.get(pluginServiceId.toLowerCase());
			if (pluginProviderService != null) {
				break;
			}
		}
		return pluginProviderService;
	}
	
	public Collection<ServiceDesc> getAllProviderService() {
		return Collections.unmodifiableCollection(serviceList);
	}

}
