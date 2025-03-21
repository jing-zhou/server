package com.illiad.server.codec;

import com.illiad.server.config.Params;
import com.illiad.server.security.Secret;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
public class Header {
    private final Params params;
    private final Random random;
    @Getter
    private final Secret secret;

    public Header(Params params, Secret secret){
        this.params = params;
        this.secret = secret;
        this.random = new Random();

    }

    public byte[] offset() {
        int length = random.nextInt(params.getMax() - params.getMin()) + params.getMin();
        byte[] byteArray = new byte[length];
        random.nextBytes(byteArray);
        return byteArray;
    }
}
