package guideme.internal.util.config;

import com.google.common.base.CaseFormat;
import java.util.Locale;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
@FunctionalInterface
public interface IFormatter {
    String format(@Nullable String str);

    /**
     * ExampleString -> ExampleString
     */
    IFormatter IDENTITY = input -> input == null ? "" : input;

    /**
     * ExampleString -> examplestring
     */
    IFormatter LOWER_CASE = input -> input == null || input.isEmpty() ? "" : input.toLowerCase(Locale.ENGLISH);

    /**
     * ExampleString -> example_string
     */
    IFormatter CAMEL_TO_SNAKE = input -> input == null || input.isEmpty() ? ""
            : CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, input);
}
