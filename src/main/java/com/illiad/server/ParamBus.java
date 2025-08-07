package com.illiad.server;

import com.illiad.server.codec.v5.V5AddressDecoder;
import com.illiad.server.codec.v5.V5ServerEncoder;
import com.illiad.server.config.Params;
import com.illiad.server.handler.Utils;
import com.illiad.server.security.Secret;
import com.illiad.server.security.Ssl;
import org.springframework.stereotype.Component;


/**
 * this is a helper to hold all the parameters (singleton objects)
 * the itention is to simplify the constructor of other classes
 * so that we don't need to add too many parameters in their constructors
 */
@Component
public class ParamBus {
    public Params params;
    public HandlerNamer namer;
    public V5ServerEncoder v5ServerEncoder;
    public V5AddressDecoder v5AddressDecoder;
    public Ssl ssl;
    public Secret secret;
    public UdpChannel udpChannel;
    public Utils utils;

    public ParamBus(Params params, HandlerNamer namer, V5ServerEncoder v5ServerEncoder, V5AddressDecoder v5AddressDecoder, Ssl ssl, Secret secret, UdpChannel udpChannel, Utils utils) {
        this.params = params;
        this.namer = namer;
        this.v5ServerEncoder = v5ServerEncoder;
        this.v5AddressDecoder = v5AddressDecoder;
        this.ssl = ssl;
        this.secret = secret;
        this.udpChannel = udpChannel;
        this.utils = utils;
    }
}
