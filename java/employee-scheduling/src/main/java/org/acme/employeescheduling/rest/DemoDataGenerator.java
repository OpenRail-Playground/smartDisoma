package org.acme.employeescheduling.rest;

import static org.instancio.Select.field;

import jakarta.enterprise.context.ApplicationScoped;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import net.datafaker.Faker;
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

        schedule.setResources(IntStream.range(0, 73)
            .mapToObj(i -> generateResource())
            .collect(Collectors.toList())
        );

        calculateRandomAvailability(DemoData.SMALL.getParameters(), schedule.getResources());

        return schedule;
    }

    private Resource generateResource() {
        Faker faker = new Faker();
        return Instancio.of(Resource.class)
            .supply(field(Resource::getName), res -> UUID.randomUUID().toString())
            .supply(field(Resource::getResourceCategory), res -> getRandomValueFrom(StaticDataProvider.getResourceCategories()))
            .supply(field(Resource::getQualifications), res -> randomQualifications())
            .create();
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

    private void calculateRandomAvailability(DemoDataParameters parameters, List<Resource> resources) {

        LocalDate startDate = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));

        Random random = new Random(parameters.randomSeed);
        for (int i = 0; i < parameters.daysInSchedule; i++) {
            Set<Resource> resourceWithAvailabilitiesOnDay = pickSubset(resources, random,
                AVAILABILITY_COUNT_DISTRIBUTION_LARGE);
            LocalDate date = startDate.plusDays(i);
            for (Resource resource : resourceWithAvailabilitiesOnDay) {
                switch (random.nextInt(3)) {
                    case 0 -> resource.getUnavailableDates().add(date);
                    case 1 -> resource.getUndesiredDates().add(date);
                }
            }
        }
    }

    private int pickCount(Random random, List<CountDistribution> countDistribution) {
        double probabilitySum = 0;
        for (var possibility : countDistribution) {
            probabilitySum += possibility.weight;
        }
        var choice = random.nextDouble(probabilitySum);
        int numOfItems = 0;
        while (choice >= countDistribution.get(numOfItems).weight) {
            choice -= countDistribution.get(numOfItems).weight;
            numOfItems++;
        }
        return countDistribution.get(numOfItems).count;
    }

    private <T> Set<T> pickSubset(List<T> sourceSet, Random random, List<CountDistribution> countDistribution) {
        var count = pickCount(random, countDistribution);
        List<T> items = new ArrayList<>(sourceSet);
        Collections.shuffle(items, random);
        return new HashSet<>(items.subList(0, count));
    }
}
