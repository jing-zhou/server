package com.illiad.server.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("params")
@Data
public class Params {
    int localPort = Integer.parseInt(System.getProperty("localPort", "2080"));
    String localHost = System.getProperty("localHost", "127.0.0.1");
    String udpHost = System.getProperty("udpHost", "127.0.0.1");
    String secret = "password";

}
