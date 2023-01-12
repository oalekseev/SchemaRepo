# SchemaRepo
Telegram Bot written in Java.

This is a bot guide to service manuals and equipment diagrams.

It uses postgres database to store all entities like sections, brands and models.

Service manuals and equipment diagrams files are stored in file system and their paths saves in DB.

It provides search and navigation capabilities.

Also saves information about registered users and their actions in the database.

With minimal modifications, it will ensure the provision of any type/kind of files along their paths (of any nesting level) specified in the database.

The internal cache is made due to the limitations of the telegram API and is an example of how, without relying on the telegram API, to save and be able to access the session data of the current user.

Set in src/main/resources/application.yaml
your Bot token and comma separated admin chatIds (to recieve messages about new registrations and user actions).

Example is here @SchemaRepoBot (use Telegram to search)
