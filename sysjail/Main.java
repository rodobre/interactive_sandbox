package sysjail;

import java.util.ArrayList;

public class Main {
	public static void main(String[] args) {
		if (args.length != 1) {
			Logger.Log(LogType.ERROR, "Error! Invalid arguments. Specify a process to sandbox.");
			return;
		}

		ArrayList<Argument> process_names = new ArrayList<Argument>();

		for(String arg : args) {
			if (arg.contains("[]=")) {
				String[] parse_value = arg.split("=", 2);
				String[] multi_values = parse_value[1].split(",");

				ArrayList<String> names = new ArrayList<String>();
				ArrayList<String> values = new ArrayList<String>();

				for(String value : multi_values) {
					String[] kv_pair = value.split(":");
					names.add(kv_pair[0]);
					names.add(kv_pair[1]);
				}

				process_names.add(new MultiArgument(names, values));
			} else if (arg.contains("=")) {
				String[] parse_value = arg.split("=", 2);
				process_names.add(new Argument(parse_value[0], parse_value[1]));
			}
		}

		ArgumentParser arg_parser = new ArgumentParser(process_names);
		NativeInterfaceConnector native_interface = new NativeInterfaceConnector();
		native_interface.sandbox_processes(arg_parser.get_argument_list());
	}
}
