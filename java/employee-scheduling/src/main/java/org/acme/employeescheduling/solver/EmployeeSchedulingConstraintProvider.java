package org.acme.employeescheduling.solver;

import static ai.timefold.solver.core.api.score.stream.Joiners.equal;
import static ai.timefold.solver.core.api.score.stream.Joiners.filtering;
import static ai.timefold.solver.core.api.score.stream.Joiners.lessThan;
import static ai.timefold.solver.core.api.score.stream.Joiners.lessThanOrEqual;
import static ai.timefold.solver.core.api.score.stream.Joiners.overlapping;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.stream.Joiners;
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
import org.apache.commons.math3.util.Pair;

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
//                requiredQualifications(constraintFactory),
//                noOverlappingShifts(constraintFactory),
//                atLeastHoursBetweenTwoShifts(constraintFactory, 12),
//                unavailableEmployee(constraintFactory),
//                // Soft constraints
//                undesiredDayForEmployee(constraintFactory),
//                balanceEmployeeShiftAssignments(constraintFactory),
//                constructionSiteSwitching(constraintFactory),
//                shiftChanges(constraintFactory),
//                balanceNightShifts(constraintFactory),
//                teamStability(constraintFactory)
        };
    }

    Constraint requiredResourceCategory(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Demand.class)
                .filter(shift -> !shift.getResource().getResourceCategory().equals(shift.getRequiredResourceCategory()))
                .penalize(HardSoftBigDecimalScore.ONE_HARD)
                .asConstraint("Missing required resource category");
    }

//    Constraint requiredQualifications(ConstraintFactory constraintFactory) {
//        return constraintFactory.forEach(Demand.class)
//                .filter(shift -> !shift.getResource().getQualifications().containsAll(shift.getRequiredQualifications()))
//                .penalize(HardSoftBigDecimalScore.ONE_HARD)
//                .asConstraint("Missing required qualification");
//    }
//
//    Constraint noOverlappingShifts(ConstraintFactory constraintFactory) {
//        return constraintFactory.forEachUniquePair(Demand.class, equal(Demand::getResource),
//                overlapping(Demand::getStart, Demand::getEnd))
//                .penalize(HardSoftBigDecimalScore.ONE_HARD,
//                        EmployeeSchedulingConstraintProvider::getMinuteOverlap)
//                .asConstraint("Overlapping shift");
//    }
//
//    Constraint atLeastHoursBetweenTwoShifts(ConstraintFactory constraintFactory, int minHoursBetweenShifts) {
//        return constraintFactory.forEach(Demand.class)
//                .join(Demand.class, equal(Demand::getResource), lessThanOrEqual(Demand::getEnd, Demand::getStart))
//                .filter((firstShift,
//                        secondShift) -> Duration.between(firstShift.getEnd(), secondShift.getStart()).toHours() < minHoursBetweenShifts)
//                .penalize(HardSoftBigDecimalScore.ONE_HARD,
//                        (firstShift, secondShift) -> {
//                            int breakLength = (int) Duration.between(firstShift.getEnd(), secondShift.getStart()).toMinutes();
//                            return (minHoursBetweenShifts * 60) - breakLength;
//                        })
//                .asConstraint("At least %d hours between 2 shifts".formatted( minHoursBetweenShifts));
//    }
//
//    Constraint unavailableEmployee(ConstraintFactory constraintFactory) {
//        return constraintFactory.forEach(Demand.class)
//                .join(Resource.class, equal(Demand::getResource, Function.identity()))
//                .flattenLast(Resource::getUnavailableDates)
//                .filter(Demand::isOverlappingWithDate)
//                .penalize(HardSoftBigDecimalScore.ONE_HARD, Demand::getOverlappingDurationInMinutes)
//                .asConstraint("Unavailable employee");
//    }
//
//    Constraint undesiredDayForEmployee(ConstraintFactory constraintFactory) {
//        return constraintFactory.forEach(Demand.class)
//                .join(Resource.class, equal(Demand::getResource, Function.identity()))
//                .flattenLast(Resource::getUndesiredDates)
//                .filter(Demand::isOverlappingWithDate)
//                .penalize(HardSoftBigDecimalScore.ONE_SOFT, Demand::getOverlappingDurationInMinutes)
//                .asConstraint("Undesired day for employee");
//    }
//
//    Constraint balanceEmployeeShiftAssignments(ConstraintFactory constraintFactory) {
//        return constraintFactory.forEach(Demand.class)
//                .groupBy(Demand::getResource, ConstraintCollectors.count())
//                .complement(Resource.class, e -> 0) // Include all employees which are not assigned to any shift.c
//                .groupBy(ConstraintCollectors.loadBalance((employee, shiftCount) -> employee,
//                        (employee, shiftCount) -> shiftCount))
//                .penalizeBigDecimal(HardSoftBigDecimalScore.ONE_SOFT, LoadBalance::unfairness)
//                .asConstraint("Balance employee shift assignments");
//    }
//
//    private Constraint constructionSiteSwitching(ConstraintFactory constraintFactory) {
//        return constraintFactory
//            .forEach(Demand.class)
//            .join(Demand.class,
//                equal(Demand::getResource),
//                filtering((d1, d2) -> consecutiveDemandsWithinDays(d1, d2, 4)))  //
//            .filter((demand1, demand2) -> demand1.getConstructionSite().equals(demand2.getConstructionSite()))
//            .reward(HardSoftBigDecimalScore.ONE_SOFT) // TODO may need a reward value
//            .asConstraint("Resource switching construction site");
//    }
//
//    private Constraint shiftChanges(ConstraintFactory constraintFactory) {
//        return constraintFactory
//            .forEach(Demand.class)
//            .join(Demand.class,
//                equal(Demand::getResource),
//                filtering((d1, d2) -> consecutiveDemandsWithinDays(d1, d2, 1)))
//            .filter((demand1, demand2) -> demand1.isNightShift() != demand2.isNightShift())
//            .penalize(HardSoftScore.ONE_SOFT)
//            .asConstraint("Shift changes");
//    }
//
//    Constraint balanceNightShifts(ConstraintFactory constraintFactory) {
//        return constraintFactory.forEach(Demand.class)
//            .filter(Demand::isNightShift)
//            .groupBy(Demand::getResource, ConstraintCollectors.count())
//            .complement(Resource.class, e -> 0) // Include all employees which are not assigned to any shift.c
//            .groupBy(ConstraintCollectors.loadBalance((employee, shiftCount) -> employee,
//                (employee, shiftCount) -> shiftCount))
//            .penalizeBigDecimal(HardSoftBigDecimalScore.ONE_SOFT, LoadBalance::unfairness)
//            .asConstraint("Balance employee night shift assignments");
//    }
//
//    private boolean consecutiveDemandsWithinDays(Demand demand1, Demand demand2, int daysBetween) {
//        if (demand1.getStart().isAfter(demand2.getStart())) {
//            return false;
//        }
//
//        return Duration.between(demand1.getStart(), demand2.getStart()).toDays() <= daysBetween;
//    }
//
//    private Constraint teamStability(ConstraintFactory constraintFactory) {
//        return constraintFactory.forEach(Demand.class)
//                .filter(demand -> demand.getResource() != null)
//                .groupBy(Demand::getShiftId,           // Group by shift/site
//                        demand -> demand.getResource().getTeam(), // Group by team
//                        ConstraintCollectors.count())             // Count team members
//                .filter((shiftId, team, count) -> count > 1)        // More than 1 team member
//                .reward(HardSoftScore.ONE_SOFT,
//                        (site, team, count) -> calculateTeamBonus(count))
//                .asConstraint("Reward team cohesion");
//        }
//
//    private int calculateTeamBonus(long workerCount) {
//        // Example: reward 10 points for each additional worker in the team
//        return (int) (workerCount - 1);
//    }
}
