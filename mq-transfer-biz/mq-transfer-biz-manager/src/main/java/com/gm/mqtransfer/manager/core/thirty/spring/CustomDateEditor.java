package com.gm.mqtransfer.manager.core.thirty.spring;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.springframework.beans.propertyeditors.PropertiesEditor;

import com.gm.mqtransfer.facade.common.util.DateUtils;

public class CustomDateEditor extends PropertiesEditor {

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        Date date = null;
        if (text != null && text.trim().length() > 0) {
            try {
				date = DateUtils.StrToDateTime(text, "yyyy-MM-dd HH:mm:ss");
			} catch (Exception e) {
				throw new IllegalArgumentException("Value cannot be converted to date");
			}
        }
        setValue(date);
    }

    @Override
    public String getAsText() {
        String ret = "";
        Date date = (Date) getValue();
        if (date != null) {
            String temp = this.getDateTime(date, "yyyy-MM-dd HH:mm:ss");
            String temp1 = "00:00:00";
            if (temp.endsWith(temp1)) {
                ret = temp.substring(0, temp.length() - temp1.length() + 1);
            } else {
                ret = temp;
            }
        }
        return ret;
    }
    public String getDateTime(Date date, String pattern) {
        if (date == null) {
            return "";
        }
        if (pattern == null || pattern.trim().length() == 0)
            pattern = "yyyy-MM-dd HH:mm:ss";
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        return sdf.format(date);
    }
}
