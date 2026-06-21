package guideme.internal;

import guideme.internal.util.config.ConfigBuilder;
import guideme.internal.util.config.IFormatter;
import java.io.File;
import java.util.List;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.config.IConfigElement;
import net.minecraftforge.fml.relauncher.Side;

final class GuideMEConfig {
    private static Configuration config;

    static void init(File configFile, Side side) {
        if (config != null)
            throw new IllegalStateException("Init have been performed!");
        config = new Configuration(configFile);
        readFromFile(side);
    }

    static void readFromFile(Side side) {
        build(ConfigBuilder.startReadingFromFile(config), side);
    }

    static void readFromProp() {
        build(ConfigBuilder.startReadingFromProp(config), Side.CLIENT);
    }

    static void save() {
        build(ConfigBuilder.startSaving(config), Side.CLIENT);
    }

    private static void build(ConfigBuilder builder, Side side) {
        builder.setLangKeyPrefix("guideme.configuration");
        builder.setLangKeyFormatter(IFormatter.IDENTITY);

        if (side.isClient()) {
            builder.pushCategory(Configuration.CATEGORY_CLIENT, null, null);
            GuideMEClient.ClientConfig.build(builder);
            builder.popCategoryWithoutLangKey();
        }

        builder.finishBuilding();
    }

    static List<IConfigElement> getRootConfigElements() {
        return new ConfigElement(config.getCategory(Configuration.CATEGORY_CLIENT)).getChildElements();
    }

    // static List<IConfigElement> getRootConfigElements() {
    // return config.getCategoryNames().stream()
    // .map(config::getCategory)
    // .filter(category -> !category.isChild())
    // .map(ConfigElement::new)
    // .collect(Collectors.toList());
    // }
}
