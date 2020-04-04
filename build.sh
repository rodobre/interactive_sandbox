#!/bin/sh
javac -d ./bin/ -cp "./:/usr/share/java/jna.jar:/usr/share/java/jna-platform.jar" sysjail/ArgumentParser.java sysjail/LogType.java sysjail/Logger.java sysjail/NativeInterfaceConnector.java sysjail/Main.java
cd bin && jar cvfmP ../builds/sysjail.jar ../META-INF/MANIFEST.MF -C . sysjail/
ln ./builds/sysjail.jar /bin/sysjail.jar
