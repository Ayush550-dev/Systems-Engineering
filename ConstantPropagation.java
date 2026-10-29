import java.util.HashMap;

class Optimizer {
    private HashMap<String, Integer> constants = new HashMap<>();

    private boolean isNumber(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    private String replace(String token) {
        if (constants.containsKey(token)) {
            return String.valueOf(constants.get(token));
        }
        return token;
    }

    private int calculate(int a, String op, int b) {
        if (op.equals("+")) return a + b;
        if (op.equals("-")) return a - b;
        if (op.equals("*")) return a * b;
        return a / b;
    }

    public String optimizeLine(String line) {
        line = line.replace(";", "").trim();
        String[] parts = line.split("=");
        String var = parts[0].trim();
        String[] tokens = parts[1].trim().split(" ");

        String result;
        if (tokens.length == 1) {
            result = replace(tokens[0]);
        } else {
            String left = replace(tokens[0]);
            String op = tokens[1];
            String right = replace(tokens[2]);

            boolean divideByZero = op.equals("/") && right.equals("0");
            if (isNumber(left) && isNumber(right) && !divideByZero) {
                int value = calculate(Integer.parseInt(left), op, Integer.parseInt(right));
                result = String.valueOf(value);
            } else {
                result = left + " " + op + " " + right;
            }
        }

        if (isNumber(result)) {
            constants.put(var, Integer.parseInt(result));
        } else {
            constants.remove(var);
        }

        return var + " = " + result + ";";
    }
}

class ConstantPropagation {
    public static void main(String[] args) {
        String[] program = {
                "a = 5;",
                "b = a + 3;",
                "c = b * 2;",
                "a = 7;",
                "d = a + 1;"
        };
        Optimizer opt = new Optimizer();
        for (int i = 0; i < program.length; i++) {
            System.out.println(opt.optimizeLine(program[i]));
        }
    }
}