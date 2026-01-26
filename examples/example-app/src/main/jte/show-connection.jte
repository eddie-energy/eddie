<!--
SPDX-FileCopyrightText: 2023 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
SPDX-License-Identifier: Apache-2.0
-->

@param String connectionId
@param java.util.List<String> consumptionRecordSummaries
@param java.util.Map<String, java.util.List<java.util.Map<String, Object>>> dataPointsForMeteringPointId
@param String dataPointsForMeteringPointIdJson

@template.layout(
title = "Connection " + connectionId,
content = @`
    <p>Consumption records:
        @for(String summary : consumptionRecordSummaries) ${summary}@endfor</p>

    <%--    <ul class="uk-subnav uk-subnav-pill" uk-switcher>--%>
    <ul uk-tab>
        <li><a href="#">Chart</a></li>
        <li><a href="#">Table</a></li>
    </ul>
    <ul class="uk-switcher">
        <li>
            <canvas id="chart"></canvas>
            <script src="https://cdn.jsdelivr.net/npm/chart.js@4.2.1/dist/chart.umd.min.js"></script>
            <script src="https://cdn.jsdelivr.net/npm/chartjs-adapter-date-fns@3.0.0/dist/chartjs-adapter-date-fns.bundle.min.js"></script>
            <script>
                (function () {
                    const dataPointsForMeteringPointId = JSON.parse('${dataPointsForMeteringPointIdJson}');
                    const meteringPoints = Object.keys(dataPointsForMeteringPointId).sort();
                    const datasets = meteringPoints.map(mp =>
                        ({
                            label: mp,
                            data: dataPointsForMeteringPointId[mp]
                                .map(dp =>
                                    ({x: dp.start * 1000, y: dp.consumption}))
                        }));
                    new Chart(document.getElementById("chart"), {
                        type: 'line',
                        data: {
                            datasets
                        },
                        options: {
                            scales: {
                                x: {
                                    type: "time",
                                    time: {unit: "day"},
                                    title: {display: true, text: "Time"},
                                    display: true,
                                },
                                y: {
                                    type: "linear",
                                    title: {display: true, text: "Consumption"},
                                    display: true,
                                    suggestedMin: 0,
                                    suggestedMax: 10,
                                },
                            }
                        }
                    });
                })();
            </script>
        </li>
        <li>
            @for(String meteringPointId : dataPointsForMeteringPointId.keySet().stream().sorted().toList())
                <h3>Metering Point ${meteringPointId}</h3>
                <table class="uk-table uk-width-2-3@m">
                    <thead>
                    <tr>
                        <th class="uk-width-small">Start</th>
                        <th class="uk-width-small">End</th>
                        <th class="uk-width-small">Consumption</th>
                        <th class="uk-width-small">Type</th>
                    </tr>
                    </thead>
                    <tbody>
                    !{var meteringPointList = dataPointsForMeteringPointId.get(meteringPointId);}
                    @for(java.util.Map<String,Object> meteringPoint: meteringPointList)
                        <tr>
                            <td>${meteringPoint.get("start").toString()}</td>
                            <td>${meteringPoint.get("end_").toString()}</td>
                            <td>${meteringPoint.get("consumption").toString()}</td>
                            <td>${meteringPoint.get("metering_type").toString()}</td>
                        </tr>
                    @endfor
                    </tbody>
                </table>
            @endfor
        </li>
    </ul>
`
)