This project is a *demo application* to start the Matrix Messenger UI and experiment with it.

If you want to build a product based on the Messenger, we strongly recommend to take a look at [Tammy](https://gitlab.com/connect2x/trixnity-messenger/tammy) on how to whitelabel the client.

## Starting the client
* `./gradlew :trixnity-messenger-compose-app:run`
* if you are not in a CI environment (env variable `CI` is _not_ set), you are in `DEV` mode
  * this means, your local database, logs and settings files are located here: [app-data](app-data)
  * in order to reset your messenger, you can delete this folder
    * *Attention*: this will not log you out of your account, but you will have to log in again if you restart the client (and redo verification)
  * you can also (re)move single accounts that are in folders that are numbered, the messenger will pick up any matching files inside this folder
