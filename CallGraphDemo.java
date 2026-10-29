import java.util.ArrayList;

class CallGraph {
    private ArrayList<String> functions = new ArrayList<>();
    private ArrayList<String> callers = new ArrayList<>();
    private ArrayList<String> callees = new ArrayList<>();

    private boolean isSignature(String line) {
        return line.startsWith("int ") && line.endsWith("{");
    }

    private String getName(String line) {
        return line.substring(line.indexOf(" ") + 1, line.indexOf("("));
    }

    public void findFunctions(String[] code) {
        for (int i = 0; i < code.length; i++) {
            String line = code[i].trim();
            if (isSignature(line)) {
                functions.add(getName(line));
            }
        }
    }

    public void findCalls(String[] code) {
        String current = "";
        for (int i = 0; i < code.length; i++) {
            String line = code[i].trim();

            if (isSignature(line)) {
                current = getName(line);
            } else {
                for (int j = 0; j < functions.size(); j++) {
                    String f = functions.get(j);
                    if (line.contains(f + "(")) {
                        addEdge(current, f);
                    }
                }
            }
        }
    }

    private void addEdge(String from, String to) {
        for (int i = 0; i < callers.size(); i++) {
            if (callers.get(i).equals(from) && callees.get(i).equals(to)) {
                return;
            }
        }
        callers.add(from);
        callees.add(to);
    }

    public void printGraph() {
        for (int i = 0; i < functions.size(); i++) {
            String f = functions.get(i);
            String result = "";
            for (int j = 0; j < callers.size(); j++) {
                if (callers.get(j).equals(f)) {
                    result = result + callees.get(j) + " ";
                }
            }
            if (result.equals("")) {
                result = "(none)";
            }
            System.out.println(f + " -> " + result);
        }
    }

    public void detectRecursion() {
        for (int i = 0; i < callers.size(); i++) {
            String from = callers.get(i);
            String to = callees.get(i);
            if (from.equals(to)) {
                System.out.println("Direct recursion: " + from);
            } else {
                for (int j = 0; j < callers.size(); j++) {
                    if (callers.get(j).equals(to) && callees.get(j).equals(from) && from.compareTo(to) < 0) {
                        System.out.println("Mutual recursion: " + from + " <-> " + to);
                    }
                }
            }
        }
    }
}

class CallGraphDemo {
    public static void main(String[] args) {
        String[] code = {
                "int square(int x) {",
                "    return x * x;",
                "}",
                "int calculate(int n) {",
                "    return square(n) + helper(n);",
                "}",
                "int helper(int x) {",
                "    if (x <= 1) return 1;",
                "    return helper(x - 1) + 1;",
                "}",
                "int main() {",
                "    return calculate(5);",
                "}"
        };

        CallGraph cg = new CallGraph();
        cg.findFunctions(code);
        cg.findCalls(code);
        cg.printGraph();
        cg.detectRecursion();
    }
}