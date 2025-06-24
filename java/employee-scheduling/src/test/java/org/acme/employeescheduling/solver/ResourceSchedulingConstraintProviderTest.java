package org.acme.employeescheduling.solver;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;

import jakarta.inject.Inject;

import ai.timefold.solver.test.api.score.stream.ConstraintVerifier;

import org.acme.employeescheduling.domain.Demand;
import org.acme.employeescheduling.domain.Resource;
import org.acme.employeescheduling.domain.Schedule;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class ResourceSchedulingConstraintProviderTest {
    private static final LocalDate DAY_1 = LocalDate.of(2021, 2, 1);
    private static final LocalDate DAY_3 = LocalDate.of(2021, 2, 3);

    private static final LocalDateTime DAY_START_TIME = DAY_1.atTime(LocalTime.of(9, 0));
    private static final LocalDateTime DAY_END_TIME = DAY_1.atTime(LocalTime.of(17, 0));
    private static final LocalDateTime AFTERNOON_START_TIME = DAY_1.atTime(LocalTime.of(13, 0));
    private static final LocalDateTime AFTERNOON_END_TIME = DAY_1.atTime(LocalTime.of(21, 0));

    @Inject
    ConstraintVerifier<EmployeeSchedulingConstraintProvider, Schedule> constraintVerifier;

    @Test
    void requiredSkill() {
        Resource resource = new Resource("Amy", "Category", Set.of(), null, null, null);
        constraintVerifier.verifyThat(EmployeeSchedulingConstraintProvider::requiredSkill)
                .given(resource,
                        new Demand("1", DAY_START_TIME, DAY_END_TIME, "Location", "Skill", resource))
                .penalizes(1);

        resource = new Resource("Beth", "Category", Set.of("Skill"), null, null, null);
        constraintVerifier.verifyThat(EmployeeSchedulingConstraintProvider::requiredSkill)
                .given(resource,
                        new Demand("2", DAY_START_TIME, DAY_END_TIME, "Location", "Skill", resource))
                .penalizes(0);
    }

    @Test
    void overlappingShifts() {
        Resource resource1 = new Resource("Amy", null, null, null, null, null);
        Resource resource2 = new Resource("Beth", null, null, null, null, null);
        constraintVerifier.verifyThat(EmployeeSchedulingConstraintProvider::noOverlappingShifts)
                .given(resource1, resource2,
                        new Demand("1", DAY_START_TIME, DAY_END_TIME, "Location", "Skill", resource1),
                        new Demand("2", DAY_START_TIME, DAY_END_TIME, "Location 2", "Skill", resource1))
                .penalizesBy((int) Duration.ofHours(8).toMinutes());

        constraintVerifier.verifyThat(EmployeeSchedulingConstraintProvider::noOverlappingShifts)
                .given(resource1, resource2,
                        new Demand("1", DAY_START_TIME, DAY_END_TIME, "Location", "Skill", resource1),
                        new Demand("2", DAY_START_TIME, DAY_END_TIME, "Location 2", "Skill", resource2))
                .penalizes(0);

        constraintVerifier.verifyThat(EmployeeSchedulingConstraintProvider::noOverlappingShifts)
                .given(resource1, resource2,
                        new Demand("1", DAY_START_TIME, DAY_END_TIME, "Location", "Skill", resource1),
                        new Demand("2", AFTERNOON_START_TIME, AFTERNOON_END_TIME, "Location 2", "Skill", resource1))
                .penalizesBy((int) Duration.ofHours(4).toMinutes());
    }

    @Test
    void oneShiftPerDay() {
        Resource resource1 = new Resource("Amy", null, null, null, null, null);
        Resource resource2 = new Resource("Beth", null, null, null, null, null);
        constraintVerifier.verifyThat(EmployeeSchedulingConstraintProvider::noOverlappingShifts)
                .given(resource1, resource2,
                        new Demand("1", DAY_START_TIME, DAY_END_TIME, "Location", "Skill", resource1),
                        new Demand("2", DAY_START_TIME, DAY_END_TIME, "Location 2", "Skill", resource1))
                .penalizes(1);

        constraintVerifier.verifyThat(EmployeeSchedulingConstraintProvider::noOverlappingShifts)
                .given(resource1, resource2,
                        new Demand("1", DAY_START_TIME, DAY_END_TIME, "Location", "Skill", resource1),
                        new Demand("2", DAY_START_TIME, DAY_END_TIME, "Location 2", "Skill", resource2))
                .penalizes(0);

        constraintVerifier.verifyThat(EmployeeSchedulingConstraintProvider::noOverlappingShifts)
                .given(resource1, resource2,
                        new Demand("1", DAY_START_TIME, DAY_END_TIME, "Location", "Skill", resource1),
                        new Demand("2", AFTERNOON_START_TIME, AFTERNOON_END_TIME, "Location 2", "Skill", resource1))
                .penalizes(1);

        constraintVerifier.verifyThat(EmployeeSchedulingConstraintProvider::noOverlappingShifts)
                .given(resource1, resource2,
                        new Demand("1", DAY_START_TIME, DAY_END_TIME, "Location", "Skill", resource1),
                        new Demand("2", DAY_START_TIME.plusDays(1), DAY_END_TIME.plusDays(1), "Location 2", "Skill", resource1))
                .penalizes(0);
    }

    @Test
    void atLeast10HoursBetweenConsecutiveShifts() {
        Resource resource1 = new Resource("Amy", null, null, null, null, null);
        Resource resource2 = new Resource("Beth", null, null, null, null, null);
        constraintVerifier.verifyThat(EmployeeSchedulingConstraintProvider::atLeast10HoursBetweenTwoShifts)
                .given(resource1, resource2,
                        new Demand("1", DAY_START_TIME, DAY_END_TIME, "Location", "Skill", resource1),
                        new Demand("2", AFTERNOON_END_TIME, DAY_START_TIME.plusDays(1), "Location 2", "Skill", resource1))
                .penalizesBy(360);
        constraintVerifier.verifyThat(EmployeeSchedulingConstraintProvider::atLeast10HoursBetweenTwoShifts)
                .given(resource1, resource2,
                        new Demand("1", DAY_START_TIME, DAY_END_TIME, "Location", "Skill", resource1),
                        new Demand("2", DAY_END_TIME, DAY_START_TIME.plusDays(1), "Location 2", "Skill", resource1))
                .penalizesBy(600);
        constraintVerifier.verifyThat(EmployeeSchedulingConstraintProvider::atLeast10HoursBetweenTwoShifts)
                .given(resource1, resource2,
                        new Demand("1", DAY_END_TIME, DAY_START_TIME.plusDays(1), "Location", "Skill", resource1),
                        new Demand("2", DAY_START_TIME, DAY_END_TIME, "Location 2", "Skill", resource1))
                .penalizesBy(600);
        constraintVerifier.verifyThat(EmployeeSchedulingConstraintProvider::atLeast10HoursBetweenTwoShifts)
                .given(resource1, resource2,
                        new Demand("1", DAY_START_TIME, DAY_END_TIME, "Location", "Skill", resource1),
                        new Demand("2", DAY_END_TIME.plusHours(10), DAY_START_TIME.plusDays(1), "Location 2", "Skill",
                            resource1))
                .penalizes(0);
        constraintVerifier.verifyThat(EmployeeSchedulingConstraintProvider::atLeast10HoursBetweenTwoShifts)
                .given(resource1, resource2,
                        new Demand("1", DAY_START_TIME, DAY_END_TIME, "Location", "Skill", resource1),
                        new Demand("2", AFTERNOON_END_TIME, DAY_START_TIME.plusDays(1), "Location 2", "Skill", resource2))
                .penalizes(0);
        constraintVerifier.verifyThat(EmployeeSchedulingConstraintProvider::noOverlappingShifts)
                .given(resource1, resource2,
                        new Demand("1", DAY_START_TIME, DAY_END_TIME, "Location", "Skill", resource1),
                        new Demand("2", DAY_START_TIME.plusDays(1), DAY_END_TIME.plusDays(1), "Location 2", "Skill", resource1))
                .penalizes(0);
    }

    @Test
    void unavailableEmployee() {
        Resource resource1 = new Resource("Amy", null, null, Set.of(DAY_1, DAY_3), null, null);
        Resource resource2 = new Resource("Beth", null, null, Set.of(), null, null);
        constraintVerifier.verifyThat(EmployeeSchedulingConstraintProvider::unavailableEmployee)
                .given(resource1, resource2,
                        new Demand("1", DAY_START_TIME, DAY_END_TIME, "Location", "Skill", resource1))
                .penalizesBy((int) Duration.ofHours(8).toMinutes());
        constraintVerifier.verifyThat(EmployeeSchedulingConstraintProvider::unavailableEmployee)
                .given(resource1, resource2,
                        new Demand("1", DAY_START_TIME.minusDays(1), DAY_END_TIME, "Location", "Skill", resource1))
                .penalizesBy((int) Duration.ofHours(17).toMinutes());
        constraintVerifier.verifyThat(EmployeeSchedulingConstraintProvider::unavailableEmployee)
                .given(resource1, resource2,
                        new Demand("1", DAY_START_TIME.plusDays(1), DAY_END_TIME.plusDays(1), "Location", "Skill", resource1))
                .penalizes(0);
        constraintVerifier.verifyThat(EmployeeSchedulingConstraintProvider::unavailableEmployee)
                .given(resource1, resource2,
                        new Demand("1", DAY_START_TIME, DAY_END_TIME, "Location", "Skill", resource2))
                .penalizes(0);
    }

    @Test
    void undesiredDayForEmployee() {
        Resource resource1 = new Resource("Amy", null, null, null, Set.of(DAY_1, DAY_3), null);
        Resource resource2 = new Resource("Beth", null, null, null, Set.of(), null);
        constraintVerifier.verifyThat(EmployeeSchedulingConstraintProvider::undesiredDayForEmployee)
                .given(resource1, resource2,
                        new Demand("1", DAY_START_TIME, DAY_END_TIME, "Location", "Skill", resource1))
                .penalizesBy((int) Duration.ofHours(8).toMinutes());
        constraintVerifier.verifyThat(EmployeeSchedulingConstraintProvider::undesiredDayForEmployee)
                .given(resource1, resource2,
                        new Demand("1", DAY_START_TIME.minusDays(1), DAY_END_TIME, "Location", "Skill", resource1))
                .penalizesBy((int) Duration.ofHours(17).toMinutes());
        constraintVerifier.verifyThat(EmployeeSchedulingConstraintProvider::undesiredDayForEmployee)
                .given(resource1, resource2,
                        new Demand("1", DAY_START_TIME.plusDays(1), DAY_END_TIME.plusDays(1), "Location", "Skill", resource1))
                .penalizes(0);
        constraintVerifier.verifyThat(EmployeeSchedulingConstraintProvider::undesiredDayForEmployee)
                .given(resource1, resource2,
                        new Demand("1", DAY_START_TIME, DAY_END_TIME, "Location", "Skill", resource2))
                .penalizes(0);
    }

    @Test
    void balanceEmployeeShiftAssignments() {
        Resource resource1 = new Resource("Amy", null, null, null, null, "team");
        Resource resource2 = new Resource("Beth", null, null, null, null, "team");
        // No employees have shifts assigned; the schedule is perfectly balanced.
        constraintVerifier.verifyThat(EmployeeSchedulingConstraintProvider::balanceEmployeeShiftAssignments)
                .given(resource1, resource2)
                .penalizesBy(0);
        // Only one employee has shifts assigned; the schedule is less balanced.
        constraintVerifier.verifyThat(EmployeeSchedulingConstraintProvider::balanceEmployeeShiftAssignments)
                .given(resource1, resource2,
                        new Demand("1", DAY_START_TIME.minusDays(1), DAY_END_TIME, "Location", "Skill", resource1))
                .penalizesByMoreThan(0);
        // Every employee has a shift assigned; the schedule is once again perfectly balanced.
        constraintVerifier.verifyThat(EmployeeSchedulingConstraintProvider::balanceEmployeeShiftAssignments)
                .given(resource1, resource2,
                        new Demand("1", DAY_START_TIME.minusDays(1), DAY_END_TIME, "Location", "Skill", resource1),
                        new Demand("2", DAY_START_TIME.minusDays(1), DAY_END_TIME, "Location", "Skill", resource2))
                .penalizesBy(0);

    }
}
