import java.util.*;

class Stmt {
    String type, text;
    List<Stmt> body = new ArrayList<>();
    List<Stmt> other = new ArrayList<>();

    Stmt(String type, String text) {
        this.type = type;
        this.text = text;
    }
}

class Instr {
    String type, text;
    int target = -1, falseTarget = -1;

    Instr(String type, String text) {
        this.type = type;
        this.text = text;
    }
}

class Block {
    int id;
    List<Instr> instructions = new ArrayList<>();
    Set<Integer> edges = new LinkedHashSet<>();
    Set<Integer> backEdges = new HashSet<>();

    Block(int id) {
        this.id = id;
    }
}

class SimpleParser {
    private List<String> tokens = new ArrayList<>();
    private int pos = 0;

    SimpleParser(String source) {
        java.util.regex.Matcher m =
                java.util.regex.Pattern.compile(
                        "==|!=|<=|>=|[a-zA-Z_]\\w*|\\d+|[^\\s]"
                ).matcher(source);

        while (m.find()) {
            tokens.add(m.group());
        }
    }

    private String peek() {
        return pos < tokens.size() ? tokens.get(pos) : "";
    }

    private String take() {
        if (pos >= tokens.size()) {
            throw new IllegalArgumentException("Unexpected end of input");
        }
        return tokens.get(pos++);
    }

    private void expect(String value) {
        if (!take().equals(value)) {
            throw new IllegalArgumentException("Expected " + value);
        }
    }

    private String expression(String end) {
        StringBuilder s = new StringBuilder();

        while (pos < tokens.size() && !peek().equals(end)) {
            if (s.length() > 0) {
                s.append(" ");
            }
            s.append(take());
        }
        return s.toString();
    }

    List<Stmt> parseProgram() {
        List<Stmt> result = new ArrayList<>();

        while (pos < tokens.size()) {
            result.add(parseStatement());
        }
        return result;
    }

    private List<Stmt> parseBlock() {
        expect("{");
        List<Stmt> result = new ArrayList<>();

        while (!peek().equals("}")) {
            if (pos >= tokens.size()) {
                throw new IllegalArgumentException("Missing }");
            }
            result.add(parseStatement());
        }
        expect("}");
        return result;
    }

    private Stmt parseStatement() {
        if (peek().equals("if") || peek().equals("while")) {
            String type = take();
            expect("(");
            String condition = expression(")");
            expect(")");

            Stmt s = new Stmt(type, condition);

            if (peek().equals("{")) {
                s.body.addAll(parseBlock());
            } else {
                s.body.add(parseStatement());
            }

            if (type.equals("if") && peek().equals("else")) {
                take();

                if (peek().equals("{")) {
                    s.other.addAll(parseBlock());
                } else {
                    s.other.add(parseStatement());
                }
            }
            return s;
        }

        String assignment = expression(";");
        expect(";");
        return new Stmt("assign", assignment);
    }
}

class ControlFlowGraph {
    private String source;
    private List<Instr> code = new ArrayList<>();
    private Map<Integer, Integer> labelAt = new HashMap<>();
    private int labelCount = 0;
    private List<Block> blocks = new ArrayList<>();

    public void read_CFG(String source) {
        this.source = source;
        code.clear();
        labelAt.clear();
        blocks.clear();
        labelCount = 0;
    }

    private int newLabel() {
        return labelCount++;
    }

    private void emitLabel(int label) {
        code.add(new Instr("LABEL", "L" + label + ":"));
    }

    private void emitJump(int label) {
        Instr i = new Instr("GOTO", "goto L" + label);
        i.target = label;
        code.add(i);
    }

    public void lowerProgram() {
        code.clear();
        labelAt.clear();
        blocks.clear();
        labelCount = 0;

        SimpleParser parser = new SimpleParser(source);
        lower(parser.parseProgram());

        for (int i = 0; i < code.size(); i++) {
            Instr ins = code.get(i);

            if (ins.type.equals("LABEL")) {
                int label = Integer.parseInt(
                        ins.text.substring(1, ins.text.length() - 1));
                labelAt.put(label, i);
            }
        }
    }

    private void lower(List<Stmt> list) {
        for (Stmt s : list) {
            if (s.type.equals("assign")) {
                code.add(new Instr("ASSIGN", s.text));
            } else if (s.type.equals("while")) {
                int start = newLabel();
                int body = newLabel();
                int end = newLabel();

                emitLabel(start);

                Instr branch = new Instr("IF", s.text);
                branch.target = body;
                branch.falseTarget = end;
                code.add(branch);

                emitLabel(body);
                lower(s.body);
                emitJump(start);
                emitLabel(end);
            } else if (s.type.equals("if")) {
                int yes = newLabel();
                int no = newLabel();
                int end = newLabel();

                Instr branch = new Instr("IF", s.text);
                branch.target = yes;
                branch.falseTarget = no;
                code.add(branch);

                emitLabel(yes);
                lower(s.body);
                emitJump(end);

                emitLabel(no);
                lower(s.other);
                emitLabel(end);
            }
        }
    }

    public void buildBlocks() {
        blocks.clear();

        if (code.isEmpty()) {
            return;
        }

        Set<Integer> leaders = new TreeSet<>();
        leaders.add(0);

        for (int i = 0; i < code.size(); i++) {
            Instr ins = code.get(i);

            if (ins.type.equals("LABEL")) {
                leaders.add(i);
            }

            if (ins.type.equals("GOTO") || ins.type.equals("IF")) {
                if (i + 1 < code.size()) {
                    leaders.add(i + 1);
                }
                leaders.add(labelAt.get(ins.target));

                if (ins.type.equals("IF")) {
                    leaders.add(labelAt.get(ins.falseTarget));
                }
            }
        }

        List<Integer> starts = new ArrayList<>(leaders);
        Map<Integer, Integer> owner = new HashMap<>();

        for (int i = 0; i < starts.size(); i++) {
            Block b = new Block(i);
            int end = i + 1 < starts.size()
                    ? starts.get(i + 1) : code.size();

            for (int j = starts.get(i); j < end; j++) {
                b.instructions.add(code.get(j));
                owner.put(j, i);
            }
            blocks.add(b);
        }

        for (int i = 0; i < blocks.size(); i++) {
            Block b = blocks.get(i);
            Instr last = b.instructions.get(b.instructions.size() - 1);

            if (last.type.equals("GOTO")) {
                b.edges.add(owner.get(labelAt.get(last.target)));
            } else if (last.type.equals("IF")) {
                b.edges.add(owner.get(labelAt.get(last.target)));
                b.edges.add(owner.get(labelAt.get(last.falseTarget)));
            } else if (i + 1 < blocks.size()) {
                b.edges.add(i + 1);
            }
        }
        markBackEdges();
    }

    private void markBackEdges() {
        int[] state = new int[blocks.size()];

        for (int i = 0; i < blocks.size(); i++) {
            if (state[i] == 0) {
                dfs(i, state);
            }
        }
    }

    private void dfs(int u, int[] state) {
        state[u] = 1;

        for (int v : blocks.get(u).edges) {
            if (state[v] == 1) {
                blocks.get(u).backEdges.add(v);
            } else if (state[v] == 0) {
                dfs(v, state);
            }
        }
        state[u] = 2;
    }

    public void displayBlocks() {
        for (Block b : blocks) {
            System.out.println("B" + b.id + ":");

            for (Instr i : b.instructions) {
                System.out.println("  " + i.text);
            }
            System.out.println();
        }
    }

    public void displayAdjacencyList() {
        System.out.println("Adjacency List:");

        for (Block b : blocks) {
            System.out.print("B" + b.id + " -> ");

            for (int v : b.edges) {
                System.out.print("B" + v);

                if (b.backEdges.contains(v)) {
                    System.out.print(" (back edge)");
                }
                System.out.print(" ");
            }
            System.out.println();
        }
    }

    public void optimize() {
        Optimizer optimizer = new Optimizer(code);
        optimizer.optimize();

        labelAt.clear();

        for (int i = 0; i < code.size(); i++) {
            Instr ins = code.get(i);

            if (ins.type.equals("LABEL")) {
                int n = Integer.parseInt(
                        ins.text.substring(1, ins.text.length() - 1));
                labelAt.put(n, i);
            }
        }
        buildBlocks();
    }
}

class Optimizer {
    private List<Instr> code;

    Optimizer(List<Instr> code) {
        this.code = code;
    }

    public boolean constantFolding() {
        boolean changed = false;

        for (Instr ins : code) {
            if (!ins.type.equals("ASSIGN")) continue;

            java.util.regex.Matcher m =
                    java.util.regex.Pattern.compile(
                            "^(\\w+\\s*=\\s*)(-?\\d+)\\s*([+\\-*/%])\\s*(-?\\d+)$"
                    ).matcher(ins.text);

            if (!m.matches()) continue;

            int a = Integer.parseInt(m.group(2));
            int b = Integer.parseInt(m.group(4));
            String op = m.group(3);

            if ((op.equals("/") || op.equals("%")) && b == 0)
                continue;

            int result;

            switch (op) {
                case "+": result = a + b; break;
                case "-": result = a - b; break;
                case "*": result = a * b; break;
                case "/": result = a / b; break;
                default: result = a % b;
            }

            ins.text = m.group(1) + result;
            changed = true;
        }
        return changed;
    }

    public boolean copyPropagation() {
        boolean changed = false;
        Map<String, String> copies = new HashMap<>();

        for (Instr ins : code) {
            if (ins.type.equals("LABEL") ||
                    ins.type.equals("IF") ||
                    ins.type.equals("GOTO")) {
                copies.clear();
                continue;
            }

            if (!ins.type.equals("ASSIGN")) continue;

            String[] parts = ins.text.split("=", 2);
            String target = parts[0].trim();
            String value = parts[1].trim();

            for (String name : copies.keySet()) {
                value = value.replaceAll(
                        "\\b" + java.util.regex.Pattern.quote(name) + "\\b",
                        java.util.regex.Matcher.quoteReplacement(copies.get(name))
                );
            }

            if (!ins.text.equals(target + " = " + value))
                changed = true;

            ins.text = target + " = " + value;

            Iterator<Map.Entry<String, String>> it =
                    copies.entrySet().iterator();

            while (it.hasNext()) {
                if (it.next().getValue().equals(target))
                    it.remove();
            }

            copies.remove(target);

            if (value.matches("\\w+|\\d+"))
                copies.put(target, value);
        }
        return changed;
    }

    public boolean deadCodeElimination() {
        boolean changed = false;

        for (int i = 0; i < code.size(); i++) {
            Instr ins = code.get(i);
            if (!ins.type.equals("ASSIGN")) continue;

            String[] parts = ins.text.split("=", 2);
            String target = parts[0].trim();
            String value = parts[1].trim();

            if (value.contains("/") || value.contains("%"))
                continue;

            boolean used = false;

            for (int j = 0; j < code.size(); j++) {
                if (i == j) continue;

                if (java.util.regex.Pattern.compile(
                        "\\b" + java.util.regex.Pattern.quote(target) + "\\b"
                ).matcher(code.get(j).text).find()) {
                    used = true;
                    break;
                }
            }

            if (!used) {
                code.remove(i);
                i--;
                changed = true;
            }
        }
        return changed;
    }

    public void optimize() {
        boolean changed;

        do {
            changed = false;

            if (constantFolding()) changed = true;
            if (copyPropagation()) changed = true;
            if (deadCodeElimination()) changed = true;
        } while (changed);
    }
}

public class Main {
    public static void main(String[] args) {
        String program =
                "x = 0;\n" +
                        "while (x < 10) {\n" +
                        "    if (x % 2 == 0)\n" +
                        "        y = x * 2;\n" +
                        "    else\n" +
                        "        y = x + 1;\n" +
                        "    x = x + 1;\n" +
                        "}";

        ControlFlowGraph cfg = new ControlFlowGraph();

        cfg.read_CFG(program);
        cfg.lowerProgram();
        cfg.buildBlocks();

        System.out.println("Before Optimization:");
        cfg.displayAdjacencyList();

        cfg.optimize();

        System.out.println("\nAfter Optimization:");
        cfg.displayBlocks();
        cfg.displayAdjacencyList();
    }
}
