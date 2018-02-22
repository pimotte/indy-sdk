package nl.quintor.studybits.indy.wrapper;

import org.hyperledger.indy.sdk.IndyException;

import java.util.concurrent.CompletableFuture;

import static nl.quintor.studybits.indy.wrapper.util.AsyncUtil.wrapException;
import static org.hyperledger.indy.sdk.did.Did.createAndStoreMyDid;
import static org.hyperledger.indy.sdk.ledger.Ledger.buildNymRequest;

public class TrustAnchor extends WalletOwner {
    public TrustAnchor(String name, IndyPool pool, IndyWallet wallet) {
        super(name, pool, wallet);
    }

    public CompletableFuture<String> createConnectionRequest(String newcomerName, String role) throws IndyException {
        System.out.printf("\"%s\" -> Create and store in Wallet \"%s %s\"\n", name, name, newcomerName);
        return createAndStoreMyDid(wallet.getWallet(), "{}")
                .thenCompose(wrapException(
                        (didResult) ->
                                sendNym(didResult.getDid(), didResult.getVerkey(), role)
                        )
                );
    }

    private CompletableFuture<String> sendNym(String newDid, String newKey, String role) throws Exception {
        return buildNymRequest(wallet.getMainDid(), newDid, newKey, null, role)
                .thenCompose(wrapException(this::signAndSubmitRequest));
    }
}
