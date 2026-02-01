package DrDan.AnimalsGrow.example;

import DrDan.AnimalsGrow.annotation.ReturnIfNull;

/**
 * Example usage of the @ReturnIfNull annotation.
 * 
 * This file demonstrates how the annotation transforms code at compile time.
 */
public class ReturnIfNullExample {
    
    private static String getValue() {
        return "example";
    }
    
    private static Integer getNumber() {
        return null;
    }
    
    /**
     * Example: Before transformation
     * @ReturnIfNull String value = getValue();
     * 
     * After transformation:
     * String value = getValue();
     * if (value == null) return;
     */
    public void exampleUsage() {
        @ReturnIfNull
        String value = getValue();
        
        System.out.println("Value: " + value);
        
        // Multiple uses in same method
        @ReturnIfNull
        Integer number = getNumber();
        
        System.out.println("Number: " + number);
    }
    
    /**
     * Demonstrates with complex expressions
     */
    public void complexExample() {
        @ReturnIfNull
        String result = getValue() != null ? getValue().toUpperCase() : null;
        
        System.out.println("Result: " + result);
    }
}
