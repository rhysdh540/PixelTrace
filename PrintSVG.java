import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class PrintSVG {
    private PrintStream ps;
    private int indentAmount = 0;
    private String indent = "";
    private boolean startOfLine = true;

    public PrintSVG(File output) throws IOException {
        ps = new PrintStream(new BufferedOutputStream(Files.newOutputStream(output.toPath())), true, StandardCharsets.UTF_8);
    }

    private void updateIndent(){
        indent = "    ".repeat(indentAmount);
    }

    public void moreIndent(){
        if(indentAmount == Integer.MAX_VALUE) return;
        indentAmount++;
        updateIndent();
    }

    public void lessIndent(){
        if(indentAmount == 0) return;
        indentAmount--;
        updateIndent();
    }

    public void print(String input) throws IOException{
        if(ps == null) throw new IOException("Attempting to write to a closed PrintSVG.");
        if(startOfLine) ps.print(indent);
        ps.print(input);
        startOfLine = input.charAt(input.length() - 1) == '\n';
    }

    public void println(String input) throws IOException{
        print(input + "\n");
    }

    public void close(){
        ps.close();
        ps = null;
    }
}