package nl.quintor.studybits.indy.wrapper;

import lombok.extern.slf4j.Slf4j;
import nl.quintor.studybits.indy.wrapper.dto.AnonCryptable;
import nl.quintor.studybits.indy.wrapper.dto.ConnectionRequest;
import nl.quintor.studybits.indy.wrapper.dto.ConnectionResponse;
import nl.quintor.studybits.indy.wrapper.dto.TheirDidInfo;
import nl.quintor.studybits.indy.wrapper.util.JSONUtil;
import org.hyperledger.indy.sdk.IndyException;
import org.hyperledger.indy.sdk.crypto.Crypto;
import org.hyperledger.indy.sdk.did.Did;
import org.hyperledger.indy.sdk.pairwise.Pairwise;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

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

    public CompletableFuture<Void> acceptConnectionResponse(byte[] encryptedConnectionResponse, String nonce) throws IndyException {
        return anonDecrypt(encryptedConnectionResponse, nonce, ConnectionResponse.class)
                .thenCompose(wrapException(connectionResponse -> {
                    log.debug("Accepting connection response: {}", connectionResponse);
                    return acceptConnectionResponse(connectionResponse);
                }));
    }

    private CompletableFuture<Void> acceptConnectionResponse(ConnectionResponse connectionResponse) throws IndyException {
        if (!openConnectionRequests.containsKey(connectionResponse.getNonce())) {
            log.info("No open connection request for nonce {}", connectionResponse.getNonce());
            return CompletableFuture.completedFuture(null);
        }

        ConnectionRequest connectionRequest = openConnectionRequests.get(connectionResponse.getNonce());

        return sendNym(connectionResponse.getDid(), connectionResponse.getVerkey(), connectionRequest.getRole())
                .thenCompose(wrapException((nymResponse) -> {
                    log.debug("Storing theirDid: {}", connectionResponse.getDid());
                    return Did.storeTheirDid(wallet.getWallet(), new TheirDidInfo(connectionResponse.getDid()).toJSON());
                }))
                .thenCompose(wrapException((nymResponse) -> {
                            log.debug("Creating pairwise theirDid: {}, myDid: {}, metadata: {}", connectionResponse.getDid(), connectionRequest.getDid(), connectionRequest.getNewcomerName());
                            return Pairwise.createPairwise(wallet.getWallet(), connectionResponse.getDid(), connectionRequest.getDid(), connectionRequest.getNewcomerName());
                        }
                ));
    }

    private CompletableFuture<String> sendNym(String newDid, String newKey, String role) throws IndyException {
        log.debug("Called sendNym with newDid: {}, newKey {}, role {}", newDid, newKey, role);
        return buildNymRequest(wallet.getMainDid(), newDid, newKey, null, role)
                .thenCompose(wrapException(this::signAndSubmitRequest));
    }

    private <T extends AnonCryptable> CompletableFuture<T> anonDecrypt(byte[] message, String nonce, Class<T> valueType) throws IndyException {
        return Crypto.anonDecrypt(wallet.getWallet(), openConnectionRequests.get(nonce).getVerkey(), message)
                .thenApply(wrapException((decryptedMessage) -> JSONUtil.mapper.readValue(new String(decryptedMessage, Charset.forName("utf8")), valueType)));
    }
}
