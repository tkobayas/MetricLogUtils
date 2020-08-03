(cd dependency && sh install.sh)

mvn clean install -DskipTests -Denforcer.fail=false

java -jar target/drools-benchmarks.jar -jvmArgs "-Xms4g -Xmx4g" -foe true -f 5 -rf csv -rff results.csv org.drools.benchmarks.session.FireOnlyWithJoinsBenchmark

