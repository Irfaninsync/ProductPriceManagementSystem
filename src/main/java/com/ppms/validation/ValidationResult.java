package com.ppms.validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Encapsulates the outcome of a domain or input validation operation.
 */
public class ValidationResult {
    private final List<String> errors;

    public ValidationResult() {
        this.errors = new ArrayList<>();
    }

    public ValidationResult(List<String> errors) {
        this.errors = new ArrayList<>(errors != null ? errors : Collections.emptyList());
    }

    public static ValidationResult valid() {
        return new ValidationResult();
    }

    public static ValidationResult invalid(String error) {
        ValidationResult result = new ValidationResult();
        result.addError(error);
        return result;
    }

    public static ValidationResult invalid(List<String> errors) {
        return new ValidationResult(errors);
    }

    public void addError(String error) {
        if (error != null && !error.trim().isEmpty()) {
            this.errors.add(error.trim());
        }
    }

    public boolean isValid() {
        return errors.isEmpty();
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public List<String> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    public String getFormattedErrors() {
        if (isValid()) {
            return "No validation errors.";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < errors.size(); i++) {
            sb.append(i + 1).append(". ").append(errors.get(i));
            if (i < errors.size() - 1) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return isValid() ? "ValidationResult[Valid]" : "ValidationResult[Errors=" + errors + "]";
    }
}
