package nl.quintor.studybits.indy.wrapper.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ConnectionResponse implements AnonCryptable, Serializable {
    private String did;
    private String verkey;
    private String nonce;
    @JsonIgnore
    private String theirKey;
}
