package nl.quintor.studybits.indy.wrapper.dto;

import lombok.*;

@Getter
@AllArgsConstructor
@Data
@NoArgsConstructor
public class PairwiseMetadata implements Serializable {
    private String myKey;
    private String theirKey;
}
