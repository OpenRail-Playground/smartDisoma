package org.acme.employeescheduling.rest;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import ai.timefold.solver.core.api.solver.SolverStatus;

import org.acme.employeescheduling.domain.Schedule;
import org.acme.employeescheduling.domain.Demand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
class ResourceScheduleResourceTest {

    @Test
    @Timeout(600_000)
    void solveDemoDataUntilFeasible() {

        Schedule testSchedule = given()
                .when().get("/demo-data/SMALL")
                .then()
                .statusCode(200)
                .extract()
                .as(Schedule.class);

        String jobId = given()
                .contentType(ContentType.JSON)
                .body(testSchedule)
                .expect().contentType(ContentType.TEXT)
                .when().post("/schedules")
                .then()
                .statusCode(200)
                .extract()
                .asString();

        await()
                .atMost(Duration.ofMinutes(5))
                .pollInterval(Duration.ofMillis(500L))
                .until(() -> SolverStatus.NOT_SOLVING.name().equals(
                        get("/schedules/" + jobId + "/status")
                                .jsonPath().get("solverStatus")));

        Schedule solution = get("/schedules/" + jobId).then().extract().as(Schedule.class);
        assertEquals(SolverStatus.NOT_SOLVING, solution.getSolverStatus());
        assertNotNull(solution.getResources());
        assertNotNull(solution.getDemands());
        assertFalse(solution.getDemands().isEmpty());
        for (Demand demand : solution.getDemands()) {
            assertNotNull(demand.getEmployee());
        }
        assertTrue(solution.getScore().isFeasible());
    }
}
