@import energy.eddie.examples.exampleapp.Env
@import gg.jte.Content

@param String title
@param boolean showMenu = true
@param Content content

<!DOCTYPE html>
<html lang="en">

<head>
    <meta charset="UTF-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link href=" https://cdn.jsdelivr.net/npm/uikit@3.16.11/dist/css/uikit.min.css " rel="stylesheet">
    <script src=" https://cdn.jsdelivr.net/npm/uikit@3.16.11/dist/js/uikit.min.js "></script>
    <script src=" https://cdn.jsdelivr.net/npm/uikit@3.16.11/dist/js/uikit-icons.min.js "></script>
    <title>${title}</title>
    <style>
        header {
            border-bottom: 3pt solid rgb(30, 135, 240);
            background-color: #dfefff;
        }
    </style>
</head>

<body>
<header>
    <div class="uk-container uk-flex uk-flex-between">

        <h1 class="uk-margin-top uk-margin-small-bottom">Awesome energy service&trade;<span
                    class="uk-badge">demo app</span></h1>
        @if (showMenu)
            !{var contextPath = Env.PUBLIC_CONTEXT_PATH.get();}
            <ul class="uk-subnav uk-subnav-divider">
                <li class="uk-active"><a href="${contextPath}/connections/">Connections</a></li>
                <li><a href="${contextPath}/login">Logout</a></li>
            </ul>
        @endif
    </div>
</header>
<div class="uk-container">
    <h1>${title}</h1>
    ${content}
</div>
</body>
</html>