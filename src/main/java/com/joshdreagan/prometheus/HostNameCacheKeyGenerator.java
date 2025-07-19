package com.joshdreagan.prometheus;

import inet.ipaddr.HostName;
import io.quarkus.cache.CacheKeyGenerator;
import jakarta.enterprise.context.ApplicationScoped;

import java.lang.reflect.Method;

@ApplicationScoped
public class HostNameCacheKeyGenerator implements CacheKeyGenerator {

  @Override
  public Object generate(Method method, Object... methodParams) {
    for (Object param : methodParams) {
      if (param instanceof HostName) {
        return ((HostName) param).getHost();
      }
    }
    return null;
  }
}
