package org.acme.employeescheduling.domain;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Set;

import ai.timefold.solver.core.api.domain.lookup.PlanningId;

public class Resource {

    @PlanningId
    private String name;
    private String resourceCategory;
    private Set<String> qualifications;

    private Set<LocalDate> unavailableDates;
    private Set<LocalDate> undesiredDates;

    private String team;

    public Resource() {

    }

    public Resource(String name, String resourceCategory, Set<String> skills,
        Set<LocalDate> unavailableDates, Set<LocalDate> undesiredDates, String team) {
        this.name = name;
        this.resourceCategory = resourceCategory;
        this.qualifications = skills;
        this.unavailableDates = unavailableDates;
        this.undesiredDates = undesiredDates;
        this.team = team;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getResourceCategory() {
        return resourceCategory;
    }

    public void setResourceCategory(String resourceCategory) {
        this.resourceCategory = resourceCategory;
    }

    public Set<String> getQualifications() {
        return qualifications;
    }

    public void setQualifications(Set<String> qualifications) {
        this.qualifications = qualifications;
    }

    public Set<LocalDate> getUnavailableDates() {
        return unavailableDates;
    }

    public void setUnavailableDates(Set<LocalDate> unavailableDates) {
        this.unavailableDates = unavailableDates;
    }

    public Set<LocalDate> getUndesiredDates() {
        return undesiredDates;
    }

    public void setUndesiredDates(Set<LocalDate> undesiredDates) {
        this.undesiredDates = undesiredDates;
    }

    public String getTeam() {
        return team;
    }

    public void setTeam(String team) {
        this.team = team;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Resource resource)) {
            return false;
        }
        return Objects.equals(getName(), resource.getName());
    }

    @Override
    public int hashCode() {
        return getName().hashCode();
    }
}
