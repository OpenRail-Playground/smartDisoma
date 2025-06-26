package org.acme.employeescheduling.rest;

import static org.instancio.Select.field;

import jakarta.enterprise.context.ApplicationScoped;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import net.datafaker.Faker;
import org.acme.employeescheduling.domain.Demand;
import org.acme.employeescheduling.domain.Resource;
import org.acme.employeescheduling.domain.Schedule;
import org.instancio.Instancio;

@ApplicationScoped
public class DemoDataGenerator {
    private static List<CountDistribution> AVAILABILITY_COUNT_DISTRIBUTION = List.of(new CountDistribution(1, 4),
                        new CountDistribution(2, 3),
                        new CountDistribution(3, 2),
                        new CountDistribution(4, 1)
                );

    private static List<CountDistribution> AVAILABILITY_COUNT_DISTRIBUTION_LARGE = List.of(
        new CountDistribution(5, 4),
        new CountDistribution(10, 3),
        new CountDistribution(15, 2),
        new CountDistribution(20, 1)
    );

    public enum DemoData {
        SMALL(new DemoDataParameters(
                List.of("Ambulatory care", "Critical care", "Pediatric care"),
                List.of(),
                List.of("Doctor", "Nurse"),
                List.of("Anaesthetics", "Cardiology"),
                14,
                15,
                0
        )),
        LARGE(new DemoDataParameters(
                List.of("Ambulatory care",
                        "Neurology",
                        "Critical care",
                        "Pediatric care",
                        "Surgery",
                        "Radiology",
                        "Outpatient"),
                List.of(),
                List.of("Doctor", "Nurse"),
                List.of("Anaesthetics", "Cardiology", "Radiology"),
                28,
                50,
                0
        ));

        private final DemoDataParameters parameters;

        DemoData(DemoDataParameters parameters) {
            this.parameters = parameters;
        }

        public DemoDataParameters getParameters() {
            return parameters;
        }
    }

    public record CountDistribution(int count, double weight) {}

    public record DemoDataParameters(List<String> constructionSite,
                                     List<String> requiredResourceCategories,
                                     List<String> requiredQualifications,
                                     List<String> optionalQualifications,
                                     int daysInSchedule,
                                     int resourceCount,
                                     int randomSeed) {}


    public Schedule generateDemoData(DemoData demoData) {
        Schedule schedule = new Schedule();

        schedule.setDemands(DemandDataProvider.readDemands().subList(0, 1000));

        schedule.setResources(generateResources());
        setRandomUnavailabilities(DemoData.SMALL.getParameters(), schedule);
        return schedule;
    }

    private List<Resource> generateResources() {
        return StaticDataProvider.RESOURCES.stream()
            .map(DemoDataGenerator::assignRandomQualifications)
            .collect(Collectors.toList());
    }

    private static Resource assignRandomQualifications(Resource resource) {
        resource.setQualifications(randomQualifications());
        return resource;
    }

    private static Set<String> randomQualifications() {
        Set<String> qualifications = new HashSet<>();
        List<String> qualificationValues = StaticDataProvider.getQualifications();
        IntStream
            .range(0, new Random().nextInt(qualificationValues.size())) // Randomly pick the amount of qualifications
            .forEach(index -> qualifications.add(getRandomValueFrom(qualificationValues)));
        return qualifications;
    }

    private static String getRandomValueFrom(List<String> possibleValues) {
        return possibleValues.get(new Random().nextInt(possibleValues.size()));
    }

    private void setRandomUnavailabilities(DemoDataParameters parameters, Schedule schedule) {
        LocalDateTime earliestStart = schedule.getDemands().stream().map(Demand::getStart).min(Comparator.naturalOrder()).orElse(null);
        LocalDate startDay = earliestStart.toLocalDate();
        LocalDateTime latestEnd = schedule.getDemands().stream().map(Demand::getEnd).max(Comparator.naturalOrder()).orElse(null);
        int daysInHorizon = latestEnd.getDayOfYear() - earliestStart.getDayOfYear();
        int averageUnavailableDays = (int) (daysInHorizon * 0.07);
        Random random = new Random(parameters.randomSeed);
        schedule.getResources().forEach(resource -> {
            Set<LocalDate> unavailableDates = IntStream.range(0, averageUnavailableDays)
                    .mapToObj(index ->
                            startDay.plusDays(random.nextInt(daysInHorizon))
                    ).collect(Collectors.toSet());
            resource.getUnavailableDates().addAll(unavailableDates);
            Set<LocalDate> undesiredDates = IntStream.range(0, averageUnavailableDays)
                    .mapToObj(index ->
                            startDay.plusDays(random.nextInt(daysInHorizon))
                    )
                    .filter(date -> !unavailableDates.contains(date))
                    .collect(Collectors.toSet());
            resource.getUndesiredDates().addAll(undesiredDates);
        });
    }
}
