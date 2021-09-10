package info.kgeorgiy.ja.slastin.bank.common;

import info.kgeorgiy.ja.slastin.bank.server.persons.LocalPerson;
import info.kgeorgiy.ja.slastin.bank.server.persons.RemotePerson;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Bank extends Remote {
    Account createAccount(String name, String surname, int passport, int accountId) throws RemoteException;

    Account createAccount(String id) throws RemoteException;

    Account getAccount(int passport, int accountId) throws RemoteException;

    boolean containsAccount(int passport, int accountId) throws RemoteException;

    Account getAccount(String id) throws RemoteException;

    boolean containsAccount(String id) throws RemoteException;

    Person getLocalPerson(int passport) throws RemoteException;

    Person getRemotePerson(int passport) throws RemoteException;

    Person createPerson(String name, String surname, int passport) throws RemoteException;

    boolean containsPerson(int passport) throws RemoteException;
}
