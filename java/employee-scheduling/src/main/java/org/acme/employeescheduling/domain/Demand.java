package org.acme.employeescheduling.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import java.util.Set;

@PlanningEntity
public class Demand {
    @PlanningId
    private String id;

    private LocalDateTime start;
    private LocalDateTime end;

    private String constructionSite;
    private String shiftId; 
    private String requiredResourceCategory;
    private Set<String> requiredQualifications;

    @PlanningVariable
    private Resource resource;

    public Demand() {
    }

    public Demand(LocalDateTime start, LocalDateTime end, String constructionSite, String requiredResourceCategory, Set<String> requiredQualifications) {
        this(start, end, constructionSite, requiredResourceCategory, null, requiredQualifications);
    }

    public Demand(LocalDateTime start, LocalDateTime end, String constructionSite, String requiredResourceCategory, Resource resource, Set<String> requiredQualifications) {
        this(null, start, end, constructionSite, requiredResourceCategory, resource, requiredQualifications);
    }

    public Demand(String id, LocalDateTime start, LocalDateTime end, String constructionSite, String requiredResourceCategory, Resource resource, Set<String> requiredQualifications) {
        this.id = id;
        this.start = start;
        this.end = end;
        this.constructionSite = constructionSite;
        this.requiredResourceCategory = requiredResourceCategory;
        this.requiredQualifications = requiredQualifications;
        this.resource = resource;
        this.shiftId = constructionSite + "-" + start.toEpochSecond(ZoneOffset.UTC);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public LocalDateTime getStart() {
        return start;
    }

    public void setStart(LocalDateTime start) {
        this.start = start;
    }

    public LocalDateTime getEnd() {
        return end;
    }

    public void setEnd(LocalDateTime end) {
        this.end = end;
    }

    public String getConstructionSite() {
        return constructionSite;
    }

    public String getShiftId() {
        return shiftId;
    }

    public void setConstructionSite(String constructionSite) {
        this.constructionSite = constructionSite;
    }

    public String getRequiredResourceCategory() {
        return requiredResourceCategory;
    }

    public void setRequiredResourceCategory(String requiredResourceCategory) {
        this.requiredResourceCategory = requiredResourceCategory;
    }

    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    public Set<String> getRequiredQualifications() {
        return requiredQualifications;
    }

    public void setRequiredQualifications(Set<String> requiredQualifications) {
        this.requiredQualifications = requiredQualifications;
    }

    public boolean isOverlappingWithDate(LocalDate date) {
        return getStart().toLocalDate().equals(date) || getEnd().toLocalDate().equals(date);
    }

    public int getOverlappingDurationInMinutes(LocalDate date) {
        LocalDateTime startDateTime = LocalDateTime.of(date, LocalTime.MIN);
        LocalDateTime endDateTime = LocalDateTime.of(date, LocalTime.MAX);
        return getOverlappingDurationInMinutes(startDateTime, endDateTime, getStart(), getEnd());
    }

    private int getOverlappingDurationInMinutes(LocalDateTime firstStartDateTime, LocalDateTime firstEndDateTime,
            LocalDateTime secondStartDateTime, LocalDateTime secondEndDateTime) {
        LocalDateTime maxStartTime = firstStartDateTime.isAfter(secondStartDateTime) ? firstStartDateTime : secondStartDateTime;
        LocalDateTime minEndTime = firstEndDateTime.isBefore(secondEndDateTime) ? firstEndDateTime : secondEndDateTime;
        long minutes = maxStartTime.until(minEndTime, ChronoUnit.MINUTES);
        return minutes > 0 ? (int) minutes : 0;
    }

    @Override
    public String toString() {
        return constructionSite + " " + start + "-" + end;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Demand demand)) {
            return false;
        }
        return Objects.equals(getId(), demand.getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    public boolean isNightShift() {
        return getOverlappingDurationInMinutes(this.start, this.end,
            LocalDateTime.of(this.end.toLocalDate(), LocalTime.parse("00:00:00")),
            LocalDateTime.of(this.end.toLocalDate(), LocalTime.parse("04:00:00"))) > 0;
    }
}
