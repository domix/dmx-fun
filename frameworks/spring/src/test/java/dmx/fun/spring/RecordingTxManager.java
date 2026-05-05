package dmx.fun.spring;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

class RecordingTxManager implements PlatformTransactionManager {
    private final PlatformTransactionManager delegate;
    private boolean used = false;
    private TransactionDefinition lastDefinition = null;

    RecordingTxManager(PlatformTransactionManager delegate) {
        this.delegate = delegate;
    }

    void reset() { used = false; lastDefinition = null; }
    boolean wasUsed() { return used; }
    TransactionDefinition lastDefinition() { return lastDefinition; }

    @Override
    public TransactionStatus getTransaction(TransactionDefinition definition) {
        used = true;
        lastDefinition = definition;
        return delegate.getTransaction(definition);
    }

    @Override
    public void commit(TransactionStatus status) { delegate.commit(status); }

    @Override
    public void rollback(TransactionStatus status) { delegate.rollback(status); }
}
