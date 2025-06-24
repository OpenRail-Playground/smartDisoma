package org.acme.employeescheduling.solver;

import static ai.timefold.solver.core.api.score.stream.Joiners.equal;
import static ai.timefold.solver.core.api.score.stream.Joiners.lessThanOrEqual;
import static ai.timefold.solver.core.api.score.stream.Joiners.overlapping;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.function.Function;

import ai.timefold.solver.core.api.score.buildin.hardsoftbigdecimal.HardSoftBigDecimalScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintCollectors;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.common.LoadBalance;

import org.acme.employeescheduling.domain.Demand;
import org.acme.employeescheduling.domain.Resource;

public class EmployeeSchedulingConstraintProvider implements ConstraintProvider {

    private static int getMinuteOverlap(Demand demand1, Demand demand2) {
        // The overlap of two timeslot occurs in the range common to both timeslots.
        // Both timeslots are active after the higher of their two start times,
        // and before the lower of their two end times.
        LocalDateTime shift1Start = demand1.getStart();
        LocalDateTime shift1End = demand1.getEnd();
        LocalDateTime shift2Start = demand2.getStart();
        LocalDateTime shift2End = demand2.getEnd();
        return (int) Duration.between((shift1Start.isAfter(shift2Start)) ? shift1Start : shift2Start,
                (shift1End.isBefore(shift2End)) ? shift1End : shift2End).toMinutes();
    }

    @Override
    public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
        return new Constraint[] {
                // Hard constraints
                requiredResourceCategory(constraintFactory),
                requiredQualifications(constraintFactory),
                noOverlappingShifts(constraintFactory),
                atLeast10HoursBetweenTwoShifts(constraintFactory),
                oneShiftPerDay(constraintFactory),
                unavailableEmployee(constraintFactory),
                // Soft constraints
                undesiredDayForEmployee(constraintFactory),
                balanceEmployeeShiftAssignments(constraintFactory)
        };
    }

    Constraint requiredResourceCategory(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Demand.class)
                .filter(shift -> !shift.getEmployee().getResourceCategory().equals(shift.getRequiredResourceCategory()))
                .penalize(HardSoftBigDecimalScore.ONE_HARD)
                .asConstraint("Missing required resource category");
    }

    Constraint requiredQualifications(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Demand.class)
                .filter(shift -> !shift.getEmployee().getQualifications().containsAll(shift.getRequiredQualifications()))
                .penalize(HardSoftBigDecimalScore.ONE_HARD)
                .asConstraint("Missing required qualification");
    }

    Constraint noOverlappingShifts(ConstraintFactory constraintFactory) {
        return constraintFactory.forEachUniquePair(Demand.class, equal(Demand::getEmployee),
                overlapping(Demand::getStart, Demand::getEnd))
                .penalize(HardSoftBigDecimalScore.ONE_HARD,
                        EmployeeSchedulingConstraintProvider::getMinuteOverlap)
                .asConstraint("Overlapping shift");
    }

    Constraint atLeast10HoursBetweenTwoShifts(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Demand.class)
                .join(Demand.class, equal(Demand::getEmployee), lessThanOrEqual(Demand::getEnd, Demand::getStart))
                .filter((firstShift,
                        secondShift) -> Duration.between(firstShift.getEnd(), secondShift.getStart()).toHours() < 10)
                .penalize(HardSoftBigDecimalScore.ONE_HARD,
                        (firstShift, secondShift) -> {
                            int breakLength = (int) Duration.between(firstShift.getEnd(), secondShift.getStart()).toMinutes();
                            return (10 * 60) - breakLength;
                        })
                .asConstraint("At least 10 hours between 2 shifts");
    }

    Constraint oneShiftPerDay(ConstraintFactory constraintFactory) {
        return constraintFactory.forEachUniquePair(Demand.class, equal(Demand::getEmployee),
                equal(shift -> shift.getStart().toLocalDate()))
                .penalize(HardSoftBigDecimalScore.ONE_HARD)
                .asConstraint("Max one shift per day");
    }

    Constraint unavailableEmployee(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Demand.class)
                .join(Resource.class, equal(Demand::getEmployee, Function.identity()))
                .flattenLast(Resource::getUnavailableDates)
                .filter(Demand::isOverlappingWithDate)
                .penalize(HardSoftBigDecimalScore.ONE_HARD, Demand::getOverlappingDurationInMinutes)
                .asConstraint("Unavailable employee");
    }

    Constraint undesiredDayForEmployee(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Demand.class)
                .join(Resource.class, equal(Demand::getEmployee, Function.identity()))
                .flattenLast(Resource::getUndesiredDates)
                .filter(Demand::isOverlappingWithDate)
                .penalize(HardSoftBigDecimalScore.ONE_SOFT, Demand::getOverlappingDurationInMinutes)
                .asConstraint("Undesired day for employee");
    }

    Constraint balanceEmployeeShiftAssignments(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Demand.class)
                .groupBy(Demand::getEmployee, ConstraintCollectors.count())
                .complement(Resource.class, e -> 0) // Include all employees which are not assigned to any shift.c
                .groupBy(ConstraintCollectors.loadBalance((employee, shiftCount) -> employee,
                        (employee, shiftCount) -> shiftCount))
                .penalizeBigDecimal(HardSoftBigDecimalScore.ONE_SOFT, LoadBalance::unfairness)
                .asConstraint("Balance employee shift assignments");
    }

}
