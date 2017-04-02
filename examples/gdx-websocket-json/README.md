# gdx-websocket

This is a simple example of a web socket based communication between the server and the clients, exchanging JSON-serialized messages. Note that it does not come packed with Gradle wrapper, so make sure to install it on your own.

Client uses *LibGDX* (obviously) along with [`gdx-websocket`](../../websocket) library. Desktop uses `gdx-websocket-common` natives library, while GWT project depends on `gdx-websocket-gwt`. Server is built with a few lines of code thanks to the amazing *Vert.x* framework.

### Running the application

The server should listen to web socket connections on `8000` port, wait a bit and send a simple message after each request, and disconnect every user after 5 seconds. Clients should connect with the server, send a simple JSON message after successful connection and keep on printing current message on the screen.

Server can be run using `ServerLauncher` class in the `server` project; desktop client uses standard `DesktopLauncher` class, partially generated by the `gdx-setup` app in the `desktop` project.

You can also check out application's behavior without an IDE thanks to Gradle tasks:

- `gradle server:run` launches the server.
- `gradle desktop:run` launches desktop client. Note that `gdx-websocket-common` library should also work on Android (hence the name), so Android code and dependencies would be similar.
- `gradle html:superDev` compiles GWT application and provides the web client version on `8080` port. Visit `http://localhost:8080` to check it out.

Note that this example sends JSON as byte arrays. Although binary format might not be supported by some browsers, WebGL would probably be unavailable there anyway, so this is the preferred way. If you want to send JSON data as string web socket frames, use `WebSocket#setSerializeAsString` method.