package info.kgeorgiy.ja.slastin.bank.server.accounts;

public class RemoteAccount extends AbstractAccount {
    public RemoteAccount(final String id) {
        super(id);
    }

    public RemoteAccount(final String id, final int amount) {
        super(id, amount);
    }
}
