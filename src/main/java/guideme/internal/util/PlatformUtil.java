package guideme.internal.util;

import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PlatformUtil {
    private static final Logger LOG = LoggerFactory.getLogger(PlatformUtil.class);

    private PlatformUtil() {
    }

    public static void openUri(URI uri) {
        try {
            Class<?> oclass = Class.forName("java.awt.Desktop");
            Object object = oclass.getMethod("getDesktop").invoke(null);
            oclass.getMethod("browse", URI.class).invoke(object, uri);
        } catch (Throwable throwable) {
            Throwable cause = throwable.getCause();
            LOG.error("Couldn't open link: {}", cause == null ? "<UNKNOWN>" : cause.getMessage());
        }
    }
}
