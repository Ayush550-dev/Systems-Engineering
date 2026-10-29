import java.util.ArrayList;
import java.util.HashSet;

class Liveness {
    private ArrayList<String> code;

    public Liveness(ArrayList<String> code) {
        this.code = code;
    }
    private boolean isNumber(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isOperator(String s) {
        return s.equals("+") || s.equals("-") || s.equals("*") || s.equals("/");
    }

    private String getDef(String line) {
        if (line.startsWith("print")) {
            return null;
        }
        return line.split("=")[0].trim();
    }

    private HashSet<String> getUse(String line) {
        HashSet<String> use = new HashSet<>();
        String right;
        if (line.startsWith("print")) {
            right = line.substring(line.indexOf("(") + 1, line.indexOf(")"));
        } else {
            right = line.split("=")[1].replace(";", "").trim();
        }
        String[] tokens = right.split(" ");
        for (int i = 0; i < tokens.length; i++) {
            String t = tokens[i];
            if (!isNumber(t) && !isOperator(t) && !t.equals("input()")) {
                use.add(t);
            }
        }
        return use;
    }

    private boolean hasSideEffect(String line) {
        return line.startsWith("print") || line.contains("input");
    }

    public boolean removeDeadCode() {
        int n = code.size();
        ArrayList<HashSet<String>> in = new ArrayList<>();
        ArrayList<HashSet<String>> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            in.add(new HashSet<String>());
            out.add(new HashSet<String>());
        }

        boolean changed = true;
        while (changed) {
            changed = false;
            for (int i = n - 1; i >= 0; i--) {   // backward
                // OUT[i] = IN[next statement]
                HashSet<String> newOut = new HashSet<>();
                if (i < n - 1) {
                    newOut.addAll(in.get(i + 1));
                }

                HashSet<String> newIn = new HashSet<>(newOut);
                String def = getDef(code.get(i));
                if (def != null) {
                    newIn.remove(def);
                }
                newIn.addAll(getUse(code.get(i)));

                if (!newIn.equals(in.get(i)) || !newOut.equals(out.get(i))) {
                    changed = true;
                }
                in.set(i, newIn);
                out.set(i, newOut);
            }
        }

        for (int i = 0; i < n; i++) {
            String line = code.get(i);
            System.out.println(line + "   USE=" + getUse(line) + " DEF=" + getDef(line)
                    + " IN=" + in.get(i) + " OUT=" + out.get(i));
        }

        boolean removed = false;
        for (int i = n - 1; i >= 0; i--) {
            String def = getDef(code.get(i));
            boolean dead = def != null && !out.get(i).contains(def);
            if (dead && !hasSideEffect(code.get(i))) {
                System.out.println("Removing dead line: " + code.get(i));
                code.remove(i);
                removed = true;
            }
        }
        return removed;
    }

    public void printCode() {
        for (int i = 0; i < code.size(); i++) {
            System.out.println(code.get(i));
        }
    }
}

class DeadCodeElimination {
    public static void main(String[] args) {
        ArrayList<String> program = new ArrayList<>();
        program.add("a = input();");
        program.add("b = 10;");
        program.add("c = a + 2;");
        program.add("d = b * 3;");
        program.add("print(c);");

        Liveness live = new Liveness(program);

        int round = 1;
        boolean removed = true;
        while (removed) {
            System.out.println("\n--- Round " + round + " ---");
            removed = live.removeDeadCode();
            round++;
        }

        System.out.println("\nOptimized program:");
        live.printCode();
    }
}