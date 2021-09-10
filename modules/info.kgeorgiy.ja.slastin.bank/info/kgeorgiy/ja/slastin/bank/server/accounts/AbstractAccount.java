package info.kgeorgiy.ja.slastin.bank.server.accounts;

import info.kgeorgiy.ja.slastin.bank.common.Account;
import info.kgeorgiy.ja.slastin.bank.utils.LoggerFactory;

import java.util.logging.Logger;

public abstract class AbstractAccount implements Account {
    private final String id;
    private int amount;
    private final Logger logger;

    public AbstractAccount(final String id) {
        this(id, 0);
    }

    public AbstractAccount(final String id, final int amount) {
        if (id == null) {
            throw new IllegalArgumentException("Account id can not be null");
        }
        this.id = id;
        this.amount = amount;
        logger = LoggerFactory.getAccountLogger(getClass(), id);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public synchronized int getAmount() {
        logger.info("Getting amount of money");
        return amount;
    }

    @Override
    public synchronized void setAmount(final int amount) {
        logger.info("Setting amount of money: " + amount);
        this.amount = amount;
    }

    @Override
    public synchronized void addAmount(final int amount) {
        logger.info("Add amount of money: " + amount);
        this.amount += amount;
    }
}
