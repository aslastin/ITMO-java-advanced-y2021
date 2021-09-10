package info.kgeorgiy.ja.slastin.bank.utils;

import info.kgeorgiy.ja.slastin.bank.common.Account;
import info.kgeorgiy.ja.slastin.bank.common.Bank;
import info.kgeorgiy.ja.slastin.bank.common.Person;

import java.util.logging.Logger;

public class LoggerFactory {
    public static <T extends Account> Logger getAccountLogger(Class<T> clazz, String id) {
        return Logger.getLogger(String.format("%s - id: %s -  ", clazz.getSimpleName(), id));
    }

    public static <T extends Person> Logger getPersonLogger(Class<T> clazz, String name, String surname, int passport) {
        return Logger.getLogger(String.format("%s - name: %s, surname: %s, passport: %d - ",
                clazz.getSimpleName(), name, surname, passport)
        );
    }

    public static <T extends Bank> Logger getBankLogger(Class<T> clazz, int port) {
        return Logger.getLogger(String.format("%s - port: %d - ", clazz.getSimpleName(), port));
    }
}
