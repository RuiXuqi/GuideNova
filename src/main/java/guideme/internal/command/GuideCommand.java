package guideme.internal.command;

import guideme.Guides;
import guideme.compiler.IdUtils;
import guideme.internal.GuideMEProxy;
import java.util.Collections;
import java.util.List;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.server.command.CommandTreeBase;
import org.jetbrains.annotations.Nullable;

public class GuideCommand extends CommandTreeBase {
    public GuideCommand() {
        this.addSubcommand(new OpenGuideCommand());
        this.addSubcommand(new GiveGuideCommand());
    }

    @Override
    public String getName() {
        return "guideme";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "guideme.commands.guideme.usage";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    private static class OpenGuideCommand extends CommandBase {
        @Override
        public String getName() {
            return "open";
        }

        @Override
        public String getUsage(ICommandSender sender) {
            return "guideme.commands.guideme.open.usage";
        }

        @Override
        public int getRequiredPermissionLevel() {
            return 2;
        }

        @Override
        public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
            if (args.length != 2 && args.length != 3)
                throw new WrongUsageException(this.getUsage(sender));

            var targets = getPlayers(server, sender, args[0]);
            var guideId = GuideIdArgument.parse(args[1]);
            var anchor = args.length == 3 ? PageAnchorArgument.parse(args[2]) : null;

            for (var target : targets) {
                if (anchor == null) {
                    GuideMEProxy.instance().openGuide(target, guideId);
                } else {
                    GuideMEProxy.instance().openGuide(target, guideId, anchor);
                }
            }
        }

        @Override
        public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args,
                @Nullable BlockPos targetPos) {
            return switch (args.length) {
                case 1 -> getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());
                case 2 -> getListOfStringsMatchingLastWord(args, GuideIdArgument.listSuggestions());
                case 3 -> {
                    ResourceLocation guideId = IdUtils.tryParse(args[1]);
                    yield guideId == null
                            ? Collections.emptyList()
                            : getListOfStringsMatchingLastWord(args, PageAnchorArgument.listSuggestions(guideId));
                }
                default -> Collections.emptyList();
            };
        }
    }

    private static class GiveGuideCommand extends CommandBase {
        @Override
        public String getName() {
            return "give";
        }

        @Override
        public String getUsage(ICommandSender sender) {
            return "guideme.commands.guideme.give.usage";
        }

        @Override
        public int getRequiredPermissionLevel() {
            return 2;
        }

        @Override
        public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
            if (args.length != 2)
                throw new WrongUsageException(this.getUsage(sender));

            var targets = getPlayers(server, sender, args[0]);
            var guideId = GuideIdArgument.parse(args[1]);
            var guideItem = Guides.createGuideItem(guideId);

            for (var target : targets) {
                ItemHandlerHelper.giveItemToPlayer(target, guideItem.copy());
                notifyCommandListener(sender, this, "commands.give.success",
                        guideItem.getTextComponent(), 1, target.getName());
            }
        }

        @Override
        public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args,
                @Nullable BlockPos targetPos) {
            return switch (args.length) {
                case 1 -> getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());
                case 2 -> getListOfStringsMatchingLastWord(args, GuideIdArgument.listSuggestions());
                default -> Collections.emptyList();
            };
        }
    }
}
