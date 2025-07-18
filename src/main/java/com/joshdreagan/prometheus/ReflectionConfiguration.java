package com.joshdreagan.prometheus;

import inet.ipaddr.ipv4.IPv4Address;
import inet.ipaddr.ipv6.IPv6Address;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection(targets = {IPv4Address.class, IPv6Address.class})
public class ReflectionConfiguration {
}
