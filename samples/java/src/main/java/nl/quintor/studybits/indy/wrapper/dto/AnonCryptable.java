package nl.quintor.studybits.indy.wrapper.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import nl.quintor.studybits.indy.wrapper.util.JSONUtil;
import org.hyperledger.indy.sdk.wallet.Wallet;

public interface AnonCryptable extends Serializable {
    String getTheirKey();
}
