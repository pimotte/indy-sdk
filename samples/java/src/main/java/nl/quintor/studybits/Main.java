package nl.quintor.studybits;

import nl.quintor.studybits.indy.wrapper.IndyPool;
import nl.quintor.studybits.indy.wrapper.IndyWallet;
import nl.quintor.studybits.indy.wrapper.TrustAnchor;
import nl.quintor.studybits.indy.wrapper.WalletOwner;
import nl.quintor.studybits.indy.wrapper.dto.ConnectionRequest;
import nl.quintor.studybits.indy.wrapper.util.JSONUtil;
import utils.PoolUtils;

public class Main {
    public static void main(String[] args) throws Exception {
        Runtime.getRuntime().exec("rm -rf /home/potte/.indy_client");

        String poolName = PoolUtils.createPoolLedgerConfig();

        try (IndyPool indyPool = new IndyPool(poolName)) {
            TrustAnchor steward = new TrustAnchor("Steward", indyPool, IndyWallet.create(indyPool, "steward_wallet", "000000000000000000000000Steward1"));

            String governmentConnectionRequest = steward.createConnectionRequest("Government", "TRUST_ANCHOR").get().toJSON();

            TrustAnchor government = new TrustAnchor("Government", indyPool, IndyWallet.create(indyPool, "government_wallet",null));

            byte[] governmentConnectionResponse = government.acceptConnectionRequest(JSONUtil.mapper.readValue(governmentConnectionRequest, ConnectionRequest.class)).get();

        }


    }
}
