package com.joshdreagan.prometheus;

import inet.ipaddr.HostName;
import inet.ipaddr.HostNameException;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.RestResponse.ResponseBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.UnknownHostException;
import java.util.*;

@Path("/targets")
public class TargetsResource {

  private static final Logger log = LoggerFactory.getLogger(TargetsResource.class);

  @ConfigProperty(name = "prometheus.http-sd.hosts")
  private List<String> hosts;

  @ConfigProperty(name = "prometheus.http-sd.cidrs", defaultValue = "192.168.0.0/16")
  private List<String> cidrs;

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public RestResponse<List<Map<String, Object>>> getAll() {
    List<Map<String, Object>> results = new ArrayList<>();

    for (String host : hosts) {
      HostName hostName = new HostName(host);
      try {
        IPAddress[] resolvedHostIpAddresses = hostName.toAllAddresses();
        log.info("Resolved IP Addresses {} for host [{}].", Arrays.toString(resolvedHostIpAddresses), host);
        boolean foundMatch = false;
        for (IPAddress hostIpAddress : resolvedHostIpAddresses) {
          for (String cidr : cidrs) {
            IPAddressString cidrIpAddressString = new IPAddressString(cidr);
            if (cidrIpAddressString.contains(hostIpAddress.toAddressString())) {
              log.info("IP address [{}] for host [{}] matches CIDR {}.", hostIpAddress.toString(), host, cidr);
              results.add(
                Map.of(
                  "labels", Map.of("job", host),
                  "targets", Collections.singletonList(hostIpAddress.toString() + ":9100")
                )
              );
              foundMatch = true;
              break;
            } else {
              log.debug("IP address [{}] for host [{}] did not match CIDR [{}].", hostIpAddress, host, cidr);
            }
          }
          if (foundMatch) {
            break;
          }
        }
        if (!foundMatch) {
          log.warn("Could not find any IP address for host [{}] that matches any configured CIDR [{}].", host, Arrays.toString(cidrs.toArray()));
        }
      } catch (UnknownHostException | HostNameException e) {
        log.error("Could not resolve IP address for host [{}].", host);
        log.debug("Error:", e);
      }
    }
    return ResponseBuilder.ok(results).build();
  }
}
