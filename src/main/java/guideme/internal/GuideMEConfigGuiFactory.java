package guideme.internal;

import java.util.Collections;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.client.IModGuiFactory;
import net.minecraftforge.fml.client.config.GuiConfig;

@SuppressWarnings("unused")
public class GuideMEConfigGuiFactory implements IModGuiFactory {
    @Override
    public void initialize(Minecraft instance) {
    }

    @Override
    public boolean hasConfigGui() {
        return true;
    }

    @Override
    public GuiScreen createConfigGui(GuiScreen parent) {
        return new GuiConfig(
                parent,
                GuideMEConfig.getRootConfigElements(),
                Reference.MOD_ID,
                false,
                false,
                I18n.format("guideme.configuration.title"));
    }

    @Override
    public Set<RuntimeOptionCategoryElement> runtimeGuiCategories() {
        return Collections.emptySet();
    }
}
