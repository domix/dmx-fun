package dmx.fun.quarkus;

import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.SystemException;
import jakarta.transaction.UserTransaction;

/**
 * Test stub for {@link UserTransaction} that counts invocations and can be
 * pre-configured to throw specific JTA exceptions on the next call.
 */
class StubUserTransaction implements UserTransaction {

    int beginCount;
    int commitCount;
    int rollbackCount;

    Exception throwOnBegin;
    Exception throwOnCommit;
    Exception throwOnRollback;

    @Override
    public void begin() throws NotSupportedException, SystemException {
        beginCount++;
        if (throwOnBegin instanceof NotSupportedException e) throw e;
        if (throwOnBegin instanceof SystemException e)       throw e;
    }

    @Override
    public void commit() throws RollbackException, HeuristicMixedException,
            HeuristicRollbackException, SecurityException, IllegalStateException, SystemException {
        commitCount++;
        if (throwOnCommit instanceof RollbackException e)         throw e;
        if (throwOnCommit instanceof HeuristicMixedException e)   throw e;
        if (throwOnCommit instanceof HeuristicRollbackException e) throw e;
        if (throwOnCommit instanceof SystemException e)           throw e;
    }

    @Override
    public void rollback() throws IllegalStateException, SecurityException, SystemException {
        rollbackCount++;
        if (throwOnRollback instanceof SystemException e) throw e;
    }

    @Override
    public void setRollbackOnly() throws IllegalStateException, SystemException {}

    @Override
    public int getStatus() throws SystemException { return 0; }

    @Override
    public void setTransactionTimeout(int seconds) throws SystemException {}
}
