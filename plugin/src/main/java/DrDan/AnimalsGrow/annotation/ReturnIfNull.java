package DrDan.AnimalsGrow.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that marks a variable assignment to automatically return if the value is null.
 * 
 * Usage:
 * @ReturnIfNull String value = getString();
 * 
 * This will be rewritten at compile time to:
 * String value = getString();
 * if (value == null) return;
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.LOCAL_VARIABLE)
public @interface ReturnIfNull {
    /**
     * Optional custom message to log before returning (if needed)
     */
    String message() default "";
}
