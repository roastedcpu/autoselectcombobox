package org.vaadin.addons.autoselectcombobox;

/**
 * Validates a custom (non-existing) value before it is passed to the
 * {@link CustomValueHandler} or accepted directly.
 * <p>
 * Return a {@link Result} indicating whether the value is acceptable.
 * If invalid, the error message is shown on the component.
 */
@FunctionalInterface
public interface CustomValueValidator {

    Result validate(String customText);

    static CustomValueValidator acceptAll() {
        return text -> Result.ok();
    }

    final class Result {

        private final boolean valid;
        private final String errorMessage;

        private Result(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public static Result ok() {
            return new Result(true, null);
        }

        public static Result error(String message) {
            return new Result(false, message);
        }

        public boolean valid() {
            return valid;
        }

        public String errorMessage() {
            return errorMessage;
        }
    }
}
