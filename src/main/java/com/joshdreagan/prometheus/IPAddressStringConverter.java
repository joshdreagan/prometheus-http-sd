package com.joshdreagan.prometheus;

import inet.ipaddr.IPAddressString;
import org.eclipse.microprofile.config.spi.Converter;

public class IPAddressStringConverter implements Converter<IPAddressString> {

    @Override
    public IPAddressString convert(String value) throws IllegalArgumentException, NullPointerException {
        return new IPAddressString(value);
    }
}
