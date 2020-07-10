/*
 * Copyright 2005 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.drools.modelcompiler;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.drools.compiler.Address;
import org.drools.compiler.Person;
import org.junit.Before;
import org.junit.Test;
import org.kie.api.KieBase;
import org.kie.api.runtime.KieSession;

import static org.junit.Assert.assertEquals;

public class MetricModelCompilerTest extends BaseModelTest {

    private static final int LOOP = 1;

    public MetricModelCompilerTest(RUN_TYPE testRunType) {
        super(testRunType);
    }

    @Before
    public void setup() {
        System.setProperty("drools.metric.logger.enabled", "true");
        System.setProperty("drools.metric.logger.threshold", "-1");
    }

    private void dump(KieBase kbase) {
//        System.out.println("===== ReteDumper");
//        ReteDumper dumper = new ReteDumper();
//        dumper.setNodeInfoOnly(true);
//        dumper.dump(kbase);
//        System.out.println("-----");
//        dumper.dumpAssociatedRules(kbase);
//        System.out.println();
    }

    private int runRules(KieBase kbase, List<Person> personList) {
        int fired = -1;
        for (int i = 0; i < LOOP; i++) {
            KieSession ksession = kbase.newKieSession();
            personList.stream().forEach(ksession::insert);

            long start = System.nanoTime();
            fired = ksession.fireAllRules();
            System.out.println("  fired = " + fired);
            System.out.println("  total elapsedMicro : " + (System.nanoTime() - start) / 1000);
            ksession.dispose();
        }
        return fired;
    }

    @Test
    public void testJoin() {

        String str =
                "import " + Address.class.getCanonicalName() + "\n" +
                     "import " + Person.class.getCanonicalName() + "\n" +
                     "rule R1\n" +
                     "when\n" +
                     "  $p1 : Person(age > 5)\n" +
                     "  $p2 : Person(age > $p1.age)\n" +
                     "then\n" +
                     "end\n" +
                     "rule R2\n" +
                     "when\n" +
                     "  $p1 : Person(age > 5)\n" +
                     "  $p2 : Person(age < $p1.age)\n" +
                     "then\n" +
                     "end\n";

        KieBase kbase = getKieSession(str).getKieBase();

        dump(kbase);

        List<Person> personList = IntStream.range(0, 100)
                                           .mapToObj(i -> new Person("John" + i, i))
                                           .collect(Collectors.toList());

        int fired = runRules(kbase, personList);
        assertEquals(9306, fired);
    }

    @Test
    public void testFrom() {

        String str =
                "import " + Address.class.getCanonicalName() + "\n" +
                     "import " + Person.class.getCanonicalName() + "\n" +
                     "rule R1\n" +
                     "when\n" +
                     "  $p : Person()\n" +
                     "  $a1 : Address() from $p.addresses\n" +
                     "  $a2 : Address(suburb != \"XYZ\", zipCode == $a1.zipCode, this != $a1) from $p.addresses\n" +
                     "then\n" +
                     "end\n";

        KieBase kbase = getKieSession(str).getKieBase();

        dump(kbase);

        List<Person> personList = IntStream.range(0, 100)
                                           .mapToObj(i -> new Person("John" + i, i))
                                           .map(p -> {
                                               p.addAddress(new Address("StreetX" + p.getAge(), "ABC", "111"));
                                               p.addAddress(new Address("StreetY" + p.getAge(), "ABC", "111"));
                                               p.addAddress(new Address("StreetZ" + p.getAge(), "ABC", "999"));
                                               return p;
                                           })
                                           .collect(Collectors.toList());

        int fired = runRules(kbase, personList);
    }

    @Test
    public void testNot() {

        String str =
                "import " + Address.class.getCanonicalName() + "\n" +
                     "import " + Person.class.getCanonicalName() + "\n" +
                     "rule R1\n" +
                     "when\n" +
                     "  $p1 : Person()\n" +
                     "  $p2 : Person(this != $p1)\n" +
                     "  not Person(this != $p1, this != $p2, (age == $p1.age || age == $p2.age))\n" +
                     "then\n" +
                     "end\n";

        KieBase kbase = getKieSession(str).getKieBase();

        dump(kbase);

        List<Person> personList = IntStream.range(0, 100)
                                           .mapToObj(i -> new Person("John" + i, i))
                                           .collect(Collectors.toList());

        int fired = runRules(kbase, personList);
        assertEquals(9900, fired);
    }

    @Test
    public void testExists() {

        String str =
                "import " + Address.class.getCanonicalName() + "\n" +
                     "import " + Person.class.getCanonicalName() + "\n" +
                     "rule R1\n" +
                     "when\n" +
                     "  $p1 : Person()\n" +
                     "  $p2 : Person(this != $p1)\n" +
                     "  exists Person(this != $p1, this != $p2, age != $p1.age, age != $p2.age)\n" +
                     "then\n" +
                     "end\n";

        KieBase kbase = getKieSession(str).getKieBase();

        dump(kbase);

        List<Person> personList = IntStream.range(0, 100)
                                           .mapToObj(i -> new Person("John" + i, i))
                                           .collect(Collectors.toList());

        int fired = runRules(kbase, personList);
        assertEquals(9900, fired);
    }

    @Test
    public void testAccumulate() {

        String str =
                "import " + Address.class.getCanonicalName() + "\n" +
                     "import " + Person.class.getCanonicalName() + "\n" +
                     "rule R1\n" +
                     "when\n" +
                     "  $p1 : Person()\n" +
                     "  accumulate ( $p2: Person ( getName().startsWith(\"J\"), this != $p1);\n" +
                     "                $average : average($p2.getAge());\n" +
                     "                $average > $p1.age, $average > 30\n" +
                     "             )\n" +
                     "then\n" +
                     //                     "  System.out.println(\"$p1.name = \" + $p1.getName() + \", other's $average = \" + $average);\n" +
                     "end\n";

        KieBase kbase = getKieSession(str).getKieBase();

        dump(kbase);

        List<Person> personList = IntStream.range(0, 100)
                                           .mapToObj(i -> new Person("John" + i, i))
                                           .collect(Collectors.toList());

        int fired = runRules(kbase, personList);
        assertEquals(50, fired);
    }

    @Test
    public void testFromAccumulate() {

        String str =
                "import " + Address.class.getCanonicalName() + "\n" +
                     "import " + Person.class.getCanonicalName() + "\n" +
                     "rule R1\n" +
                     "when\n" +
                     "  $p1 : Person()\n" +
                     "  $average  : Double(this > $p1.age, this > 30) from accumulate ( $p2: Person ( getName().startsWith(\"J\"), this != $p1);\n" +
                     "                average($p2.getAge())\n" +
                     "             )\n" +
                     "then\n" +
                     //                     "  System.out.println(\"$p1.name = \" + $p1.getName() + \", other's $average = \" + $average);\n" +
                     "end\n";

        KieBase kbase = getKieSession(str).getKieBase();

        dump(kbase);

        List<Person> personList = IntStream.range(0, 100)
                                           .mapToObj(i -> new Person("John" + i, i))
                                           .collect(Collectors.toList());

        int fired = runRules(kbase, personList);
        assertEquals(50, fired);
    }

    @Test
    public void testEval() {

        String str =
                "import " + Address.class.getCanonicalName() + "\n" +
                     "import " + Person.class.getCanonicalName() + "\n" +
                     "rule R1\n" +
                     "dialect \"mvel\"\n" +
                     "when\n" +
                     "  $p1 : Person()\n" +
                     "  eval($p1.age > 90)" +
                     "then\n" +
                     //                                          "  System.out.println(\"$p1.name = \" + $p1.getName());\n" +
                     "end\n";

        KieBase kbase = getKieSession(str).getKieBase();

        dump(kbase);

        List<Person> personList = IntStream.range(0, 100)
                                           .mapToObj(i -> new Person("John" + i, i))
                                           .collect(Collectors.toList());

        int fired = runRules(kbase, personList);
        assertEquals(9, fired);
    }
    
    @Test
    public void testComplexEval() throws Exception {
        String drl =
                "rule R1 when\n" +
                     "    $s : String()\n" +
                     "    Integer()\n" +
                     "    not( ( eval($s.length() < 2) and (eval(true) or eval(false))))\n" +
                     "then \n" +
                     "end\n";

        KieSession kieSession = getKieSession(drl);

        kieSession.insert(42);
        kieSession.insert("test");
        assertEquals(1, kieSession.fireAllRules());
    }
}
