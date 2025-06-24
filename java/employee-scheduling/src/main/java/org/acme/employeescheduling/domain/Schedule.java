package org.acme.employeescheduling.domain;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.hardsoftbigdecimal.HardSoftBigDecimalScore;
import ai.timefold.solver.core.api.solver.SolverStatus;

@PlanningSolution
public class Schedule {

    @ProblemFactCollectionProperty
    @ValueRangeProvider
    private List<Resource> resources;

    @PlanningEntityCollectionProperty
    private List<Demand> demands;

    @PlanningScore
    private HardSoftBigDecimalScore score;

    private SolverStatus solverStatus;

    // No-arg constructor required for Timefold
    public Schedule() {}

    public Schedule(List<Resource> resources, List<Demand> demands) {
        this.resources = resources;
        this.demands = demands;
    }

    public Schedule(HardSoftBigDecimalScore score, SolverStatus solverStatus) {
        this.score = score;
        this.solverStatus = solverStatus;
    }

    public List<Resource> getEmployees() {
        return resources;
    }

    public void setEmployees(List<Resource> resources) {
        this.resources = resources;
    }

    public List<Demand> getShifts() {
        return demands;
    }

    public void setShifts(List<Demand> demands) {
        this.demands = demands;
    }

    public HardSoftBigDecimalScore getScore() {
        return score;
    }

    public void setScore(HardSoftBigDecimalScore score) {
        this.score = score;
    }

    public SolverStatus getSolverStatus() {
        return solverStatus;
    }

    public void setSolverStatus(SolverStatus solverStatus) {
        this.solverStatus = solverStatus;
    }
}
