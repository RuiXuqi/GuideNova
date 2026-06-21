package guideme.internal.network;

import io.netty.buffer.ByteBuf;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.network.PacketBuffer;
import org.jetbrains.annotations.Nullable;

public class ExtendedBuffer extends PacketBuffer {
    public ExtendedBuffer(ByteBuf wrapped) {
        super(wrapped);
    }

    public String readString() {
        return this.readString(Short.MAX_VALUE);
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public <T> void writeOptional(Optional<T> optional, ExtendedBuffer.Writer<T> writer) {
        if (optional.isPresent()) {
            this.writeBoolean(true);
            writer.accept(this, optional.get());
        } else {
            this.writeBoolean(false);
        }

    }

    public <T> Optional<T> readOptional(ExtendedBuffer.Reader<T> reader) {
        return this.readBoolean() ? Optional.of(reader.apply(this)) : Optional.empty();
    }

    @Nullable
    public <T> T readNullable(ExtendedBuffer.Reader<T> reader) {
        return this.readBoolean() ? reader.apply(this) : null;
    }

    public <T> void writeNullable(@Nullable T value, ExtendedBuffer.Writer<T> writer) {
        if (value != null) {
            this.writeBoolean(true);
            writer.accept(this, value);
        } else {
            this.writeBoolean(false);
        }

    }

    @FunctionalInterface
    public interface Reader<T> extends Function<ExtendedBuffer, T> {
        default ExtendedBuffer.Reader<Optional<T>> asOptional() {
            return buffer -> buffer.readOptional(this);
        }
    }

    @FunctionalInterface
    public interface Writer<T> extends BiConsumer<ExtendedBuffer, T> {
        default ExtendedBuffer.Writer<Optional<T>> asOptional() {
            return (buffer, optional) -> buffer.writeOptional(optional, this);
        }
    }
}
