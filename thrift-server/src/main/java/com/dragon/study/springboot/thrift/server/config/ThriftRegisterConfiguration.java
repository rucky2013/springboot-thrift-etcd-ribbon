package com.dragon.study.springboot.thrift.server.config;


import com.dragon.study.springboot.etcd.EtcdAutoConfiguration;
import com.dragon.study.springboot.etcd.config.EtcdDiscoveryProperties;
import com.dragon.study.springboot.etcd.register.EtcdRegister;
import com.dragon.study.springboot.thrift.server.exception.ThriftServerException;
import com.dragon.study.springboot.thrift.server.utils.InetAddressUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.regex.Pattern;

import mousio.etcd4j.EtcdClient;

/**
 * Created by dragon on 16/6/3.
 */
@Configuration
@Import(EtcdAutoConfiguration.class)
@AutoConfigureAfter({ThriftAutoConfiguration.class})
@EnableConfigurationProperties({ThriftServerProperties.class, EtcdDiscoveryProperties.class})
public class ThriftRegisterConfiguration {

  private final Pattern DEFAULT_PACKAGE_PATTERN = Pattern.compile(
      "(\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*\\.)*\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*");

  private static final Logger logger = LoggerFactory.getLogger(ThriftRegisterConfiguration.class);

  @Bean
  @ConditionalOnMissingBean
  @ConditionalOnProperty(value = "thrift.server.port", matchIfMissing = false)
  public EtcdRegister etcdRegister(EtcdClient etcdClient,
      ThriftServerProperties thriftServerProperties,
      EtcdDiscoveryProperties etcdRegisterProperties) {
    EtcdRegister register = new EtcdRegister();
    String serviceName = thriftServerProperties.getServiceName();

    int lastComma = serviceName.lastIndexOf(".");
    String interfaceName = serviceName.substring(0, lastComma);
    if (!DEFAULT_PACKAGE_PATTERN.matcher(interfaceName).matches()) {
      throw new ThriftServerException("interface name is not match to package pattern");
    }

    register.setPath("/dragon/service/" + interfaceName);

    String ip = "127.0.0.1";
    try {
      ip = InetAddressUtil.getLocalHostLANAddress().getHostAddress();
    } catch (Exception e) {
      e.printStackTrace();
    }

    String address = ip + ":" + String.valueOf(thriftServerProperties.getPort());
    register.setKey(address);
    register.setValue(address);

    register.setClient(etcdClient);
    register.setStart(true);

    String path = register.getPath() + "/" + register.getKey();
    logger.info("path is {} register success!", path);
    return register;
  }

}
