package info.kgeorgiy.ja.slastin.bank.server;

import info.kgeorgiy.ja.slastin.bank.common.Account;
import info.kgeorgiy.ja.slastin.bank.common.Bank;
import info.kgeorgiy.ja.slastin.bank.common.Person;
import info.kgeorgiy.ja.slastin.bank.server.accounts.RemoteAccount;
import info.kgeorgiy.ja.slastin.bank.server.persons.AbstractPerson;
import info.kgeorgiy.ja.slastin.bank.server.persons.LocalPerson;
import info.kgeorgiy.ja.slastin.bank.server.persons.RemotePerson;
import info.kgeorgiy.ja.slastin.bank.utils.LoggerFactory;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

import static info.kgeorgiy.ja.slastin.bank.server.persons.AbstractPerson.checkPersonData;
import static info.kgeorgiy.ja.slastin.bank.server.persons.AbstractPerson.getAccountId;

public class RemoteBank implements Bank {
    private final int port;
    private final ConcurrentMap<String, RemoteAccount> accountById = new ConcurrentHashMap<>();
    private final ConcurrentMap<Integer, RemotePerson> personByPassport = new ConcurrentHashMap<>();
    private final Logger logger;

    public RemoteBank(final int port) {
        this.port = port;
        logger = LoggerFactory.getBankLogger(getClass(), port);
    }

    @Override
    public Account createAccount(String name, String surname, int passport, int accountId) throws RemoteException {
        checkPersonData(name, surname, passport);
        if (!containsAccount(passport, accountId)) {
            String id = getAccountId(passport, accountId);
            if (containsAccount(id)) {
                logger.info("Bank already contains account with id " + id +
                        ", but it's not belong to the person. Please choose another accountId");
                return null;
            }
            RemoteAccount account = new RemoteAccount(id);
            UnicastRemoteObject.exportObject(account, port);
            accountById.put(id, account);
            Person person = createPerson(name, surname, passport);
            person.createAccount()
        }
        logger.info(String.format("Creating account with id %d for person with name:%s, surname:%s, passport:%d",
                accountId, name, surname, passport)
        );
        return getAccount(passport, accountId);
    }

    @Override
    public Account createAccount(String id) throws RemoteException {
        if (!accountById.containsKey(id)) {
            logger.info("Creating account with id " + id);
            RemoteAccount account = new RemoteAccount(id);
            UnicastRemoteObject.exportObject(account, port);
            accountById.put(id, account);
        } else {
            logger.info("Account with id " + id + " already exists");
        }
        return accountById.get(id);
    }

    @Override
    public boolean containsAccount(String id) {
        logger.info("Checks whether contains id " + id);
        return accountById.containsKey(id);
    }

    @Override
    public Account getAccount(int passport, int accountId) throws RemoteException {
        logger.info("Getting account with passport " + passport + " and accountId " + accountId);
        if (personByPassport.containsKey(passport)) {
            return personByPassport.get(passport).getAccount(accountId);
        }
        return null;
    }

    @Override
    public boolean containsAccount(int passport, int accountId) throws RemoteException {
        logger.info("Checks whether contains account with " + accountId + " which belongs to person with passport " + passport);
        return personByPassport.containsKey(passport) && (personByPassport.get(passport).getAccount(accountId) != null);
    }

    @Override
    public Account getAccount(String id) {
        logger.info("Getting account with id " + id);
        return accountById.get(id);
    }

    @Override
    public Person getLocalPerson(int passport) throws RemoteException {
        logger.info("Getting local person with passport " + passport);
        if (personByPassport.containsKey(passport)) {
            return new LocalPerson(personByPassport.get(passport));
        }
        return null;
    }

    @Override
    public Person getRemotePerson(int passport) {
        logger.info("Getting remote person with passport " + passport);
        return personByPassport.getOrDefault(passport, null);
    }

    @Override
    public Person createPerson(String name, String surname, int passport) throws RemoteException {
        if (!personByPassport.containsKey(passport)) {
            logger.info(String.format("Creating person with name:%s, surname:%s, passport:%d", name, surname, passport));
            RemotePerson person = new RemotePerson(name, surname, passport, this);
            UnicastRemoteObject.exportObject(person, port);
            personByPassport.put(passport, person);
        } else {
            logger.info("Person with passport " + passport + " already exists");
        }
        return personByPassport.get(passport);
    }

    @Override
    public boolean containsPerson(int passport) {
        logger.info("Checks whether contains person with passport " + passport);
        return personByPassport.containsKey(passport);
    }
}
