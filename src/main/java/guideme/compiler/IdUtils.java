package guideme.compiler;

import java.net.URI;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.jetbrains.annotations.Nullable;

/**
 * Helper to resolve shorthand and relative IDs found in markdown pages.
 */
public final class IdUtils {

    private IdUtils() {
    }

    public static ResourceLocation resolveId(String idText, String defaultNamespace) {
        if (!idText.contains(":")) {
            return build(defaultNamespace, idText);
        }
        return parse(idText);
    }

    /**
     * Supports relative resource locations such as: ./somepath, which would resolve relative to a given anchor
     * location. Relative locations must not be namespaced since we would otherwise run into the problem if namespaced
     * locations potentially having a different namespace than the anchor.
     */
    public static ResourceLocation resolveLink(String idText, ResourceLocation anchor) {
        if (idText.startsWith("/")) {
            // Absolute path, but relative to namespace
            return build(anchor.getNamespace(), idText.substring(1));
        } else if (!idText.contains(":")) {
            URI uri = URI.create(anchor.getPath());
            uri = uri.resolve(idText);

            var relativeId = uri.toString();

            return build(anchor.getNamespace(), relativeId);
        }

        // if it contains a ":" it's assumed to be absolute
        return parse(idText);
    }

    public static ResourceLocation withPath(ResourceLocation resourceLocation, String path) {
        return build(resourceLocation.getNamespace(), path);
    }

    public static ResourceLocation withPrefix(ResourceLocation resourceLocation, String prefix) {
        return withPath(resourceLocation, prefix + resourceLocation.getPath());
    }

    public static ResourceLocation withSuffix(ResourceLocation resourceLocation, String suffix) {
        return withPath(resourceLocation, resourceLocation.getPath() + suffix);
    }

    public static ResourceLocation withPrefixAndSuffix(ResourceLocation resourceLocation,
            String prefix, String suffix) {
        return withPath(resourceLocation, prefix + resourceLocation.getPath() + suffix);
    }

    public static ResourceLocation stripPrefix(ResourceLocation resourceLocation, String prefix) {
        var path = resourceLocation.getPath();
        if (!path.startsWith(prefix))
            return null;
        return withPath(resourceLocation, path.substring(prefix.length()));
    }

    public static ResourceLocation stripSuffix(ResourceLocation resourceLocation, String suffix) {
        var path = resourceLocation.getPath();
        if (!path.endsWith(suffix))
            return null;
        return withPath(resourceLocation, path.substring(0, path.length() - suffix.length()));
    }

    public static ResourceLocation stripPrefixAndSuffix(ResourceLocation resourceLocation, String prefix,
            String suffix) {
        var path = resourceLocation.getPath();
        if (!path.startsWith(prefix) || !path.endsWith(suffix))
            return null;
        return withPath(resourceLocation, path.substring(prefix.length(), path.length() - suffix.length()));
    }

    // Modern calls of "new ResourceLocation" MUST be converted to this
    public static ResourceLocation build(String namespace, String path) {
        return new ResourceLocation(assertValidNamespace(namespace, path), assertValidPath(namespace, path));
    }

    @Nullable
    public static ResourceLocation tryBuild(String namespace, String path) {
        try {
            return new ResourceLocation(namespace, path);
        } catch (ResourceLocationException exception) {
            return null;
        }
    }

    public static ResourceLocation parse(String location) {
        String[] astring = ResourceLocation.splitObjectName(location);
        return build(astring[0], astring[1]);
    }

    /**
     * Attempts to parse the specified {@code location} as a {@code ResourceLocation} by splitting it into a namespace
     * and path by a colon.
     * <p>
     * If no colon is present in the {@code location}, the namespace defaults to {@code minecraft}, taking the {@code
     * location} as the path.
     *
     * @param location the location string to try to parse as a {@code ResourceLocation}
     * @return the parsed resource location; otherwise {@code null} if there is a non {@code [a-z0-9_.-]} character in
     *         the decomposed namespace or a non {@code [a-z0-9/._-]} character in the decomposed path
     */
    @Nullable
    public static ResourceLocation tryParse(String location) {
        try {
            return parse(location);
        } catch (ResourceLocationException exception) {
            return null;
        }
    }

    /**
     * Splits the specified {@code location} into a namespace and path by a colon, checking both are valid.
     * <p>
     * If no colon is present in the {@code location}, the namespace defaults to {@code minecraft}, taking the {@code
     * location} as the path.
     * </p>
     *
     * @return {@code true} if both the decomposed namespace and path are valid
     * @see #isValidPath(String)
     * @see #isValidNamespace(String)
     */
    public static boolean isValidResourceLocation(String location) {
        String[] astring = ResourceLocation.splitObjectName(location);
        return isValidNamespace(StringUtils.isEmpty(astring[0]) ? "minecraft" : astring[0]) && isValidPath(astring[1]);
    }

    public static boolean isValidPath(String path) {
        for (int i = 0; i < path.length(); ++i) {
            if (!validPathChar(path.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean validPathChar(char charValue) {
        return validNamespaceChar(charValue) || charValue == '/';
    }

    public static boolean isValidNamespace(String namespace) {
        for (int i = 0; i < namespace.length(); ++i) {
            if (!validNamespaceChar(namespace.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean validNamespaceChar(char charValue) {
        return charValue == '_' || charValue == '-' || charValue >= 'a' && charValue <= 'z'
                || charValue >= '0' && charValue <= '9' || charValue == '.';
    }

    private static String assertValidPath(String namespace, String path) {
        if (!isValidPath(path)) {
            throw new ResourceLocationException(
                    "Non [a-z0-9/._-] character in path of location: " + namespace + ":" + path);
        } else {
            return path;
        }
    }

    private static String assertValidNamespace(String namespace, String path) {
        if (!isValidNamespace(namespace)) {
            throw new ResourceLocationException(
                    "Non [a-z0-9_.-] character in namespace of location: " + namespace + ":" + path);
        } else {
            return namespace;
        }
    }

    public static class ResourceLocationException extends RuntimeException {
        public ResourceLocationException(String message) {
            super(StringEscapeUtils.escapeJava(message));
        }

        public ResourceLocationException(String message, Throwable cause) {
            super(StringEscapeUtils.escapeJava(message), cause);
        }
    }
}
