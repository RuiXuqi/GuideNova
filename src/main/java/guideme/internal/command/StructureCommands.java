package guideme.internal.command;

import guideme.internal.GuideRegistry;
import guideme.internal.GuidebookText;
import guideme.internal.MutableGuide;
import guideme.internal.util.ResourceUtil;
import guideme.internal.util.SNBTUtil;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.FixTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.structure.template.PlacementSettings;
import net.minecraft.world.gen.structure.template.Template;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.server.command.CommandTreeBase;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements commands that help with the workflow to create and edit structures for use in the guidebook. The commands
 * will not be used directly by users, but rather by command blocks built by
 * {@link appeng.server.testplots.GuidebookPlot}.
 */
public final class StructureCommands {
    private static final Logger LOG = LoggerFactory.getLogger(StructureCommands.class);

    private StructureCommands() {
    }

    @Nullable
    private static String lastOpenedOrSavedPath;

    private static final String[] FILE_PATTERNS = { "*.snbt", "*.nbt" };

    private static final String FILE_PATTERN_DESC = "Structure NBT Files (*.snbt, *.nbt)";

    public static void register(CommandTreeBase rootCommand) {
        rootCommand.addSubcommand(new PlaceAllStructuresCommand());
        rootCommand.addSubcommand(new ImportStructureCommand());
        rootCommand.addSubcommand(new ExportStructureCommand());
    }

    @Nullable
    private static WorldServer getIntegratedServerLevel(MinecraftServer server, ICommandSender sender) {
        if (!server.isSinglePlayer()) {
            sender.sendMessage(GuidebookText.CommandOnlyWorksInSinglePlayer.text());
            return null;
        }
        return server.getWorld(sender.getEntityWorld().provider.getDimension());
    }

    private static void placeAllStructures(WorldServer level, BlockPos origin) {
        var currentPos = new MutableObject<>(origin);

        for (var guide : GuideRegistry.getAll()) {
            placeAllStructures(level, currentPos, guide);
        }

    }

    private static void placeAllStructures(WorldServer level, MutableObject<BlockPos> origin, MutableGuide guide) {
        var minecraft = Minecraft.getMinecraft();
        var server = minecraft.getIntegratedServer();
        var player = minecraft.player;
        if (server == null || player == null) {
            return;
        }

        var sourceFolder = guide.getDevelopmentSourceFolder();

        List<Pair<String, Supplier<String>>> structures = new ArrayList<>();
        if (sourceFolder == null) {
            var resources = ResourceUtil.scanResources(
                    guide.getContentRootFolder(),
                    path -> path.endsWith(".snbt"));
            for (var entry : resources.entrySet()) {
                structures.add(Pair.of(entry.getKey().toString(), () -> {
                    try (var in = entry.getValue().open()) {
                        return new String(in.readAllBytes());
                    } catch (IOException e) {
                        LOG.error("Failed to read structure {}", entry.getKey(), e);
                        return null;
                    }
                }));
            }
        } else {
            try (var s = Files.walk(sourceFolder)
                    .filter(p -> Files.isRegularFile(p) && p.getFileName().toString().endsWith(".snbt"))) {
                s.forEach(path -> {
                    structures.add(Pair.of(
                            path.toString(),
                            () -> {
                                try {
                                    return Files.readString(path);
                                } catch (IOException e) {
                                    LOG.error("Failed to read structure {}", path, e);
                                    return null;
                                }
                            }));
                });
            } catch (IOException e) {
                LOG.error("Failed to find all structures.", e);
                player.sendMessage(new TextComponentString(e.toString()));
                return;
            }
        }

        for (var pair : structures) {
            var snbtFile = pair.getLeft();
            var contentSupplier = pair.getRight();
            LOG.info("Placing {}", snbtFile);
            try {
                var textInFile = contentSupplier.get();
                if (textInFile == null) {
                    continue;
                }

                var compound = SNBTUtil.snbtToStructure(textInFile);
                var structure = readStructure(compound);
                var pos = origin.get();
                structure.addBlocksToWorld(
                        level,
                        pos,
                        new PlacementSettings(),
                        Constants.BlockFlags.SEND_TO_CLIENTS);
                origin.setValue(origin.get().add(structure.getSize().getX() + 2, 0, 0));
            } catch (Exception e) {
                LOG.error("Failed to place {}.", snbtFile, e);
                player.sendMessage(new TextComponentString("Failed to place " + snbtFile + ": " + e));
            }
        }
    }

    private static void importStructure(WorldServer level, BlockPos origin) {
        var minecraft = Minecraft.getMinecraft();
        var server = minecraft.getIntegratedServer();
        var player = minecraft.player;
        if (server == null || player == null) {
            return;
        }

        minecraft.addScheduledTask(() -> {
            String selectedPath = pickFileForOpen();
            if (selectedPath != null) {
                lastOpenedOrSavedPath = selectedPath; // remember for save dialog
                level.addScheduledTask(() -> {
                    try {
                        if (placeStructure(level, origin, selectedPath)) {
                            player.sendMessage(new TextComponentString("Placed structure"));
                        } else {
                            player.sendMessage(new TextComponentString("Failed to place structure"));
                        }
                    } catch (Exception e) {
                        LOG.error("Failed to place structure.", e);
                        player.sendMessage(new TextComponentString(e.toString()));
                    }
                });
            }
            minecraft.addScheduledTask(() -> {
                if (minecraft.currentScreen instanceof GuiIngameMenu) {
                    minecraft.displayGuiScreen(null);
                }
            });
        });
    }

    private static boolean placeStructure(WorldServer level,
            BlockPos origin,
            String structurePath) throws IOException, NBTException {
        NBTTagCompound compound;
        if (structurePath.toLowerCase(Locale.ROOT).endsWith(".snbt")) {
            var textInFile = Files.readString(Paths.get(structurePath), StandardCharsets.UTF_8);
            compound = SNBTUtil.snbtToStructure(textInFile);
        } else {
            try (var input = new BufferedInputStream(new FileInputStream(structurePath))) {
                compound = CompressedStreamTools.readCompressed(input);
            }
        }
        var structure = readStructure(compound);

        structure.addBlocksToWorld(
                level,
                origin,
                new PlacementSettings(),
                Constants.BlockFlags.SEND_TO_CLIENTS);
        return true;
    }

    /// {@link net.minecraft.world.gen.structure.template.TemplateManager#readTemplateFromStream(String, InputStream)}
    @SuppressWarnings("JavadocReference")
    private static Template readStructure(NBTTagCompound compound) {
        var fixer = Minecraft.getMinecraft().getDataFixer();
        if (!compound.hasKey("DataVersion", Constants.NBT.TAG_ANY_NUMERIC)) {
            compound.setInteger("DataVersion", 500);
        }
        var template = new Template();
        template.read(fixer.process(FixTypes.STRUCTURE, compound));
        return template;
    }

    private static void exportStructure(WorldServer level, BlockPos origin, Vec3i size) {
        var minecraft = Minecraft.getMinecraft();
        var server = minecraft.getIntegratedServer();
        var player = minecraft.player;
        if (server == null || player == null) {
            return;
        }

        minecraft.addScheduledTask(() -> {
            String selectedPath = pickFileForSave();
            if (selectedPath != null) {
                lastOpenedOrSavedPath = selectedPath; // remember for open dialog
                level.addScheduledTask(() -> {
                    try {
                        // Find the smallest box containing the placed blocks
                        var end = origin.add(size.getX() - 1, size.getY() - 1, size.getZ() - 1);
                        var max = origin;
                        for (var pos : BlockPos.getAllInBox(origin, end)) {
                            if (!level.isAirBlock(pos)) {
                                max = new BlockPos(
                                        Math.max(max.getX(), pos.getX()),
                                        Math.max(max.getY(), pos.getY()),
                                        Math.max(max.getZ(), pos.getZ()));
                            }
                        }

                        var actualSize = new BlockPos(
                                1 + max.getX() - origin.getX(),
                                1 + max.getY() - origin.getY(),
                                1 + max.getZ() - origin.getZ());

                        var structureTemplate = new Template();
                        structureTemplate.takeBlocksFromWorld(
                                level,
                                origin,
                                actualSize,
                                false,
                                Blocks.AIR);

                        var compound = structureTemplate.writeToNBT(new NBTTagCompound());
                        if (selectedPath.toLowerCase(Locale.ROOT).endsWith(".snbt")) {
                            Files.writeString(
                                    Paths.get(selectedPath),
                                    SNBTUtil.structureToSnbt(compound),
                                    StandardCharsets.UTF_8);
                        } else {
                            try (var output = new BufferedOutputStream(new FileOutputStream(selectedPath))) {
                                CompressedStreamTools.writeCompressed(compound, output);
                            }
                        }

                        player.sendMessage(new TextComponentString("Saved structure"));
                    } catch (IOException e) {
                        LOG.error("Failed to save structure.", e);
                        player.sendMessage(new TextComponentString(e.toString()));
                    }
                });
            }
            minecraft.addScheduledTask(() -> {
                if (minecraft.currentScreen instanceof GuiIngameMenu) {
                    minecraft.displayGuiScreen(null);
                }
            });
        });
    }

    private static String pickFileForOpen() {
        setDefaultFolder();

        try (var stack = MemoryStack.stackPush()) {

            var result = TinyFileDialogs.ntinyfd_openFileDialog(
                    MemoryUtil.memAddress(stack.UTF8("Load Structure")),
                    MemoryUtil.memAddressSafe(stack.UTF8Safe(lastOpenedOrSavedPath)),
                    FILE_PATTERNS.length,
                    createFilterPatterns(stack),
                    MemoryUtil.memAddress(stack.UTF8(FILE_PATTERN_DESC)),
                    0);
            return MemoryUtil.memUTF8Safe(result);
        }
    }

    private static String pickFileForSave() {
        setDefaultFolder();

        try (var stack = MemoryStack.stackPush()) {

            var result = TinyFileDialogs.ntinyfd_saveFileDialog(
                    MemoryUtil.memAddress(stack.UTF8("Save Structure")),
                    MemoryUtil.memAddressSafe(stack.UTF8Safe(lastOpenedOrSavedPath)),
                    FILE_PATTERNS.length,
                    createFilterPatterns(stack),
                    MemoryUtil.memAddress(stack.UTF8(FILE_PATTERN_DESC)));
            return MemoryUtil.memUTF8Safe(result);
        }
    }

    private static long createFilterPatterns(MemoryStack stack) {
        var filterPatternsBuffer = stack.mallocLong(FILE_PATTERNS.length);
        for (var pattern : FILE_PATTERNS) {
            filterPatternsBuffer.put(MemoryUtil.memAddress(stack.UTF8(pattern)));
        }
        filterPatternsBuffer.flip();
        return MemoryUtil.memAddress(filterPatternsBuffer);
    }

    private static void setDefaultFolder() {
        // If any guide has development sources, default to that folder
        if (lastOpenedOrSavedPath == null) {
            for (var guide : GuideRegistry.getAll()) {
                if (guide.getDevelopmentSourceFolder() != null) {
                    lastOpenedOrSavedPath = guide.getDevelopmentSourceFolder().toString();
                    if (!lastOpenedOrSavedPath.endsWith("/") && !lastOpenedOrSavedPath.endsWith("\\")) {
                        lastOpenedOrSavedPath += File.separator;
                    }
                    break;
                }
            }
        }
    }

    private static class PlaceAllStructuresCommand extends CommandBase {
        @Override
        public String getName() {
            return "placeallstructures";
        }

        @Override
        public String getUsage(ICommandSender sender) {
            return "guideme.commands.guideme.placeallstructures.usage";
        }

        @Override
        public int getRequiredPermissionLevel() {
            return 2;
        }

        @Override
        public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
            if (args.length != 3 && args.length != 4)
                throw new WrongUsageException(this.getUsage(sender));

            var level = getIntegratedServerLevel(server, sender);
            if (level == null) {
                return;
            }

            var origin = parseBlockPos(sender, args, 0, false);
            if (args.length == 3) {
                placeAllStructures(level, origin);
                return;
            }

            var guideId = GuideIdArgument.parse(args[3]);
            var guide = GuideRegistry.getById(guideId);
            if (guide != null)
                placeAllStructures(level, new MutableObject<>(origin), guide);
        }

        @Override
        public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args,
                @Nullable BlockPos targetPos) {
            if (args.length <= 3)
                return getTabCompletionCoordinate(args, 0, targetPos);
            return args.length == 4
                    ? getListOfStringsMatchingLastWord(args, GuideIdArgument.listSuggestions())
                    : Collections.emptyList();
        }
    }

    private static class ImportStructureCommand extends CommandBase {
        @Override
        public String getName() {
            return "importstructure";
        }

        @Override
        public String getUsage(ICommandSender sender) {
            return "guideme.commands.guideme.importstructure.usage";
        }

        @Override
        public int getRequiredPermissionLevel() {
            return 2;
        }

        @Override
        public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
            if (args.length != 3)
                throw new WrongUsageException(this.getUsage(sender));

            var level = getIntegratedServerLevel(server, sender);
            if (level == null) {
                return;
            }

            importStructure(level, parseBlockPos(sender, args, 0, false));
        }

        @Override
        public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args,
                @Nullable BlockPos targetPos) {
            return args.length <= 3 ? getTabCompletionCoordinate(args, 0, targetPos) : Collections.emptyList();
        }
    }

    private static class ExportStructureCommand extends CommandBase {
        @Override
        public String getName() {
            return "exportstructure";
        }

        @Override
        public String getUsage(ICommandSender sender) {
            return "guideme.commands.guideme.exportstructure.usage";
        }

        @Override
        public int getRequiredPermissionLevel() {
            return 2;
        }

        @Override
        public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
            if (args.length != 6)
                throw new WrongUsageException(this.getUsage(sender));

            var level = getIntegratedServerLevel(server, sender);
            if (level == null) {
                return;
            }

            var origin = parseBlockPos(sender, args, 0, false);
            var size = new Vec3i(
                    parseInt(args[3], 1),
                    parseInt(args[4], 1),
                    parseInt(args[5], 1));
            exportStructure(level, origin, size);
        }

        @Override
        public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args,
                @Nullable BlockPos targetPos) {
            return args.length <= 3 ? getTabCompletionCoordinate(args, 0, targetPos) : Collections.emptyList();
        }
    }
}
