package guideme.internal.command;

import guideme.PageAnchor;
import guideme.compiler.IdUtils;
import guideme.internal.GuideMEProxy;
import java.util.Collection;
import java.util.stream.Collectors;
import net.minecraft.command.CommandException;
import net.minecraft.util.ResourceLocation;

public final class PageAnchorArgument {
    private PageAnchorArgument() {
    }

    public static PageAnchor parse(String value) throws CommandException {
        try {
            return PageAnchor.parse(value);
        } catch (IdUtils.ResourceLocationException e) {
            throw new CommandException("guideme.argument.resource_or_id.invalid", value);
        }
    }

    public static Collection<String> listSuggestions(ResourceLocation guideId) {
        return GuideMEProxy.instance().getAvailablePages(guideId)
                .map(ResourceLocation::toString)
                .collect(Collectors.toList());
    }
}
