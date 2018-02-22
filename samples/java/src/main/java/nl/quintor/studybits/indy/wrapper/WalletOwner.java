package nl.quintor.studybits.indy.wrapper;

import org.hyperledger.indy.sdk.IndyException;
import org.hyperledger.indy.sdk.ledger.Ledger;

import java.util.concurrent.CompletableFuture;

public class WalletOwner {
    protected IndyPool pool;
    protected IndyWallet wallet;
    protected String name;

    public WalletOwner(String name, IndyPool pool, IndyWallet wallet) {
        this.name = name;
        this.pool = pool;
        this.wallet = wallet;
    }

    protected CompletableFuture<String> signAndSubmitRequest(String request) throws IndyException {
        return Ledger.signAndSubmitRequest(pool.getPool(), wallet.getWallet(), wallet.getMainDid(), request);
    }
}
