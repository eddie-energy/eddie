@template.layout(
title = "Login",
showMenu = false,
content = @`
    <div class="uk-card uk-card-body uk-card-primary uk-width-2-3@m">
        <h3 class="uk-card-title">Login to our awesome service</h3>
        !{var contextPath = energy.eddie.examples.exampleapp.Env.PUBLIC_CONTEXT_PATH.get();}
        <form class="uk-form-horizontal uk-margin-large" action="${contextPath}/login" method="post">
            <div class="uk-margin">
                <label for="input-user" class="uk-form-label">Email</label>
                <div class="uk-form-controls"><input id="input-user" name="input-user" class="uk-input" type="text"
                                                     value="testuser@example.com"></div>
            </div>
            <div class="uk-margin">
                <label for="input-password" class="uk-form-label">Password</label>
                <div class="uk-form-controls"><input id="input-password" name="input-password" class="uk-input"
                                                     type="password" value="testuser@example.com"></div>
            </div>
            <input class="uk-input" type="submit" value="Login">
        </form>
    </div>
`
)
