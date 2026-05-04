package dmx.fun.quarkus;

import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.InvalidTransactionException;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.Status;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;
import org.jspecify.annotations.Nullable;

class StubTransactionManager implements TransactionManager {

    int beginCount;
    int commitCount;
    int rollbackCount;
    int suspendCount;
    int resumeCount;
    int setRollbackOnlyCount;

    /** Controls what {@link #getStatus()} returns. Defaults to {@link Status#STATUS_ACTIVE}. */
    int statusToReturn = Status.STATUS_ACTIVE;

    @Nullable Transaction lastResumedTx;
    @Nullable Transaction transactionToReturn;

    @Nullable Exception throwOnBegin;
    @Nullable Exception throwOnCommit;
    @Nullable Exception throwOnRollback;
    @Nullable Exception throwOnSuspend;
    @Nullable Exception throwOnResume;
    @Nullable SystemException throwOnGetStatus;
    @Nullable SystemException throwOnSetRollbackOnly;

    @Override
    public void begin() throws NotSupportedException, SystemException {
        beginCount++;
        if (throwOnBegin instanceof NotSupportedException e) {
            throw e;
        }

        if (throwOnBegin instanceof SystemException e) {
            throw e;
        }
    }

    @Override
    public void commit() throws RollbackException, HeuristicMixedException,
            HeuristicRollbackException, SecurityException, IllegalStateException, SystemException {
        commitCount++;
        if (throwOnCommit instanceof RollbackException e) {
            throw e;
        }
        if (throwOnCommit instanceof HeuristicMixedException e) {
            throw e;
        }
        if (throwOnCommit instanceof HeuristicRollbackException e) {
            throw e;
        }
        if (throwOnCommit instanceof SystemException e) {
            throw e;
        }
    }

    @Override
    public void rollback() throws IllegalStateException, SecurityException, SystemException {
        rollbackCount++;
        if (throwOnRollback instanceof SystemException e) {
            throw e;
        }
    }

    @Override
    public void setRollbackOnly() throws IllegalStateException, SystemException {
        setRollbackOnlyCount++;
        if (throwOnSetRollbackOnly != null) {
            throw throwOnSetRollbackOnly;
        }
    }

    @Override
    public int getStatus() throws SystemException {
        if (throwOnGetStatus != null) {
            throw throwOnGetStatus;
        }
        return statusToReturn;
    }

    @Override
    public Transaction getTransaction() throws SystemException { return null; }

    @Override
    public Transaction suspend() throws SystemException {
        suspendCount++;
        if (throwOnSuspend instanceof SystemException e) {
            throw e;
        }
        return transactionToReturn;
    }

    @Override
    public void resume(Transaction tobj)
            throws InvalidTransactionException, IllegalStateException, SystemException {
        resumeCount++;
        lastResumedTx = tobj;
        if (throwOnResume instanceof InvalidTransactionException e) {
            throw e;
        }
        if (throwOnResume instanceof SystemException e) {
            throw e;
        }
    }

    @Override
    public void setTransactionTimeout(int seconds) throws SystemException {}
}
