## BokBytarAppen Server

This project provides the server functionality of the BokBytarAppen app.

Follow these steps to get started:

1. Git-clone this repository.

        $ git clone git://github.com/nilewapp/BokBytarAppenServer

2. cd into the project directory

        $ cd BokBytarAppenServer

3. Launch SBT:

        $ sbt

4. Start the application:

        > re-start

5. Access the application at 'https://localhost:8443/\<service from Service.scala\>'
in any web browser.

6. Stop the application:

        > re-stop

### Using the Server with the app

1. Follow the instructions listed in the app repository [README](https://github.com/nilewapp/BokBytarAppen)

2. cd into the server project directory

3. Generate trust stores and certificates

        $ ./tools/gentruststores "your password"

4. Copy the generated file keystore.jks to \<server directory\>/src/main/resources/

5. Update Security.scala to contain your password

        ...
        val password = "your password"
        ...

6. Copy the generated file public\_truststore.bks to \<app directory\>/res/raw/

7. Update password in \<app directory\>/res/values/strings.xml

        ...
        <string name="truststore_password">your password</string>
        ...

8. Update the url to the server in \<app directory\>/res/values/strings.xml

        ...
        <string name="server_url">https://yourdomain.com:8443/</string>
        ...
