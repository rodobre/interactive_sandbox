package sysjail;

import java.util.ArrayList;
import java.util.HashMap;

class Argument {
    protected String name;
    protected String value;

    public Argument() {
        name = "";
        value = "";
    }

    public Argument(String _name, String _value) {
        name = _name;
        value = _value;
    }

    public void set_name(String _name) {
        name = _name;
    }

    public String get_name() {
        return name;
    }

    public void set_value(String _value) {
        value = _value;
    }

    public String get_value() {
        return value;
    }
}

class MultiArgument extends Argument {
    private HashMap<String, String> indexed_values;

    MultiArgument(ArrayList<String> argument_names, ArrayList<String> argument_values) {
        name = "";
        value = "";

        indexed_values = new HashMap<String, String>();

        if(argument_names.size() != argument_values.size()) {
            throw new RuntimeException("Error! Constructing set of lists with invalid sizes!");
        }

        for(int i = 0; i < argument_names.size(); ++i) {
            indexed_values.put(argument_names.get(i), argument_values.get(i));
        }
    }
    
    MultiArgument(HashMap<String, String> arguments) {
        indexed_values = arguments;
    }

    public HashMap<String, String> get_values() {
        return indexed_values;
    }

    public void set_values(HashMap<String, String> arguments) {
        indexed_values = arguments;
    }
}

public class ArgumentParser {
    private ArrayList<Argument> arguments;

    public ArgumentParser(ArrayList<Argument> argument_list) {
        arguments = argument_list;
    }

    public Argument get_at(int idx) { 
        return arguments.get(idx);
    }

    public MultiArgument get_multiarg_at(int idx) {
        Argument arg = arguments.get(idx);
        if (arguments.get(idx) instanceof MultiArgument) {
            return (MultiArgument)arg;
        }
        else {
            throw new RuntimeException("Invalid object type");
        }
    }

    public void set_at(int idx, Argument arg) {
        arguments.set(idx, arg);
    }

    public void set_multiarg_at(int idx, MultiArgument arg) {
        arguments.set(idx, arg);
    }

    public boolean does_argument_exist(Argument arg) {
        return arguments.contains(arg);
    }

    public ArrayList<Argument> get_argument_list() {
        return arguments;
    }
}