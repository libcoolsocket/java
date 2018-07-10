<h1>CoolSocket - Java Socket Implementation</h1>
CoolSocket is developed to enable you to develop nearby socket communication capable applications without having you write complicated codes.
It is currently used in TrebleShot file-sharing application


<h3>How to</h3>

```java

public static class Main {
    public static void main(String[] args) {
        try {
            // To start without blocking, use CoolSocket.start()
            new CommunicationServer().startDelayed(2000);

            // Server is now started

           CoolSocket.connect(new CoolSocket.ConnectionHandler() {
                @Override
                public void onConnect(Client client) {
                    try {
                        ActiveConnection activeConnection = client.connect(new InetSocketAddress("127.0.0.1", 8080), CoolSocket.NO_TIMEOUT);

                        JSON replyJSON = new JSONObject();
                        replyJSON.put("task", "do good not evil");

                        activeConnection.reply(replyJSON.toString());

                        System.out.println("Server said: " + activeConnection.receive().response());
                        // Server said: {"result": true}
                    } catch (Exception e) {}
                }
           });

        } catch (Exception e) {
            // Failed to start the server defined below
            // The port is already in use
        }
    }
}

public class CommunicationServer extends CoolSocket
{
		public CommunicationServer()
		{
		    // listen on port 8080
			super(8080);

			// Can be set using milliseconds to close connection when
			// their response or our reply cannot be delievered in a given time
			setSocketTimeout(CoolSocket.NO_TIMEOUT);
		}

		@Override
		protected void onConnected(final ActiveConnection activeConnection)
		{
			try {
			    // We can also use activeConnection.reply() if the client has called
			    // activeConnection.receive() first.
			    // The order of these two method to be called is important.
			    // They can be changed in the runtime but cannot be expected to work
			    // when the condition is listener-listener or receiver-receiver.
			    // You can use these in a loop and they will work indefinetely
			    // as long as the response can reach to the other within the time set
				ActiveConnection.Response clientRequest = activeConnection.receive();

				JSONObject responseJSON = new JSONObject(clientRequest.response);

				System.out.println("Client said in JSON: " + responseJSON);
				// {"task": "do good not evil"}

				JSONObject replyJSON = new JSONObject();
				replyJSON.put("result", true);

				activeConnection.reply(replyJSON.toString);
			} catch (Exception e) {
			    // TODO: Handle error.
			    // This could be due to the request timed out
			    // Or the JSON element was faulty
			}
		}
	}
}
```

<h3>Implement CoolSocket</h3>
<h4>Maven</h4>

```xml
...
<profiles>
        <profile>
            <repositories>
                <repository>
                    <snapshots>
                        <enabled>false</enabled>
                    </snapshots>
                    <id>bintray-genonbeta-coolsocket</id>
                    <name>bintray</name>
                    <url>https://dl.bintray.com/genonbeta/coolsocket</url>
                </repository>
            </repositories>
            <pluginRepositories>
                <pluginRepository>
                    <snapshots>
                        <enabled>false</enabled>
                    </snapshots>
                    <id>bintray-genonbeta-coolsocket</id>
                    <name>bintray-plugins</name>
                    <url>https://dl.bintray.com/genonbeta/coolsocket</url>
                </pluginRepository>
            </pluginRepositories>
            <id>bintray</id>
        </profile>
    </profiles>
    <activeProfiles>
        <activeProfile>bintray</activeProfile>
    </activeProfiles>
</profiles
...
```

<h4>Gradle</h4>

```xml
...
repositories {
    ...
    maven { url "https://dl.bintray.com/genonbeta/coolsocket" }
    ...
}
...
dependencies {
    ...
    implementation 'com.genonbeta.coolsocket:main:1.0'
    ...
}
...
```