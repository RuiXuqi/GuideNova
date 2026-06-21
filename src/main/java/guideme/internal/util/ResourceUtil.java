package guideme.internal.util;

import guideme.compiler.IdUtils;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.AbstractResourcePack;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.LegacyV2Adapter;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.FMLContainerHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ResourceUtil {
    private static final Logger LOG = LoggerFactory.getLogger(ResourceUtil.class);
    private static final String ASSETS_PREFIX = "assets/";

    private ResourceUtil() {
    }

    public static boolean resourceExists(IResourceManager resourceManager, ResourceLocation location) {
        try (var _ = resourceManager.getResource(location)) {
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    public static List<IResourcePack> getActiveResourcePacks() {
        // Minecraft#refreshResources()
        var client = Minecraft.getMinecraft();
        // Packs added later have higher priority

        // Mods
        var result = new ArrayList<>(FMLClientHandler.instance().getResourcePackList());

        // Active user resource packs
        for (var entry : client.getResourcePackRepository().getRepositoryEntries()) {
            result.add(entry.getResourcePack());
        }

        // Server resource packs
        var server = client.getResourcePackRepository().getServerResourcePack();
        if (server != null)
            result.add(server);

        return result;
    }

    public static ScannedResources scanAllResources(Predicate<String> pathFilter) {
        var result = new LinkedHashMap<ResourceLocation, PackResource>();
        for (var resourcePack : getActiveResourcePacks()) {
            scanPack(resourcePack, pathFilter, result);
        }
        return new ScannedResources(result);
    }

    public static Map<ResourceLocation, PackResource> scanResources(String root, Predicate<String> pathFilter) {
        root = normalizeRoot(root);
        var rootPrefix = root.isEmpty() ? "" : root + "/";
        return scanAllResources(path -> path.startsWith(rootPrefix) && pathFilter.test(path)).resources();
    }

    private static void scanPack(
            IResourcePack resourcePack,
            Predicate<String> pathFilter, Map<ResourceLocation, PackResource> result) {
        if (!(unwrap(resourcePack) instanceof AbstractResourcePack abstractResourcePack))
            return;
        // OF's patch ruined CRL's method, so use AT here
        var packFile = abstractResourcePack.resourcePackFile;
        if (packFile.isDirectory()) {
            scanDirectoryPack(resourcePack, packFile, pathFilter, result);
        } else if (packFile.isFile()) {
            scanZipPack(resourcePack, packFile, pathFilter, result);
        }
    }

    private static void scanDirectoryPack(
            IResourcePack resourcePack, File packFolder,
            Predicate<String> pathFilter, Map<ResourceLocation, PackResource> result) {
        var assetsFolder = new File(packFolder, "assets");
        var namespaceFolders = assetsFolder.listFiles(File::isDirectory);
        if (namespaceFolders == null)
            return;

        for (var namespaceFolder : namespaceFolders) {
            var namespace = namespaceFolder.getName();
            if (!IdUtils.isValidNamespace(namespace))
                continue;

            var namespacePath = namespaceFolder.toPath();
            try (Stream<Path> paths = Files.walk(namespacePath)) {
                paths.filter(Files::isRegularFile)
                        .forEach(path -> addScanResult(resourcePack, namespace,
                                toResourcePath(namespacePath.relativize(path)), pathFilter, result));
            } catch (IOException e) {
                LOG.warn("Failed to scan guide resources in pack {} under {}", resourcePack.getPackName(),
                        namespacePath, e);
            }
        }
    }

    private static void scanZipPack(
            IResourcePack resourcePack, File packFile,
            Predicate<String> pathFilter, Map<ResourceLocation, PackResource> result) {
        try (var zipFile = new ZipFile(packFile)) {
            var entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                scanZipEntry(resourcePack, entries.nextElement(), pathFilter, result);
            }
        } catch (IOException e) {
            LOG.warn("Failed to scan guide resources in pack {}", resourcePack.getPackName(), e);
        }
    }

    private static void scanZipEntry(
            IResourcePack resourcePack, ZipEntry entry,
            Predicate<String> pathFilter, Map<ResourceLocation, PackResource> result) {
        if (entry.isDirectory())
            return;

        var name = entry.getName();
        if (!name.startsWith(ASSETS_PREFIX))
            return;

        var pathWithNamespace = name.substring(ASSETS_PREFIX.length());
        var namespaceEnd = pathWithNamespace.indexOf('/');
        if (namespaceEnd <= 0 || namespaceEnd == pathWithNamespace.length() - 1)
            return;

        var namespace = pathWithNamespace.substring(0, namespaceEnd);
        var path = pathWithNamespace.substring(namespaceEnd + 1);
        addScanResult(resourcePack, namespace, path, pathFilter, result);
    }

    private static void addScanResult(
            IResourcePack resourcePack,
            String namespace, String path,
            Predicate<String> pathFilter, Map<ResourceLocation, PackResource> result) {
        if (!pathFilter.test(path))
            return;

        var resourceId = IdUtils.tryBuild(namespace, path);
        if (resourceId != null) {
            result.put(resourceId, new PackResource(resourcePack, resourceId));
        }
    }

    private static String toResourcePath(Path path) {
        return path.toString().replace(File.separatorChar, '/');
    }

    private static IResourcePack unwrap(IResourcePack resourcePack) {
        if (resourcePack instanceof LegacyV2Adapter legacyV2Adapter) {
            // The adapter has no backing file of its own. Use the wrapped pack only to find the folder/zip to scan.
            // PackResource keeps the original pack so reads still go through the adapter.
            return legacyV2Adapter.getUnadaptedPack();
        }
        return resourcePack;
    }

    /**
     * Normalizes a resource root to the namespace-relative form used inside resource packs.
     * <p>
     * Example:
     * 
     * <pre>
     * "/guides/ae2/main/" -> "guides/ae2/main"
     * "\\guides\\ae2\\main" -> "guides/ae2/main"
     * </pre>
     */
    private static String normalizeRoot(String root) {
        var normalized = root.replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    /**
     * A resource found during scanning.
     * <p>
     * {@code id} is the full resource id that can be passed back to {@link IResourcePack#getInputStream}. For example,
     * {@code assets/ae2/guides/ae2/main/index.md} is represented as {@code ae2:guides/ae2/main/index.md}.
     */
    public record PackResource(IResourcePack pack, ResourceLocation id) {
        public String sourcePackId() {
            if (unwrap(this.pack) instanceof FMLContainerHolder containerHolder) {
                return "mod:" + containerHolder.getFMLContainer().getModId();
            }
            return this.pack.getPackName();
        }

        public InputStream open() throws IOException {
            return this.pack.getInputStream(this.id);
        }
    }

    /**
     * Resources discovered from resource packs, keyed by their full path below {@code assets/<namespace>}.
     * <p>
     * For example, {@code assets/ae2/guides/ae2/main/index.md} is stored as {@code ae2:guides/ae2/main/index.md}.
     * Callers that need a guide-local id still need to strip their content root from the path.
     */
    public record ScannedResources(Map<ResourceLocation, PackResource> resources) {
        public Map<ResourceLocation, PackResource> scanResources(String root, Predicate<String> pathFilter) {
            root = normalizeRoot(root);
            var result = new LinkedHashMap<ResourceLocation, PackResource>();
            var rootPrefix = root.isEmpty() ? "" : root + "/";

            for (var entry : this.resources().entrySet()) {
                var path = entry.getKey().getPath();
                if (path.startsWith(rootPrefix) && pathFilter.test(path)) {
                    result.put(entry.getKey(), entry.getValue());
                }
            }

            return result;
        }
    }
}
