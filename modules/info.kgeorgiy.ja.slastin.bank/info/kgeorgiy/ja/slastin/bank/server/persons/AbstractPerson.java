package info.kgeorgiy.ja.slastin.bank.server.persons;

import info.kgeorgiy.ja.slastin.bank.common.Account;
import info.kgeorgiy.ja.slastin.bank.common.Person;
import info.kgeorgiy.ja.slastin.bank.utils.LoggerFactory;

import java.rmi.RemoteException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

public abstract class AbstractPerson implements Person {
    protected final String name, surname;
    protected final int passport;
    protected final ConcurrentMap<Integer, Account> accountById;
    protected final Logger logger;

    public AbstractPerson(final String name, final String surname, final int passport) {
        this(name, surname, passport, new ConcurrentHashMap<>());
    }

    protected AbstractPerson(final String name, final String surname, final int passport,
                             final ConcurrentMap<Integer, Account> accountById) {
        checkPersonData(name, surname, passport);
        this.name = name;
        this.surname = surname;
        this.passport = passport;
        this.accountById = accountById;
        logger = LoggerFactory.getPersonLogger(getClass(), name, surname, passport);
    }

    @Override
    public String getName() {
        logger.info("Getting name");
        return name;
    }

    @Override
    public String getSurname() {
        logger.info("Getting surname");
        return surname;
    }

    @Override
    public int getPassport() {
        logger.info("Getting passport");
        return passport;
    }

    @Override
    public Account getAccount(int accountId) throws RemoteException {
        logger.info("Getting account with id " + accountId);
        return accountById.get(accountId);
    }

    public static void checkPersonData(final String name, final String surname, final int passport) {
        if (name == null || surname == null || passport < 0) {
            throw new IllegalArgumentException("Person can not have null name or null surname or negative passport");
        }
    }

    public static String getAccountId(int passport, int accountId) {
        return passport + ":" + accountId;
    }
}
