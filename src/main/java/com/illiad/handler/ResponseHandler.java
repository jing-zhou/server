package com.illiad.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.concurrent.Promise;

/**
 * attached to the backend pipeline, handls the response for the connect request to the remote server
 */
public class ResponseHandler extends ChannelInboundHandlerAdapter {

    private final Promise<Channel> promise;

    public ResponseHandler(Promise<Channel> promise) {
        this.promise = promise;
    }

    // trigger the promise from the frontend channel, and pass the backend channel to the promise
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        promise.setSuccess(ctx.channel());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable throwable) {
        promise.setFailure(throwable);
    }

}
