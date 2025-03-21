package com.illiad.handler;

import com.illiad.codec.v4.V4ServerDecoder;
import com.illiad.codec.v4.V4ServerEncoder;
import com.illiad.codec.v5.V5InitReqDecoder;
import com.illiad.codec.v5.V5ServerEncoder;
import com.illiad.handler.v4.V4CommandHandler;
import com.illiad.handler.v5.V5CommandHandler;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.socksx.SocksVersion;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Detects the version of the current SOCKS connection and initializes the pipeline with
 * either {@link V4ServerDecoder} or {@link V5InitReqDecoder}.
 */

@Component
@ChannelHandler.Sharable
public class VersionHandler extends ByteToMessageDecoder {

    private static final InternalLogger logger =
            InternalLoggerFactory.getInstance(VersionHandler.class);

    private final V4ServerEncoder v4ServerEncoder;
    private final V5ServerEncoder v5ServerEncoder;
    private final V4CommandHandler v4CommandHandler;
    private final V5CommandHandler v5CommandHandler;

    public VersionHandler(V4ServerEncoder v4ServerEncoder, V4CommandHandler v4CommandHandler, V5ServerEncoder v5ServerEncoder, V5CommandHandler v5CommandHandler) {
        this.v4ServerEncoder = v4ServerEncoder;
        this.v4CommandHandler = v4CommandHandler;
        this.v5ServerEncoder = v5ServerEncoder;
        this.v5CommandHandler = v5CommandHandler;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        final int readerIndex = in.readerIndex();
        if (in.writerIndex() == readerIndex) {
            return;
        }

        ChannelPipeline p = ctx.pipeline();
        final byte versionVal = in.getByte(readerIndex);
        SocksVersion version = SocksVersion.valueOf(versionVal);

        switch (version) {
            case SOCKS4a:
                logKnownVersion(ctx, version);
                p.addAfter(ctx.name(), null, v4ServerEncoder);
                p.addAfter(ctx.name(), null, new V4ServerDecoder());
                p.addAfter(ctx.name(), null, v4CommandHandler);
                break;
            case SOCKS5:
                logKnownVersion(ctx, version);
                p.addAfter(ctx.name(), null, v5ServerEncoder);
                p.addAfter(ctx.name(), null, new V5InitReqDecoder());
                p.addAfter(ctx.name(), null, v5CommandHandler);
                break;
            default:
                logUnknownVersion(ctx, versionVal);
                in.skipBytes(in.readableBytes());
                ctx.close();
                return;
        }

        p.remove(this);
    }

    private static void logKnownVersion(ChannelHandlerContext ctx, SocksVersion version) {
        logger.debug("{} Protocol version: {}({})", ctx.channel(), version);
    }

    private static void logUnknownVersion(ChannelHandlerContext ctx, byte versionVal) {
        if (logger.isDebugEnabled()) {
            logger.debug("{} Unknown protocol version: {}", ctx.channel(), versionVal & 0xFF);
        }
    }
}
