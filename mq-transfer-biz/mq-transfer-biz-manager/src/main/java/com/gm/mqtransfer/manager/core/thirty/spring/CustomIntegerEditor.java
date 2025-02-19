package com.gm.mqtransfer.manager.core.thirty.spring;

import org.springframework.beans.propertyeditors.PropertiesEditor;

public class CustomIntegerEditor extends PropertiesEditor{
	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		if (text == null || text.trim().equals("")){
			return;
		}
		setValue(Integer.parseInt(text)); 
	}

	@Override
	public String getAsText() {
		 return getValue().toString();
	}
}
