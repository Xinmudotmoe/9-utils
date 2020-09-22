package moe.xinmu.beanutils2;

import lombok.AllArgsConstructor;
import lombok.Data;


@Data
@AllArgsConstructor
public class SimpleDoubleKey<A, B> {
    final A first;
    final B second;
}
