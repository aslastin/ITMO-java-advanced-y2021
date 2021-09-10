package info.kgeorgiy.ja.slastin.bank.server.persons;

import info.kgeorgiy.ja.slastin.bank.common.Account;
import info.kgeorgiy.ja.slastin.bank.common.Bank;

import java.rmi.RemoteException;

public class RemotePerson extends AbstractPerson {
    private final Bank bank;

    public RemotePerson(final String name, final String surname, final int passportNumber, final Bank bank) {
        super(name, surname, passportNumber);
        this.bank = bank;
    }

    @Override
    public Account createAccount(int accountId) throws RemoteException {
        if (!accountById.containsKey(accountId)) {
            String id = getAccountId(passport, accountId);
            if (bank.containsAccount(id)) {

            } else {
                logger.info("Creating account with id " + accountId);
                bank.createAccount(name, surname, passport, accountId);
            }
        } else {
            logger.info("Account with id " + accountId + " already exists");
        }
        return accountById.get(accountId);
    }
}
