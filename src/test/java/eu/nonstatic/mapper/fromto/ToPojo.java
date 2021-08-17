package eu.nonstatic.mapper.fromto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder
@AllArgsConstructor @NoArgsConstructor
public class ToPojo {

    private String foo;
    private String bar;
    private String baz;
    private Number wut;
    private int pii;
    String shadow;
}
