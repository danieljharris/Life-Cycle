package DrDan.AnimalsGrow.annotation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Build-time source transformer that rewrites @ReturnIfNull annotations.
 * 
 * This is meant to be run as a pre-compilation step in Bazel or Maven
 * to transform source files before they're compiled by javac.
 * 
 * Pattern to match:
 * @ReturnIfNull [Type] [varName] = [expr];
 * 
 * Transforms to:
 * [Type] [varName] = [expr];
 * if ([varName] == null) return;
 */
public class ReturnIfNullSourceTransformer {
    
    private static final Pattern ANNOTATION_PATTERN = Pattern.compile(
        "@ReturnIfNull\\s+([\\w<>\\[\\],\\.\\s]+)\\s+(\\w+)\\s*=\\s*(.+?);",
        Pattern.MULTILINE | Pattern.DOTALL
    );
    
    private static final Pattern SINGLE_LINE_PATTERN = Pattern.compile(
        "@ReturnIfNull\\s+([\\w<>\\[\\],\\.\\s]+)\\s+(\\w+)\\s*=\\s*([^;]+);",
        Pattern.MULTILINE
    );
    
    /**
     * Transform a Java source file by rewriting @ReturnIfNull annotations
     */
    public static String transformSource(String source) {
        // Match @ReturnIfNull annotations and replace them
        Matcher matcher = SINGLE_LINE_PATTERN.matcher(source);
        StringBuffer sb = new StringBuffer();
        
        while (matcher.find()) {
            String type = matcher.group(1).trim();
            String varName = matcher.group(2).trim();
            String expr = matcher.group(3).trim();
            
            // Build the replacement: declaration + null check
            String replacement = type + " " + varName + " = " + expr + ";\n" +
                                "if (" + varName + " == null) return;";
            
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        
        return sb.toString();
    }
    
    /**
     * Transform a source file on disk
     */
    public static void transformFile(Path sourceFile) throws IOException {
        String content = Files.readString(sourceFile);
        String transformed = transformSource(content);
        
        if (!content.equals(transformed)) {
            Files.writeString(sourceFile, transformed);
            System.out.println("Transformed: " + sourceFile);
        }
    }
    
    public static void main(String[] args) {
        // Command-line usage for testing:
        // java ReturnIfNullSourceTransformer <source-file>
        if (args.length < 1) {
            System.err.println("Usage: java ReturnIfNullSourceTransformer <source-file>");
            System.exit(1);
        }
        
        try {
            transformFile(Path.of(args[0]));
        } catch (IOException e) {
            System.err.println("Error transforming file: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
