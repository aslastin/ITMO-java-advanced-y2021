package info.kgeorgiy.ja.slastin.bank.server.accounts;

import info.kgeorgiy.ja.slastin.bank.common.Account;

import java.io.Serializable;
import java.rmi.RemoteException;

public class LocalAccount extends AbstractAccount implements Serializable {
    public LocalAccount(final Account account) throws RemoteException {
        super(account.getId(), account.getAmount());
    }

    public LocalAccount(String id) {
        super(id);
    }

    public LocalAccount(String id, int amount) {
        super(id, amount);
    }
}
