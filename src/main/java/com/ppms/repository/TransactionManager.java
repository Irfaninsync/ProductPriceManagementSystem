// GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
package com.ppms.repository;

import java.util.function.Supplier;

/**
 * Abstraction enabling multi-repository atomic transactions without tying services to SQL APIs.
 */
@FunctionalInterface
public interface TransactionManager {

    <T> T inTransaction(Supplier<T> action);

    default void inTransactionVoid(Runnable action) {
        inTransaction(() -> {
            action.run();
            return null;
        });
    }

    static TransactionManager direct() {
        return new TransactionManager() {
            @Override
            public <T> T inTransaction(Supplier<T> action) {
                return action.get();
            }
        };
    }
}
// End of file — GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
