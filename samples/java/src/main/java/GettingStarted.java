import org.hyperledger.indy.sdk.IndyException;
import org.hyperledger.indy.sdk.crypto.Crypto;
import org.hyperledger.indy.sdk.did.DidResults;
import org.hyperledger.indy.sdk.pool.Pool;
import org.hyperledger.indy.sdk.wallet.Wallet;
import org.json.JSONObject;
import utils.PoolUtils;

import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.hyperledger.indy.sdk.anoncreds.Anoncreds.issuerCreateAndStoreClaimDef;
import static org.hyperledger.indy.sdk.did.Did.createAndStoreMyDid;
import static org.hyperledger.indy.sdk.did.Did.keyForDid;
import static org.hyperledger.indy.sdk.ledger.Ledger.*;

public class GettingStarted {

    static void demo() throws Exception {
        System.out.println("Starting 'Getting started'");

        String poolName = PoolUtils.createPoolLedgerConfig();
        Pool pool = Pool.openPoolLedger(poolName, "{}").get();

        System.out.println("Getting Trust Anchor credentials for Faber, Acme and Thrift");

        System.out.println("Creating wallet 'Sovrin Steward'");

        String stewartWalletName = "sovrin_steward_wallet";

        Wallet.createWallet(poolName, stewartWalletName, "default", null, null).get();
        Wallet stewartWallet = Wallet.openWallet(stewartWalletName, null, null).get();

        DidResults.CreateAndStoreMyDidResult stewartDidAndVerkey = createAndStoreMyDid(stewartWallet, "{\"seed\": \"000000000000000000000000Steward1\"}").get();

        System.out.println("Getting Trust Anchor credentials - Government Onboarding");
        OnboardingResult governmentOnboarding = onboard(pool, poolName, "Sovrin Steward", stewartWallet, stewartDidAndVerkey.getDid(), "Government", null, "government_wallet", "TRUST_ANCHOR");

        System.out.println("Getting Trust Anchor credentials - Faber Onboarding");
        OnboardingResult faberOnboarding = onboard(pool, poolName, "Sovrin Steward", stewartWallet, stewartDidAndVerkey.getDid(), "Faber", null, "faber_wallet", "TRUST_ANCHOR");

        System.out.println("Getting Trust Anchor credentials - Acme Onboarding");
        OnboardingResult acmeOnboarding = onboard(pool, poolName, "Sovrin Steward", stewartWallet, stewartDidAndVerkey.getDid(), "Acme", null, "acme_wallet", "TRUST_ANCHOR");

        System.out.println("Getting Trust Anchor credentials - Thrift Onboarding");
        OnboardingResult thriftOnboarding = onboard(pool, poolName, "Sovrin Steward", stewartWallet, stewartDidAndVerkey.getDid(), "Thrift", null, "thrift_wallet", "TRUST_ANCHOR");


        System.out.println("Onboarding finished, proceeding to claim schema setup");

        DidResults.CreateAndStoreMyDidResult govDidAndVerkey = createAndStoreMyDid(governmentOnboarding.getToWallet(), "{\"seed\": \"000000000000000000000Government1\"}").get();


        sendSchema(pool, governmentOnboarding.getToWallet(), govDidAndVerkey.getDid(), createSchema("Transcript", "1.2",
                "first_name", "last_name", "degree", "status", "year", "average", "ssn")).get();

        sendSchema(pool, governmentOnboarding.getToWallet(), govDidAndVerkey.getDid(), createSchema("Job-Certificate", "0.2",
                "first_name", "last_name", "salary", "employee_status", "experience")).get();


        System.out.println("Proceeding to Faber publishing a Claim Definition");

        DidResults.CreateAndStoreMyDidResult faberDidAndVerkey = createAndStoreMyDid(faberOnboarding.getToWallet(), "{\"seed\": \"00000000000000000000000000Faber1\"}").get();

        Object schema = getSchema(pool, faberOnboarding.getToWallet(), faberDidAndVerkey.getDid(), govDidAndVerkey.getDid(), createGetSchema("Transcript", "1.2")).get();

        System.out.println("SCHEMA: " + schema);

        issuerCreateAndStoreClaimDef(faberOnboarding.getToWallet(), faberDidAndVerkey.getDid(), schema.toString(), "RC", false);
    }

    static OnboardingResult onboard(Pool pool, String poolName, String from,  Wallet fromWallet, String fromDid, String to,  Wallet toWallet, String toWalletName, String role) throws Exception {
        System.out.printf("\"%s\" -> Create and store in Wallet \"%s %s\"\n", from, from, to);
        DidResults.CreateAndStoreMyDidResult fromToDidAndKey = createAndStoreMyDid(fromWallet, "{}").get();

        Map<String, String> connectionRequest = new HashMap<>();
        connectionRequest.put("did", fromToDidAndKey.getDid());
        connectionRequest.put("nonce", Long.toString(System.currentTimeMillis()));

        System.out.printf("\"%s\" -> Send Nym to Ledger for \"%s %s\"", from, from, to);
        sendNym(pool, fromWallet, fromDid, fromToDidAndKey.getDid(), fromToDidAndKey.getVerkey(), null).get();

        if (toWallet == null) {
            System.out.printf("\"%s\" -> Creating wallet", to);
            Wallet.createWallet(poolName, toWalletName, "default", null, null).get();
            toWallet = Wallet.openWallet(toWalletName, null, null).get();
        }

        System.out.printf("\"%s\" -> Create and store in Wallet \"%s %s\"\n", to, to, from);
        DidResults.CreateAndStoreMyDidResult toFromDidAndKey = createAndStoreMyDid(toWallet, "{}").get();

        System.out.printf("\"%s\" -> Get key for did from \"%s\" connection request", to, from);
        String fromToVerkey = keyForDid(pool, toWallet, connectionRequest.get("did")).get();

        System.out.printf("\"%s\" -> Anoncrypt connection response for \"%s\" with \"%s %s\" DID, verkey and nonce", to, from, to, from);

        Map<String, String> connectionResponse = new HashMap<>();

        connectionResponse.put("did", toFromDidAndKey.getDid());
        connectionResponse.put("verkey", toFromDidAndKey.getVerkey());
        connectionResponse.put("nonce", connectionRequest.get("nonce"));

        System.out.printf("\"%s\" -> Send anoncrypted connection response to \"%s\"", to, from);
        byte[] anonCryptedConnectionResponse = Crypto.anonCrypt(fromToVerkey, new JSONObject(connectionResponse).toString().getBytes(Charset.forName("utf8"))).get();

        System.out.printf("\"%s\" -> Anoncrypted connection response from \"%s\"", from, to);

        Map<String, Object> receivedConnectionResponse = new JSONObject(new String(Crypto.anonDecrypt(fromWallet, fromToVerkey, anonCryptedConnectionResponse).get(), Charset.forName("utf8"))).toMap();

        System.out.printf("\"%s\" -> Send Nym to Ledger for \"%s %s\" DID with %s Role", from, to, from, role);

        sendNym(pool, fromWallet, fromDid, receivedConnectionResponse.get("did").toString(), receivedConnectionResponse.get("verkey").toString(), role).get();

        return new OnboardingResult(toWallet, toWalletName, toFromDidAndKey, receivedConnectionResponse);
    }

    static CompletableFuture<String> sendNym(Pool pool, Wallet wallet, String did, String newDid, String newKey, String role) throws Exception {
        String nymRequest = buildNymRequest(did, newDid, newKey, null, role).get();
        return signAndSubmitRequest(pool, wallet, did, nymRequest);
    }

    static String createSchema(String name, String version, String... attributeNames) {
        Map<String, Object> json = new HashMap<>();

        json.put("name", name);
        json.put("version", version);
        json.put("attr_names", Arrays.asList(attributeNames));

        return new JSONObject(json).toString();
    }

    static CompletableFuture<String> sendSchema(Pool pool, Wallet wallet, String did, String schema) throws IndyException, ExecutionException, InterruptedException {
        String schemaRequest = buildSchemaRequest(did, schema).get();
        return signAndSubmitRequest(pool, wallet, did, schemaRequest);
    }

    static String createGetSchema(String name, String version) {
        Map<String, Object> json = new HashMap<>();

        json.put("name", name);
        json.put("version", version);

        return new JSONObject(json).toString();
    }

    static CompletableFuture<Object> getSchema(Pool pool, Wallet wallet, String submitterdDid, String destinationDid, String request) throws Exception {
        String getSchemaRequest = buildGetSchemaRequest(submitterdDid, destinationDid, request).get();
        return signAndSubmitRequest(pool, wallet, submitterdDid, getSchemaRequest).thenApply(rawJson -> new JSONObject(rawJson).get("result"));
    }

    static class OnboardingResult {
        Wallet toWallet;
        String toWalletName;
        DidResults.CreateAndStoreMyDidResult fromToDidAndKey;
        Map<String, Object> decryptedConnectionResponse;

        public OnboardingResult(Wallet toWallet, String toWalletName, DidResults.CreateAndStoreMyDidResult fromToDidAndKey, Map<String, Object> decryptedConnectionResponse) {
            this.toWallet = toWallet;
            this.toWalletName = toWalletName;
            this.fromToDidAndKey = fromToDidAndKey;
            this.decryptedConnectionResponse = decryptedConnectionResponse;
        }

        public Wallet getToWallet() {
            return toWallet;
        }

        public String getToWalletName() {
            return toWalletName;
        }

        public DidResults.CreateAndStoreMyDidResult getFromToDidAndKey() {
            return fromToDidAndKey;
        }

        public Map<String, Object> getDecryptedConnectionResponse() {
            return decryptedConnectionResponse;
        }

        @Override
        public String toString() {
            return "OnboardingResult{" +
                    "toWallet=" + toWallet +
                    ", toWalletName='" + toWalletName + '\'' +
                    ", fromToDidAndKey=" + fromToDidAndKey +
                    ", decryptedConnectionResponse=" + decryptedConnectionResponse +
                    '}';
        }
    }
}
