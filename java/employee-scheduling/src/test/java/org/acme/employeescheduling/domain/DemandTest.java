package org.acme.employeescheduling.domain;

import java.time.LocalDateTime;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class DemandTest {

    @Test
    void isNightShiftTest() {
        Assertions.assertThat(new Demand(LocalDateTime.parse("2011-12-03T10:15:30"), LocalDateTime.parse("2011-12-03T18:15:30"), null, null, null, null).isNightShift())
            .isFalse();
        Assertions.assertThat(new Demand(LocalDateTime.parse("2011-12-03T23:15:30"), LocalDateTime.parse("2011-12-04T03:15:30"), null, null, null, null).isNightShift())
            .isTrue();
        Assertions.assertThat(new Demand(LocalDateTime.parse("2011-12-03T23:15:30"), LocalDateTime.parse("2011-12-04T05:15:30"), null, null, null, null).isNightShift())
            .isTrue();
        Assertions.assertThat(new Demand(LocalDateTime.parse("2011-12-04T01:15:30"), LocalDateTime.parse("2011-12-04T05:15:30"), null, null, null, null).isNightShift())
            .isTrue();
    }

}
