import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class PrintSVG {
    private PrintStream ps;
    private int indentAmount = 0;
    private String indent = "";
    private boolean startOfLine = true;

    private static final String[] indents = new String[100];

    public PrintSVG(File output) throws IOException {
        ps = new PrintStream(new BufferedOutputStream(Files.newOutputStream(output.toPath())), false, StandardCharsets.UTF_8);
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
                indents[indentAmount] = (this.indent = indent);
            }
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
        if(startOfLine) ps.print(indent);
        ps.print(input);
        startOfLine = input.charAt(input.length() - 1) == '\n';
    }

    public void println(String input) throws IOException{
        checkClosed();
        if(startOfLine) ps.print(indent);
        ps.println(input);
        startOfLine = true;
    }

    private void checkClosed() throws IOException{
        if(ps == null) throw new IOException("Attempting to write to a closed PrintSVG.");
    }

    public void close(){
        ps.close();
        ps = null;
    }
}