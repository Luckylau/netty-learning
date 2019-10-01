package luckylau.netty.privateprotocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.jboss.marshalling.Marshaller;

import java.io.IOException;
import java.util.Map;

/**
 * @Author luckylau
 * @Date 2019/9/6
 */
public class NettyMessageEncoder extends
        MessageToByteEncoder<NettyMessage> {

    MarshallingEncoder marshallingEncoder;

    public NettyMessageEncoder() throws IOException {
        this.marshallingEncoder = new MarshallingEncoder();
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, NettyMessage msg,
                          ByteBuf sendBuf) throws Exception {
        if (msg == null || msg.getHeader() == null)
            throw new Exception("The encode message is null");
        sendBuf.writeInt((msg.getHeader().getCrcCode()));
        sendBuf.writeInt((msg.getHeader().getLength()));
        sendBuf.writeLong((msg.getHeader().getSessionID()));
        sendBuf.writeByte((msg.getHeader().getType()));
        sendBuf.writeByte((msg.getHeader().getPriority()));
        sendBuf.writeInt((msg.getHeader().getAttachment().size()));
        String key;
        byte[] keyArray;
        Object value;
        for (Map.Entry<String, Object> param : msg.getHeader().getAttachment()
                .entrySet()) {
            key = param.getKey();
            keyArray = key.getBytes("UTF-8");
            sendBuf.writeInt(keyArray.length);
            sendBuf.writeBytes(keyArray);
            value = param.getValue();
            marshallingEncoder.encode(value, sendBuf);
        }
        key = null;
        keyArray = null;
        value = null;
        if (msg.getBody() != null) {
            marshallingEncoder.encode(msg.getBody(), sendBuf);
        } else
            sendBuf.writeInt(0);
        sendBuf.setInt(4, sendBuf.readableBytes() - 8);
    }

    private class MarshallingEncoder {

        private final byte[] LENGTH_PLACEHOLDER = new byte[4];
        Marshaller marshaller;

        public MarshallingEncoder() throws IOException {
            marshaller = MarshallingCodecFactory.buildMarshalling();
        }

        protected void encode(Object msg, ByteBuf out) throws Exception {
            try {
                int lengthPos = out.writerIndex();
                out.writeBytes(LENGTH_PLACEHOLDER);
                ChannelBufferByteOutput output = new ChannelBufferByteOutput(out);
                marshaller.start(output);
                marshaller.writeObject(msg);
                marshaller.finish();
                out.setInt(lengthPos, out.writerIndex() - lengthPos - 4);
            } finally {
                marshaller.close();
            }
        }

    }
}
