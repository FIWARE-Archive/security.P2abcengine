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
 */
package ch.zhaw.ficore.p2abc;

import java.util.HashMap;
import java.util.Map;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.RootDoc;
import com.sun.javadoc.Tag;


public class FicoreUnitTestDoclet {

    private enum UnitTestTag {
        NAME,
        INITIAL_CONDITION,
        TEST,
        EXPECTED_OUTCOME,
    }
    
    private static final Map<UnitTestTag, String> tagToName;
    private static final Map<UnitTestTag, String> tagToMarkdown;
    
    static {
        tagToName = new HashMap<>();
        tagToName.put(UnitTestTag.NAME, "fiware-unit-test-name");
        tagToName.put(UnitTestTag.INITIAL_CONDITION, "fiware-unit-test-initial-condition");
        tagToName.put(UnitTestTag.TEST, "fiware-unit-test-test");
        tagToName.put(UnitTestTag.EXPECTED_OUTCOME, "fiware-unit-test-expected-outcome");
        
        tagToMarkdown = new HashMap<>();
        tagToMarkdown.put(UnitTestTag.NAME, "'''Name'''");
        tagToMarkdown.put(UnitTestTag.INITIAL_CONDITION, "'''Initial Condition of Test'''");
        tagToMarkdown.put(UnitTestTag.TEST, "'''Test'''");
        tagToMarkdown.put(UnitTestTag.EXPECTED_OUTCOME, "'''Expected Outcome'''");        
    }
    
    public static boolean start(RootDoc root){ 
        writeContents(root.classes());
        return true;
    }

    public static void writeContents(ClassDoc[] classes) {
        for (int i = 0; i < classes.length; i++) {
            boolean classNamePrinted = false;
            MethodDoc[] methods = classes[i].methods();
            for (int j = 0; j < methods.length; j++) {
                boolean methodNamePrinted = false;
                for (UnitTestTag tag : UnitTestTag.values()) { 
                    Tag[] tags = methods[j].tags(tagToName.get(tag));
                    if (tags.length > 0) {
                        if (!classNamePrinted) {
                            System.out.println("===" + classes[i].name() + "===\n\n");
                            classNamePrinted = true;
                        }
                        if (!methodNamePrinted) {
                            System.out.println("====" + methods[j].name() + "=====\n");
                            methodNamePrinted = true;
                        }
                        for (int k=0; k < tags.length; k++) {
                            System.out.println(tagToMarkdown.get(tag) + ": " 
                                    + tags[k].text());
                        }
                    }
                } 
            }
        }
    }
}