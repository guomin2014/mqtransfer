package com.gm.mqtransfer.manager.core.thirty.spring;

import org.springframework.beans.propertyeditors.PropertiesEditor;

public class CustomLongDefEditor extends PropertiesEditor{
	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		if (text == null || text.trim().equals("")){
			text = "0";
		}
		 setValue(Long.parseLong(text)); 
	}

	@Override
	public String getAsText() {
		 return getValue().toString();
	}
}
