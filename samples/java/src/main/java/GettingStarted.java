import org.hyperledger.indy.sdk.IndyException;
import org.hyperledger.indy.sdk.crypto.Crypto;
import org.hyperledger.indy.sdk.crypto.CryptoResults;
import org.hyperledger.indy.sdk.did.Did;
import org.hyperledger.indy.sdk.did.DidResults;
import org.hyperledger.indy.sdk.pool.Pool;
import org.hyperledger.indy.sdk.wallet.Wallet;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import utils.PoolUtils;

import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.hyperledger.indy.sdk.anoncreds.Anoncreds.*;
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


        System.out.println("'Faber' -> Create and store in Wallet 'Faber Issuer' DID");
        DidResults.CreateAndStoreMyDidResult faberIssuer = createAndStoreMyDid(faberOnboarding.getToWallet(), "{}").get();

        System.out.println("'Faber' -> Send Nym to Ledger for 'Faber Issuer' DID");
        sendNym(pool, faberOnboarding.getToWallet(), faberDid, faberIssuer.getDid(), faberIssuer.getVerkey(), null).get();

        System.out.println("\"Faber\" -> Get \"Transcript\" Schema from Ledger");

        Object transcriptSchema = getSchema(pool, faberIssuer.getDid(), governmentIssuer.getDid(), createGetSchema("Transcript", "1.2")).get();

        System.out.println("\"Faber\" -> Create and store in Wallet \"Faber Transcript\" Claim Definition");
        createAndSendClaimDef(pool, faberOnboarding.getToWallet(), faberIssuer, transcriptSchema.toString());


        System.out.println("'Acme' -> Create and store in Wallet 'Acme Issuer' DID");
        DidResults.CreateAndStoreMyDidResult acmeIssuer = createAndStoreMyDid(acmeOnboarding.getToWallet(), "{}").get();

        System.out.println("'Acme' -> Send Nym to Ledger for 'Acme Issuer' DID");
        sendNym(pool, acmeOnboarding.getToWallet(), acmeDid, acmeIssuer.getDid(), acmeIssuer.getVerkey(), null).get();

        System.out.println("\"Acme\" -> Get \"Job-Certificate\" Schema from Ledger");

        Object jobCertSchema = getSchema(pool, acmeIssuer.getDid(), governmentIssuer.getDid(), createGetSchema("Job-Certificate", "0.2")).get();

        System.out.println("\"Acme\" -> Create and store in Wallet \"Acme Job-Certificate\" Claim Definition");
        createAndSendClaimDef(pool, acmeOnboarding.getToWallet(), acmeIssuer, jobCertSchema.toString());


        System.out.println("Getting Transcript with Faber");
        System.out.println("Getting Transcript with Faber - Onboarding");
        OnboardingResult aliceOnboarding = onboard(pool, poolName, "Faber", faberOnboarding.getToWallet(), faberDid, "Alice", null, "alice_wallet");

        System.out.println("'Faber' -> Create 'Transcript' Claim offer for Alice");

        String transcriptClaimOffer = issuerCreateClaimOffer(faberOnboarding.getToWallet(), transcriptSchema.toString(), faberIssuer.getDid(), aliceOnboarding.getToFromDidAndKey().getDid()).get();

        System.out.println("'Faber' -> Authcrypt 'Transcript' Claim Offer for Alice");
        byte[] authcryptedClaimOffer = Crypto.authCrypt(faberOnboarding.getToWallet(), aliceOnboarding.getFromToKey(), aliceOnboarding.getToFromDidAndKey().getVerkey(), transcriptClaimOffer.getBytes(Charset.forName("utf8"))).get();

        System.out.println("'Faber' -> Send authcrypted 'Transcript' Claim Offer to Alice");

        System.out.println("'Alice' -> Authdecrypted 'Transcript' Claim Offer from Faber");
        CryptoResults.AuthDecryptResult authDecryptClaimOfferResult = Crypto.authDecrypt(aliceOnboarding.getToWallet(), aliceOnboarding.getToFromDidAndKey().getVerkey(), authcryptedClaimOffer).get();

        System.out.println(new String(authDecryptClaimOfferResult.getDecryptedMessage(), Charset.forName("utf8")));

        System.out.println("'Alice' -> Store 'Transcript' Claim Offer in Wallet from Faber");
        proverStoreClaimOffer(aliceOnboarding.getToWallet(), new String(authDecryptClaimOfferResult.getDecryptedMessage(), Charset.forName("utf8"))).get();

        System.out.println("'Alice' -> Create and store 'Alice' Master Secret in Wallet");
        String aliceMasterSecretName = "alice_master_secret";
        proverCreateMasterSecret(aliceOnboarding.getToWallet(), aliceMasterSecretName);

        JSONObject transcriptClaimOfferJson = new JSONObject(new String(authDecryptClaimOfferResult.getDecryptedMessage(), Charset.forName("utf8")));

        System.out.println("'Alice' -> Get 'Transcript' Schema from Ledger");
        JSONObject claimSchema = getSchemaByKey(pool, aliceOnboarding.getToFromDidAndKey().getDid(), transcriptClaimOfferJson.getJSONObject("schema_key")).get();

        System.out.println("'Alice' -> Get 'Faber Transcript' Claim Definition from Ledger");
        JSONObject faberTranscriptClaimDef = getClaimDef(pool, aliceOnboarding.getToFromDidAndKey().getDid(), claimSchema, transcriptClaimOfferJson.getString("issuer_did"));

        System.out.println("CLAIM DEF: " + faberTranscriptClaimDef.toString());

        System.out.println("'Alice' -> Create and store in Wallet 'Transcript' Claim Request for Faber");
        String transcriptClaimRequestJson = proverCreateAndStoreClaimReq(aliceOnboarding.getToWallet(), aliceOnboarding.getToFromDidAndKey().getDid(), transcriptClaimOfferJson.toString(), faberTranscriptClaimDef.toString(), aliceMasterSecretName).get();

        System.out.println("'Alice' -> Authcrypt 'Transcript' Claim request for Faber");
        byte[] authcryptedTranscriptClaimRequest = Crypto.authCrypt(aliceOnboarding.getToWallet(), aliceOnboarding.getToFromDidAndKey().getVerkey(), aliceOnboarding.getFromToKey(), transcriptClaimRequestJson.getBytes(Charset.forName("utf8"))).get();

        System.out.println("'Alice' -> Send authcrypted 'Transcript' Claim Request to Faber");

        System.out.println("'Faber' -> Authdecrypt 'Transcript' Claim Request from Alice");
        CryptoResults.AuthDecryptResult authdecryptedTranscriptClaimRequest = Crypto.authDecrypt(faberOnboarding.getToWallet(), aliceOnboarding.getFromToKey(), authcryptedTranscriptClaimRequest).get();

        String authdecryptedTranscriptClaimRequestJson =new String(authdecryptedTranscriptClaimRequest.getDecryptedMessage(), Charset.forName("utf8"));

        System.out.println("'Faber' -> Create 'Transcript' Claim for Alice");
        JSONObject transcriptClaimValues = new JSONObject();

        claimValuesWithNew(transcriptClaimValues, "first_name", "Alice", "1139481716457488690172217916278103335");
        claimValuesWithNew(transcriptClaimValues, "last_name", "Garcia", "5321642780241790123587902456789123452");
        claimValuesWithNew(transcriptClaimValues, "degree", "Bachelor of Science, Marketing", "12434523576212321");
        claimValuesWithNew(transcriptClaimValues, "status", "graduated", "2213454313412354");
        claimValuesWithNew(transcriptClaimValues, "ssn", "123-45-6789", "3124141231422543541");
        claimValuesWithNew(transcriptClaimValues, "year", "2015", "2015");
        claimValuesWithNew(transcriptClaimValues, "average", "5", "5");

        String transcriptClaimJson = issuerCreateClaim(faberOnboarding.getToWallet(), authdecryptedTranscriptClaimRequestJson, transcriptClaimValues.toString(), -1).get().getClaimJson();

        System.out.println("'Faber' -> Authcrypt 'Transcript' Claim to Alice");
        byte[] authcryptedIssuerCreateClaimResult = Crypto.authCrypt(faberOnboarding.getToWallet(), aliceOnboarding.getFromToKey(), aliceOnboarding.getToFromDidAndKey().getVerkey(), transcriptClaimJson.getBytes(Charset.forName("utf8"))).get();

        System.out.println("'Faber' -> Send authcrypted 'Transcript' Claim to Alice");

        System.out.println("'Alice' -> Authdecrypted 'Transcript' Claim from Faber");
        byte[] authDecryptIssuerCreateClaimResult = Crypto.authDecrypt(aliceOnboarding.getToWallet(), aliceOnboarding.getToFromDidAndKey().getVerkey(), authcryptedIssuerCreateClaimResult).get().getDecryptedMessage();


        System.out.println("'Alice' -> Store 'Transcript' Claim for Faber");
        proverStoreClaim(aliceOnboarding.getToWallet(), new String(authDecryptIssuerCreateClaimResult, Charset.forName("utf8")), null).get();

        System.out.println("Apply for the job with Acme - Onboarding");

        OnboardingResult aliceAcmeOnboarding = onboard(pool, poolName, "Acme", acmeOnboarding.getToWallet(), acmeDid, "Alice", aliceOnboarding.getToWallet(), "alice_wallet");

        System.out.println("Apply for the job with Acme - Transcript proving");

        System.out.println("'Acme' -> Create 'Job-Application' Proof request");

        JSONObject jobApplicationProofRequestJson = createJobApplicationProofRequest(Long.toString(System.currentTimeMillis()), "Job-Application", "0.1");
        extendJobApplicationProofRequest(jobApplicationProofRequestJson, "first_name", null, null);
        extendJobApplicationProofRequest(jobApplicationProofRequestJson, "last_name", null, null);
        extendJobApplicationProofRequest(jobApplicationProofRequestJson, "degree", faberIssuer.getDid(), transcriptClaimOfferJson.getJSONObject("schema_key"));
        extendJobApplicationProofRequest(jobApplicationProofRequestJson, "status", faberIssuer.getDid(), transcriptClaimOfferJson.getJSONObject("schema_key"));
        extendJobApplicationProofRequest(jobApplicationProofRequestJson, "ssn", faberIssuer.getDid(), transcriptClaimOfferJson.getJSONObject("schema_key"));
        extendJobApplicationProofRequest(jobApplicationProofRequestJson, "phone_number", null, null);
        extendJobApplicationProofRequestWithPredicate(jobApplicationProofRequestJson, "average", ">=", 4, faberIssuer.getDid(),transcriptClaimOfferJson.getJSONObject("schema_key"));

        System.out.println("JOB_APPLICATION_PROOF_REQUEST_JSON: " + jobApplicationProofRequestJson.toString());


        System.out.println("'Acme' -> Get key for Alice did");
        String aliceAcmeVerkey = keyForDidWithLog(pool, acmeOnboarding.getToWallet(), aliceAcmeOnboarding.getDecryptedConnectionResponse().getString("did"));

        System.out.println("'Acme' -> Authcrypt 'Job-Application' Proof request for Alice");

        byte[] authcryptedJobApplicationProofRequest = Crypto.authCrypt(acmeOnboarding.getToWallet(), aliceAcmeOnboarding.getFromToKey(),
                aliceAcmeVerkey, jobApplicationProofRequestJson.toString().getBytes(Charset.forName("utf8"))).get();

        System.out.println("'Acme' -> Send authcrypted 'Job-Application' Proof Request to Alice");

        System.out.println("'Alice' -> Authdecrypt 'Job-Application Proof Request from Acme");

        CryptoResults.AuthDecryptResult authdecryptedJobApplicationProofRequest = Crypto.authDecrypt(aliceOnboarding.getToWallet(),
                aliceAcmeOnboarding.getToFromDidAndKey().getVerkey(), authcryptedJobApplicationProofRequest).get();
        
        
        System.out.println("'Alice' -> Get claims for 'Job-Application' Poor Request");
        JSONObject claimsForJobApplicationProofRequest = new JSONObject(proverGetClaimsForProofReq(aliceOnboarding.getToWallet(),
                new String(authdecryptedJobApplicationProofRequest.getDecryptedMessage(), Charset.forName("utf8"))).get());

        JSONObject claimsForJobApplicationProof = extractClaimsFromRequest(claimsForJobApplicationProofRequest);

        JSONObject entitiesFromLedger = getEntitiesFromLedger(pool, aliceOnboarding.getToFromDidAndKey().getDid(), claimsForJobApplicationProof, "Alice");
        JSONObject schemasJson = entitiesFromLedger.getJSONObject("schemas");
        JSONObject claimDefsJson = entitiesFromLedger.getJSONObject("claimDefs");

        System.out.println("'Alice' -> Create 'Job-Application' Proof");

        JSONObject jobApplicationRequestedClaimsJson = new JSONObject();
        jobApplicationRequestedClaimsJson.put("self_attested_attributes", new JSONObject("{'attr1_referent':'Alice', 'attr2_referent': 'Garcia', 'attr6_referent': '123-45-6789'}"));

        JSONObject requestedAttrs = new JSONObject();
        requestedAttrs.put("attr3_referent", new JSONArray("['" + claimsForJobApplicationProofRequest.getJSONObject("attrs").getJSONArray("attr3_referent").getJSONObject(0).getString("referent") + "', true]"));
        requestedAttrs.put("attr4_referent", new JSONArray("['" + claimsForJobApplicationProofRequest.getJSONObject("attrs").getJSONArray("attr4_referent").getJSONObject(0).getString("referent") + "', true]"));
        requestedAttrs.put("attr5_referent", new JSONArray("['" + claimsForJobApplicationProofRequest.getJSONObject("attrs").getJSONArray("attr5_referent").getJSONObject(0).getString("referent") + "', true]"));


        jobApplicationRequestedClaimsJson.put("requested_attrs", requestedAttrs);


        System.out.println("PREDICATES " + claimsForJobApplicationProofRequest.getJSONObject("predicates").toString());
        jobApplicationRequestedClaimsJson.put("requested_predicates", new JSONObject("{'predicate1_referent': '" + claimsForJobApplicationProofRequest.getJSONObject("predicates").getJSONArray("predicate1_referent").getJSONObject(0).getString("referent") + "'}" ));


        String jobApplicationProofJson = proverCreateProof(aliceOnboarding.getToWallet(), new String(authdecryptedJobApplicationProofRequest.getDecryptedMessage(), Charset.forName("utf8")),
                jobApplicationRequestedClaimsJson.toString(), schemasJson.toString(), aliceMasterSecretName, claimDefsJson.toString(), "{}").get();

        System.out.println("'Alice' -> Authcrypt 'Job-Application' Proof for Acme");
        byte[] authcryptedJobApplicationProofJson = Crypto.authCrypt(aliceOnboarding.getToWallet(), aliceAcmeOnboarding.getToFromDidAndKey().getVerkey(), aliceAcmeOnboarding.getFromToKey(), jobApplicationProofJson.getBytes(Charset.forName("utf8"))).get();

        System.out.println("'Alice' -> Send authcrypted 'Job-Application' Proof to Acme");

        System.out.println("'Acme' -> Authdecrypted 'Job-Application' Proof from Alice");
        byte[] authdecryptedJobApplicationProof = Crypto.authDecrypt(acmeOnboarding.getToWallet(), aliceAcmeOnboarding.getFromToKey(), authcryptedJobApplicationProofJson).get().getDecryptedMessage();

        JSONObject authdecryptedJobApplicationProofJson = new JSONObject(new String(authdecryptedJobApplicationProof, Charset.forName("utf8")));

        JSONObject ledgerEntities = getEntitiesFromLedger(pool, acmeOnboarding.getToFromDidAndKey().getDid(), authdecryptedJobApplicationProofJson.getJSONObject("identifiers"), "Acme");


        System.out.println("'Acme' -> Verify 'Job-Application' Proof from Alice");
        assert "Bachelor of Science, Marketing".equals(authdecryptedJobApplicationProofJson.getJSONObject("requested_proof").getJSONObject("revealed_attrs").getJSONArray("attr3_referent").get(1));
        assert "graduated".equals(authdecryptedJobApplicationProofJson.getJSONObject("requested_proof").getJSONObject("revealed_attrs").getJSONArray("attr4_referent").get(1));
        assert "123-45-6789".equals(authdecryptedJobApplicationProofJson.getJSONObject("requested_proof").getJSONObject("revealed_attrs").getJSONArray("attr5_referent").get(1));

        assert "Alice".equals(authdecryptedJobApplicationProofJson.getJSONObject("requested_proof").getJSONObject("self_attested_attrs").getJSONArray("attr1_referent").get(1));
        assert "Garcia".equals(authdecryptedJobApplicationProofJson.getJSONObject("requested_proof").getJSONObject("self_attested_attrs").getJSONArray("attr2_referent").get(1));
        assert "123-45-6789".equals(authdecryptedJobApplicationProofJson.getJSONObject("requested_proof").getJSONObject("self_attested_attrs").getJSONArray("attr6_referent").get(1));

        assert verifierVerifyProof(jobApplicationProofRequestJson.toString(), authdecryptedJobApplicationProofJson.toString(), ledgerEntities.getJSONObject("schemas").toString(), ledgerEntities.getJSONObject("claimDefs").toString(), "{}").get();

        System.out.println("Finished");
    }

    private static String keyForDidWithLog(Pool pool, Wallet wallet, String did) throws Exception {
        System.out.println("Pool: " + pool + " Wallet: " + wallet + " Did: " + did);
        String result = keyForDid(pool, wallet, did).get();
        System.out.println("KeyForDid result: " + result);
        return result;
    }

    private static JSONObject extractClaimsFromRequest(JSONObject claimsForJobApplicationProofRequest) {
        JSONObject claimsForJobApplicationProof = new JSONObject();

        // Gather claims for attributes
        for (String key : claimsForJobApplicationProofRequest.getJSONObject("attrs").keySet()) {
            try {
                JSONObject claimForAttr = claimsForJobApplicationProofRequest.getJSONObject("attrs").getJSONArray(key).getJSONObject(0);

                claimsForJobApplicationProof.put(claimForAttr.getString("referent"), claimForAttr);
            }
            catch (JSONException e) {
                System.out.println("Ignoring key: " + key + " due to Exception: " + e.getMessage());
            }
        }

        // Gather claims for predicates
        for (String key : claimsForJobApplicationProofRequest.getJSONObject("predicates").keySet()) {
            JSONObject claimForPredicate = claimsForJobApplicationProofRequest.getJSONObject("predicates").getJSONArray(key).getJSONObject(0);

            claimsForJobApplicationProof.put(claimForPredicate.getString("referent"), claimForPredicate);
        }
        return claimsForJobApplicationProof;
    }

    private static JSONObject getEntitiesFromLedger(Pool pool, String did, JSONObject identifiers, String actor) throws Exception {
        JSONObject schemas = new JSONObject();
        JSONObject claimDefs = new JSONObject();
        for (String referent : identifiers.keySet()) {
            JSONObject item = identifiers.getJSONObject(referent);
            System.out.printf("'%s' -> Get Claim Definition from Ledger\n", actor);

            JSONObject schema = getSchemaByKey(pool, did, item.getJSONObject("schema_key")).get();
            schemas.put(referent, schema);

            System.out.printf("'%s' -> Get Claim Definition from Ledger\n", actor);
            JSONObject claimDef = getClaimDef(pool, did, schema, item.getString("issuer_did"));
            claimDefs.put(referent, claimDef);
        }

        JSONObject result = new JSONObject();
        result.put("schemas", schemas);
        result.put("claimDefs", claimDefs);
        return result;
    }

    private static JSONObject createJobApplicationProofRequest(String nonce, String name, String version) {
        JSONObject result = new JSONObject();
        result.put("nonce", nonce);
        result.put("name", name);
        result.put("version", version);
        result.put("requested_attrs", new JSONObject());
        result.put("requested_predicates", new JSONObject());

        return result;
    }

    private static void extendJobApplicationProofRequest(JSONObject request, String attributeName, String issuerDid, JSONObject schemaKey) {
        // Find first unused property
        int i = 1;
        for (; i < 1000; i++) {
            if (!request.getJSONObject("requested_attrs").has("attr" + i + "_referent")) {
                break;
            }
        }

        JSONObject referent = new JSONObject();
        referent.put("name", attributeName);
        if (issuerDid != null && schemaKey != null) {
            JSONArray restrictions = new JSONArray();
            restrictions.put(new JSONObject("{'issuer_did': " + issuerDid + ", 'schema_key': " + schemaKey + "}"));
            referent.put("restrictions", restrictions);
        }

        request.getJSONObject("requested_attrs").put("attr" + i + "_referent", referent);
    }

    private static void extendJobApplicationProofRequestWithPredicate(JSONObject request, String attributeName, String pType, int value, String issuerDid, JSONObject schemaKey) {
        // Find first unused property
        int i = 1;
        for (; i < 1000; i++) {
            if (!request.getJSONObject("requested_predicates").has("predicate" + i + "_referent")) {
                break;
            }
        }

        JSONObject referent = new JSONObject();
        referent.put("attr_name", attributeName);
        referent.put("p_type", pType);
        referent.put("value", value);
        JSONArray restrictions = new JSONArray();
        restrictions.put(new JSONObject("{'issuer_did': " + issuerDid + ", 'schema_key': " + schemaKey + "}"));
        referent.put("restrictions", restrictions);

        request.getJSONObject("requested_predicates").put("predicate" + i + "_referent", referent);
    }


    private static JSONObject getClaimDef(Pool pool, String did, JSONObject schema, String issuerDid) throws Exception {
        System.out.println("Starting getClaimDef");
        System.out.println("SCHEMA" + schema.toString());
        System.out.println("issuerDid " + issuerDid);
        String claimDefTxn = buildGetClaimDefTxn(did, schema.getInt("seqNo"), "CL", issuerDid).get();
        String claimDefResponse = submitRequest(pool, claimDefTxn).get();
        return new JSONObject(claimDefResponse).getJSONObject("result");

    }

    private static void claimValuesWithNew(JSONObject base, String key, String actualValue, String integerValue) {
        base.put(key, Arrays.asList(actualValue, integerValue));
    }


    private static JSONObject createClaimOffer(String offerIssuerDid, String schemaIssuerDid, String name, String version) {
        JSONObject claimOffer = new JSONObject();
        claimOffer.put("issuer_did", offerIssuerDid);

        Map<String, String> schemaKey = new HashMap<>();
        schemaKey.put("did", schemaIssuerDid);
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

        System.out.printf("\"%s\" -> Send Nym to Ledger for \"%s %s\"\n", from, from, to);
        sendNym(pool, fromWallet, fromDid, fromToDidAndKey.getDid(), fromToDidAndKey.getVerkey(), null).get();

        if (toWallet == null) {
            System.out.printf("\"%s\" -> Creating wallet", to);
            Wallet.createWallet(poolName, toWalletName, "default", null, null).get();
            toWallet = Wallet.openWallet(toWalletName, null, null).get();
        }

        System.out.printf("\"%s\" -> Create and store in Wallet \"%s %s\"\n", to, to, from);
        DidResults.CreateAndStoreMyDidResult toFromDidAndKey = createAndStoreMyDid(toWallet, "{}").get();

        System.out.printf("\"%s\" -> Get key for did from \"%s\" connection request\n", to, from);
        String fromToVerkey = keyForDidWithLog(pool, toWallet, connectionRequest.get("did"));

        System.out.printf("\"%s\" -> Anoncrypt connection response for \"%s\" with \"%s %s\" DID, verkey and nonce\n", to, from, to, from);

        Map<String, String> connectionResponse = createConnectionResponse(connectionRequest, toFromDidAndKey);

        System.out.printf("\"%s\" -> Send anoncrypted connection response to \"%s\"\n", to, from);
        byte[] anonCryptedConnectionResponse = Crypto.anonCrypt(fromToVerkey, new JSONObject(connectionResponse).toString().getBytes(Charset.forName("utf8"))).get();

        System.out.printf("\"%s\" -> Anoncrypted connection response from \"%s\"\n", from, to);

        JSONObject receivedConnectionResponse = new JSONObject(new String(Crypto.anonDecrypt(fromWallet, fromToVerkey, anonCryptedConnectionResponse).get(), Charset.forName("utf8")));

        System.out.printf("\"%s\" -> Authenticates \"%s\" by comparison of nonce\n", from, to);
        assert receivedConnectionResponse.getString("nonce").equals(connectionResponse.get("nonce"));

        System.out.printf("\"%s\" -> Send Nym to Ledger for \"%s %s\" DID\n", from, to, from);
        sendNym(pool, fromWallet, fromDid, toFromDidAndKey.getDid(), toFromDidAndKey.getVerkey(), null).get();

        OnboardingResult result = new OnboardingResult(toWallet, toWalletName, fromToDidAndKey.getVerkey(), toFromDidAndKey, receivedConnectionResponse);
        System.out.println("Finished onboarding: " + result);
        return result;
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


    static CompletableFuture<JSONObject> getSchemaByKey(Pool pool, String submitterDid, JSONObject schemaKey) throws Exception {
        return getSchema(pool, submitterDid, schemaKey.getString("did"), createGetSchema(schemaKey.getString("name"), schemaKey.getString("version")));
    }
    static CompletableFuture<JSONObject> getSchema(Pool pool, String submitterdDid, String destinationDid, String request) throws Exception {
        System.out.println("destinationDid " + destinationDid);
        System.out.println("request " + request);

        String getSchemaRequest = buildGetSchemaRequest(submitterdDid, destinationDid, request).get();
        return submitRequest(pool, getSchemaRequest).thenApply(rawJson -> new JSONObject(rawJson).getJSONObject("result"));
    }


    static class OnboardingResult {
        Wallet toWallet;
        String toWalletName;
        String fromToKey;
        DidResults.CreateAndStoreMyDidResult toFromDidAndKey;
        JSONObject decryptedConnectionResponse;

        public OnboardingResult(Wallet toWallet, String toWalletName, String fromToKey, DidResults.CreateAndStoreMyDidResult toFromDidAndKey, JSONObject decryptedConnectionResponse) {
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

        public JSONObject getDecryptedConnectionResponse() {
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
