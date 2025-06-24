let autoRefreshIntervalId = null;
const zoomMin = 2 * 1000 * 60 * 60 * 24 // 2 day in milliseconds
const zoomMax = 4 * 7 * 1000 * 60 * 60 * 24 // 4 weeks in milliseconds

const UNAVAILABLE_COLOR = '#ef2929' // Tango Scarlet Red
const UNDESIRED_COLOR = '#f57900' // Tango Orange
const DESIRED_COLOR = '#73d216' // Tango Chameleon

let demoDataId = null;
let scheduleId = null;
let loadedSchedule = null;

const byResourcePanel = document.getElementById("byResourcePanel");
const byResourceTimelineOptions = {
    timeAxis: {scale: "hour", step: 6},
    orientation: {axis: "top"},
    stack: false,
    xss: {disabled: true}, // Items are XSS safe through JQuery
    zoomMin: zoomMin,
    zoomMax: zoomMax,
};
let byResourceGroupDataSet = new vis.DataSet();
let byResourceItemDataSet = new vis.DataSet();
let byResourceTimeline = new vis.Timeline(byResourcePanel, byResourceItemDataSet, byResourceGroupDataSet, byResourceTimelineOptions);

const byConstructionSitePanel = document.getElementById("byConstructionSitePanel");
const byConstructionSiteTimelineOptions = {
    timeAxis: {scale: "hour", step: 6},
    orientation: {axis: "top"},
    xss: {disabled: true}, // Items are XSS safe through JQuery
    zoomMin: zoomMin,
    zoomMax: zoomMax,
};
let byConstructionSiteGroupDataSet = new vis.DataSet();
let byConstructionSiteItemDataSet = new vis.DataSet();
let byConstructionSiteTimeline = new vis.Timeline(byConstructionSitePanel, byConstructionSiteItemDataSet, byConstructionSiteGroupDataSet, byConstructionSiteTimelineOptions);

let windowStart = JSJoda.LocalDate.now().toString();
let windowEnd = JSJoda.LocalDate.parse(windowStart).plusDays(7).toString();

$(document).ready(function () {
    replaceQuickstartTimefoldAutoHeaderFooter();

    $("#solveButton").click(function () {
        solve();
    });
    $("#stopSolvingButton").click(function () {
        stopSolving();
    });
    $("#analyzeButton").click(function () {
        analyze();
    });
    // HACK to allow vis-timeline to work within Bootstrap tabs
    $("#byResourceTab").on('shown.bs.tab', function (event) {
        byResourceTimeline.redraw();
    })
    $("#byConstructionSiteTab").on('shown.bs.tab', function (event) {
        byConstructionSiteTimeline.redraw();
    })

    setupAjax();
    fetchDemoData();
});

function setupAjax() {
    $.ajaxSetup({
        headers: {
            'Content-Type': 'application/json',
            'Accept': 'application/json,text/plain', // plain text is required by solve() returning UUID of the solver job
        }
    });
    // Extend jQuery to support $.put() and $.delete()
    jQuery.each(["put", "delete"], function (i, method) {
        jQuery[method] = function (url, data, callback, type) {
            if (jQuery.isFunction(data)) {
                type = type || callback;
                callback = data;
                data = undefined;
            }
            return jQuery.ajax({
                url: url,
                type: method,
                dataType: type,
                data: data,
                success: callback
            });
        };
    });
}

function fetchDemoData() {
    $.get("/demo-data", function (data) {
        data.forEach(item => {
            $("#testDataButton").append($('<a id="' + item + 'TestData" class="dropdown-item" href="#">' + item + '</a>'));
            $("#" + item + "TestData").click(function () {
                switchDataDropDownItemActive(item);
                scheduleId = null;
                demoDataId = item;

                refreshSchedule();
            });
        });
        demoDataId = data[0];
        switchDataDropDownItemActive(demoDataId);
        refreshSchedule();
    }).fail(function (xhr, ajaxOptions, thrownError) {
        // disable this page as there is no data
        let $demo = $("#demo");
        $demo.empty();
        $demo.html("<h1><p align=\"center\">No test data available</p></h1>")
    });
}

function switchDataDropDownItemActive(newItem) {
    activeCssClass = "active";
    $("#testDataButton > a." + activeCssClass).removeClass(activeCssClass);
    $("#" + newItem + "TestData").addClass(activeCssClass);
}

function getDemandColor(demand, resource) {
    const demandStart = JSJoda.LocalDateTime.parse(demand.start);
    const demandStartDateString = demandStart.toLocalDate().toString();
    const demandEnd = JSJoda.LocalDateTime.parse(demand.end);
    const demandEndDateString = demandEnd.toLocalDate().toString();
    if (resource.unavailableDates.includes(demandStartDateString) ||
        // The contains() check is ignored for a demand end at midnight (00:00:00).
        (demandEnd.isAfter(demandStart.toLocalDate().plusDays(1).atStartOfDay()) &&
            resource.unavailableDates.includes(demandEndDateString))) {
        return UNAVAILABLE_COLOR
    } else if (resource.undesiredDates.includes(demandStartDateString) ||
        // The contains() check is ignored for a demand end at midnight (00:00:00).
        (demandEnd.isAfter(demandStart.toLocalDate().plusDays(1).atStartOfDay()) &&
            resource.undesiredDates.includes(demandEndDateString))) {
        return UNDESIRED_COLOR
    } else {
        return " #729fcf"; // Tango Sky Blue
    }
}

function refreshSchedule() {
    let path = "/schedules/" + scheduleId;
    if (scheduleId === null) {
        if (demoDataId === null) {
            alert("Please select a test data set.");
            return;
        }

        path = "/demo-data/" + demoDataId;
    }
    $.getJSON(path, function (schedule) {
        loadedSchedule = schedule;
        renderSchedule(schedule);
    })
        .fail(function (xhr, ajaxOptions, thrownError) {
            showError("Getting the schedule has failed.", xhr);
            refreshSolvingButtons(false);
        });
}

function renderSchedule(schedule) {
    refreshSolvingButtons(schedule.solverStatus != null && schedule.solverStatus !== "NOT_SOLVING");
    $("#score").text("Score: " + (schedule.score == null ? "?" : schedule.score));

    const unassignedDemands = $("#unassignedDemands");
    const groups = [];

    // Show only first 7 days of draft
    const scheduleStart = schedule.demands.map(demand => JSJoda.LocalDateTime.parse(demand.start).toLocalDate()).sort()[0].toString();
    const scheduleEnd = JSJoda.LocalDate.parse(scheduleStart).plusDays(7).toString();

    windowStart = scheduleStart;
    windowEnd = scheduleEnd;

    unassignedDemands.children().remove();
    let unassignedDemandsCount = 0;
    byResourceGroupDataSet.clear();
    byConstructionSiteGroupDataSet.clear();

    byResourceItemDataSet.clear();
    byConstructionSiteItemDataSet.clear();

    schedule.resources.forEach((resources, index) => {
        const resourceGroupElement = $('<div class="card-body p-2"/>')
            .append($(`<h5 class="card-title mb-2"/>)`)
                .append(resources.name))
            .append($('<div/>')
                .append($(resources.qualifications.map(skill => `<span class="badge me-1 mt-1" style="background-color:#d3d7cf">${skill}</span>`).join(''))));
        byResourceGroupDataSet.add({id: resources.name, content: resourceGroupElement.html()});

        resources.unavailableDates.forEach((rawDate, dateIndex) => {
            const date = JSJoda.LocalDate.parse(rawDate)
            const start = date.atStartOfDay().toString();
            const end = date.plusDays(1).atStartOfDay().toString();
            const byResourceDemandElement = $(`<div/>`)
                .append($(`<h5 class="card-title mb-1"/>`).text("Unavailable"));
            byResourceItemDataSet.add({
                id: "resource-" + index + "-unavailability-" + dateIndex, group: resources.name,
                content: byResourceDemandElement.html(),
                start: start, end: end,
                type: "background",
                style: "opacity: 0.5; background-color: " + UNAVAILABLE_COLOR,
            });
        });
        resources.undesiredDates.forEach((rawDate, dateIndex) => {
            const date = JSJoda.LocalDate.parse(rawDate)
            const start = date.atStartOfDay().toString();
            const end = date.plusDays(1).atStartOfDay().toString();
            const byResourceDemandElement = $(`<div/>`)
                .append($(`<h5 class="card-title mb-1"/>`).text("Undesired"));
            byResourceItemDataSet.add({
                id: "resource-" + index + "-undesired-" + dateIndex, group: resources.name,
                content: byResourceDemandElement.html(),
                start: start, end: end,
                type: "background",
                style: "opacity: 0.5; background-color: " + UNDESIRED_COLOR,
            });
        });
    });

    schedule.demands.forEach((demand, index) => {
        if (groups.indexOf(demand.location) === -1) {
            groups.push(demand.location);
            byConstructionSiteGroupDataSet.add({
                id: demand.constructionSite,
                content: demand.constructionSite,
            });
        }

        if (demand.resource == null) {
            unassignedDemandsCount++;

            const byConstructionSiteDemandElement = $('<div class="card-body p-2"/>')
                .append($(`<h5 class="card-title mb-2"/>)`)
                    .append("Unassigned"))
                .append($('<div/>')
                    .append($(`<span class="badge me-1 mt-1" style="background-color:#d3d7cf">${demand.requiredQualifications}</span>`)));

            byConstructionSiteItemDataSet.add({
                id: 'demand-' + index, group: demand.constructionSite,
                content: byConstructionSiteDemandElement.html(),
                start: demand.start, end: demand.end,
                style: "background-color: #EF292999"
            });
        } else {
            const skillColor = (demand.resource.skills.indexOf(demand.requiredQualifications) === -1 ? '#ef2929' : '#8ae234');
            const byResourceDemandElement = $('<div class="card-body p-2"/>')
                .append($(`<h5 class="card-title mb-2"/>)`)
                    .append(demand.constructionSite))
                .append($('<div/>')
                    .append($(`<span class="badge me-1 mt-1" style="background-color:${skillColor}">${demand.requiredQualifications}</span>`)));
            const byConstructionSiteDemandElement = $('<div class="card-body p-2"/>')
                .append($(`<h5 class="card-title mb-2"/>)`)
                    .append(demand.resource.name))
                .append($('<div/>')
                    .append($(`<span class="badge me-1 mt-1" style="background-color:${skillColor}">${demand.requiredQualifications}</span>`)));

            const demandColor = getDemandColor(demand, demand.resource);
            byResourceItemDataSet.add({
                id: 'demand-' + index, group: demand.resource.name,
                content: byResourceDemandElement.html(),
                start: demand.start, end: demand.end,
                style: "background-color: " + demandColor
            });
            byConstructionSiteItemDataSet.add({
                id: 'demand-' + index, group: demand.constructionSite,
                content: byConstructionSiteDemandElement.html(),
                start: demand.start, end: demand.end,
                style: "background-color: " + demandColor
            });
        }
    });


    if (unassignedDemandsCount === 0) {
        unassignedDemands.append($(`<p/>`).text(`There are no unassigned demands.`));
    } else {
        unassignedDemands.append($(`<p/>`).text(`There are ${unassignedDemandsCount} unassigned demands.`));
    }
    byResourceTimeline.setWindow(scheduleStart, scheduleEnd);
    byConstructionSiteTimeline.setWindow(scheduleStart, scheduleEnd);
}

function solve() {
    $.post("/schedules", JSON.stringify(loadedSchedule), function (data) {
        scheduleId = data;
        refreshSolvingButtons(true);
    }).fail(function (xhr, ajaxOptions, thrownError) {
            showError("Start solving failed.", xhr);
            refreshSolvingButtons(false);
        },
        "text");
}

function analyze() {
    new bootstrap.Modal("#scoreAnalysisModal").show()
    const scoreAnalysisModalContent = $("#scoreAnalysisModalContent");
    scoreAnalysisModalContent.children().remove();
    if (loadedSchedule.score == null) {
        scoreAnalysisModalContent.text("No score to analyze yet, please first press the 'solve' button.");
    } else {
        $('#scoreAnalysisScoreLabel').text(`(${loadedSchedule.score})`);
        $.put("/schedules/analyze", JSON.stringify(loadedSchedule), function (scoreAnalysis) {
            let constraints = scoreAnalysis.constraints;
            constraints.sort((a, b) => {
                let aComponents = getScoreComponents(a.score), bComponents = getScoreComponents(b.score);
                if (aComponents.hard < 0 && bComponents.hard > 0) return -1;
                if (aComponents.hard > 0 && bComponents.soft < 0) return 1;
                if (Math.abs(aComponents.hard) > Math.abs(bComponents.hard)) {
                    return -1;
                } else {
                    if (aComponents.medium < 0 && bComponents.medium > 0) return -1;
                    if (aComponents.medium > 0 && bComponents.medium < 0) return 1;
                    if (Math.abs(aComponents.medium) > Math.abs(bComponents.medium)) {
                        return -1;
                    } else {
                        if (aComponents.soft < 0 && bComponents.soft > 0) return -1;
                        if (aComponents.soft > 0 && bComponents.soft < 0) return 1;

                        return Math.abs(bComponents.soft) - Math.abs(aComponents.soft);
                    }
                }
            });
            constraints.map((e) => {
                let components = getScoreComponents(e.weight);
                e.type = components.hard != 0 ? 'hard' : (components.medium != 0 ? 'medium' : 'soft');
                e.weight = components[e.type];
                let scores = getScoreComponents(e.score);
                e.implicitScore = scores.hard != 0 ? scores.hard : (scores.medium != 0 ? scores.medium : scores.soft);
            });
            scoreAnalysis.constraints = constraints;

            scoreAnalysisModalContent.children().remove();
            scoreAnalysisModalContent.text("");

            const analysisTable = $(`<table class="table"/>`).css({textAlign: 'center'});
            const analysisTHead = $(`<thead/>`).append($(`<tr/>`)
                .append($(`<th></th>`))
                .append($(`<th>Constraint</th>`).css({textAlign: 'left'}))
                .append($(`<th>Type</th>`))
                .append($(`<th># Matches</th>`))
                .append($(`<th>Weight</th>`))
                .append($(`<th>Score</th>`))
                .append($(`<th></th>`)));
            analysisTable.append(analysisTHead);
            const analysisTBody = $(`<tbody/>`)
            $.each(scoreAnalysis.constraints, (index, constraintAnalysis) => {
                let icon = constraintAnalysis.type == "hard" && constraintAnalysis.implicitScore < 0 ? '<span class="fas fa-exclamation-triangle" style="color: red"></span>' : '';
                if (!icon) icon = constraintAnalysis.matches.length == 0 ? '<span class="fas fa-check-circle" style="color: green"></span>' : '';

                let row = $(`<tr/>`);
                row.append($(`<td/>`).html(icon))
                    .append($(`<td/>`).text(constraintAnalysis.name).css({textAlign: 'left'}))
                    .append($(`<td/>`).text(constraintAnalysis.type))
                    .append($(`<td/>`).html(`<b>${constraintAnalysis.matches.length}</b>`))
                    .append($(`<td/>`).text(constraintAnalysis.weight))
                    .append($(`<td/>`).text(constraintAnalysis.implicitScore));
                analysisTBody.append(row);
                row.append($(`<td/>`));
            });
            analysisTable.append(analysisTBody);
            scoreAnalysisModalContent.append(analysisTable);
        }).fail(function (xhr, ajaxOptions, thrownError) {
            showError("Analyze failed.", xhr);
        }, "text");
    }
}

function getScoreComponents(score) {
    let components = {hard: 0, medium: 0, soft: 0};

    $.each([...score.matchAll(/(-?\d*(\.\d+)?)(hard|medium|soft)/g)], (i, parts) => {
        components[parts[3]] = parseFloat(parts[1], 10);
    });

    return components;
}

function refreshSolvingButtons(solving) {
    if (solving) {
        $("#solveButton").hide();
        $("#stopSolvingButton").show();
        if (autoRefreshIntervalId == null) {
            autoRefreshIntervalId = setInterval(refreshSchedule, 2000);
        }
    } else {
        $("#solveButton").show();
        $("#stopSolvingButton").hide();
        if (autoRefreshIntervalId != null) {
            clearInterval(autoRefreshIntervalId);
            autoRefreshIntervalId = null;
        }
    }
}

function refreshSolvingButtons(solving) {
    if (solving) {
        $("#solveButton").hide();
        $("#stopSolvingButton").show();
        if (autoRefreshIntervalId == null) {
            autoRefreshIntervalId = setInterval(refreshSchedule, 2000);
        }
    } else {
        $("#solveButton").show();
        $("#stopSolvingButton").hide();
        if (autoRefreshIntervalId != null) {
            clearInterval(autoRefreshIntervalId);
            autoRefreshIntervalId = null;
        }
    }
}

function stopSolving() {
    $.delete(`/schedules/${scheduleId}`, function () {
        refreshSolvingButtons(false);
        refreshSchedule();
    }).fail(function (xhr, ajaxOptions, thrownError) {
        showError("Stop solving failed.", xhr);
    });
}

function replaceQuickstartTimefoldAutoHeaderFooter() {
    const timefoldHeader = $("header#timefold-auto-header");
    if (timefoldHeader != null) {
        timefoldHeader.addClass("bg-black")
        timefoldHeader.append(
            $(`<div class="container-fluid">
        <nav class="navbar sticky-top navbar-expand-lg navbar-dark shadow mb-3">
          <a class="navbar-brand" href="https://timefold.ai">
            <img src="/webjars/timefold/img/timefold-logo-horizontal-negative.svg" alt="Timefold logo" width="200">
          </a>
          <button class="navbar-toggler" type="button" data-toggle="collapse" data-target="#navbarNav" aria-controls="navbarNav" aria-expanded="false" aria-label="Toggle navigation">
            <span class="navbar-toggler-icon"></span>
          </button>
          <div class="collapse navbar-collapse" id="navbarNav">
            <ul class="nav nav-pills">
              <li class="nav-item active" id="navUIItem">
                <button class="nav-link active" id="navUI" data-bs-toggle="pill" data-bs-target="#demo" type="button">Demo UI</button>
              </li>
              <li class="nav-item" id="navRestItem">
                <button class="nav-link" id="navRest" data-bs-toggle="pill" data-bs-target="#rest" type="button">Guide</button>
              </li>
              <li class="nav-item" id="navOpenApiItem">
                <button class="nav-link" id="navOpenApi" data-bs-toggle="pill" data-bs-target="#openapi" type="button">REST API</button>
              </li>
            </ul>
          </div>
          <div class="ms-auto">
              <div class="dropdown">
                  <button class="btn btn-secondary dropdown-toggle" type="button" id="dropdownMenuButton" data-bs-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
                      Data
                  </button>
                  <div id="testDataButton" class="dropdown-menu" aria-labelledby="dropdownMenuButton"></div>
              </div>
          </div>
        </nav>
      </div>`));
    }

    const timefoldFooter = $("footer#timefold-auto-footer");
    if (timefoldFooter != null) {
        timefoldFooter.append(
            $(`<footer class="bg-black text-white-50">
               <div class="container">
                 <div class="hstack gap-3 p-4">
                   <div class="ms-auto"><a class="text-white" href="https://timefold.ai">Timefold</a></div>
                   <div class="vr"></div>
                   <div><a class="text-white" href="https://timefold.ai/docs">Documentation</a></div>
                   <div class="vr"></div>
                   <div><a class="text-white" href="https://github.com/TimefoldAI/timefold-quickstarts">Code</a></div>
                   <div class="vr"></div>
                   <div class="me-auto"><a class="text-white" href="https://timefold.ai/product/support/">Support</a></div>
                 </div>
               </div>
             </footer>`));
    }
}
