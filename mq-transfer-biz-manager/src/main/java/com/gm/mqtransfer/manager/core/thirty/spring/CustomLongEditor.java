package com.gm.mqtransfer.manager.core.thirty.spring;

import org.springframework.beans.propertyeditors.PropertiesEditor;

public class CustomLongEditor extends PropertiesEditor {

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        if (text == null || text.trim().equals("")) {
            return;
        }
        setValue(Long.parseLong(text));
    }

    @Override
    public String getAsText() {
        return getValue().toString();
    }
}
