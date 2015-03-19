/** A FIWARE unit test documenting tool.
 * 
 * Copyright (c) 2015 Zürcher Hochschule der Angewandeten Wissenschaften.
 * All rights reserved.
 * 
 * Use as follows:
 * 
 * javadoc -doclet ch.zhaw.ficore.p2abc.FicoreUnitTestDoclet \
 *   -docletpath /path/to/root/of/classes/or/jar \
 *   -classpath /path/to/java/home/lib/tools.jar \
 *   path/to/your/java/files
 *   
 * Or, if you use maven:
 * 
 * 
 */
package ch.zhaw.ficore.p2abc;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.RootDoc;
import com.sun.javadoc.Tag;


public class FicoreUnitTestDoclet {

    private enum UnitTestTag {
        FEATURE,
        INITIAL_CONDITION,
        TEST,
        EXPECTED_OUTCOME,
    }
    
    private static final Map<UnitTestTag, String> tagToName;
    private static final Map<UnitTestTag, String> tagToMarkdown;
    
    private static final Charset charset = StandardCharsets.UTF_8;

    static {
        tagToName = new HashMap<>();
        tagToName.put(UnitTestTag.FEATURE, "fiware-unit-test-feature");
        tagToName.put(UnitTestTag.INITIAL_CONDITION, "fiware-unit-test-initial-condition");
        tagToName.put(UnitTestTag.TEST, "fiware-unit-test-test");
        tagToName.put(UnitTestTag.EXPECTED_OUTCOME, "fiware-unit-test-expected-outcome");
        
        tagToMarkdown = new HashMap<>();
        tagToMarkdown.put(UnitTestTag.FEATURE, "===Tested Features===");
        tagToMarkdown.put(UnitTestTag.INITIAL_CONDITION, "===Initial Condition of Test===");
        tagToMarkdown.put(UnitTestTag.TEST, "===Test===");
        tagToMarkdown.put(UnitTestTag.EXPECTED_OUTCOME, "===Expected Outcome===");
    }
    
    public static boolean start(RootDoc root) throws IOException{ 
        writeContents(root.classes());
        return true;
    }

    private static PrintWriter openMarkdownFile(String className) throws IOException {
        Path p = FileSystems.getDefault().getPath(className + ".md");
        BufferedWriter w =Files.newBufferedWriter(p, charset); 
        return new PrintWriter(w);
    }
    
    public static void writeContents(ClassDoc[] classes) throws IOException {
        for (int i = 0; i < classes.length; i++) {
            PrintWriter writer = null;
            boolean fileOpened = false;
            
            MethodDoc[] methods = classes[i].methods();
            for (int j = 0; j < methods.length; j++) {
                boolean methodNamePrinted = false;
                
                for (UnitTestTag tag : UnitTestTag.values()) { 
                    Tag[] tags = methods[j].tags(tagToName.get(tag));
                    
                    if (tags.length > 0) {
                        if (!fileOpened) {
                            writer = openMarkdownFile(classes[i].qualifiedName());
                            fileOpened = true;
                        }

                        if (!methodNamePrinted) {
                            writer.println("== Unit Test " + methods[j].name() + " ==\n");
                            methodNamePrinted = true;
                        }
                        
                        if (tag == UnitTestTag.FEATURE) {
                            writer.println(tagToMarkdown.get(tag) + "\n");
                            for (int k = 0; k < tags.length; k++) {
                                writer.println("* [[" + tags[k].text() + "]]");
                            }
                            writer.println();
                        } else {
                            for (int k = 0; k < tags.length; k++) {
                                // The "text" is a multi-line string where
                                // all lines except the first begin with
                                // spaces. These need to be removed.
                                String[] texts = tags[k].text().split("\n");
                                writer.println(tagToMarkdown.get(tag) + "\n");
                                for (int l = 0; l < texts.length; l++) {
                                    writer.println(texts[l].trim());
                                }
                            }
                            writer.println();
                        }
                    }
                }
                
                if (methodNamePrinted) {
                    writer.println();
                }
            }
            
            if (fileOpened) {
                writer.close();
                fileOpened = false;
            }
        }
    }
}
