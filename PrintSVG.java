import java.io.*;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;

public class PrintSVG {
    private BufferedWriter bw = null;
    private int indentAmount = 0;
    private String indent = "";
    private boolean startOfLine = true;

    private static final String[] indents = new String[256];

    public PrintSVG(File output) throws IOException{
        //ps = new PrintStream(new BufferedOutputStream(new FileOutputStream(output, false)), true, StandardCharsets.UTF_8);
        bw = Files.newBufferedWriter(output.toPath(), StandardCharsets.UTF_8);
    }

    private void updateIndent(){
        int indentAmount = this.indentAmount;
        if(indentAmount == indent.length() / 4) {
            return;
        }

        if(indentAmount < indents.length) {
            String indent = indents[indentAmount];
            if(indent == null) {
                indent = " ".repeat(indentAmount * 4);
                indents[indentAmount] = indent;
            }
            this.indent = indent;
        } else {
            this.indent = " ".repeat(indentAmount * 4);
        }
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
        checkClosed();
        if(startOfLine) bw.write(indent);
        bw.write(input);
        startOfLine = input.charAt(input.length()-1) == '\n';
    }

    public void print(int input) throws IOException{
        print(Integer.toString(input));
    }

    public void println(String input) throws IOException{
        checkClosed();
        if(startOfLine) bw.write(indent);
        bw.write(input);
        bw.newLine();
        startOfLine = true;
    }

    private void checkClosed() throws IOException{
        if(bw == null) throw new IOException("Attempting to write to a closed PrintSVG.");
    }

    public void println(int input) throws IOException{
        println(Integer.toString(input));
    }

    public void close() throws IOException{
        bw.close();
        bw = null;
    }
}