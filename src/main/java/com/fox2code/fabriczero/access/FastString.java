package com.fox2code.fabriczero.access;

import org.apache.commons.lang3.StringUtils;

public class FastString {
    public static String replace(String text, CharSequence from,CharSequence to) {
        return StringUtils.replace(text, from.toString(), to.toString());
    }
}
