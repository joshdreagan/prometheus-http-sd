package com.joshdreagan.prometheus;

import inet.ipaddr.HostName;
import org.eclipse.microprofile.config.spi.Converter;

public class HostNameConverter implements Converter<HostName> {

    @Override
    public HostName convert(String value) throws IllegalArgumentException, NullPointerException {
        return new HostName(value);
    }
}
