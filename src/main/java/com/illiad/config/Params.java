package com.illiad.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("params")
@Data
public class Params {
    int localPort = Integer.parseInt(System.getProperty("localPort", "1080"));
    String remoteHost = System.getProperty("remoteHost", "www.google.com");
    int remotePort = Integer.parseInt(System.getProperty("remotePort", "443"));
    int min = Integer.parseInt(System.getProperty("min", "1"));
    int max = Integer.parseInt(System.getProperty("max", "128"));

}
