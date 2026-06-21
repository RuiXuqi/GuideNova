package guideme.internal.command;

import guideme.Guides;
import guideme.compiler.IdUtils;
import guideme.internal.GuideMEClient;
import guideme.internal.GuidebookText;
import java.util.Collections;
import java.util.List;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.server.command.CommandTreeBase;
import org.jetbrains.annotations.Nullable;

public class GuideClientCommand extends CommandTreeBase {
    public GuideClientCommand() {
        this.addSubcommand(new OpenCommand());
        this.addSubcommand(new ExportCommand());
    }

    @Override
    public String getName() {
        return "guidemec";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "guideme.commands.client.usage";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    private static class OpenCommand extends CommandBase {
        @Override
        public String getName() {
            return "open";
        }

        @Override
        public String getUsage(ICommandSender sender) {
            return "guideme.commands.guidemec.open.usage";
        }

        @Override
        public int getRequiredPermissionLevel() {
            return 0;
        }

        @Override
        public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
            if (args.length != 1 && args.length != 2)
                throw new WrongUsageException(this.getUsage(sender));

            var guideId = GuideIdArgument.parse(args[0]);
            var guide = Guides.getById(guideId);
            if (guide == null) {
                sender.sendMessage(GuidebookText.ItemInvalidGuideId.text(guideId.toString()));
                return;
            }

            if (args.length == 1) {
                GuideMEClient.openGuideAtPreviousPage(guide, guide.getStartPage());
            } else {
                GuideMEClient.openGuideAtAnchor(guide, PageAnchorArgument.parse(args[1]));
            }
        }

        @Override
        public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args,
                @Nullable BlockPos targetPos) {
            return switch (args.length) {
                case 1 -> getListOfStringsMatchingLastWord(args, GuideIdArgument.listSuggestions());
                case 2 -> {
                    ResourceLocation guideId = IdUtils.tryParse(args[0]);
                    yield guideId == null
                            ? Collections.emptyList()
                            : getListOfStringsMatchingLastWord(args, PageAnchorArgument.listSuggestions(guideId));
                }
                default -> Collections.emptyList();
            };
        }
    }

    private static class ExportCommand extends CommandBase {
        @Override
        public String getName() {
            return "export";
        }

        @Override
        public String getUsage(ICommandSender sender) {
            return "guideme.commands.guidemec.export.usage";
        }

        @Override
        public int getRequiredPermissionLevel() {
            return 0;
        }

        @Override
        public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
            if (args.length != 1)
                throw new WrongUsageException(this.getUsage(sender));

            GuideIdArgument.parse(args[0]);
            sender.sendMessage(new TextComponentTranslation("guideme.commands.guidemec.export.unavailable"));
        }

        @Override
        public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args,
                @Nullable BlockPos targetPos) {
            return args.length == 1
                    ? getListOfStringsMatchingLastWord(args, GuideIdArgument.listSuggestions())
                    : Collections.emptyList();
        }
    }
}
