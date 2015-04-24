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
 * <build>
 *   <plugins>
 *      ...
 *      <plugin>
 *        <groupId>org.apache.maven.plugins</groupId>
 *        <artifactId>maven-javadoc-plugin</artifactId>
 *        <version>2.10.2</version>
 *        <configuration>
 *          <doclet>ch.zhaw.ficore.p2abc.FicoreUnitTestDoclet</doclet>
 *          <docletPath>${project.parent.basedir}/documenter/target/documenter.jar</docletPath>
 *          <useStandardDocletOptions>false</useStandardDocletOptions>
 *          <destDir>unit-test</destDir>
 *          <name>Unit Test</name>
 *          <description>Unit Test Plan.</description>
 *        </configuration>
 *      </plugin>
 *   </plugins>
 * </build>
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

import org.apache.commons.lang3.builder.HashCodeBuilder;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.RootDoc;
import com.sun.javadoc.Tag;


public class FicoreUnitTestDoclet {

    private static class FicoreTag {
        public enum CustomTag {
            TEST_FEATURE,
            TEST_INITIAL_CONDITION,
            TEST_TEST,
            TEST_EXPECTED_OUTCOME,
            REST_PATH,
            REST_METHOD,
            REST_DESCRIPTION,
            REST_PATH_PARAM,
            REST_POST_PARAM,
            REST_RESPONSE,
        }

        private CustomTag tag;
        private String name;
        private String markdown;
        private boolean isParamLike;
        private boolean withParagraph;

        public FicoreTag(CustomTag tag, String name, String markdown,
                boolean isParamLike, boolean withParagraph) {
            super();
            this.tag = tag;
            this.name = name;
            this.markdown = markdown;
            this.isParamLike = isParamLike;
            this.withParagraph = withParagraph;
        }

        public CustomTag getCustomTag() {
            return tag;
        }

        public String getName() {
            return name;
        }

        public String getMarkdown() {
            return markdown;
        }

        public boolean isParamLike() {
            return isParamLike;
        }

        public boolean wantsParagraph() {
            return withParagraph;
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder()
                .append(tag)
                .append(name)
                .append(markdown)
                .append(isParamLike)
                .append(withParagraph)
                .hashCode();
        }
    }

    private static String makeHeading(String header, int level) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < level; i++) {
            sb.append('=');
        }
        sb.append(header);
        for (int i = 0; i < level; i++) {
            sb.append('=');
        }

        return sb.toString();
    }

    @SuppressWarnings("unused")
    private static String makeItalic(String header) {
        StringBuilder sb = new StringBuilder();

        sb.append("''");
        sb.append(header);
        sb.append("''");

        return sb.toString();
    }

    private static String makeBold(String header) {
        StringBuilder sb = new StringBuilder();

        sb.append("'''");
        sb.append(header);
        sb.append("'''");

        return sb.toString();
    }

    private static final Map<String, FicoreTag> nameToTag;
    private static final Map<FicoreTag.CustomTag, FicoreTag> enumToTag;

    private static final Charset charset = StandardCharsets.UTF_8;
    private static final String PARAM_INDENT = ":";
    private static final String PARAM_DASH = " &mdash; ";
    private static final String LINE_BREAK = "<br/>";

    static {
        nameToTag = new HashMap<>();
        nameToTag.put("fiware-unit-test-feature", new FicoreTag(FicoreTag.CustomTag.TEST_FEATURE,  "fiware-unit-test-feature", makeHeading("Tested Features", 3), false, true));
        nameToTag.put("fiware-unit-test-initial-condition", new FicoreTag(FicoreTag.CustomTag.TEST_INITIAL_CONDITION, "fiware-unit-test-initial-condition", makeHeading("Initial Condition of Test", 3), false, true));
        nameToTag.put("fiware-unit-test-test", new FicoreTag(FicoreTag.CustomTag.TEST_TEST, "fiware-unit-test-test", makeHeading("Test", 3), false, true));
        nameToTag.put("fiware-unit-test-expected-outcome", new FicoreTag(FicoreTag.CustomTag.TEST_EXPECTED_OUTCOME, "fiware-unit-test-expected-outcome", makeHeading("Expected Outcome", 3), false, true));
        nameToTag.put("fiware-rest-path", new FicoreTag(FicoreTag.CustomTag.REST_PATH, "fiware-rest-path", makeBold("REST Path:"), false, false));
        nameToTag.put("fiware-rest-method", new FicoreTag(FicoreTag.CustomTag.REST_METHOD, "fiware-rest-method", makeBold("Method:"), false, false));
        nameToTag.put("fiware-rest-description", new FicoreTag(FicoreTag.CustomTag.REST_DESCRIPTION, "fiware-rest-description", makeBold("Description:"), false, false));
        nameToTag.put("fiware-rest-path-param", new FicoreTag(FicoreTag.CustomTag.REST_PATH_PARAM, "fiware-rest-path-param", makeBold("Parameters:"), true, true));
        nameToTag.put("fiware-rest-post-param", new FicoreTag(FicoreTag.CustomTag.REST_POST_PARAM, "fiware-rest-post-param", makeBold("POST Parameters:"), true, true));
        nameToTag.put("fiware-rest-response", new FicoreTag(FicoreTag.CustomTag.REST_RESPONSE, "fiware-rest-response", makeBold("HTTP responses:"), true, true));

        enumToTag = new HashMap<>();
        enumToTag.put(FicoreTag.CustomTag.TEST_FEATURE, nameToTag.get("fiware-unit-test-feature"));
        enumToTag.put(FicoreTag.CustomTag.TEST_INITIAL_CONDITION, nameToTag.get("fiware-unit-test-initial-condition"));
        enumToTag.put(FicoreTag.CustomTag.TEST_TEST, nameToTag.get("fiware-unit-test-test"));
        enumToTag.put(FicoreTag.CustomTag.TEST_EXPECTED_OUTCOME, nameToTag.get("fiware-unit-test-expected-outcome"));
        enumToTag.put(FicoreTag.CustomTag.REST_PATH, nameToTag.get("fiware-rest-path"));
        enumToTag.put(FicoreTag.CustomTag.REST_METHOD, nameToTag.get("fiware-rest-method"));
        enumToTag.put(FicoreTag.CustomTag.REST_DESCRIPTION, nameToTag.get("fiware-rest-description"));
        enumToTag.put(FicoreTag.CustomTag.REST_PATH_PARAM, nameToTag.get("fiware-rest-path-param"));
        enumToTag.put(FicoreTag.CustomTag.REST_POST_PARAM, nameToTag.get("fiware-rest-post-param"));
        enumToTag.put(FicoreTag.CustomTag.REST_RESPONSE, nameToTag.get("fiware-rest-response"));
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

                for (FicoreTag.CustomTag cTag : FicoreTag.CustomTag.values()) {
                    FicoreTag ficoreTag = enumToTag.get(cTag);
                    Tag[] theseTags = methods[j].tags(ficoreTag.getName());

                    if (theseTags.length > 0) {
                        if (!fileOpened) {
                            writer = openMarkdownFile(classes[i].qualifiedName());
                            fileOpened = true;
                        }

                        if (!methodNamePrinted) {
                            String what = null;
                            if (isTestClass(classes[i])) {
                                what = "Unit Test";
                            } else {
                                what = "REST method";
                            }
                            writer.println(makeHeading(what + " " + methods[j].name(), 2));
                            writer.println();
                            methodNamePrinted = true;
                        }

                        if (ficoreTag.wantsParagraph()) {
                            writer.println(ficoreTag.getMarkdown() + "\n");
                        } else {
                            writer.print(ficoreTag.getMarkdown() + " ");
                        }

                        if (ficoreTag.getCustomTag() == FicoreTag.CustomTag.TEST_FEATURE) {
                            for (int k = 0; k < theseTags.length; k++) {
                                writer.println("* [[" + theseTags[k].text() + "]]");
                            }
                            writer.println();
                        } else {
                            for (int k = 0; k < theseTags.length; k++) {
                                // The "text" is a multi-line string where
                                // all lines except the first begin with
                                // spaces. These need to be removed.
                                String[] texts = theseTags[k].text().split("\n");
                                if (ficoreTag.isParamLike()) {
                                    for (int l = 0; l < texts.length; l++) {
                                        writer.print(PARAM_INDENT);
                                        writer.print(makeParam(texts[l].trim()));
                                        writer.println(LINE_BREAK);
                                    }
                                } else {
                                    for (int l = 0; l < texts.length; l++) {
                                        writer.println(texts[l].trim());
                                    }
                                }
                            }
                        }
                        writer.println();
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


    private static String makeParam(String text) {
        int spacePosition = text.indexOf(' ');
        if (spacePosition == -1) {
            return text;
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append(text.substring(0, spacePosition));
            sb.append(PARAM_DASH);
            sb.append(text.substring(spacePosition + 1));
            return sb.toString();
        }
    }

    private static boolean isTestClass(ClassDoc classDoc) {
        return classDoc.qualifiedName().endsWith("Test");
    }
}
