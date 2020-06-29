/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.drools.benchmarks.session;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.drools.benchmarks.common.AbstractBenchmark;
import org.drools.benchmarks.common.DRLProvider;
import org.drools.benchmarks.common.providers.RulesWithJoinsProvider;
import org.drools.benchmarks.common.util.BuildtimeUtil;
import org.drools.benchmarks.common.util.RuntimeUtil;
import org.drools.benchmarks.model.A;
import org.drools.benchmarks.model.B;
import org.drools.benchmarks.model.C;
import org.drools.benchmarks.model.D;
import org.drools.core.impl.StatefulKnowledgeSessionImpl;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

//@Warmup(iterations = 2000)
@Warmup(iterations = 1000)
@Measurement(iterations = 500)
public class FireOnlyWithJoinsBenchmark extends AbstractBenchmark {

    @Param({"32"})
    private int rulesNr;

    //    @Param({"10", "15"})
    @Param({"15"})
    private int factsNr;

    //    @Param({"2", "3"})
    @Param({"3"})
    private int joinsNr;

    @Param({"true", "false"})
    private boolean perfLog;

    @Setup
    public void setupKieBase() {
        System.out.println();
        System.out.println("drools.performance.logger.enabled = " + System.getProperty("drools.performance.logger.enabled"));

        System.setProperty("drools.performance.logger.enabled", String.valueOf(perfLog));
        System.setProperty("drools.performance.logger.threshold", "-1");

        System.out.println("drools.performance.logger.enabled = " + System.getProperty("drools.performance.logger.enabled"));

        final DRLProvider drlProvider = new RulesWithJoinsProvider(joinsNr, false, true);

        kieBase = BuildtimeUtil.createKieBaseFromDrl(drlProvider.getDrl(rulesNr));

        //        ReteDumper.dumpRete(kieBase);
    }

    @Setup(Level.Iteration)
    @Override
    public void setup() {
        kieSession = RuntimeUtil.createKieSession(kieBase);
        StatefulKnowledgeSessionImpl session = (StatefulKnowledgeSessionImpl) kieSession;
        A a = new A(rulesNr + 1);

        session.insert(a);

        for (int i = 0; i < factsNr; i++) {

            session.insert(new B(rulesNr + i + 3));
            if (joinsNr > 1) {
                session.insert(new C(rulesNr + factsNr + i + 3));
            }
            if (joinsNr > 2) {
                session.insert(new D(rulesNr + factsNr * 2 + i + 3));
            }

        }
    }

    @Benchmark
    public int test() {
        return kieSession.fireAllRules();
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder().include(FireOnlyWithJoinsBenchmark.class.getSimpleName())
                                          .warmupIterations(0)
                                          .measurementIterations(1)
                                          .forks(1)
                                          .build();

        new Runner(opt).run();
    }
}
