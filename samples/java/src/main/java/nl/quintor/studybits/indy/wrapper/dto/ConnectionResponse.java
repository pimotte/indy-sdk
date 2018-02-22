package nl.quintor.studybits.indy.wrapper.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@AllArgsConstructor(onConstructor=@__(@JsonCreator))
@Getter
@ToString
public class ConnectionResponse implements AnonCryptable, Serializable {
    private String did;
    private String verkey;
    private String nonce;
    private String theirKey;
}
