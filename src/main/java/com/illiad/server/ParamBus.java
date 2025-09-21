package com.illiad.server;

import com.illiad.server.codec.v5.V5AddressDecoder;
import com.illiad.server.codec.v5.V5ServerEncoder;
import com.illiad.server.config.Params;
import com.illiad.server.handler.Utils;
import com.illiad.server.handler.udp.Asos;
import com.illiad.server.security.Secret;
import com.illiad.server.security.Ssl;
import org.springframework.stereotype.Component;


/**
 * this is a helper to hold all the parameters (singleton objects)
 * the itention is to simplify class constructor
 */
@Component
public class ParamBus {
    public final Params params;
    public final HandlerNamer namer;
    public final V5ServerEncoder v5ServerEncoder;
    public final V5AddressDecoder v5AddressDecoder;
    public final Ssl ssl;
    public final Secret secret;
    public final Asos asos;
    public final Utils utils;

    public ParamBus(Params params, HandlerNamer namer, V5ServerEncoder v5ServerEncoder, V5AddressDecoder v5AddressDecoder, Ssl ssl, Secret secret, Asos asos, Utils utils) {
        this.params = params;
        this.namer = namer;
        this.v5ServerEncoder = v5ServerEncoder;
        this.v5AddressDecoder = v5AddressDecoder;
        this.ssl = ssl;
        this.secret = secret;
        this.asos = asos;
        this.utils = utils;
    }
}
