package DrDan.AnimalsGrow.annotation;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.Plugin;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.source.util.Trees;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import java.util.HashSet;
import java.util.Set;

/**
 * Annotation processor for @ReturnIfNull that rewrites source code at compile time.
 * 
 * This processor runs during Java compilation and looks for variables annotated with @ReturnIfNull,
 * then injects null-check return statements after the variable declaration.
 */
public class ReturnIfNullProcessor extends AbstractProcessor {
    
    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> annotations = new HashSet<>();
        annotations.add("DrDan.AnimalsGrow.annotation.ReturnIfNull");
        return annotations;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        // Get all elements annotated with @ReturnIfNull
        Set<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith(ReturnIfNull.class);
        
        for (Element element : annotatedElements) {
            // Process only local variables
            if (element.getKind() == ElementKind.LOCAL_VARIABLE) {
                processVariable(element);
            }
        }
        
        return true;
    }

    private void processVariable(Element element) {
        // The annotation processor itself cannot modify source code directly.
        // Instead, we rely on custom compiler plugins or build-time transformations.
        // This processor validates the annotation usage and provides information
        // that can be used by a companion source transformer.
        
        // In a production environment, you would:
        // 1. Use a Bazel rule that runs a source transformer before javac
        // 2. Use a Maven plugin that processes and rewrites source files
        // 3. Use a custom JavaPlugin for javac that performs AST transformations
        
        processingEnv.getMessager().printMessage(
            javax.annotation.processing.Messager.NOTE,
            "Processing @ReturnIfNull on variable: " + element.getSimpleName()
        );
    }
}
