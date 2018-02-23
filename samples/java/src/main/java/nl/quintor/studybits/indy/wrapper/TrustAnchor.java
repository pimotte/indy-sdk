package nl.quintor.studybits.indy.wrapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import nl.quintor.studybits.indy.wrapper.dto.*;
import nl.quintor.studybits.indy.wrapper.exception.IndyWrapperException;
import nl.quintor.studybits.indy.wrapper.util.JSONUtil;
import org.hyperledger.indy.sdk.IndyException;
import org.hyperledger.indy.sdk.crypto.Crypto;
import org.hyperledger.indy.sdk.did.Did;
import org.hyperledger.indy.sdk.pairwise.Pairwise;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static nl.quintor.studybits.indy.wrapper.util.AsyncUtil.wrapException;
import static org.hyperledger.indy.sdk.did.Did.createAndStoreMyDid;
import static org.hyperledger.indy.sdk.ledger.Ledger.buildNymRequest;

@Slf4j
public class TrustAnchor extends WalletOwner {
    private Map<String, ConnectionRequest> openConnectionRequests = new HashMap<>();
    public TrustAnchor(String name, IndyPool pool, IndyWallet wallet) {
        super(name, pool, wallet);
    }

    public CompletableFuture<ConnectionRequest> createConnectionRequest(String newcomerName, String role) throws IndyException {
        log.info("'{}' -> Create and store in Wallet '{} {}'", name, name, newcomerName);
        return createAndStoreMyDid(wallet.getWallet(), "{}")
                .thenCompose(wrapException(
                        (didResult) ->
                                sendNym(didResult.getDid(), didResult.getVerkey(), role)
                                        .thenApply(
                                        // TODO: Generate nonce properly
                                        (nymResponse) -> {
                                            ConnectionRequest connectionRequest = new ConnectionRequest(didResult.getDid(), Long.toString(System.currentTimeMillis()), role, newcomerName, didResult.getVerkey());
                                            log.debug("Returning ConnectionRequest: {}", connectionRequest);
                                            openConnectionRequests.put(connectionRequest.getNonce(), connectionRequest);
                                            return connectionRequest;
                                        }
                                )
                        )
                );
    }

    public CompletableFuture<EncryptedMessage> registerVerinym(String targetDid) throws IndyException, JsonProcessingException {
        return authcrypt(Pairwise.getPairwise(wallet.getWallet(), targetDid)
                .thenApply(wrapException((String getPairwiseResult) ->
                        JSONUtil.mapper.readValue(getPairwiseResult, GetPairwiseResult.class)
                ))
                .thenCompose(wrapException((GetPairwiseResult getPairwiseResult) ->
                        Did.keyForDid(pool.getPool(), wallet.getWallet(), getPairwiseResult.getMyDid())
                                .thenApply(wrapException((myKey) ->
                                        new Verinym(getPairwiseResult.getMyDid(), myKey, getPairwiseResult.getParsedMetadata().getMyKey(),
                                                getPairwiseResult.getParsedMetadata().getTheirKey())
                                )))));
    }

//    public CompletableFuture<Void> acceptVerinym(byte[] verinym) {
//
//    }

    public CompletableFuture<Void> acceptConnectionResponse(EncryptedMessage encryptedConnectionResponse) throws IndyException {
        return anonDecrypt(encryptedConnectionResponse, ConnectionResponse.class)
                .thenCompose(wrapException(connectionResponse -> {
                    log.debug("Accepting connection response: {}", connectionResponse);
                    return acceptConnectionResponse(connectionResponse);
                }));
    }

    private CompletableFuture<Void> acceptConnectionResponse(ConnectionResponse connectionResponse) throws IndyException {
        if (!openConnectionRequests.containsKey(connectionResponse.getNonce())) {
            log.info("No open connection request for nonce {}", connectionResponse.getNonce());
            throw new IndyWrapperException("Nonce not found");
        }

        ConnectionRequest connectionRequest = openConnectionRequests.get(connectionResponse.getNonce());

        return sendNym(connectionResponse.getDid(), connectionResponse.getVerkey(), connectionRequest.getRole())
                .thenCompose(wrapException((nymResponse) ->
                        storeDidAndPairwise(connectionRequest.getDid(), connectionResponse.getDid(), connectionRequest.getVerkey(), connectionResponse.getVerkey())))
                .thenApply((void_) -> {
                    log.debug("Removing connectionRequest with nonce {}", connectionRequest.getNonce());
                    openConnectionRequests.remove(connectionRequest.getNonce());
                    return void_;
                });
    }

    private CompletableFuture<String> sendNym(String newDid, String newKey, String role) throws IndyException {
        log.debug("Called sendNym with newDid: {}, newKey {}, role {}", newDid, newKey, role);
        return buildNymRequest(wallet.getMainDid(), newDid, newKey, null, role)
                .thenCompose(wrapException(this::signAndSubmitRequest));
    }
}
