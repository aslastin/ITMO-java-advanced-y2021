package info.kgeorgiy.ja.slastin.bank.server.persons;

import info.kgeorgiy.ja.slastin.bank.common.Account;
import info.kgeorgiy.ja.slastin.bank.server.accounts.LocalAccount;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class LocalPerson extends AbstractPerson implements Serializable {
    public LocalPerson(final RemotePerson person) throws RemoteException {
        super(person.getName(), person.getSurname(), person.getPassport(), toLocalAccountById(person.accountById));
    }

    @Override
    public Account createAccount(int accountId) throws RemoteException {
        if (!accountById.containsKey(accountId)) {
            logger.info("Creating account with id " + accountId);
            accountById.put(accountId, new LocalAccount(getAccountId(passport, accountId)));
        } else {
            logger.info("Account with id " + accountId + " already exists");
        }
        return accountById.get(accountId);
    }

    private static ConcurrentMap<Integer, Account> toLocalAccountById(final ConcurrentMap<Integer, Account> accountById)
            throws RemoteException {
        final ConcurrentMap<Integer, Account> localizedAccountById = new ConcurrentHashMap<>();
        for (var idAndAccount : accountById.entrySet()) {
            localizedAccountById.put(idAndAccount.getKey(), new LocalAccount(idAndAccount.getValue()));
        }
        return localizedAccountById;
    }
}
