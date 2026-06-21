package guideme.internal.command;

import guideme.compiler.IdUtils;
import guideme.internal.GuideMEProxy;
import java.util.Collection;
import java.util.stream.Collectors;
import net.minecraft.command.CommandException;
import net.minecraft.util.ResourceLocation;

public final class GuideIdArgument {
    private GuideIdArgument() {
    }

    public static ResourceLocation parse(String value) throws CommandException {
        var guideId = IdUtils.tryParse(value);
        if (guideId == null)
            throw new CommandException("guideme.argument.resource_or_id.invalid", value);
        return guideId;
    }

    public static Collection<String> listSuggestions() {
        return GuideMEProxy.instance().getAvailableGuides()
                .map(ResourceLocation::toString)
                .collect(Collectors.toList());
    }
}
