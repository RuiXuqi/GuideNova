package guideme.internal.data;

import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public interface LocalizationEnum {

    String getTranslationKey();

    @SideOnly(Side.CLIENT)
    default String str() {
        return I18n.format(getTranslationKey());
    }

    @SideOnly(Side.CLIENT)
    default String str(Object... args) {
        return I18n.format(getTranslationKey(), args);
    }

    default ITextComponent text() {
        return new TextComponentTranslation(getTranslationKey());
    }

    default ITextComponent text(Object... args) {
        return new TextComponentTranslation(getTranslationKey(), args);
    }

    default ITextComponent withSuffix(String text) {
        return text().createCopy().appendText(text);
    }

    default ITextComponent withSuffix(ITextComponent text) {
        return text().createCopy().appendSibling(text);
    }

}
