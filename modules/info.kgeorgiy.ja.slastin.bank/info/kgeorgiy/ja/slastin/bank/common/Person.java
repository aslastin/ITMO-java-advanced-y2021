package info.kgeorgiy.ja.slastin.bank.common;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.concurrent.ConcurrentMap;

public interface Person extends Remote {
    String getName() throws RemoteException;

    String getSurname() throws RemoteException;

    int getPassport() throws RemoteException;

    Account createAccount(int accountId) throws RemoteException;

    Account getAccount(int accountId) throws RemoteException;
}
