package com.gm.mqtransfer.manager.core.thirty.spring;

import org.springframework.beans.propertyeditors.PropertiesEditor;

public class CustomFloatEditor extends PropertiesEditor {

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        if (text == null || text.trim().equals("")) {
            return;
        }
        setValue(Float.parseFloat(text));
    }

    @Override
    public String getAsText() {
        return getValue().toString();
    }
}
