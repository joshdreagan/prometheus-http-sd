package com.joshdreagan.prometheus;

import inet.ipaddr.HostName;
import inet.ipaddr.HostNameException;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import io.quarkus.cache.CacheKey;
import io.quarkus.cache.CacheResult;
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

  private static final int DEFAULT_PORT = 9100;

  @ConfigProperty(name = "prometheus.http-sd.hosts")
  private HostName[] hosts;

  @ConfigProperty(name = "prometheus.http-sd.resolve-hosts", defaultValue = "false")
  private boolean resolveHosts;

  @ConfigProperty(name = "prometheus.http-sd.skip-unknown-hosts", defaultValue = "true")
  private boolean skipUnknownHosts;

  @ConfigProperty(name = "prometheus.http-sd.cidrs")
  private Optional<IPAddressString[]> cidrs;

  @ConfigProperty(name = "prometheus.http-sd.skip-unmatched-hosts", defaultValue = "true")
  private boolean skipUnmatchedHosts;

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public RestResponse<?> getAllTargets() {
    List<Map<String, Object>> results = new ArrayList<>();

    for (HostName hostName : hosts) {
      log.info("Generating target data for host [{}].",  hostName);

      String job = hostName.getHost();
      String target = hostName.getHost() + ":" + ((hostName.getPort() != null) ? hostName.getPort() : DEFAULT_PORT);

      if (!resolveHosts && !cidrs.isPresent()) {
        results.add(generateTargetJson(job, target));
        continue;
      }

      IPAddress[] resolvedHostIpAddresses = null;
      try {
        resolvedHostIpAddresses = resolveHostName(hostName);
        log.debug("Resolved IP Addresses {} for host [{}].", Arrays.toString(resolvedHostIpAddresses), hostName.getHost());
      } catch (UnknownHostException | HostNameException e) {
        log.error("Could not resolve IP address for host [{}].", hostName.getHost());
        log.debug("StackTrace", e);
        if (!skipUnknownHosts) {
          return ResponseBuilder.serverError().build();
        }
        continue;
      }

      IPAddress firstMatchingResolvedIpAddress = resolvedHostIpAddresses[0];
      if (cidrs.isPresent()) {
        firstMatchingResolvedIpAddress = findFirstMatchingResolvedIpAddress(resolvedHostIpAddresses, cidrs.get());
        if (firstMatchingResolvedIpAddress == null) {
          log.warn("Could not find any IP address for host [{}] that matches any configured CIDR [{}].", hostName.getHost(), Arrays.toString(cidrs.get()));
          if (!skipUnmatchedHosts) {
            return ResponseBuilder.serverError().build();
          }
          continue;
        }
      }

      target = firstMatchingResolvedIpAddress + ":" + ((hostName.getPort() != null) ? hostName.getPort() : DEFAULT_PORT);
      results.add(generateTargetJson(job, target));
    }

    return ResponseBuilder.ok(results).build();
  }

  @CacheResult(cacheName = "resolved-host-names", keyGenerator = HostNameCacheKeyGenerator.class)
  protected IPAddress[] resolveHostName(@CacheKey HostName hostName) throws HostNameException, UnknownHostException {
    log.info("Resolving IP Addresses for host [{}].", hostName.getHost());
    return hostName.toAllAddresses();
  }

  protected Map<String, Object> generateTargetJson(String job, String target) {
    return Map.of(
      "labels", Map.of("job", job),
      "targets", Collections.singletonList(target)
    );
  }

  protected IPAddress findFirstMatchingResolvedIpAddress(IPAddress[] resolvedHostIpAddresses, IPAddressString[] cidrIpAddresses) {
    for (IPAddress resolvedHostIpAddress : resolvedHostIpAddresses) {
      for (IPAddressString cidrIpAddress : cidrIpAddresses) {
        if (cidrIpAddress.contains(resolvedHostIpAddress.toAddressString())) {
          log.debug("IP address [{}] for host [{}] matches CIDR [{}].", resolvedHostIpAddress, resolvedHostIpAddress.toHostName(), cidrIpAddress);
          return resolvedHostIpAddress;
        } else {
          log.debug("IP address [{}] for host [{}] did not match CIDR [{}].", resolvedHostIpAddress, resolvedHostIpAddress.toHostName(), cidrIpAddress);
        }
      }
    }
    log.warn("Could not find any IP address for host [{}] that matches any configured CIDR [{}].", resolvedHostIpAddresses[0].toHostName(), Arrays.toString(cidrIpAddresses));
    return null;
  }
}
