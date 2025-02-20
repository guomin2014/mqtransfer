package com.gm.mqtransfer.facade.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.gm.mqtransfer.facade.service.IApplicationService;
import com.gm.mqtransfer.facade.service.IApplicationStartedService;

/**
 * 
 * 按顺序启动服务
 * @author GuoMin
 *
 */
@Component
public class TransferStartRunner implements ApplicationRunner {

	@Resource
    private ApplicationContext applicationContext;
	
	@Override
	public void run(ApplicationArguments args) throws Exception {
		SpringContextUtils.updateContext(applicationContext);
		Map<String, IApplicationService> map = applicationContext.getBeansOfType(IApplicationService.class);
        if (map != null) {
        	List<IApplicationService> services = new ArrayList<>(map.values());
        	Collections.sort(services, new Comparator<IApplicationService>() {
				@Override
				public int compare(IApplicationService o1, IApplicationService o2) {
					return o1.order() > o2.order() ? 1 : -1;
				}
			});
        	for (IApplicationService service : services) {
        		service.start();
        	}
        }
        Map<String, IApplicationStartedService> startedMap = applicationContext.getBeansOfType(IApplicationStartedService.class);
        if (startedMap != null) {
        	List<IApplicationStartedService> services = new ArrayList<>(startedMap.values());
        	Collections.sort(services, new Comparator<IApplicationStartedService>() {
        		@Override
        		public int compare(IApplicationStartedService o1, IApplicationStartedService o2) {
        			return o1.order() > o2.order() ? 1 : -1;
        		}
        	});
        	for (IApplicationStartedService service : services) {
        		service.start();
        	}
        }
	}
	
}
