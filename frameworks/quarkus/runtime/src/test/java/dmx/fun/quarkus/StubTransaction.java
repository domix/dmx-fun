package dmx.fun.quarkus;

import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.Synchronization;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import javax.transaction.xa.XAResource;

/** Opaque token used to verify that suspend/resume wires the right Transaction instance. */
class StubTransaction implements Transaction {

    @Override
    public void commit() throws RollbackException, HeuristicMixedException,
            HeuristicRollbackException, SecurityException, SystemException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean delistResource(XAResource xaRes, int flag)
            throws IllegalStateException, SystemException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean enlistResource(XAResource xaRes)
            throws RollbackException, IllegalStateException, SystemException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getStatus() throws SystemException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void registerSynchronization(Synchronization sync)
            throws RollbackException, IllegalStateException, SystemException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void rollback() throws IllegalStateException, SystemException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setRollbackOnly() throws IllegalStateException, SystemException {
        throw new UnsupportedOperationException();
    }
}
