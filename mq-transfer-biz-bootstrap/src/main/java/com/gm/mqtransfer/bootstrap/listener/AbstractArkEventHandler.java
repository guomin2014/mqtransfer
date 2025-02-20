package com.gm.mqtransfer.bootstrap.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.alipay.sofa.ark.spi.event.AbstractArkEvent;
import com.alipay.sofa.ark.spi.service.event.EventHandler;

@Component
public class AbstractArkEventHandler implements EventHandler<AbstractArkEvent> {

	private Logger logger = LoggerFactory.getLogger(getClass());
	
	@Override
	public int getPriority() {
		return 0;
	}

	@Override
	public void handleEvent(AbstractArkEvent event) {
		logger.info("*****************Source:{},Event Topic:{}", event.getSource(), event.getTopic());
//		Object source = event.getSource();//事件源所属的Biz
////		ArkClient.getBizManagerService().getBizByClassLoader(this.getClass().getClassLoader());//获取当前处理事件所属的Biz
//		if (event instanceof AfterBizStartupEvent) {//biz启动后
//			if (source != null && source instanceof Biz) {
//				try {
//					Biz model = (Biz)source;
//					BizModelUtils.putState(model.getBizName(), model.getBizState());
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//			}
//		} else if (event instanceof BeforeBizStopEvent) {//biz停止前
//			if (source != null && source instanceof Biz) {
//				Biz model = (Biz)source;
//				BizModelUtils.putState(model.getBizName(), model.getBizState());
//			}
//		}
		
	}

}
