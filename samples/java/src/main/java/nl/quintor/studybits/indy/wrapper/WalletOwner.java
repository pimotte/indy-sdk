package nl.quintor.studybits.indy.wrapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import nl.quintor.studybits.indy.wrapper.dto.AnonCryptable;
import nl.quintor.studybits.indy.wrapper.dto.ConnectionRequest;
import nl.quintor.studybits.indy.wrapper.dto.ConnectionResponse;
import org.hyperledger.indy.sdk.IndyException;
import org.hyperledger.indy.sdk.crypto.Crypto;
import org.hyperledger.indy.sdk.did.Did;
import org.hyperledger.indy.sdk.ledger.Ledger;

import java.nio.charset.Charset;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static nl.quintor.studybits.indy.wrapper.util.AsyncUtil.wrapException;

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

    public CompletableFuture<byte[]> acceptConnectionRequest(ConnectionRequest connectionRequest) throws JsonProcessingException, IndyException, ExecutionException, InterruptedException {
        return anoncrypt(wallet.newDid("{}")
                .thenCombineAsync(Did.keyForDid(pool.getPool(), wallet.getWallet(), connectionRequest.getDid()),
                        (myDid, theirKey) -> new ConnectionResponse(myDid.getDid(), myDid.getVerkey(), connectionRequest.getNonce(), theirKey)));
    }

    protected CompletableFuture<byte[]> anoncrypt(CompletableFuture<? extends AnonCryptable> messageFuture) throws JsonProcessingException, IndyException {
        return messageFuture.thenCompose(wrapException(
                (message) -> Crypto.anonCrypt(message.getTheirKey(), message.toJSON().getBytes(Charset.forName("utf8")))));
    }
}
