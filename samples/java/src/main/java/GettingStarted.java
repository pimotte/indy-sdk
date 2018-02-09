import org.hyperledger.indy.sdk.IndyException;
import org.hyperledger.indy.sdk.crypto.Crypto;
import org.hyperledger.indy.sdk.crypto.CryptoResults;
import org.hyperledger.indy.sdk.did.DidResults;
import org.hyperledger.indy.sdk.pool.Pool;
import org.hyperledger.indy.sdk.wallet.Wallet;
import org.json.JSONObject;
import utils.PoolUtils;

import java.nio.charset.Charset;
import java.sql.Time;
import java.time.Instant;
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
        OnboardingResult governmentOnboarding = onboard(pool, poolName, "Sovrin Steward", stewartWallet, stewartDidAndVerkey.getDid(), "Government", null, "government_wallet");

        System.out.println("Getting Trust Anchor credentials - Government Verinym");
        String governmentDid = getVerinym(pool, "Sovrin Steward", stewartWallet, stewartDidAndVerkey.getDid(), governmentOnboarding.getFromToKey(), "Government", 
                governmentOnboarding.getToWallet(), governmentOnboarding.getToFromDidAndKey(), "TRUST_ANCHOR");

        System.out.println("Getting Trust Anchor credentials - Faber Onboarding");
        OnboardingResult faberOnboarding = onboard(pool, poolName, "Sovrin Steward", stewartWallet, stewartDidAndVerkey.getDid(), "Faber", null, "faber_wallet");

        System.out.println("Getting Trust Anchor credentials - Faber Verinym");
        String faberDid = getVerinym(pool, "Sovrin Steward", stewartWallet, stewartDidAndVerkey.getDid(), faberOnboarding.getFromToKey(), "Faber",
                faberOnboarding.getToWallet(), faberOnboarding.getToFromDidAndKey(), "TRUST_ANCHOR");

        System.out.println("Getting Trust Anchor credentials - Acme Onboarding");
        OnboardingResult acmeOnboarding = onboard(pool, poolName, "Sovrin Steward", stewartWallet, stewartDidAndVerkey.getDid(), "Acme", null, "acme_wallet");

        System.out.println("Getting Trust Anchor credentials - Acme Verinym");
        String acmeDid = getVerinym(pool, "Sovrin Steward", stewartWallet, stewartDidAndVerkey.getDid(), acmeOnboarding.getFromToKey(), "Acme",
                acmeOnboarding.getToWallet(), acmeOnboarding.getToFromDidAndKey(), "TRUST_ANCHOR");
        
        System.out.println("Getting Trust Anchor credentials - Thrift Onboarding");
        OnboardingResult thriftOnboarding = onboard(pool, poolName, "Sovrin Steward", stewartWallet, stewartDidAndVerkey.getDid(), "Thrift", null, "thrift_wallet");

        System.out.println("Getting Trust Anchor credentials - Thrift Verinym");
        String thriftDid = getVerinym(pool, "Sovrin Steward", stewartWallet, stewartDidAndVerkey.getDid(), thriftOnboarding.getFromToKey(), "Thrift",
                thriftOnboarding.getToWallet(), thriftOnboarding.getToFromDidAndKey(), "TRUST_ANCHOR");

        System.out.println("Onboarding finished, proceeding to claim schema setup");

        System.out.println("'Government' -> Create and store in Wallet 'Government Issuer' DID");
        DidResults.CreateAndStoreMyDidResult governmentIssuer = createAndStoreMyDid(governmentOnboarding.getToWallet(), "{}").get();

        System.out.println("'Government' -> Send Nym to Ledger for 'Government Issuer' DID");
        sendNym(pool, governmentOnboarding.getToWallet(), governmentDid, governmentIssuer.getDid(), governmentIssuer.getVerkey(), null).get();

        System.out.println("'Government' -> Send to Ledger 'Transcript' schema");
        sendSchema(pool, governmentOnboarding.getToWallet(), governmentIssuer.getDid(), createSchema("Transcript", "1.2",
                "first_name", "last_name", "degree", "status", "year", "average", "ssn")).get();

        System.out.println("'Government' -> Send to Ledger 'Job-Certificate' schema");
        sendSchema(pool, governmentOnboarding.getToWallet(), governmentIssuer.getDid(), createSchema("Job-Certificate", "0.2",
                "first_name", "last_name", "salary", "employee_status", "experience")).get();


        System.out.println("Allow schema's to settle");
        Thread.sleep(3000);
        Instant schemaCreation = Instant.now();
        Thread.sleep(1000);

        System.out.println("'Faber' -> Create and store in Wallet 'Faber Issuer' DID");
        DidResults.CreateAndStoreMyDidResult faberIssuer = createAndStoreMyDid(faberOnboarding.getToWallet(), "{}").get();

        System.out.println("'Faber' -> Send Nym to Ledger for 'Faber Issuer' DID");
        sendNym(pool, faberOnboarding.getToWallet(), faberDid, faberIssuer.getDid(), faberIssuer.getVerkey(), null).get();

        System.out.println("\"Faber\" -> Get \"Transcript\" Schema from Ledger");

        Object transcriptSchema = getSchema(pool, faberOnboarding.getToWallet(), faberIssuer.getDid(), faberIssuer.getDid(), createGetSchema("Transcript", "1.2")).get();

        Instant stateProofTime =Instant.ofEpochSecond(((JSONObject) transcriptSchema).getJSONObject("state_proof").getJSONObject("multi_signature").getJSONObject("value").getInt("timestamp"));

        System.out.println("STATE_PROOF: " + stateProofTime);

        System.out.println(stateProofTime.isAfter(schemaCreation));

        System.out.println("\"Faber\" -> Create and store in Wallet \"Faber Transcript\" Claim Definition");
        createAndSendClaimDef(pool, faberOnboarding.getToWallet(), faberIssuer, transcriptSchema.toString());


        System.out.println("'Acme' -> Create and store in Wallet 'Acme Issuer' DID");
        DidResults.CreateAndStoreMyDidResult acmeIssuer = createAndStoreMyDid(acmeOnboarding.getToWallet(), "{}").get();

        System.out.println("'Acme' -> Send Nym to Ledger for 'Acme Issuer' DID");
        sendNym(pool, acmeOnboarding.getToWallet(), acmeDid, acmeIssuer.getDid(), acmeIssuer.getVerkey(), null).get();

        System.out.println("\"Acme\" -> Get \"Job-Certificate\" Schema from Ledger");

        Object jobCertSchema = getSchema(pool, acmeOnboarding.getToWallet(), acmeIssuer.getDid(), acmeIssuer.getDid(), createGetSchema("Job-Certificate", "0.2")).get();

        System.out.println("\"Acme\" -> Create and store in Wallet \"Acme Job-Certificate\" Claim Definition");
        createAndSendClaimDef(pool, acmeOnboarding.getToWallet(), acmeIssuer, jobCertSchema.toString());


        System.out.println("Getting Transcript with Faber");
        System.out.println("Getting Transcript with Faber - Onboarding");
        OnboardingResult aliceOnboarding = onboard(pool, poolName, "Faber", faberOnboarding.getToWallet(), faberDid, "Alice", null, "alice_wallet");

    }


    private static JSONObject getClaimOffer(String issuerDid, String name, String version) {
        JSONObject claimOffer = new JSONObject();
        claimOffer.put("issuer_did", issuerDid);

        Map<String, String> schemaKey = new HashMap<>();
        schemaKey.put("did", issuerDid);
        schemaKey.put("name", name);
        schemaKey.put("version", version);

        claimOffer.put("schema_key", schemaKey);
        return claimOffer;
    }

    private static void createAndSendClaimDef(Pool pool, Wallet wallet, DidResults.CreateAndStoreMyDidResult didAndKey, String schema) throws InterruptedException, ExecutionException, IndyException {
        String claimDef = issuerCreateAndStoreClaimDef(wallet, didAndKey.getDid(), schema, "CL", false).get();
        JSONObject claimDefJson = new JSONObject(claimDef);

        String claimDefTxn = buildClaimDefTxn(didAndKey.getDid(), claimDefJson.getInt("ref"), claimDefJson.getString("signature_type"), claimDefJson.get("data").toString()).get();

        signAndSubmitRequest(pool, wallet, didAndKey.getDid(), claimDefTxn).get();
    }

    static OnboardingResult onboard(Pool pool, String poolName, String from,  Wallet fromWallet, String fromDid, String to,  Wallet toWallet, String toWalletName
    ) throws Exception {
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

        Map<String, String> connectionResponse = createConnectionResponse(connectionRequest, toFromDidAndKey);

        System.out.printf("\"%s\" -> Send anoncrypted connection response to \"%s\"", to, from);
        byte[] anonCryptedConnectionResponse = Crypto.anonCrypt(fromToVerkey, new JSONObject(connectionResponse).toString().getBytes(Charset.forName("utf8"))).get();

        System.out.printf("\"%s\" -> Anoncrypted connection response from \"%s\"", from, to);

        Map<String, Object> receivedConnectionResponse = new JSONObject(new String(Crypto.anonDecrypt(fromWallet, fromToVerkey, anonCryptedConnectionResponse).get(), Charset.forName("utf8"))).toMap();

        System.out.printf("\"%s\" -> Authenticates \"%s\" by comparison of nonce", from, to);
        assert receivedConnectionResponse.get("nonce").equals(connectionResponse.get("nonce"));

        System.out.printf("\"%s\" -> Send Nym to Ledger for \"%s %s\" DID", from, to, from);
        sendNym(pool, fromWallet, fromDid, toFromDidAndKey.getDid(), toFromDidAndKey.getVerkey(), null);

        return new OnboardingResult(toWallet, toWalletName, fromToDidAndKey.getVerkey(), toFromDidAndKey, receivedConnectionResponse);
    }

    private static Map<String, String> createConnectionResponse(Map<String, String> connectionRequest, DidResults.CreateAndStoreMyDidResult toFromDidAndKey) {
        Map<String, String> connectionResponse = new HashMap<>();

        connectionResponse.put("did", toFromDidAndKey.getDid());
        connectionResponse.put("verkey", toFromDidAndKey.getVerkey());
        connectionResponse.put("nonce", connectionRequest.get("nonce"));
        return connectionResponse;
    }

    static String getVerinym(Pool pool, String from, Wallet fromWallet, String fromDid, String fromToKey, String to, Wallet toWallet, DidResults.CreateAndStoreMyDidResult toFromDidAndKey, String role) throws Exception {
        System.out.printf("'%s' -> Create and store new DID in Wallet '%s'", to, to);
        DidResults.CreateAndStoreMyDidResult toDidAndKey = createAndStoreMyDid(toWallet, "{}").get();

        JSONObject didInfoJson = new JSONObject();
        didInfoJson.put("did", toDidAndKey.getDid());
        didInfoJson.put("verkey", toDidAndKey.getVerkey());

        System.out.printf("'%s' -> Authcrypt '%s DID info' for '%s'", from, to, to);
        byte[] authCryptedDidInfoJson = Crypto.authCrypt(toWallet, toFromDidAndKey.getVerkey(), fromToKey, didInfoJson.toString().getBytes(Charset.forName("utf8"))).get();

        System.out.printf("'%s' -> Send authcrypted '%s DID info' to %s", to, to, from);

        System.out.printf("'%s' -> Authdecrypted '%s DID info' from %s", from, to, to);
        CryptoResults.AuthDecryptResult authDecryptResult = Crypto.authDecrypt(fromWallet, fromToKey, authCryptedDidInfoJson).get();

        System.out.printf("'%s' -> Authenticate %s by comparison of Verkeys", from, to);
        assert authDecryptResult.getVerkey().equals(keyForDid(pool, fromWallet, toFromDidAndKey.getDid()).get());

        JSONObject authDecryptedDidInfo = new JSONObject(new String(authDecryptResult.getDecryptedMessage(), Charset.forName("utf8")));

        System.out.printf("'%s' -> Send Nym to Ledger for '%s DID' with %s Role", from, to, role);
        sendNym(pool, fromWallet, fromDid, authDecryptedDidInfo.getString("did"), authDecryptedDidInfo.getString("verkey"), role).get();

        return toDidAndKey.getDid();
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

        System.out.println("SCHEMA_REQUEST: " + schemaRequest);
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
        String fromToKey;
        DidResults.CreateAndStoreMyDidResult toFromDidAndKey;
        Map<String, Object> decryptedConnectionResponse;

        public OnboardingResult(Wallet toWallet, String toWalletName, String fromToKey, DidResults.CreateAndStoreMyDidResult toFromDidAndKey, Map<String, Object> decryptedConnectionResponse) {
            this.toWallet = toWallet;
            this.toWalletName = toWalletName;
            this.fromToKey = fromToKey;
            this.toFromDidAndKey = toFromDidAndKey;
            this.decryptedConnectionResponse = decryptedConnectionResponse;
        }

        public Wallet getToWallet() {
            return toWallet;
        }

        public String getToWalletName() {
            return toWalletName;
        }

        public String getFromToKey() {
            return fromToKey;
        }

        public DidResults.CreateAndStoreMyDidResult getToFromDidAndKey() {
            return toFromDidAndKey;
        }

        public Map<String, Object> getDecryptedConnectionResponse() {
            return decryptedConnectionResponse;
        }

        @Override
        public String toString() {
            return "OnboardingResult{" +
                    "toWallet=" + toWallet +
                    ", toWalletName='" + toWalletName + '\'' +
                    ", fromToKey='" + fromToKey + '\'' +
                    ", toFromDidAndKey=" + toFromDidAndKey +
                    ", decryptedConnectionResponse=" + decryptedConnectionResponse +
                    '}';
        }
    }
}
