@param String user
@param String nextConnectionId
@param java.util.List<energy.eddie.epdemoapp.ConnectionStatus> connectionStatuses

@template.layout(
title = "Your data connections",
content = @`
    !{String eddieUrl = energy.eddie.epdemoapp.Env.EDDIE_FRAMEWORK_PUBLIC_URL.get();}
    !{var contextPath = energy.eddie.epdemoapp.Env.PUBLIC_CONTEXT_PATH.get();}
    <script type="module" src="${eddieUrl}/lib/eddie-components.js"></script>
    Hi ${user} here is your latest consumption data.
    <h3>Connections</h3>
    <table class="uk-table uk-width-2-3@m">
        <thead>
        <tr>
            <th class="uk-width-small">connection id</th>
            <th class="uk-width-small">time</th>
            <th class="uk-width-small">consent status</th>
            <th class="uk-width-small"></th>
        </tr>
        </thead>
        <tbody>
        @for(energy.eddie.epdemoapp.ConnectionStatus status : connectionStatuses)
            <tr>
                <td>${status.connectionId()}</td>
                <td>${status.timestamp().toString()}</td>
                <td class="uk-text-center"><span class="uk-badge uk-text-small">${status.status()}</span></td>
                <td><a class="uk-button uk-button-default uk-button-small" href="${contextPath}/connections/${status.connectionId()}">Show Data</a></td>
            </tr>
        @endfor
        </tbody>
    </table>
    <span>add data subscription:</span>
    <eddie-connect-button connectionid="${nextConnectionId}"></eddie-connect-button>
`
)