# Android layout groups

Layouts are organized by portal and feature:

- delivery: bills, dashboard, navigation, payments, products, reports, stock, subscriptions
- customer: bills, companies, dashboard, navigation, offers, orders, payments, products, statements, subscriptions, support
- user: bills, companies, dashboard, navigation, offers, orders, payments, products, stock, subscriptions
- auth: entry, login, registration, welcome
- shared: common, companies, payments, settings, subscriptions, support

Admin has its own feature groups under res-admin.

Each src/main/res-<portal>/<feature> directory is an Android resource root
registered in app/build.gradle.kts. Layout variants stay beside the base layout
folder (for example layout-w600dp). Filenames, R.layout names and binding class
names remain unchanged. Other resource types remain under src/main/res.
The resource checker discovers all feature roots recursively.
