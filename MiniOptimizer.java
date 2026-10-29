import java.util.ArrayList;
import java.util.HashSet;

class Instruction {
    String dest;
    String left;
    String op;
    String right;
    boolean isPrint;

    Instruction(String line) {
        line = line.replace(";", "").trim();
        if (line.startsWith("print")) {
            isPrint = true;
            left = line.substring(line.indexOf("(") + 1, line.indexOf(")"));
        } else {
            String[] parts = line.split("=");
            dest = parts[0].trim();
            String[] t = parts[1].trim().split(" ");
            left = t[0];
            if (t.length == 3) {
                op = t[1];
                right = t[2];
            }
        }
    }

    public String toString() {
        if (isPrint) {
            return "print(" + left + ");";
        }
        if (op == null) {
            return dest + " = " + left + ";";
        }
        return dest + " = " + left + " " + op + " " + right + ";";
    }
}

class Optimizer {
    private ArrayList<Instruction> code;

    public Optimizer(ArrayList<Instruction> code) {
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

    private boolean hasSideEffect(Instruction ins) {
        if (ins.isPrint || ins.left.contains("input")) {
            return true;
        }
        if (ins.op != null && ins.op.equals("/")) {
            return !(isNumber(ins.right) && !ins.right.equals("0"));
        }
        return false;
    }

    public boolean constantFolding() {
        boolean changed = false;
        for (int i = 0; i < code.size(); i++) {
            Instruction ins = code.get(i);
            if (ins.isPrint || ins.op == null) {
                continue;
            }
            boolean bothNumbers = isNumber(ins.left) && isNumber(ins.right);
            boolean divideByZero = ins.op.equals("/") && ins.right.equals("0");
            if (bothNumbers && !divideByZero) {
                int a = Integer.parseInt(ins.left);
                int b = Integer.parseInt(ins.right);
                int ans = 0;
                if (ins.op.equals("+")) ans = a + b;
                if (ins.op.equals("-")) ans = a - b;
                if (ins.op.equals("*")) ans = a * b;
                if (ins.op.equals("/")) ans = a / b;
                ins.left = String.valueOf(ans);
                ins.op = null;
                ins.right = null;
                changed = true;
            }
        }
        return changed;
    }

    public boolean copyPropagation() {
        boolean changed = false;
        for (int i = 0; i < code.size(); i++) {
            Instruction a = code.get(i);
            if (a.isPrint || a.op != null || a.left.contains("input") || a.left.equals(a.dest)) {
                continue;
            }
            for (int j = i + 1; j < code.size(); j++) {
                Instruction b = code.get(j);
                if (b.left.equals(a.dest)) {
                    b.left = a.left;
                    changed = true;
                }
                if (b.right != null && b.right.equals(a.dest)) {
                    b.right = a.left;
                    changed = true;
                }
                if (!b.isPrint && (b.dest.equals(a.dest) || b.dest.equals(a.left))) {
                    break;
                }
            }
        }
        return changed;
    }

    public boolean deadCodeElimination() {
        boolean changed = false;
        HashSet<String> live = new HashSet<>();

        for (int i = code.size() - 1; i >= 0; i--) {
            Instruction ins = code.get(i);
            boolean needed = ins.isPrint || hasSideEffect(ins) || live.contains(ins.dest);

            if (needed) {
                if (!ins.isPrint) {
                    live.remove(ins.dest);
                }
                addUse(live, ins.left);
                addUse(live, ins.right);
            } else {
                code.remove(i);
                changed = true;
            }
        }
        return changed;
    }

    private void addUse(HashSet<String> live, String s) {
        if (s != null && !isNumber(s) && !s.contains("input")) {
            live.add(s);
        }
    }

    public void printCode() {
        for (int i = 0; i < code.size(); i++) {
            System.out.println(code.get(i));
        }
    }
}

class MiniOptimizer {
    public static void main(String[] args) {
        ArrayList<Instruction> program = new ArrayList<>();
        program.add(new Instruction("a = 2 + 3;"));
        program.add(new Instruction("b = a;"));
        program.add(new Instruction("c = b * 4;"));
        program.add(new Instruction("unused = 99;"));
        program.add(new Instruction("print(c);"));

        Optimizer opt = new Optimizer(program);

        System.out.println("Original:");
        opt.printCode();

        int round = 1;
        boolean changed = true;
        while (changed) {
            boolean f = opt.constantFolding();
            boolean c = opt.copyPropagation();
            boolean d = opt.deadCodeElimination();
            changed = f || c || d;

            System.out.println("\nAfter round " + round + ":");
            opt.printCode();
            round++;
        }
    }
}