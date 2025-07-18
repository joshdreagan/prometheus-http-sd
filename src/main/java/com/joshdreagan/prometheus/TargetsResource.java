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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Path("/targets")
public class TargetsResource {

  private static final Logger log = LoggerFactory.getLogger(TargetsResource.class);

  @ConfigProperty(name = "hosts")
  private List<String> hosts;

  @ConfigProperty(name = "cidr", defaultValue = "192.168.0.0/24")
  private String cidr;

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public RestResponse<List<Map<String, Object>>> getAll() {
    List<Map<String, Object>> results = new ArrayList<>();

    IPAddressString cidrIPAddressString = new IPAddressString(cidr);

    for (String host : hosts) {
      HostName hostName = new HostName(host);
      try {
        IPAddress resolvedIPAddresses = hostName.toAddress();
        log.info("Resolved IP Address [{}] for host [{}].", resolvedIPAddresses.toString(), host);
        IPAddress firstMatch = null;
        for (IPAddress ipAddress : resolvedIPAddresses.getIterable()) {
          boolean matches = cidrIPAddressString.contains(ipAddress.toAddressString());
          if (matches) {
            firstMatch = ipAddress;
            break;
          }
        }
        if (firstMatch != null) {
          log.info("Successfully resolved IP address [{}] for host [{}] that matches CIDR [{}].", firstMatch.toString(), host, cidr);
          results.add(
            Map.of(
              "labels", Map.of("job", host),
              "targets", Collections.singletonList(firstMatch.toString() + ":9100")
            )
          );
        } else {
          log.warn("Could not find IP address host [{}] that matches CIDR [{}].", host, cidr);
        }
      } catch (UnknownHostException | HostNameException e) {
        log.error("Could not resolve IP address for host [{}].", host);
        log.debug("Error:", e);
      }
    }
    return ResponseBuilder.ok(results).build();
  }
}
