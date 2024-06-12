@param String user
@param String nextConnectionId
@param java.util.List<energy.eddie.examples.exampleapp.ConnectionStatus> connectionStatuses

@template.layout(
title = "Your data connections",
content = @`
    !{String eddieUrl = energy.eddie.examples.exampleapp.Env.EDDIE_PUBLIC_URL.get();}
    !{var contextPath = energy.eddie.examples.exampleapp.Env.PUBLIC_CONTEXT_PATH.get();}
    <script type="module" src="${eddieUrl}/lib/eddie-components.js"></script>
    Hi ${user}.

    <div class="uk-flex uk-margin uk-flex-middle">
        <div class="uk-width-medium">Connect without data need id</div>
        <eddie-connect-button
                class="uk-display-block"
                connection-id="${nextConnectionId}"
                allow-data-need-selection="true"
        ></eddie-connect-button>
    </div>
    <div class="uk-flex uk-margin uk-flex-middle">
        <div class="uk-width-medium">Connect with historical data:</div>
        <eddie-connect-button
                class="uk-display-block"
                connection-id="${nextConnectionId}"
                data-need-id="9bd0668f-cc19-40a8-99db-dc2cb2802b17"
        ></eddie-connect-button>
    </div>
    <div class="uk-flex uk-margin uk-flex-middle">
        <div class="uk-width-medium">Connect with future data:</div>
        <eddie-connect-button
                class="uk-display-block"
                connection-id="${nextConnectionId}"
                data-need-id="dcbc1c74-37bd-4c5b-ab2e-fd0be9c1edf3"
        ></eddie-connect-button>
    </div>
    <div class="uk-flex uk-margin uk-flex-middle">
        <div class="uk-width-medium">Connect with near realtime P1 data:</div>
        <eddie-connect-button
                class="uk-display-block"
                connection-id="${nextConnectionId}"
                data-need-id="5dc71d7e-e8cd-4403-a3a8-d3c095c97a84"
        ></eddie-connect-button>
    </div>
    <div class="uk-flex uk-margin uk-flex-middle">
        <div class="uk-width-medium">Invalid Data Need ID:</div>
        <eddie-connect-button
                class="uk-display-block"
                connection-id="${nextConnectionId}"
                data-need-id="INVALID_ID"
        ></eddie-connect-button>
    </div>
    <div class="uk-flex uk-margin uk-flex-middle">
        <div class="uk-width-medium">Connect with historical data:</div>
        <eddie-connect-button
                class="uk-display-block"
                connection-id="${nextConnectionId}"
                data-need-id="b35dc783-a25e-487a-a914-6064bdd7feb5"
        ></eddie-connect-button>
    </div>
    <div class="uk-flex uk-margin uk-flex-middle">
        <div class="uk-width-medium">Connect with accounting point data:</div>
        <eddie-connect-button
                class="uk-display-block"
                connection-id="${nextConnectionId}"
                data-need-id="8685eed4-ab97-4c57-9409-76295792ee1c"
        ></eddie-connect-button>
    </div>

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
`
)
