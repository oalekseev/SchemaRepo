# SchemaRepo
Telegram Bot written on Java.

This is a bot guide to service manuals and equipment diagrams from 2019.
It uses postgres database to store all entities like sections, brands and models.
It provides search and navigation capabilities.
Also saves information about registered users and their actions in the database.
With minimal modifications, it will ensure the provision of any type of files along their paths (of any nesting level) specified in the database.
The internal cache is made due to the limitations of the telegram API and is an example of how, without relying on the telegram API, to save and be able to access the session data of the current user.

Example is here @SchemaRepoBot (Use Telegram app)
