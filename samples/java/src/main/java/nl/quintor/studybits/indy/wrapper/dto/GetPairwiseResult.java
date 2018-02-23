package nl.quintor.studybits.indy.wrapper.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import nl.quintor.studybits.indy.wrapper.util.JSONUtil;

import java.io.IOException;

@Getter
@AllArgsConstructor
@Data
@NoArgsConstructor
public class GetPairwiseResult implements Serializable {
    @JsonProperty("my_did")
    private String myDid;
    private String metadata;

    public PairwiseMetadata getParsedMetadata() throws IOException {
        return JSONUtil.mapper.readValue(metadata, PairwiseMetadata.class);
    }
}
