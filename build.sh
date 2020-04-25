javac -d ./bin/ -cp "./:/usr/share/java/jna.jar:/usr/share/java/jna-platform.jar" sysjail/CSVReader.java sysjail/CSVWriter.java sysjail/CSVPersistenceProvider.java sysjail/ArgumentParser.java sysjail/LogType.java sysjail/Logger.java sysjail/NativeInterfaceConnector.java sysjail/Main.java
cd bin && jar cvfmP ../builds/sysjail.jar ../META-INF/MANIFEST.MF -C . sysjail/
