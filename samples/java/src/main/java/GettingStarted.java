import org.hyperledger.indy.sdk.IndyException;
import org.hyperledger.indy.sdk.anoncreds.AnoncredsResults;
import org.hyperledger.indy.sdk.crypto.Crypto;
import org.hyperledger.indy.sdk.crypto.CryptoResults;
import org.hyperledger.indy.sdk.did.DidResults;
import org.hyperledger.indy.sdk.ledger.LedgerResults;
import org.hyperledger.indy.sdk.pool.Pool;
import org.hyperledger.indy.sdk.wallet.Wallet;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import utils.PoolUtils;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
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

        System.out.println("'Government' -> Create 'Transcript' schema");
        AnoncredsResults.IssuerCreateSchemaResult createSchemaResult = issuerCreateSchema(governmentDid, "Transcript", "1.2", "[\"first_name\", \"last_name\", \"degree\", \"status\", \"year\", \"average\", \"ssn\"]").get();
        String transcriptSchemaId = createSchemaResult.getSchemaId();

        System.out.println("'Government' -> Send to Ledger 'Transcript' schema");
        sendSchema(pool, governmentOnboarding.getToWallet(), governmentDid, createSchemaResult.getSchemaJson()).get();

        System.out.println("'Government' -> Create 'Job-Certificate' schema");
        AnoncredsResults.IssuerCreateSchemaResult createJobCertSchemaResult = issuerCreateSchema(governmentDid, "Job-Certificate", "0.2", "[\"first_name\", \"last_name\", \"salary\", \"employee_status\", \"experience\"]").get();
        String jobCertificateSchemaId = createJobCertSchemaResult.getSchemaId();

        System.out.println("'Government' -> Send to Ledger 'Job-Certificate' schema");
        sendSchema(pool, governmentOnboarding.getToWallet(), governmentDid, createJobCertSchemaResult.getSchemaJson()).get();

        System.out.println("\"Faber\" -> Get \"Transcript\" Schema from Ledger");

        LedgerResults.ParseResponseResult transcriptSchema = getSchema(pool, faberDid, transcriptSchemaId).get();

        System.out.println("\"Faber\" -> Create and store in Wallet \"Faber Transcript\" Claim Definition");
        String faberTranscriptCredDefId = createAndSendCredentialDef(pool, faberOnboarding.getToWallet(), faberDid, transcriptSchema.getObjectJson());

        System.out.println("\"Acme\" -> Get \"Job-Certificate\" Schema from Ledger");

        LedgerResults.ParseResponseResult jobCertSchema = getSchema(pool, acmeDid, jobCertificateSchemaId).get();

        System.out.println("\"Acme\" -> Create and store in Wallet \"Acme Job-Certificate\" Claim Definition");
        String acmeJobCertCredDefId = createAndSendCredentialDef(pool, acmeOnboarding.getToWallet(), acmeDid, jobCertSchema.getObjectJson());


        System.out.println("Getting Transcript with Faber");
        System.out.println("Getting Transcript with Faber - Onboarding");
        OnboardingResult aliceOnboarding = onboard(pool, poolName, "Faber", faberOnboarding.getToWallet(), faberDid, "Alice", null, "alice_wallet");

        System.out.println("'Faber' -> Create 'Transcript' Credential offer for Alice");

        String transcriptClaimOffer = issuerCreateCredentialOffer(faberOnboarding.getToWallet(), faberTranscriptCredDefId).get();

        System.out.println("'Faber' -> Authcrypt 'Transcript' Credential Offer for Alice");
        byte[] authcryptedClaimOffer = Crypto.authCrypt(faberOnboarding.getToWallet(), aliceOnboarding.getFromToKey(), aliceOnboarding.getToFromDidAndKey().getVerkey(), transcriptClaimOffer.getBytes(Charset.forName("utf8"))).get();

        System.out.println("'Faber' -> Send authcrypted 'Transcript' Credential Offer to Alice");

        System.out.println("'Alice' -> Authdecrypted 'Transcript' Credential Offer from Faber");
        CryptoResults.AuthDecryptResult authDecryptClaimOfferResult = Crypto.authDecrypt(aliceOnboarding.getToWallet(), aliceOnboarding.getToFromDidAndKey().getVerkey(), authcryptedClaimOffer).get();
        JSONObject authDecryptedClaimOffer = new JSONObject(new String(authDecryptClaimOfferResult.getDecryptedMessage(), Charset.forName("utf8")));

        System.out.println(new String(authDecryptClaimOfferResult.getDecryptedMessage(), Charset.forName("utf8")));

        System.out.println("'Alice' -> Create and store 'Alice' Master Secret in Wallet");
        String aliceMasterSecretId = proverCreateMasterSecret(aliceOnboarding.getToWallet(), null).get();

        System.out.println("'Alice' -> Get 'Faber Transcript' Credential Definition from Ledger");
        LedgerResults.ParseResponseResult aliceFaberTranscriptCredDef = getCredDef(pool, aliceOnboarding.getToFromDidAndKey().getDid(), authDecryptedClaimOffer.getString("cred_def_id"));

        System.out.println("'Alice' -> Create 'Transcript' Credential Request for Faber");
        AnoncredsResults.ProverCreateCredentialRequestResult transcriptCredRequest = proverCreateCredentialReq(aliceOnboarding.getToWallet(), aliceOnboarding.getToFromDidAndKey().getDid(), authDecryptedClaimOffer.toString(), aliceFaberTranscriptCredDef.getObjectJson(), aliceMasterSecretId).get();

        System.out.println("'Alice' -> Authcrypt 'Transcript' Credential Request for Faber");
        byte[] authcryptedTranscriptCredRequest = Crypto.authCrypt(aliceOnboarding.getToWallet(), aliceOnboarding.getToFromDidAndKey().getVerkey(), aliceOnboarding.getFromToKey(), transcriptCredRequest.getCredentialRequestJson().getBytes(Charset.forName("utf8"))).get();

        System.out.println("'Alice' -> Send authcrypted 'Transcript' Credential Request from Alice");

        System.out.println("'Faber' -> Authdecrypt 'Transcript' Credential Request from Alice");

        CryptoResults.AuthDecryptResult authdecryptedTranscryptCredResult = Crypto.authDecrypt(faberOnboarding.getToWallet(), aliceOnboarding.getFromToKey(), authcryptedTranscriptCredRequest).get();

        String authdecryptedTranscriptCredRequestJson = new String(authdecryptedTranscryptCredResult.getDecryptedMessage(), Charset.forName("utf8"));

        System.out.println("'Faber' -> Create 'Transcript' Credential for Alice");
        JSONObject transcriptClaimValues = new JSONObject();

        claimValuesWithNew(transcriptClaimValues, "first_name", "Alice", "1139481716457488690172217916278103335");
        claimValuesWithNew(transcriptClaimValues, "last_name", "Garcia", "5321642780241790123587902456789123452");
        claimValuesWithNew(transcriptClaimValues, "degree", "Bachelor of Science, Marketing", "12434523576212321");
        claimValuesWithNew(transcriptClaimValues, "status", "graduated", "2213454313412354");
        claimValuesWithNew(transcriptClaimValues, "ssn", "123-45-6789", "3124141231422543541");
        claimValuesWithNew(transcriptClaimValues, "year", "2015", "2015");
        claimValuesWithNew(transcriptClaimValues, "average", "5", "5");

         String transcriptCredJson = issuerCreateCredential(faberOnboarding.getToWallet(), transcriptClaimOffer,  authdecryptedTranscriptCredRequestJson, transcriptClaimValues.toString(), null, -1).get().getCredentialJson();

        System.out.println("'Faber' -> Authcrypt 'Transcript' Claim to Alice");
        byte[] authcryptedIssuerCreateClaimResult = Crypto.authCrypt(faberOnboarding.getToWallet(), aliceOnboarding.getFromToKey(), aliceOnboarding.getToFromDidAndKey().getVerkey(), transcriptCredJson.getBytes(Charset.forName("utf8"))).get();

        System.out.println("'Faber' -> Send authcrypted 'Transcript' Claim to Alice");

        System.out.println("'Alice' -> Authdecrypted 'Transcript' Claim from Faber");
        byte[] authDecryptIssuerCreateClaimResult = Crypto.authDecrypt(aliceOnboarding.getToWallet(), aliceOnboarding.getToFromDidAndKey().getVerkey(), authcryptedIssuerCreateClaimResult).get().getDecryptedMessage();
        String authDecryptedTranscriptCredJson = new String(authDecryptIssuerCreateClaimResult, Charset.forName("utf8"));

        System.out.println("'Alice' -> Store 'Transcript' Claim for Faber");
        proverStoreCredential(aliceOnboarding.getToWallet(), null, transcriptCredRequest.getCredentialRequestJson(), transcriptCredRequest.getCredentialRequestMetadataJson(), authDecryptedTranscriptCredJson, aliceFaberTranscriptCredDef.getObjectJson(), null).get();

        System.out.println("Apply for the job with Acme - Onboarding");

        OnboardingResult aliceAcmeOnboarding = onboard(pool, poolName, "Acme", acmeOnboarding.getToWallet(), acmeDid, "Alice", aliceOnboarding.getToWallet(), "alice_wallet");

        System.out.println("Apply for the job with Acme - Transcript proving");

        System.out.println("'Acme' -> Create 'Job-Application' Proof request");

        JSONObject jobApplicationProofRequestJson = createJobApplicationProofRequest(Long.toString(System.currentTimeMillis()), "Job-Application", "0.1");
        extendJobApplicationProofRequest(jobApplicationProofRequestJson, "first_name", null);
        extendJobApplicationProofRequest(jobApplicationProofRequestJson, "last_name", null);
        extendJobApplicationProofRequest(jobApplicationProofRequestJson, "degree", faberTranscriptCredDefId);
        extendJobApplicationProofRequest(jobApplicationProofRequestJson, "status", faberTranscriptCredDefId);
        extendJobApplicationProofRequest(jobApplicationProofRequestJson, "ssn", faberTranscriptCredDefId);
        extendJobApplicationProofRequest(jobApplicationProofRequestJson, "phone_number", null);
        extendJobApplicationProofRequestWithPredicate(jobApplicationProofRequestJson, "average", ">=", 4, faberTranscriptCredDefId);

        System.out.println("'Acme' -> Get key for Alice did");
        String aliceAcmeVerkey = keyForDid(pool, acmeOnboarding.getToWallet(), aliceAcmeOnboarding.getDecryptedConnectionResponse().getString("did")).get();

        System.out.println("'Acme' -> Authcrypt 'Job-Application' Proof request for Alice");

        byte[] authcryptedJobApplicationProofRequest = Crypto.authCrypt(acmeOnboarding.getToWallet(), aliceAcmeOnboarding.getFromToKey(),
                aliceAcmeVerkey, jobApplicationProofRequestJson.toString().getBytes(Charset.forName("utf8"))).get();

        System.out.println("'Acme' -> Send authcrypted 'Job-Application' Proof Request to Alice");

        System.out.println("'Alice' -> Authdecrypt 'Job-Application Proof Request from Acme");

        CryptoResults.AuthDecryptResult authdecryptedJobApplicationProofRequest = Crypto.authDecrypt(aliceOnboarding.getToWallet(),
                aliceAcmeOnboarding.getToFromDidAndKey().getVerkey(), authcryptedJobApplicationProofRequest).get();


        System.out.println("'Alice' -> Get credentials for 'Job-Application' Poor Request");
        JSONObject credentialsForJobApplicationProofRequest = new JSONObject(proverGetCredentialsForProofReq(aliceOnboarding.getToWallet(),
                new String(authdecryptedJobApplicationProofRequest.getDecryptedMessage(), Charset.forName("utf8"))).get());

        JSONObject credentialsFromRequest = extractCredentialsFromRequest(credentialsForJobApplicationProofRequest);

        JSONObject entitiesFromLedger = getEntitiesFromLedger(pool, aliceOnboarding.getToFromDidAndKey().getDid(), credentialsFromRequest, "Alice");
        JSONObject schemasJson = entitiesFromLedger.getJSONObject("schemas");
        JSONObject claimDefsJson = entitiesFromLedger.getJSONObject("credDefs");

        System.out.println("'Alice' -> Create 'Job-Application' Proof");

        JSONObject jobApplicationRequestedClaimsJson = new JSONObject();
        jobApplicationRequestedClaimsJson.put("self_attested_attributes", new JSONObject("{'attr1_referent':'Alice', 'attr2_referent': 'Garcia', 'attr6_referent': '123-45-6789'}"));

        JSONObject requestedAttrs = new JSONObject();
        requestedAttrs.put("attr3_referent", new JSONObject("{'cred_id':'" + credentialsForJobApplicationProofRequest.getJSONObject("attrs").getJSONArray("attr3_referent").getJSONObject(0).getJSONObject("cred_info").getString("referent") + "', 'revealed': true}"));
        requestedAttrs.put("attr4_referent", new JSONObject("{'cred_id':'" + credentialsForJobApplicationProofRequest.getJSONObject("attrs").getJSONArray("attr4_referent").getJSONObject(0).getJSONObject("cred_info").getString("referent") + "', 'revealed': true}"));
        requestedAttrs.put("attr5_referent", new JSONObject("{'cred_id':'" + credentialsForJobApplicationProofRequest.getJSONObject("attrs").getJSONArray("attr5_referent").getJSONObject(0).getJSONObject("cred_info").getString("referent") + "', 'revealed': true}"));


        jobApplicationRequestedClaimsJson.put("requested_attributes", requestedAttrs);


        jobApplicationRequestedClaimsJson.put("requested_predicates", new JSONObject("{'predicate1_referent': {'revealed':true, 'cred_id':'" + credentialsForJobApplicationProofRequest.getJSONObject("predicates").getJSONArray("predicate1_referent").getJSONObject(0).getJSONObject("cred_info").getString("referent") + "'}}" ));

        System.out.println("SCHEMAS" + schemasJson.toString());

        String jobApplicationProofJson = proverCreateProof(aliceOnboarding.getToWallet(), new String(authdecryptedJobApplicationProofRequest.getDecryptedMessage(), Charset.forName("utf8")),
                jobApplicationRequestedClaimsJson.toString(), aliceMasterSecretId, schemasJson.toString(), claimDefsJson.toString(), "{}").get();

        System.out.println("'Alice' -> Authcrypt 'Job-Application' Proof for Acme");
        byte[] authcryptedJobApplicationProofJson = Crypto.authCrypt(aliceOnboarding.getToWallet(), aliceAcmeOnboarding.getToFromDidAndKey().getVerkey(), aliceAcmeOnboarding.getFromToKey(), jobApplicationProofJson.getBytes(Charset.forName("utf8"))).get();

        System.out.println("'Alice' -> Send authcrypted 'Job-Application' Proof to Acme");

        System.out.println("'Acme' -> Authdecrypted 'Job-Application' Proof from Alice");
        byte[] authdecryptedJobApplicationProof = Crypto.authDecrypt(acmeOnboarding.getToWallet(), aliceAcmeOnboarding.getFromToKey(), authcryptedJobApplicationProofJson).get().getDecryptedMessage();

        JSONObject authdecryptedJobApplicationProofJson = new JSONObject(new String(authdecryptedJobApplicationProof, Charset.forName("utf8")));

        System.out.println("JobapplicationProofjson: " + authdecryptedJobApplicationProofJson.toString());

        JSONObject ledgerEntities = verifierGetEntitiesFromLedger(pool, acmeOnboarding.getToFromDidAndKey().getDid(), authdecryptedJobApplicationProofJson.getJSONArray("identifiers"), "Acme");


        System.out.println("'Acme' -> Verify 'Job-Application' Proof from Alice");
        assert "Bachelor of Science, Marketing".equals(authdecryptedJobApplicationProofJson.getJSONObject("requested_proof").getJSONObject("revealed_attrs").getJSONArray("attr3_referent").get(1));
        assert "graduated".equals(authdecryptedJobApplicationProofJson.getJSONObject("requested_proof").getJSONObject("revealed_attrs").getJSONArray("attr4_referent").get(1));
        assert "123-45-6789".equals(authdecryptedJobApplicationProofJson.getJSONObject("requested_proof").getJSONObject("revealed_attrs").getJSONArray("attr5_referent").get(1));

        assert "Alice".equals(authdecryptedJobApplicationProofJson.getJSONObject("requested_proof").getJSONObject("self_attested_attrs").getJSONArray("attr1_referent").get(1));
        assert "Garcia".equals(authdecryptedJobApplicationProofJson.getJSONObject("requested_proof").getJSONObject("self_attested_attrs").getJSONArray("attr2_referent").get(1));
        assert "123-45-6789".equals(authdecryptedJobApplicationProofJson.getJSONObject("requested_proof").getJSONObject("self_attested_attrs").getJSONArray("attr6_referent").get(1));

        assert verifierVerifyProof(jobApplicationProofRequestJson.toString(), authdecryptedJobApplicationProofJson.toString(), ledgerEntities.getJSONObject("schemas").toString(), ledgerEntities.getJSONObject("claimDefs").toString(), "{}", "{}").get();
    }

    private static JSONObject extractCredentialsFromRequest(JSONObject claimsForJobApplicationProofRequest) {
        System.out.println("CLAIMS FOR APPLICATION: " + claimsForJobApplicationProofRequest);
        JSONObject claimsForJobApplicationProof = new JSONObject();

        // Gather claims for attributes
        for (String key : claimsForJobApplicationProofRequest.getJSONObject("attrs").keySet()) {
            try {
                JSONObject claimForAttr = claimsForJobApplicationProofRequest.getJSONObject("attrs").getJSONArray(key).getJSONObject(0);

                claimsForJobApplicationProof.put(claimForAttr.getJSONObject("cred_info").getString("referent"), claimForAttr);
            }
            catch (JSONException e) {
                System.out.println("Ignoring key: " + key + " due to Exception: " + e.getMessage());
            }
        }

        // Gather claims for predicates
        for (String key : claimsForJobApplicationProofRequest.getJSONObject("predicates").keySet()) {
            JSONObject claimForPredicate = claimsForJobApplicationProofRequest.getJSONObject("predicates").getJSONArray(key).getJSONObject(0);

            claimsForJobApplicationProof.put(claimForPredicate.getJSONObject("cred_info").getString("referent"), claimForPredicate);
        }
        return claimsForJobApplicationProof;
    }

    private static JSONObject getEntitiesFromLedger(Pool pool, String did, JSONObject identifiers, String actor) throws Exception {
        JSONObject schemas = new JSONObject();
        JSONObject claimDefs = new JSONObject();
        for (String referent : identifiers.keySet()) {
            JSONObject item = identifiers.getJSONObject(referent).getJSONObject("cred_info");
            System.out.printf("'%s' -> Get Schema from Ledger\n", actor);

            System.out.println("ITEM: " + item.toString());
            LedgerResults.ParseResponseResult schema = getSchema(pool, did, item.getString("schema_id")).get();
            schemas.put(schema.getId(), new JSONObject(schema.getObjectJson()));

            System.out.printf("'%s' -> Get Claim Definition from Ledger\n", actor);
            LedgerResults.ParseResponseResult claimDef = getCredDef(pool, did, item.getString("cred_def_id"));
            claimDefs.put(claimDef.getId(), new JSONObject(claimDef.getObjectJson()));
        }

        JSONObject result = new JSONObject();
        result.put("schemas", schemas);
        result.put("credDefs", claimDefs);
        return result;
    }

    private static JSONObject verifierGetEntitiesFromLedger(Pool pool, String did, JSONArray identifiers, String actor) throws Exception {
        JSONObject schemas = new JSONObject();
        JSONObject claimDefs = new JSONObject();
        for (int i = 0; i < identifiers.length(); i++) {
            JSONObject item = identifiers.getJSONObject(i);
            System.out.printf("'%s' -> Get Schema from Ledger\n", actor);

            System.out.println("ITEM: " + item.toString());
            LedgerResults.ParseResponseResult schema = getSchema(pool, did, item.getString("schema_id")).get();
            schemas.put(schema.getId(), new JSONObject(schema.getObjectJson()));

            System.out.printf("'%s' -> Get Claim Definition from Ledger\n", actor);
            LedgerResults.ParseResponseResult claimDef = getCredDef(pool, did, item.getString("cred_def_id"));
            claimDefs.put(claimDef.getId(), new JSONObject(claimDef.getObjectJson()));
        }

        JSONObject result = new JSONObject();
        result.put("schemas", schemas);
        result.put("credDefs", claimDefs);
        return result;
    }

    private static JSONObject createJobApplicationProofRequest(String nonce, String name, String version) {
        JSONObject result = new JSONObject();
        result.put("nonce", nonce);
        result.put("name", name);
        result.put("version", version);
        result.put("requested_attributes", new JSONObject());
        result.put("requested_predicates", new JSONObject());

        return result;
    }

    private static void extendJobApplicationProofRequest(JSONObject request, String attributeName, String credDefId) {
        // Find first unused property
        int i = 1;
        for (; i < 1000; i++) {
            if (!request.getJSONObject("requested_attributes").has("attr" + i + "_referent")) {
                break;
            }
        }

        JSONObject referent = new JSONObject();
        referent.put("name", attributeName);
        if (credDefId != null) {
            JSONArray restrictions = new JSONArray();
            restrictions.put(new JSONObject("{'cred_def_id': '" + credDefId + "'}"));
            referent.put("restrictions", restrictions);
        }

        request.getJSONObject("requested_attributes").put("attr" + i + "_referent", referent);
    }

    private static void extendJobApplicationProofRequestWithPredicate(JSONObject request, String attributeName, String pType, int value, String credDefId) {
        // Find first unused property
        int i = 1;
        for (; i < 1000; i++) {
            if (!request.getJSONObject("requested_predicates").has("predicate" + i + "_referent")) {
                break;
            }
        }

        JSONObject referent = new JSONObject();
        referent.put("name", attributeName);
        referent.put("p_type", pType);
        referent.put("p_value", value);
        JSONArray restrictions = new JSONArray();
        restrictions.put(new JSONObject("{'cred_def_id': '" + credDefId + "'}"));
        referent.put("restrictions", restrictions);

        request.getJSONObject("requested_predicates").put("predicate" + i + "_referent", referent);
    }

//
//    private static JSONObject getClaimDef(Pool pool, String did, JSONObject schema, String issuerDid) throws Exception {
//        String claimDefTxn = buildGetClaimDefTxn(did, schema.getInt("seqNo"), "CL", issuerDid).get();
//        String claimDefResponse = submitRequest(pool, claimDefTxn).get();
//        return new JSONObject(claimDefResponse).getJSONObject("result");
//
//    }

    private static void claimValuesWithNew(JSONObject base, String key, String actualValue, String integerValue) {
        JSONObject value =new JSONObject();
        value.put("raw", actualValue);
        value.put("encoded", integerValue);
        base.put(key, value);
    }

    private static LedgerResults.ParseResponseResult getCredDef(Pool pool, String did, String schemaId) throws Exception {
        String getCredDefRequest = buildGetCredDefRequest(did, schemaId).get();
        String getCredDefResponse = submitRequest(pool, getCredDefRequest).get();
        System.out.println("GET CRED DEF: " + getCredDefResponse);
        return parseGetCredDefResponse(getCredDefResponse).get();
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

    private static String createAndSendCredentialDef(Pool pool, Wallet wallet, String did, String schema) throws InterruptedException, ExecutionException, IndyException {
        AnoncredsResults.IssuerCreateAndStoreCredentialDefResult credentialDefResult = issuerCreateAndStoreCredentialDef(wallet, did, schema, "TAG1", "CL", "{\"support_revocation\": false}").get();

        String claimDefTxn = buildCredDefRequest(did, credentialDefResult.getCredDefJson()).get();

        signAndSubmitRequest(pool, wallet, did, claimDefTxn).get();

        return credentialDefResult.getCredDefId();
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
        String fromToVerkey = keyForDid(pool, toWallet, connectionRequest.get("did")).get();

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
        return signAndSubmitRequest(pool, wallet, did, schemaRequest);
    }

    static String createGetSchema(String name, String version) {
        Map<String, Object> json = new HashMap<>();

        json.put("name", name);
        json.put("version", version);

        return new JSONObject(json).toString();
    }


//    static CompletableFuture<JSONObject> getSchemaByKey(Pool pool, String submitterDid, JSONObject schemaKey) throws Exception {
//        return getSchema(pool, submitterDid, schemaKey.getString("did"), createGetSchema(schemaKey.getString("name"), schemaKey.getString("version")));
//    }
    static CompletableFuture<LedgerResults.ParseResponseResult> getSchema(Pool pool, String submitterdDid, String schemaId) throws Exception {
        String getSchemaRequest = buildGetSchemaRequest(submitterdDid, schemaId).get();
        String getSchemaResponse = submitRequest(pool, getSchemaRequest).get();
        System.out.println("GET SCHEMA RESPONSE: " + getSchemaResponse);
        return parseGetSchemaResponse(getSchemaResponse);
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
