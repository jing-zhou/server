package com.illiad.server.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("params")
@Data
public class Params {
    int localPort = Integer.parseInt(System.getProperty("localPort", "2080"));
    String remoteHost = System.getProperty("remoteHost", "www.google.com");
    int remotePort = Integer.parseInt(System.getProperty("remotePort", "443"));
    String secret = "password";

}
