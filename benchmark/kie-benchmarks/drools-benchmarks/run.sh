(cd dependency && sh install.sh)

# without perf
cp pom.xml.7.38.0-SNAPSHOT pom.xml
mvn clean install -DskipTests -Denforcer.fail=false

java -jar target/drools-benchmarks.jar -jvmArgs "-Xms4g -Xmx4g" -foe true -f 5 -rf csv -rff results-without-perf.csv org.drools.benchmarks.session.FireOnlyWithJoinsBenchmark

# with perf
cp pom.xml.7.38.0-SNAPSHOT-perf pom.xml
mvn clean install -DskipTests -Denforcer.fail=false

java -jar target/drools-benchmarks.jar -jvmArgs "-Xms4g -Xmx4g" -foe true -f 5 -rf csv -rff results-with-perf.csv org.drools.benchmarks.session.FireOnlyWithJoinsBenchmark
