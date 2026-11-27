// GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
package com.ppms.validation;

import com.ppms.model.Category;

/**
 * Validates domain constraints for product categories.
 */
public class CategoryValidator {

    private static final int MIN_NAME_LENGTH = 2;
    private static final int MAX_NAME_LENGTH = 60;
    private static final int MAX_DESCRIPTION_LENGTH = 300;

    public static ValidationResult validate(Category category) {
        ValidationResult result = new ValidationResult();
        if (category == null) {
            result.addError("Category entity cannot be null.");
            return result;
        }
        validateName(category.getName(), result);
        validateDescription(category.getDescription(), result);
        return result;
    }

    public static void validateName(String name, ValidationResult result) {
        if (name == null || name.trim().isEmpty()) {
            result.addError("Category name is required and cannot be empty.");
            return;
        }
        String clean = name.trim();
        if (clean.length() < MIN_NAME_LENGTH || clean.length() > MAX_NAME_LENGTH) {
            result.addError("Category name must be between " + MIN_NAME_LENGTH + " and " + MAX_NAME_LENGTH + " characters.");
        }
    }

    public static void validateDescription(String description, ValidationResult result) {
        if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
            result.addError("Category description cannot exceed " + MAX_DESCRIPTION_LENGTH + " characters.");
        }
    }
}
// End of file — GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
