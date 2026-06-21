package guideme.internal.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public interface IExtendedBufferMessage extends IMessage {
    @Override
    default void fromBytes(ByteBuf buf) {
        this.fromBytes(new ExtendedBuffer(buf));
    }

    @Override
    default void toBytes(ByteBuf buf) {
        this.toBytes(new ExtendedBuffer(buf));
    }

    void fromBytes(ExtendedBuffer buf);

    void toBytes(ExtendedBuffer buf);
}
