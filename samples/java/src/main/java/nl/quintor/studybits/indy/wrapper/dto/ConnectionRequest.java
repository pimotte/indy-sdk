package nl.quintor.studybits.indy.wrapper.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import org.json.JSONObject;

@AllArgsConstructor
@Getter
@ToString
public class ConnectionRequest {
    private String did;
    private String nonce;
}
