import java.util.ArrayList;
class Block {
    int id;
    ArrayList<String> instructions = new ArrayList<>();
    ArrayList<Integer> next = new ArrayList<>();

    Block(int id) {
        this.id = id;
    }
}

class ControlFlowGraph {
    private ArrayList<Block> blocks = new ArrayList<>();
    private String[] lines;
    private int pos = 0;

    public void read_CFG(String[] program) {
        lines = program;
        pos = 0;
        blocks.clear();
    }

    private int newBlock() {
        blocks.add(new Block(blocks.size()));
        return blocks.size() - 1;
    }

    private void addEdge(int from, int to) {
        blocks.get(from).next.add(to);
    }

    private int statement(int cur) {
        String line = lines[pos].trim();
        pos++;

        if (line.startsWith("while (")) {
            boolean hasBrace = line.endsWith("{");
            String cond = line.replace("{", "").trim();

            int header = newBlock();
            addEdge(cur, header);
            blocks.get(header).instructions.add(cond);

            int bodyStart = newBlock();
            addEdge(header, bodyStart);
            int bodyEnd = body(bodyStart, hasBrace);

            addEdge(bodyEnd, header);

            int exit = newBlock();
            addEdge(header, exit);
            return exit;

        } else if (line.startsWith("if (")) {
            boolean hasBrace = line.endsWith("{");
            String cond = line.replace("{", "").trim();

            blocks.get(cur).instructions.add(cond);

            int thenStart = newBlock();
            addEdge(cur, thenStart);
            int thenEnd = body(thenStart, hasBrace);

            boolean hasElse = pos < lines.length && lines[pos].trim().startsWith("else");
            int join;

            if (hasElse) {
                boolean elseBrace = lines[pos].trim().endsWith("{");
                pos++;
                int elseStart = newBlock();
                addEdge(cur, elseStart);
                int elseEnd = body(elseStart, elseBrace);
                join = newBlock();
                addEdge(thenEnd, join);
                addEdge(elseEnd, join);
            } else {
                join = newBlock();
                addEdge(cur, join);
                addEdge(thenEnd, join);
            }
            return join;
        } else {
            blocks.get(cur).instructions.add(line);
            return cur;
        }
    }

    private int body(int cur, boolean hasBrace) {
        if (hasBrace) {
            while (pos < lines.length && !lines[pos].trim().equals("}")) {
                cur = statement(cur);
            }
            pos++;
        } else {
            cur = statement(cur);
        }
        return cur;
    }

    public void buildGraph() {
        int cur = newBlock();
        while (pos < lines.length) {
            cur = statement(cur);
        }
    }

    public void displayBlocks() {
        for (int i = 0; i < blocks.size(); i++) {
            Block b = blocks.get(i);
            System.out.println("B" + b.id + ":");
            if (b.instructions.size() == 0) {
                System.out.println("  (empty)");
            }
            for (int j = 0; j < b.instructions.size(); j++) {
                System.out.println("  " + b.instructions.get(j));
            }
        }
    }

    public void displayAdjacencyList() {
        for (int i = 0; i < blocks.size(); i++) {
            Block b = blocks.get(i);
            String result = "B" + b.id + " -> ";

            if (b.next.size() == 0) {
                result = result + "(end)";
            }
            for (int j = 0; j < b.next.size(); j++) {
                int to = b.next.get(j);
                result = result + "B" + to;
                if (to <= b.id) {
                    result = result + " (back edge)";
                }
                result = result + "  ";
            }
            System.out.println(result);
        }
    }
}

class Cfg {
    public static void main(String[] args) {
        String[] program = {
                "x = 0;",
                "while (x < 10) {",
                "if (x % 2 == 0)",
                "y = x * 2;",
                "else",
                "y = x + 1;",
                "x = x + 1;",
                "}"
        };

        ControlFlowGraph cfg = new ControlFlowGraph();
        cfg.read_CFG(program);
        cfg.buildGraph();

        System.out.println("Basic Blocks:");
        cfg.displayBlocks();

        System.out.println("\nAdjacency List:");
        cfg.displayAdjacencyList();
    }
}
