# New example app

This new example app is started automatically with EDDIE core.
By default, it's accessible at http://localhost:8082/

Run this before starting the app to create the database the first time:
`docker exec -it eddie-test-db-1 createdb -U test new_example_app`

# Run app
`./gradlew run-new-example-app`

## Appearances Expectations

![Create login page & user management for new example app #1078](docs/login_page.png)

![Create landing page with two services #1080](docs/landing_page.png)

![Create admin page for creating and assigning data needs in demo app #1081](docs/admin_page.png)