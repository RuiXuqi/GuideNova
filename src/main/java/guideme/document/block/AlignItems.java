package guideme.document.block;

import java.util.Locale;
import net.minecraft.util.IStringSerializable;

public enum AlignItems implements IStringSerializable {
    CENTER,
    START,
    END;

    private final String serializedName;

    AlignItems() {
        this.serializedName = name().toLowerCase(Locale.ROOT);
    }

    @Override
    public String getName() {
        return serializedName;
    }
}
