package com.gm.mqtransfer.facade.core.condition;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public abstract class ModeCondition implements Condition {

	/** 配置的属性名 */
    public static String CLUSTER_MODE_KEY = "cluster.mode";
    
    @Override
	public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
		String propertyValue = context.getEnvironment().getProperty(CLUSTER_MODE_KEY);
		if (propertyValue == null || propertyValue.trim().length() == 0) {
			propertyValue = StandaloneCondition.MODE_NAME;
		}
		return propertyValue.equalsIgnoreCase(this.getClusterMode());
	}
    
    public abstract String getClusterMode();

}
