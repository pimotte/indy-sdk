package nl.quintor.studybits.indy.wrapper.dto;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.json.JSONObject;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConnectionRequest implements Serializable {
    private String did;
    private String nonce;
}
