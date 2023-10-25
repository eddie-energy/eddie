@param String user
@param String nextConnectionId
@param java.util.List<energy.eddie.examples.exampleapp.ConnectionStatus> connectionStatuses

@template.layout(
title = "Your data connections",
content = @`
    !{String eddieUrl = energy.eddie.examples.exampleapp.Env.EDDIE_PUBLIC_URL.get();}
    !{var contextPath = energy.eddie.examples.exampleapp.Env.PUBLIC_CONTEXT_PATH.get();}
    <script type="module" src="${eddieUrl}/lib/eddie-components.js"></script>
    Hi ${user} here is your latest consumption data.
    <h3>Connections</h3>
    <table class="uk-table uk-width-2-3@m">
        <thead>
        <tr>
            <th class="uk-width-small">connection id</th>
            <th class="uk-width-small">time</th>
            <th class="uk-width-small">connection status</th>
            <th class="uk-width-small"></th>
        </tr>
        </thead>
        <tbody>
        @for(energy.eddie.examples.exampleapp.ConnectionStatus status : connectionStatuses)
            <tr>
                <td>${status.connectionId()}</td>
                <td>${status.timestamp().toString()}</td>
                <td class="uk-text-center"><span class="uk-badge uk-text-small">${status.status()}</span></td>
                <td><a class="uk-button uk-button-default uk-button-small"
                       href="${contextPath}/connections/${status.connectionId()}">Show Data</a></td>
            </tr>
        @endfor
        </tbody>
    </table>
    <div class="uk-flex uk-margin uk-flex-middle">
        <div class="uk-width-medium">Connect with historical data:</div>
        <eddie-connect-button
                class="uk-display-block"
                connection-id="${nextConnectionId}"
                data-need-id="LAST_3_MONTHS_ONE_MEASUREMENT_PER_DAY"
        ></eddie-connect-button>
    </div>
    <div class="uk-flex uk-margin uk-flex-middle">
        <div class="uk-width-medium">Connect with near realtime P1 data:</div>
        <eddie-connect-button
                class="uk-display-block"
                connection-id="${nextConnectionId}"
                data-need-id="FUTURE_NEAR_REALTIME_DATA"
        ></eddie-connect-button>
    </div>
`
)